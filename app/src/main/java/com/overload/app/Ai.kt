package com.overload.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

/*
 * 0.23-0.25 Overload AI, through the same server endpoints as the web app (the server talks to
 * Anthropic's Claude). Nothing AI happens until you allow it, it needs a signed-in account, and
 * everything AI hides while the server says AI is off.
 *   - Plan builder: a photo, a typed list or your history becomes a whole week (/vault/ai/plan)
 *   - Coach recap after each workout and a coach note in the Monday report (/ai/recap, /ai/weekly)
 *   - Coach chat that can change your plan, with Apply / No thanks (/ai/chat)
 *   - Form tips for any exercise (/ai/cue) and a weekly AI crew recap post (crew/aihype)
 * What the server says, cached replies and the chat stay on this phone (overload-ai.json), like the
 * web app's localStorage. The permission (aiOk) and the coach notes switch (aiCoach) sync.
 */

@Serializable data class AiTips(val cues: List<String> = emptyList(), val feel: String = "", val mistake: String = "")
@Serializable data class AiEx(val id: String = "", val name: String = "", val m: String = "Other", val lo: Int = 8, val hi: Int = 12, val sets: Int = 3, val cue: String = "",
                              val w: Double = 0.0, val r: Int = 0, val isNew: Boolean = false)
@Serializable data class AiDay(val day: Int = 0, val name: String = "", val type: String = "other", val focus: String = "", val rest: Int = 90, val groups: List<List<AiEx>> = emptyList())
@Serializable data class AiPlan(val days: List<AiDay> = emptyList(), val summary: String = "", val warnings: List<String> = emptyList())
@Serializable data class AiChange(val summary: String = "", val days: List<AiDay> = emptyList(), val diff: List<List<String>> = emptyList())
/** One chat line. r = "u" (you) or "a" (Coach). st = "applied" | "no" once you answer a change card. */
@Serializable data class AiTurn(val r: String, val t: String, val ch: AiChange? = null, val st: String? = null)
@Serializable data class AiStore(
    val on: Boolean = false, val rest: Boolean = false, val left: Int? = null, val chatLeft: Int? = null,
    val recaps: Map<String, String> = emptyMap(), val weekly: Map<String, String> = emptyMap(),
    val chat: List<AiTurn> = emptyList(), val cues: Map<String, AiTips> = emptyMap(), val hype: String = ""
)

class AiImg(val d: String, val bmp: ImageBitmap)
/** The plan builder sheet's state (form → busy → preview, or consent). */
data class AiBuild(val stage: String = "form", val src: String = "photo", val imgs: List<AiImg> = emptyList(), val text: String = "", val ask: String = "",
                   val days: Int = 4, val goal: String = "both", val mins: Int = 60, val err: String = "", val step: Int = 0, val res: AiPlan? = null, val after: String = "")
data class CoachUi(val draft: String = "", val busy: Boolean = false, val err: String = "")

private val lenientAi = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true; encodeDefaults = true }

class AiService(private val vm: OverloadViewModel) {
    private val file by lazy { File(vm.appContext.filesDir, "overload-ai.json") }
    private val scope get() = vm.viewModelScope
    val store = MutableStateFlow(load())
    val build = MutableStateFlow(AiBuild())
    val coach = MutableStateFlow(CoachUi())
    val cueErr = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Bumped when a recap or note fails, so the card can disappear. */
    val tick = MutableStateFlow(0)
    private val recapBusy = HashMap<String, Int>()   // 1 = loading, 2 = failed
    private val weekBusy = HashMap<String, Int>()
    private var loop: Job? = null

    private fun load(): AiStore = runCatching { lenientAi.decodeFromString<AiStore>(file.readText()) }.getOrDefault(AiStore())
    private fun save() { val s = store.value; scope.launch(Dispatchers.IO) { runCatching { file.writeText(lenientAi.encodeToString(s)) } } }
    private fun set(f: (AiStore) -> AiStore) { store.update(f); save() }

    val on get() = store.value.on
    private val st get() = vm.data.value.settings
    /** Allowed, signed in, AI on, and coach notes not switched off. */
    val ready get() = on && st.acct != null && st.aiOk > 0 && st.aiCoach != false

    fun start() {
        loop?.cancel()
        loop = scope.launch { var n = 0; while (isActive) { if (n % 30 == 0) status(); if (n > 0) hypeCheck(); n++; delay(60_000) } }
    }
    fun status() = scope.launch {
        val (code, j) = postJson("$SERVER/aistatus", JsonObject(emptyMap()))
        if (code in 200..299) set { it.copy(on = j["on"]?.jsonPrimitive?.booleanOrNull == true, rest = j["resting"]?.jsonPrimitive?.booleanOrNull == true) }
    }
    class AiErr(msg: String, val status: Int) : Exception(msg)
    private suspend fun call(p: String, body: JsonObjectBuilder.() -> Unit): JsonObject {
        val a = st.acct ?: throw AiErr("Sign in first", 401)
        val (code, j) = postJson("$SERVER/vault$p", buildJsonObject { put("acct", a.id); put("token", a.token); body() })
        if (code == 0) throw AiErr("You’re offline. Try again", 0)
        if (code !in 200..299) throw AiErr(j.err ?: "Something went wrong. Try again.", code)
        return j
    }
    fun allow() { vm.setSettings { it.copy(aiOk = 1, aiCoach = it.aiCoach ?: true) } }

    // ============================ PLAN BUILDER (0.23) ============================
    fun openBuilder() {
        if (!on) { vm.toast("Overload AI is off right now"); return }
        val n = vm.data.value.plan?.count { it.type != "off" } ?: 4
        build.value = AiBuild(days = maxOf(2, minOf(6, n)))
        if (st.aiSeen23 == 0) vm.setSettings { it.copy(aiSeen23 = 1) }
        vm.openSheet(Sheet.AiBuild)
    }
    fun setBuild(f: (AiBuild) -> AiBuild) = build.update(f)

    /** Lifts and best sets from the last 10 weeks, most done first. */
    fun history(): List<JsonObject> {
        val c = vm.ctx(); val cut = iso(today().minusDays(70)); val m = LinkedHashMap<String, Triple<Double, Int, Int>>()
        c.rs.filter { it.date >= cut }.forEach { s -> s.ex.forEach { (id, a) -> val v = a.filter(::full); if (v.isEmpty()) return@forEach
            val b = v.reduce { x, y -> if (y.w!! > x.w!! || (y.w == x.w && y.r!! > x.r!!)) y else x }; val o = m[id] ?: Triple(0.0, 0, 0)
            m[id] = if (b.w!! > o.first || (b.w == o.first && b.r!! > o.second)) Triple(b.w, b.r!!, o.third + 1) else o.copy(third = o.third + 1) } }
        return m.entries.sortedByDescending { it.value.third }.take(80).map { (id, v) -> buildJsonObject { put("n", c.exName(id)); put("id", id); put("m", c.exMuscle(id)); put("w", v.first); put("r", v.second); put("c", v.third) } }
    }
    /** Lifts you already know (plan + history), so the AI keeps their ids and your history stays linked. */
    private fun known(): JsonArray {
        val m = LinkedHashMap<String, JsonObject>()
        vm.data.value.plan?.forEach { d -> d.groups.flatten().forEach { e -> m[e.id] = buildJsonObject { put("id", e.id); put("name", e.name); put("m", e.m) } } }
        history().forEach { h -> val id = h["id"]!!.jsonPrimitive.content; if (m[id] == null) m[id] = buildJsonObject { put("id", id); put("name", h["n"]!!); put("m", h["m"]!!) } }
        return JsonArray(m.values.take(150).toList())
    }
    private fun hasHistory(id: String) = vm.ctx().rs.any { s -> s.ex[id]?.any(::full) == true }

    fun go() {
        val b = build.value
        if (st.aiOk == 0) { build.value = b.copy(stage = "consent", after = "go"); return }
        val err = when (b.src) { "photo" -> if (b.imgs.isEmpty()) "Add a photo of your workouts first." else ""; "text" -> if (b.text.isBlank()) "Type a few lifts first." else ""
            else -> if (history().isEmpty()) "No history yet. Try a photo or type your lifts." else "" }
        if (err.isNotEmpty()) { build.value = b.copy(err = err); return }
        build.value = b.copy(stage = "busy", step = 0, err = "")
        val tick = scope.launch { while (isActive) { delay(2600); build.update { if (it.stage == "busy" && it.step < 3) it.copy(step = it.step + 1) else it } } }
        scope.launch {
            try {
                val r = call("/ai/plan") { put("src", b.src); put("days", b.days); put("goal", b.goal); put("mins", b.mins); put("unit", vm.unit); put("ask", b.ask); put("known", known())
                    when (b.src) { "photo" -> putJsonArray("images") { b.imgs.forEach { addJsonObject { put("t", "image/jpeg"); put("d", it.d) } } }
                        "text" -> put("text", b.text); else -> put("history", JsonArray(history())) } }
                set { it.copy(left = r["left"]?.jsonPrimitive?.intOrNull ?: it.left) }
                val plan = lenientAi.decodeFromJsonElement<AiPlan>(r["plan"]!!)
                if (plan.days.isEmpty()) throw AiErr("The AI didn’t send a plan. Try again.", 500)
                if (vm.ui.value.sheet == Sheet.AiBuild) { build.update { it.copy(stage = "preview", res = plan) }; vm.haptic("success") }
            } catch (e: Exception) {
                val s = (e as? AiErr)?.status ?: 0
                if (s == 429) set { it.copy(left = 0) }; if (s == 503) set { it.copy(on = false) }
                build.update { it.copy(stage = "form", err = e.message ?: "Something went wrong. Try again.") }
            }
            tick.cancel()
        }
    }
    /** The AI's week as your plan: known lifts keep their ids, day plate colours stay, weights from notes become "Try" until there's history. */
    fun toPlan(p: AiPlan): List<DayPlan> {
        val old = vm.data.value.plan ?: emptyList()
        return (0 until 7).map { i -> val d = p.days.getOrNull(i) ?: return@map DayPlan("Rest", "off"); val o = old.getOrNull(i); val seen = HashSet<String>()
            val day = DayPlan(d.name.ifBlank { DN[i] }, if (TYPES[d.type] != null) d.type else if (d.groups.isEmpty()) "off" else "other", d.focus, d.groups.map { g -> g.map { e ->
                var id = e.id.ifBlank { slug(e.name) }; if (id in seen) { var n = 2; while ("$id-$n" in seen) n++; id = "$id-$n" }; seen += id
                Exercise(id, e.name, e.m, e.lo, e.hi, e.sets, e.cue, start = if (e.w > 0 && !hasHistory(id)) StartW(e.w, if (e.r > 0) e.r else e.lo) else null, ai = if (e.isNew) "new" else null) } }.filter { it.isNotEmpty() },
                if (d.rest > 0) d.rest else 90)
            if (o?.color != null && o.type != "off" && day.type != "off") day.copy(color = o.color) else day }
    }
    fun use() {
        val p = build.value.res ?: return; val days = toPlan(p)
        vm.data.value.plan?.let { vm.trash.put("plan", "Your plan before the AI build", vm.jsonCodec.encodeToJsonElement(it)) }
        vm.update { it.copy(plan = days) }; vm.closeSheet(); vm.setTab("plan"); vm.setPDay(dayIdx(today()))
        vm.toast("New plan saved. Your old one is in Trash."); vm.confetti()
    }
    /** Photos are shrunk on the phone (max 1400px JPEG, ~300 KB) to keep each build small. */
    fun addPhotos(uris: List<Uri>) = scope.launch {
        val room = 3 - build.value.imgs.size; var err = ""
        uris.take(room).forEach { u -> val img = withContext(Dispatchers.IO) { runCatching { shrink(u) }.getOrNull() }
            if (img != null) build.update { it.copy(imgs = it.imgs + img, err = "") } else err = "That photo could not be read" }
        if (err.isNotEmpty()) build.update { it.copy(err = err) }
    }
    private fun shrink(u: Uri): AiImg {
        val cr = vm.appContext.contentResolver
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }; cr.openInputStream(u)!!.use { BitmapFactory.decodeStream(it, null, o) }
        var sample = 1; while (maxOf(o.outWidth, o.outHeight) / (sample * 2) >= 1400) sample *= 2
        val raw = cr.openInputStream(u)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }!!
        val k = minOf(1f, 1400f / maxOf(raw.width, raw.height))
        val bmp = if (k < 1f) Bitmap.createScaledBitmap(raw, (raw.width * k).roundToInt(), (raw.height * k).roundToInt(), true) else raw
        var q = 75; var bytes: ByteArray
        while (true) { val out = ByteArrayOutputStream(); bmp.compress(Bitmap.CompressFormat.JPEG, q, out); bytes = out.toByteArray(); if (bytes.size * 4 / 3 <= 420_000 || q <= 35) break; q -= 10 }
        return AiImg(Base64.encodeToString(bytes, Base64.NO_WRAP), bmp.asImageBitmap())
    }

    // ============================ COACH NOTES (0.23) ============================
    /** The recap under a finished workout: written once per workout, never on demand. null = nothing to show, "" = writing. */
    fun recap(key: String, s: Session): String? {
        if (!ready || s.end == null) return null
        store.value.recaps[key]?.let { return it }
        if (System.currentTimeMillis() - s.end > 6 * 3_600_000L || recapBusy[key] == 2) return null
        if (recapBusy[key] == null) { recapBusy[key] = 1; scope.launch {
            try { val c = vm.ctx(); val ss = c.stats(s)
                val r = call("/ai/recap") { put("sid", key); put("name", c.plan.getOrNull(DAYS.indexOf(s.day))?.name ?: "Workout"); put("mins", (elapsed(s, 0) / 60000.0).roundToInt())
                    put("sets", ss.sets); put("vol", ss.vol.roundToInt()); put("unit", c.unit); putJsonArray("prs") { ss.prs.take(8).forEach { add(c.exName(it)) } }
                    putJsonArray("ex") { s.ex.forEach { (id, a) -> val v = a.filter(::full); if (v.isEmpty()) return@forEach
                        val l = lastSession(c.d.sessions, id, key, s.date); val p = l?.ex?.get(id)?.filter(::full) ?: emptyList()
                        addJsonObject { put("n", c.exName(id)); putJsonArray("s") { v.forEach { x -> addJsonArray { add(x.w!!); add(x.r!!) } } }; putJsonArray("p") { p.forEach { x -> addJsonArray { add(x.w!!); add(x.r!!) } } } } } } }
                val t = r["text"]?.jsonPrimitive?.contentOrNull ?: throw AiErr("empty", 500)
                set { st0 -> val m = LinkedHashMap(st0.recaps); m[key] = t; while (m.size > 40) m.remove(m.keys.first()); st0.copy(recaps = m) }
            } catch (e: Exception) { recapBusy[key] = 2; tick.update { it + 1 } }
        } }
        return ""
    }
    /** The coach note in the Monday report, once a week. */
    fun weekly(c: Ctx, r: Report): String? {
        if (!ready) return null; val key = iso(r.pw[0])
        store.value.weekly[key]?.let { return it }
        if (weekBusy[key] == 2 || r.a.done == 0) return null
        if (weekBusy[key] == null) { weekBusy[key] = 1; scope.launch {
            try {
                val wd = r.pw.map(::iso); val ses = c.rs.filter { it.date in wd }; val mus = HashMap<String, Int>(); val ids = HashMap<String, Int>()
                ses.forEach { s -> c.muscleSets(s).forEach { (k, v) -> mus[k] = (mus[k] ?: 0) + v }; s.ex.keys.forEach { ids[it] = (ids[it] ?: 0) + 1 } }
                val res = call("/ai/weekly") { put("wk", key); put("n", r.a.done); put("planned", r.a.planned); put("sets", r.a.sets); put("vol", r.a.vol.roundToInt()); put("unit", c.unit)
                    putJsonArray("prs") { if (r.a.prs > 0) add("${r.a.prs} PRs") }
                    putJsonArray("lifts") { ids.entries.sortedByDescending { it.value }.take(10).forEach { (id) ->
                        val wk = java.util.TreeMap<String, Double>(); c.rs.forEach { s -> val w = iso(weekStart(toD(s.date))); if (w <= key) s.ex[id]?.filter(::full)?.forEach { x -> wk[w] = maxOf(wk[w] ?: 0.0, e1(x)) } }
                        addJsonObject { put("n", c.exName(id)); put("e", (wk[key] ?: 0.0).roundToInt()); putJsonArray("h") { wk.values.toList().takeLast(6).forEach { add(it.roundToInt()) } } } } }
                    putJsonObject("mus") { mus.forEach { (k, v) -> put(k, v) } } }
                val t = res["text"]?.jsonPrimitive?.contentOrNull ?: throw AiErr("empty", 500)
                set { it.copy(weekly = it.weekly + (key to t)) }
            } catch (e: Exception) { weekBusy[key] = 2; tick.update { it + 1 } }
        } }
        return ""
    }

    // ============================ COACH CHAT (0.25) ============================
    fun openCoach() { if (!on) { vm.toast("Overload AI is off right now"); return }; coach.value = CoachUi(); vm.openSheet(Sheet.Coach) }
    private fun planForAI() = buildJsonArray { (vm.data.value.plan ?: emptyList()).forEach { d -> addJsonObject { put("name", d.name); put("type", d.type); put("focus", d.focus); put("rest", d.rest)
        putJsonArray("groups") { d.groups.forEach { g -> addJsonArray { g.forEach { e -> addJsonObject { put("id", e.id); put("name", e.name); put("m", e.m); put("sets", e.sets); put("lo", e.lo); put("hi", e.hi) } } } } } } } }
    private fun recentForAI() = buildJsonArray { val c = vm.ctx(); c.rs.takeLast(6).reversed().forEach { s -> addJsonObject { put("date", s.date); put("day", c.plan.getOrNull(DAYS.indexOf(s.day))?.name ?: "")
        putJsonArray("ex") { s.ex.entries.mapNotNull { (id, a) -> val v = a.filter(::full); if (v.isEmpty()) null else id to v }.take(12).forEach { (id, v) ->
            addJsonObject { put("n", c.exName(id)); putJsonArray("s") { v.forEach { x -> addJsonArray { add(x.w!!); add(x.r!!) } } } } } } } } }
    /** What a change does to one day: renamed, + added, − removed. */
    private fun diff(d: AiDay): List<String> {
        val old = vm.data.value.plan?.getOrNull(d.day); val a = old?.groups?.flatten()?.map { it.name } ?: emptyList(); val b = d.groups.flatten().map { it.name }
        val out = mutableListOf<String>(); if (old != null && old.name != d.name) out += "renamed to ${d.name}"
        b.filter { it !in a }.forEach { out += "+ $it" }; a.filter { it !in b }.forEach { out += "− $it" }
        return out.ifEmpty { listOf("sets, reps or order changed") }
    }
    fun send(text0: String) {
        val text = text0.trim().take(500); if (text.isEmpty() || coach.value.busy) return
        set { it.copy(chat = it.chat + AiTurn("u", text)) }; coach.value = CoachUi(busy = true)
        val hist = store.value.chat.takeLast(10).map { m -> buildJsonObject { put("r", m.r); put("t", m.t + if (m.ch != null && m.st != null) " [plan change ${if (m.st == "applied") "applied" else "not applied"}]" else "") } }
        scope.launch {
            try {
                val r = call("/ai/chat") { put("msgs", JsonArray(hist)); put("plan", planForAI()); put("recent", recentForAI()); put("today", dayIdx(today())); put("unit", vm.unit); put("known", known()) }
                val ch = (r["changes"] as? JsonObject)?.let { runCatching { lenientAi.decodeFromJsonElement<AiChange>(it) }.getOrNull() }?.let { c -> c.copy(diff = c.days.map(::diff)) }
                set { s -> s.copy(chatLeft = r["left"]?.jsonPrimitive?.intOrNull ?: s.chatLeft, chat = (s.chat + AiTurn("a", r["text"]?.jsonPrimitive?.contentOrNull ?: "", ch)).takeLast(60)) }
                vm.haptic("tap"); coach.value = CoachUi()
            } catch (e: Exception) {
                val s = (e as? AiErr)?.status ?: 0
                set { st0 -> st0.copy(chatLeft = if (s == 429) 0 else st0.chatLeft, rest = st0.rest || (s == 503 && (e.message ?: "").contains("budget", true)), chat = st0.chat.dropLast(1)) }
                coach.value = CoachUi(draft = text, err = e.message ?: "Coach could not answer. Try again.")
            }
        }
    }
    /** Applies a change card. The old plan goes to Trash first; ids, notes, starting weights and plate colours of lifts that stay are kept. */
    fun apply(i: Int) {
        val m = store.value.chat.getOrNull(i) ?: return; val ch = m.ch ?: return; if (m.st != null) return
        val plan = vm.data.value.plan ?: return
        vm.trash.put("plan", "Your plan before a Coach change", vm.jsonCodec.encodeToJsonElement(plan))
        val np = plan.toMutableList(); ch.days.forEach { d -> if (d.day in 0..6) np[d.day] = dayFromAI(d, plan[d.day]) }
        vm.update { it.copy(plan = np) }
        set { s -> s.copy(chat = s.chat.mapIndexed { j, x -> if (j == i) x.copy(st = "applied") else x }) }
        vm.toast("Plan updated. Old one is in Trash.")
    }
    fun decline(i: Int) = set { s -> s.copy(chat = s.chat.mapIndexed { j, x -> if (j == i && x.st == null) x.copy(st = "no") else x }) }
    fun clear() = set { it.copy(chat = emptyList()) }
    private fun dayFromAI(d: AiDay, old: DayPlan?): DayPlan {
        val seen = HashSet<String>(); val prev = old?.groups?.flatten() ?: emptyList()
        val day = DayPlan(d.name.ifBlank { old?.name ?: "Day" }, if (TYPES[d.type] != null) d.type else if (d.groups.isEmpty()) "off" else "other", d.focus, d.groups.map { g -> g.map { e ->
            var id = e.id.ifBlank { slug(e.name) }; if (id in seen) { var n = 2; while ("$id-$n" in seen) n++; id = "$id-$n" }; seen += id
            val p = prev.firstOrNull { it.id == id }
            Exercise(id, e.name, e.m, e.lo, e.hi, e.sets, e.cue.ifEmpty { p?.cue ?: "" }, start = p?.start ?: if (e.w > 0 && !hasHistory(id)) StartW(e.w, if (e.r > 0) e.r else e.lo) else null) } }.filter { it.isNotEmpty() },
            if (d.rest > 0) d.rest else 90)
        return if (old?.color != null && old.type != "off" && day.type != "off") day.copy(color = old.color) else day
    }

    // ============================ FORM TIPS (0.25) ============================
    fun cueKey(n: String) = n.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    fun openCue(name: String) { vm.openSheet(Sheet.Cue(name)); fetchCue(name) }
    fun fetchCue(name: String) {
        val k = cueKey(name); if (store.value.cues[k] != null || st.acct == null || st.aiOk == 0) return
        cueErr.update { it - k }
        scope.launch {
            try { val r = call("/ai/cue") { put("name", name) }; val t = lenientAi.decodeFromJsonElement<AiTips>(r["tips"]!!)
                set { s -> val m = LinkedHashMap(s.cues); m[k] = t; while (m.size > 150) m.remove(m.keys.first()); s.copy(cues = m) }
            } catch (e: Exception) { cueErr.update { it + (k to (e.message ?: "Could not load tips")) } }
        }
    }

    // ============================ WEEKLY AI CREW RECAP (0.25) ============================
    private fun hypeCheck() {
        val s = vm.crewState.value ?: return; if (!on || store.value.rest) return
        val key = s.crew.code + iso(weekStart(today())); if (store.value.hype == key) return
        scope.launch { val r = vm.crew.call("aihype", buildJsonObject { put("today", iso(today())) })
            if (r["ok"] != null || r["done"] != null) set { it.copy(hype = key) } }
    }
}
