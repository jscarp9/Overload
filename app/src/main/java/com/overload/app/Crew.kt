package com.overload.app

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** The Overload server (Cloudflare worker) shared with the web app. */
const val SERVER = "https://overload-push.jscarp961.workers.dev"

/** POSTs JSON to the server. Returns the JSON reply plus the HTTP status (0 = offline). */
suspend fun postJson(url: String, body: JsonObject): Pair<Int, JsonObject> = withContext(Dispatchers.IO) {
    try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 15000; c.readTimeout = 20000
        c.setRequestProperty("Content-Type", "application/json")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.use { it.readBytes().decodeToString() } ?: ""
        code to (runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: JsonObject(emptyMap()))
    } catch (e: Exception) { 0 to JsonObject(emptyMap()) }
}

val JsonObject.err: String? get() = (this["error"] as? JsonPrimitive)?.contentOrNull

/** One week's numbers, as published in the crew profile. */
private fun JsonObjectBuilder.weekStat(c: Ctx, wd: List<LocalDate>) {
    val a = iso(wd[0]); val b = iso(wd[6]); val ss = c.rs.filter { it.date in a..b }
    put("wk", a); put("days", ss.map { it.date }.toSet().size); put("sets", ss.sumOf(::sSets))
    put("vol", jsRound(ss.sumOf(::sVol))); put("prs", ss.sumOf { c.stats(it).prs.size })
    put("early", ss.count { s -> hourOf(s.start)?.let { it < 7 } == true })
}

/**
 * Crew: talks to the same server as the web app, so Android and web friends share one crew.
 * Identity is a random id + key kept in settings (crewMe), just like the web app.
 */
class CrewService(private val vm: OverloadViewModel) {
    val vmRef: OverloadViewModel get() = vm
    private val scope get() = vm.viewModelScope
    private val cacheFile by lazy { File(vm.appContext.filesDir, "overload-crew.json") }
    private val http by lazy { OkHttpClient.Builder().pingInterval(25, TimeUnit.SECONDS).build() }
    private var ws: WebSocket? = null
    private var wsTry = 0
    private var wsWanted = false
    private var busy = false
    private var last = 0L
    private var pubLast = 0L
    private var pubHash = ""
    private var loop: Job? = null
    private var beat: Job? = null
    private var foreground = true

    val on get() = vm.data.value.settings.crew != null
    private val st get() = vm.data.value.settings

    fun identity(): CrewMe {
        st.crewMe?.let { return it }
        fun r() = java.lang.Long.toString((Math.random() * Long.MAX_VALUE).toLong(), 36).take(11)
        val me = CrewMe("m" + r() + r(), r() + r() + r() + r())
        vm.setSettings { it.copy(crewMe = me) }
        return me
    }

    // ---------- lifecycle ----------
    fun start() {
        runCatching { if (cacheFile.exists()) parseCrewState(Json.parseToJsonElement(cacheFile.readText()).jsonObject)?.let { vm.crewState.value = it } }
        loop?.cancel()
        loop = scope.launch { while (isActive) { delay(30_000); if (foreground && vm.ui.value.tab == "crew") sync(false) } }
        if (on) scope.launch { delay(2500); sync(true); connect() }
    }
    fun stop() { closeWs(); wsWanted = false; vm.live.value = emptyMap(); runCatching { cacheFile.delete() } }
    fun onForeground() { foreground = true; if (on) { connect(); sync(false); scope.launch { delay(500); sendLive() } } }
    fun onBackground() { foreground = false; closeWs() }

    private fun saveCache(o: JsonObject?) = scope.launch(Dispatchers.IO) { runCatching { if (o == null) cacheFile.delete() else cacheFile.writeText(o.toString()) } }

    // ---------- server calls ----------
    suspend fun call(p: String, body: JsonObject = JsonObject(emptyMap())): JsonObject {
        val me = identity(); val code = st.crew?.code ?: ""
        val full = buildJsonObject {
            put("code", code); put("id", me.id); put("key", me.key); put("member", st.name.ifBlank { "Lifter" })
            body.forEach { (k, v) -> put(k, v) }
        }
        val (status, j) = postJson("$SERVER/crew/$p", full)
        if ((status == 403 || status == 404) && on && p != "join" && p != "create") {
            vm.setSettings { it.copy(crew = null) }; vm.crewState.value = null; saveCache(null); closeWs()
            vm.toast("You’re no longer in that crew"); return buildJsonObject { put("error", "gone") }
        }
        if (status == 0) return buildJsonObject { put("error", "offline") }
        if (status !in 200..299) return buildJsonObject { put("error", j.err ?: "HTTP $status") }
        if (j["crew"] is JsonObject) parseCrewState(j)?.let { vm.crewState.value = it; saveCache(j); check() }
        return j
    }
    private fun fire(p: String, body: JsonObject = JsonObject(emptyMap())) = scope.launch { call(p, body) }

    fun sync(force: Boolean) {
        if (!on || busy || vm.data.value.plan == null) return
        if (!force && System.currentTimeMillis() - last < 15_000) return
        busy = true; last = System.currentTimeMillis()
        scope.launch {
            val prof = withContext(vm.engDispatcher) { myProfile() }; val h = prof.toString()
            val due = h != pubHash || System.currentTimeMillis() - pubLast > 600_000
            val r = if (due) call("profile", buildJsonObject {
                put("profile", prof); st.acct?.let { put("acct", it.id); put("atoken", it.token) }; put("today", iso(today()))
            }) else call("state")
            if (due && r.err == null) { pubHash = h; pubLast = System.currentTimeMillis() }
            busy = false
        }
    }

    fun post(post: JsonObject) = fire("post", buildJsonObject { put("post", post) })

    // ---------- join / create / leave ----------
    fun joinOrCreate(join: Boolean, codeOrName: String, name: String?, done: () -> Unit) {
        if (name != null) { if (name.isBlank()) { vm.toast("Add your name first"); done(); return }; vm.setSettings { it.copy(name = name.trim().take(24)) } }
        scope.launch {
            val r: JsonObject
            if (join) {
                val code = codeOrName.trim().uppercase()
                if (!Regex("^[A-Z]{3,6}-\\d{4}$").matches(code)) { vm.toast("Codes look like IRON-4821"); done(); return@launch }
                vm.setSettings { it.copy(crew = CrewRef(code)) }; r = call("join")
            } else {
                if (codeOrName.isBlank()) { vm.toast("Give your crew a name"); done(); return@launch }
                r = call("create", buildJsonObject { put("name", codeOrName.trim()) })
            }
            done()
            val e = r.err
            if (e != null) {
                vm.setSettings { it.copy(crew = null) }
                vm.toast(when (e) { "no crew", "bad code" -> "No crew with that code"; "full" -> "That crew is full"; "offline" -> "You’re offline. Try again"; else -> "Could not do that. Try again" })
                return@launch
            }
            val c = r["crew"]?.jsonObject; val code = c?.get("code")?.jsonPrimitive?.content ?: return@launch
            vm.setSettings { it.copy(crew = CrewRef(code)) }
            vm.toast(if (join) "Welcome to ${c["name"]?.jsonPrimitive?.content ?: "the crew"} 🎉" else "Crew created. Code: $code")
            sync(true); connect()
        }
    }
    fun leave() = scope.launch {
        call("leave"); vm.setSettings { it.copy(crew = null) }; vm.crewState.value = null; saveCache(null); closeWs(); vm.live.value = emptyMap(); vm.chat.reset(); setGrp(null)
        vm.closeSheet(); vm.toast("You left the crew")
    }
    fun kick(id: String) = fire("kick", buildJsonObject { put("target", id) })

    // ---------- feed ----------
    fun react(postId: String, emoji: String) {
        val me = vm.myId() ?: return
        vm.crewState.update { cs -> cs?.copy(posts = cs.posts.map { p ->
            if (p.id != postId) p else { val a = p.reacts[emoji] ?: emptyList(); p.copy(reacts = p.reacts + (emoji to if (me in a) a - me else a + me)) } }) }
        val added = vm.crewState.value?.posts?.firstOrNull { it.id == postId }?.reacts?.get(emoji)?.contains(me) == true
        if (added) vm.setSettings { it.copy(reactN = it.reactN + 1) }
        fire("react", buildJsonObject { put("post", postId); put("emoji", emoji) })
    }
    fun comment(postId: String, text: String, done: (Boolean) -> Unit) = scope.launch {
        val r = call("comment", buildJsonObject { put("post", postId); put("text", text) }); if (r.err != null) vm.toast("Could not send"); done(r.err == null)
    }
    fun deletePost(id: String) {
        vm.crewState.update { cs -> cs?.copy(posts = cs.posts.filter { it.id != id }) }
        fire("delete", buildJsonObject { put("post", id) })
    }
    fun postWorkout(key: String, cap: String, done: () -> Unit) {
        val s = vm.data.value.sessions[key] ?: return
        scope.launch {
            val r = call("post", buildJsonObject { put("post", buildJsonObject { put("type", "workout"); put("cap", cap.trim()); put("data", postData(s)) }) })
            done()
            if (r.err != null) { vm.toast("Could not post. Check your connection"); return@launch }
            vm.update { d -> d.copy(sessions = d.sessions + (key to (d.sessions[key] ?: s).copy(posted = true))) }
            vm.closeSheet(); vm.ui.update { it.copy(tab = "crew", sub = it.sub + ("crew" to "feed")) }; vm.toast("Posted to your crew 📣")
        }
    }
    fun setChallenge(type: String, target: Int, title: String) = scope.launch {
        call("challenge", buildJsonObject { put("type", type); put("target", target); put("wk", curWk()); put("title", title) })
        vm.closeSheet(); vm.toast("Challenge set 🏁")
    }
    fun vote(cat: String, target: String) = scope.launch {
        call("vote", buildJsonObject { put("month", monthKey()); put("cat", cat); put("target", target) }); vm.haptic("tap")
    }
    fun banner(k: String) = fire("banner", buildJsonObject { put("banner", k) })
    fun hype(id: String) {
        vm.haptic("success")
        val ok = ws?.send(buildJsonObject { put("t", "hype"); put("to", id) }.toString()) == true
        vm.toast(if (ok) "Hype sent to ${member(id)?.name ?: "them"} 🔥" else "Hype will send when you’re back online")
    }

    // ---------- live (who's lifting right now) ----------
    private fun liveSession(): Session? {
        val t = today(); val s = vm.data.value.sessions[sid(iso(t), DAYS[dayIdx(t)])]
        return if (s?.start != null && s.end == null) s else null
    }
    fun sendLive() {
        if (!on) return
        val s = liveSession(); val t = today()
        val msg = if (s != null) buildJsonObject { put("t", "live"); put("on", true); put("day", vm.data.value.plan?.getOrNull(dayIdx(t))?.name ?: "Workout"); put("start", s.start!!); grpLive().forEach { (k, v) -> put(k, v) } }
        else buildJsonObject { put("t", "live"); put("on", false) }
        if (ws?.send(msg.toString()) != true && s != null) scope.launch { val r = call("live", msg); (r["live"] as? JsonObject)?.let { vm.live.value = parseLive(it) } }
        beat?.cancel()
        if (s != null) beat = scope.launch { while (isActive) { delay(240_000); if (liveSession() != null) sendLive() else break } }
    }
    fun liveEnd() {
        if (!on) return; beat?.cancel()
        val msg = buildJsonObject { put("t", "live"); put("on", false) }
        if (ws?.send(msg.toString()) != true) fire("live", buildJsonObject { put("on", false) })
    }
    private fun parseLive(o: JsonObject): Map<String, LiveInfo> = o.mapNotNull { (id, v) ->
        val j = v as? JsonObject ?: return@mapNotNull null
        id to LiveInfo(j["day"]?.jsonPrimitive?.contentOrNull ?: "Lifting", j["start"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            j["g"]?.jsonPrimitive?.contentOrNull ?: "", j["sets"]?.jsonPrimitive?.intOrNull ?: 0, j["vol"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
            j["ex"]?.jsonPrimitive?.contentOrNull ?: "", j["rest"]?.jsonPrimitive?.longOrNull ?: 0L, j["prs"]?.jsonPrimitive?.intOrNull ?: 0)
    }.toMap()

    fun connect() {
        if (!on || !foreground || ws != null) return
        wsWanted = true
        val me = identity(); val code = st.crew?.code ?: return
        fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
        val url = SERVER.replaceFirst("http", "ws") + "/crew/ws?code=${enc(code)}&id=${enc(me.id)}&key=${enc(me.key)}"
        ws = http.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { wsTry = 0; scope.launch { sendLive() } }
            override fun onMessage(webSocket: WebSocket, text: String) {
                val m = runCatching { Json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
                scope.launch {
                    when (m["t"]?.jsonPrimitive?.contentOrNull) {
                        "refresh", "kicked" -> sync(true)
                        "live" -> vm.live.value = (m["live"] as? JsonObject)?.let(::parseLive) ?: emptyMap()
                        "chat" -> parseChatMsg(m["msg"])?.let { vm.chat.onMsg(it) }
                        "chatupd" -> parseChatMsg(m["msg"])?.let { vm.chat.onUpd(it) }
                        "hype" -> { val from = m["from"]?.jsonPrimitive?.contentOrNull ?: "Your crew"; vm.haptic("heavy"); vm.ui.update { it.copy(hype = (it.hype.first + 1) to from) }; vm.addBell("🔥", "$from hyped you up") }
                    }
                }
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) = retry(webSocket)
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = retry(webSocket)
        })
    }
    private fun retry(w: WebSocket) {
        if (ws === w) ws = null
        if (wsWanted && foreground && on) scope.launch { delay(minOf(30_000L, 1000L * (1L shl minOf(wsTry++, 5)))); connect() }
    }
    private fun closeWs() { val w = ws; ws = null; runCatching { w?.close(1000, null) } }

    // ============================ PROFILE + POST DATA ============================
    fun myProfile(): JsonObject {
        val c = vm.ctx(); val e = c.eng; val wd = weekDates(today()); val st = c.st
        val lifts = HashMap<String, Triple<Double, Double, Int>>()
        c.rs.forEach { s -> s.ex.forEach { (id, a) -> a.forEach { x -> if (full(x)) { val n = c.exName(id); val v = e1(x); if (lifts[n] == null || v > lifts[n]!!.first) lifts[n] = Triple(v, x.w!!, x.r!!) } } } }
        val top = lifts.entries.sortedByDescending { it.value.first }.take(30)
        return buildJsonObject {
            put("rank", e.rank); put("div", e.div); put("pres", e.prestige); put("rname", e.rname); put("power", e.power); put("streak", e.streak)
            put("bcount", e.bcount); put("active", e.active); put("workouts", e.workouts); put("lifeVol", jsRound(e.lifeVol)); put("unit", c.unit)
            e.since?.let { put("since", it) } ?: put("since", JsonNull); put("frac", (e.frac * 100).roundToInt() / 100.0)
            putJsonArray("keys") { e.B.filter { it.earned }.forEach { add(it.k) } }
            // 0.21: the badge key and count go with each pin so others see the right medal
            putJsonArray("show") { st.showcase.mapNotNull { e.badge(it) }.forEach { b -> addJsonArray { add(b.icon); add(b.name); add(b.k); add(b.count ?: 0) } } }
            putJsonObject("week") { weekStat(c, wd) }
            putJsonObject("last") { weekStat(c, weekDates(wd[0].minusDays(7))) }
            putJsonObject("prev") { weekStat(c, weekDates(wd[0].minusDays(14))) }
            vm.data.value.plan?.let { p -> put("plan", buildJsonObject { put("days", vm.jsonCodec.encodeToJsonElement(p)) }) }
            putJsonArray("days") { c.rs.map { it.date }.distinct().takeLast(21).forEach { add(it) } }
            putJsonObject("lifts") { top.forEach { (n, v) -> putJsonObject(n) { put("e", jsRound(v.first)); put("w", v.second); put("r", v.third) } } }
            put("bw", st.bw); put("lvl", e.lvl); put("frame", st.frame); put("aura", st.aura); put("tag", st.tag); put("trial", if (e.trial != null) 1 else 0)
            // 0.14: looks and trophies
            put("nameFx", c.look("nameFx")); put("card", c.look("card")); put("pbanner", c.look("pbanner"))
            putJsonArray("troph") { c.trophies().filter { it.earned }.forEach { add(it.k) } }
            // 0.15: crew profile
            val P = st.prof
            put("motto", P.motto); put("gym", P.gym); put("phase", P.phase); put("ltype", P.ltype); put("song", P.song)
            st.av?.let { put("av", vm.jsonCodec.encodeToJsonElement(it)) } ?: put("av", JsonNull)
            put("pacc", P.acc); put("rim", st.rim); putJsonArray("tpins") { P.tpins.take(3).forEach { add(it) } }
            put("flex", vm.jsonCodec.encodeToJsonElement(c.myFlex())); put("prtl", vm.jsonCodec.encodeToJsonElement(c.prTimeline().takeLast(20)))
            c.bestLift()?.let { put("best", vm.jsonCodec.encodeToJsonElement(it)) } ?: put("best", JsonNull)
            putJsonArray("plates") { FIN.keys.filter { FIN[it]!!.cat != "finish" && c.finOwned(it) }.take(40).forEach { add(it) } }
            put("fav", P.fav); if (st.founder) put("founder", true); if (st.cofounder) put("cofounder", true)
        }
    }

    fun postData(s: Session): JsonObject {
        val c = vm.ctx(); val ss = c.stats(s); val p = c.plan.getOrNull(DAYS.indexOf(s.day))
        val top = s.ex.mapNotNull { (id, a) -> val v = a.filter(::full); if (v.isEmpty()) null else { val b = v.maxByOrNull(::e1)!!; Triple(TopLift(c.exName(id), b.w!!, b.r!!, if (id in ss.prs) 1 else 0), e1(b), id) } }
            .sortedWith(compareByDescending<Triple<TopLift, Double, String>> { it.first.pr }.thenByDescending { it.second }).take(3).map { it.first }
        return buildJsonObject {
            put("day", p?.name ?: DN[DAYS.indexOf(s.day).coerceAtLeast(0)]); put("color", p?.let(::colorOf) ?: "red"); put("date", s.date)
            put("min", if (s.start != null && s.end != null) jsRound(elapsed(s, 0) / 6e4) else 0); put("sets", sSets(s)); put("vol", jsRound(sVol(s)))
            put("prs", ss.prs.size); put("unit", c.unit)
            putJsonArray("top") { top.forEach { t -> addJsonObject { put("n", t.n); put("w", t.w); put("r", t.r); put("pr", t.pr) } } }
            putJsonObject("L") { c.mLevels(c.muscleSets(s)).forEach { (k, v) -> put(k, v) } }
            put("cmp", if (ss.vol > 0) c.volCompare(ss.vol).second else "")
            putJsonArray("badges") { c.eng.earnedOn(s.date).take(4).forEach { b -> addJsonArray { add(b.icon); add(b.name) } } }
        }
    }

    // ============================ ARENA HELPERS ============================
    val cs get() = vm.crewState.value
    fun member(id: String?) = cs?.members?.firstOrNull { it.id == id }
    fun curWk() = iso(weekStart(today()))
    private fun lastWk() = iso(weekStart(today()).minusDays(7))
    fun wkOf(p: MemberProfile?): WeekStat = p?.week?.takeIf { it.wk == curWk() } ?: WeekStat()
    fun lastOf(p: MemberProfile?): WeekStat? = p?.week?.takeIf { it.wk == lastWk() } ?: p?.last?.takeIf { it.wk == lastWk() }
    fun weekPts(p: MemberProfile?): Int { val w = wkOf(p); return w.days * 40 + w.sets * 3 + w.prs * 20 }
    private val unit get() = st.unit
    fun mine(v: Double, u: String?) = toMine(v, u, unit)

    class Title(val k: String, val name: String, val icon: String, val desc: String, val f: (MemberProfile) -> Double)
    val titles = listOf(
        Title("machine", "The Machine", "⚙️", "Most sets this week") { wkOf(it).sets.toDouble() },
        Title("hunter", "PR Hunter", "🏹", "Most PRs this week") { wkOf(it).prs.toDouble() },
        Title("king", "Volume King", "👑", "Most volume this week") { mine(wkOf(it).vol, it.unit) },
        Title("will", "Iron Will", "🛡️", "Longest week streak") { it.streak.toDouble() },
        Title("bird", "Early Bird", "🐓", "Most workouts before 7 AM this week") { wkOf(it).early.toDouble() },
        Title("steady", "Mr. Consistent", "📅", "Most training days this week") { wkOf(it).days.toDouble() }
    )
    fun titleHolders(): Map<String, String> {
        val mem = cs?.members?.filter { it.profile != null } ?: return emptyMap()
        if (mem.size < 2) return emptyMap()
        val out = HashMap<String, String>()
        titles.forEach { t -> var best: Member? = null; var bv = 0.0
            mem.forEach { m -> val v = t.f(m.profile!!); if (v > bv || (v == bv && v > 0 && best != null && m.profile.power > best!!.profile!!.power)) { bv = v; best = m } }
            if (best != null && bv > 0) out[t.k] = best!!.id }
        return out
    }
    fun h2h(a: WeekStat, au: String, b: WeekStat, bu: String): Pair<Int, Int> {
        var w = 0; var l = 0
        listOf(a.days.toDouble() to b.days.toDouble(), a.sets.toDouble() to b.sets.toDouble(), mine(a.vol, au) to mine(b.vol, bu), a.prs.toDouble() to b.prs.toDouble())
            .forEach { (x, y) -> if (x > y) w++ else if (y > x) l++ }
        return w to l
    }
    data class ChalProg(val type: String, val target: Double, val title: String, val cur: Long)
    fun chalProgress(): ChalProg? {
        val c = cs?.crew?.challenge ?: return null; if (c.wk != curWk()) return null
        val sum = cs!!.members.sumOf { m -> val p = m.profile ?: return@sumOf 0.0; val w = wkOf(p)
            when (c.type) { "vol" -> mine(w.vol, p.unit); "days" -> w.days.toDouble(); "prs" -> w.prs.toDouble(); else -> w.sets.toDouble() } }
        return ChalProg(c.type, c.target, c.title, jsRound(sum))
    }
    fun arenaOrder(mode: String): List<Member> {
        val mem = cs?.members ?: return emptyList()
        return mem.sortedByDescending { m -> m.profile?.let { if (mode == "week") weekPts(it).toDouble() else it.power } ?: -1.0 }
    }
    fun lastFinished(): Session? {
        val since = iso(today().minusDays(2))
        return vm.ctx().rs.lastOrNull { it.end != null && it.date >= since }
    }

    // ---------- monthly awards ----------
    val awards = listOf(Triple("grinder", "Biggest Grinder", "⚙️"), Triple("hype", "Best Hype Man", "📣"), Triple("comeback", "Comeback of the Month", "🔁"),
        Triple("improved", "Most Improved", "📈"), Triple("post", "Post of the Month", "⭐"))
    fun monthKey(d: LocalDate = today()) = "${d.year}-${d.monthValue.toString().padStart(2, '0')}"
    fun prevMonthKey() = monthKey(today().withDayOfMonth(1).minusMonths(1))
    fun monthName(k: String) = MON[k.substring(5).toInt() - 1]
    fun awardWinners(mo: String): Map<String, String> {
        val v = cs?.votes?.get(mo) ?: return emptyMap(); val out = HashMap<String, String>()
        awards.forEach { (k) -> val top = (v[k]?.tally ?: emptyMap()).entries.sortedByDescending { it.value }
            if (top.isNotEmpty() && (top.size < 2 || top[0].value > top[1].value)) out[k] = top[0].key }
        return out
    }

    // ============================ CREW CHECK (badges, titles, arena) ============================
    fun check() {
        val cs = cs ?: return; val me = vm.myId(); val d = iso(today()); val mem = cs.members
        val cb = st.crewB.toMutableMap(); var ch = false
        fun give(k: String) { if (cb[k] == null) { cb[k] = d; ch = true } }
        if (mem.size >= 2) { val sets = mem.map { (it.profile?.days ?: emptyList()).toSet() }
            if (sets.all { it.isNotEmpty() } && sets[0].any { x -> sets.all { x in it } }) give("squad") }
        val lifeV = mem.sumOf { m -> m.profile?.let { mine(it.lifeVol, it.unit) } ?: 0.0 }
        if (lifeV >= (if (unit == "kg") 450000.0 else 1e6)) give("cmil")
        if (st.reactN >= 50) give("hype")
        if (cs.crew.owner == me && mem.size >= 2) give("recruit")
        chalProgress()?.let { if (it.cur >= it.target) give("team") }
        val riv = member(st.rival); val my = member(me)
        if (riv?.profile != null && my?.profile != null) { val a = lastOf(my.profile); val b = lastOf(riv.profile)
            if (a != null && b != null) { val (w, l) = h2h(a, my.profile.unit, b, riv.profile.unit); if (w > l) give("rival") } }
        val t = titleHolders(); val was = st.titlesMine; val now = t.keys.filter { t[it] == me }
        was.filter { it !in now }.forEach { k -> val tt = titles.firstOrNull { it.k == k }; val thief = member(t[k])
            if (tt != null && thief != null) { vm.sayLater(600, "${thief.name} stole ${tt.name} from you 👀"); vm.addBell("👀", "${thief.name} stole ${tt.name} from you") } }
        now.filter { it !in was }.forEach { k -> titles.firstOrNull { it.k == k }?.let { tt -> vm.sayLater(900, "You took the title ${tt.icon} ${tt.name}") } }
        var titlesMine = st.titlesMine; if (was != now) { titlesMine = now; ch = true }
        val pos = arenaOrder("all").map { it.id }; var snap = st.arenaSnap; var arenaLast = st.arenaLast
        if (snap?.wk != curWk()) { snap = ArenaSnap(curWk(), st.arenaLast); ch = true }
        val lastMap = pos.withIndex().associate { (i, id) -> id to i }
        if (lastMap != arenaLast) { arenaLast = lastMap; ch = true }
        // monthly awards and last week's rival result
        val dom = today().dayOfMonth; var rivalSeen = st.rivalSeen
        if (dom <= 7) { val pm = prevMonthKey(); awardWinners(pm).forEach { (cat, id) -> val k = "award_${pm}_$cat"
            if (id == me && cb[k] == null) { cb[k] = d; ch = true; val a = awards.first { it.first == cat }
                vm.later(800) { vm.toast("🎖️ You won ${a.second}!"); vm.confetti(120); vm.addBell("🎖️", "You won ${a.second} for ${monthName(pm)}") } } } }
        if (riv?.profile != null && my?.profile != null) { val a = lastOf(my.profile); val b = lastOf(riv.profile); val lw = lastWk()
            if (a != null && b != null && rivalSeen != lw) { val (w, l) = h2h(a, my.profile.unit, b, riv.profile.unit); rivalSeen = lw; ch = true
                vm.addBell("⚔️", if (w > l) "You beat ${riv.name} last week, $w–$l" else if (w < l) "${riv.name} beat you last week, $l–$w. Get it back" else "You and ${riv.name} tied last week") } }
        if (ch) {
            vm.setSettings { it.copy(crewB = cb, titlesMine = titlesMine, arenaSnap = snap, arenaLast = arenaLast, rivalSeen = rivalSeen) }
            vm.later(50) { vm.checkUnlocks() }
        }
    }

    // ---------- activity bell ----------
    data class BellRow(val ts: Long, val icon: String, val text: String)
    fun bellItems(): List<BellRow> {
        val me = vm.myId(); val out = st.bellLocal.map { BellRow(it.ts, it.icon, it.text) }.toMutableList()
        cs?.events?.forEach { e -> if (e.from == me) return@forEach
            val who = member(e.from)?.name ?: "Someone"
            val row = when (e.type) {
                "react" -> "🔥" to "$who reacted ${REACTS[e.emoji] ?: "🔥"} to your post"; "comment" -> "💬" to "$who commented: \"${e.text}\""
                "rank" -> "🏅" to "$who ranked up to ${e.text}"; "join" -> "👋" to "$who joined the crew"
                "challenge" -> "🏁" to "$who set a challenge: ${e.text}"; "hype" -> "🔥" to "$who hyped you up"
                "mention" -> "💬" to "$who mentioned you: \"${e.text}\""; "reply" -> "↪" to "$who replied: \"${e.text}\""
                "trophy" -> "🏆" to "Crew trophy unlocked: ${e.text}"; "crewrank" -> "🛡" to "Your crew ranked up to ${e.text}"
                "rename" -> "✏️" to "$who renamed the crew to ${e.text}"; "shame" -> "🩷" to "${e.text} got the shame plate this week"
                "restore" -> "↩" to "$who restored crew posts from a backup"; else -> null
            }
            if (row != null) out += BellRow(e.ts, row.first, row.second) }
        return out.sortedByDescending { it.ts }.take(60)
    }
    fun bellUnread() = bellItems().count { it.ts > st.bellSeen }

    // ---------- copying a friend's plan ----------
    fun copyDay(memberId: String, day: Int, to: Int) {
        val src = member(memberId)?.profile?.plan?.days?.getOrNull(day) ?: return
        vm.editDay(to) { src }; vm.closeSheet(); vm.toast("Copied ${src.name} to ${DN[to]} 📋"); vm.haptic("success")
    }
    fun copySplit(memberId: String) {
        val m = member(memberId) ?: return; val days = m.profile?.plan?.days ?: return
        if (days.size != 7) return
        vm.update { it.copy(plan = days) }; vm.closeSheet()
        vm.ui.update { it.copy(tab = "plan", sub = it.sub + ("plan" to "plan")) }
        vm.toast("You’re running ${m.name}’s split now 📋"); vm.haptic("success")
    }
}

val REACTS: Map<String, String> get() = RE14
val BANNERS = linkedMapOf("steel" to Triple(0xFF3A3D46, 0xFF8C939E, 1), "ember" to Triple(0xFFFF2D2D, 0xFFFFC14D, 2), "ocean" to Triple(0xFF0B4BD6, 0xFF00C2FF, 3),
    "toxic" to Triple(0xFF16A34A, 0xFFA8FF1A, 5), "royal" to Triple(0xFF5B2BD6, 0xFFFF4FD8, 7), "gold" to Triple(0xFFB8860B, 0xFFFFE07A, 10))
fun crewXpFor(l: Int) = 60 * (l - 1) * (l - 1)
fun ago(ts: Long): String {
    val s = (System.currentTimeMillis() - ts) / 1000
    return when { s < 60 -> "now"; s < 3600 -> "${s / 60}m"; s < 86400 -> "${s / 3600}h"; s < 604800 -> "${s / 86400}d"
        else -> { val d = java.time.Instant.ofEpochMilli(ts).atZone(java.time.ZoneId.systemDefault()); "${MON[d.monthValue - 1]} ${d.dayOfMonth}" } }
}
fun initials(n: String) = n.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.toString() }.joinToString("").take(2).uppercase().ifEmpty { "?" }
fun hue(s: String): Int { var h = 0; s.forEach { c -> h = (h * 31 + c.code) % 360 }; return h }
