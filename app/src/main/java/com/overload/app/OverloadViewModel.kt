package com.overload.app

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDate
import kotlin.math.max

data class RestState(val endAt: Long, val dur: Int, val base: Int, val next: String, val buzzed: Boolean = false)
data class Picker(val ctx: String, val gi: Int = -1, val key: String = "") // ctx: today | plan | swap

data class UiState(
    val date: LocalDate = LocalDate.now(),
    val auto: Boolean = true,
    val tab: String = "log",           // log | crew | stats | plan
    val prog: String? = null,
    val pfilter: String = "All",
    val pday: Int = dayIdx(LocalDate.now()),
    val settings: Boolean = false,
    val rest: RestState? = null,
    val picker: Picker? = null,
    val warm: Set<String> = emptySet(),
    val editNote: Note? = null,
    val news: Boolean = false,         // What's new sheet open
    val profile: Boolean = false,      // rank profile screen
    val sheet: Sheet? = null,
    /** Sub-tabs: stats = ov|lifts, plan = plan|notes, crew = feed|arena, arena = all|week|lifts. */
    val sub: Map<String, String> = mapOf("stats" to "ov", "plan" to "plan", "crew" to "feed", "arena" to "all"),
    val confetti: Int = 0,             // bump to fire confetti
    val confettiN: Int = 70,
    val cine: Cine? = null,
    val yir: Boolean = false,          // year in review open
    val hype: Pair<Int, String> = 0 to "",
    val liftSel: String? = null, val liftRel: Boolean = false,
    val confirm: String? = null,       // "tap again" confirmations
    // 0.14-0.17
    val confettiKind: String = "confetti",
    val setFx: Int = 0, val setFxKind: String = "pulse",
    val plateTalk: Pair<Int, String> = 0 to "",
    val restLine: String = "",
    val chal17: Boolean = false,       // Today: challenges row opened
    val set17: String? = null,         // Settings page open (you, workout, sync, data, founder, help, danger)
    val jumpNotes: Int = 0             // Today ⋯ → Session notes
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class OverloadViewModel(app: Application) : AndroidViewModel(app) {
    private val file = File(app.filesDir, "overload.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    val data = MutableStateFlow(load())
    val ui = MutableStateFlow(UiState(news = data.value.plan != null && data.value.settings.seenNews != NEWS_ID))
    val now = MutableStateFlow(System.currentTimeMillis())
    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val crewState = MutableStateFlow<CrewState?>(null)
    val live = MutableStateFlow<Map<String, LiveInfo>>(emptyMap())
    val crew by lazy { CrewService(this) }
    val sync by lazy { SyncService(this) }
    val trash by lazy { TrashStore(this) }
    val chat by lazy { ChatService(this) }
    val dash = MutableStateFlow<JsonObject?>(null)

    init {
        // 0.14 update safety net: copy before this version touches anything, put back anything missing
        val back = safetyNet(app.filesDir, data.value, json) { d -> data.value = d; saveNow() }
        if (data.value.settings.seenVer != APP_VER) setSettings { it.copy(seenVer = APP_VER) }
        if (back > 0) later(1500) { say("Update safety net put back $back item${if (back > 1) "s" else ""}") }
        ghostCount()
        viewModelScope.launch {
            delay(1400); tourCheck()
        }
        viewModelScope.launch {
            delay(600)
            val d = data.value
            if (d.plan != null && d.settings.seen == null) checkUnlocks()
            if (d.plan != null && !ui.value.news && !weekReport()) checkUnlocks()
            crew.start(); sync.start()
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                now.value = System.currentTimeMillis()
                tickRest()
                val u = ui.value
                if (u.auto && u.date != LocalDate.now()) setDate(LocalDate.now(), false)
            }
        }
    }

    // ---------- persistence ----------
    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString<AppData>(file.readText()) else AppData()
    } catch (e: Exception) { AppData() }

    private var saveJob: Job? = null
    internal fun update(f: (AppData) -> AppData) {
        data.update(f)
        sync.queue()
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250)
            try { file.writeText(json.encodeToString(data.value)) } catch (_: Exception) {}
        }
    }
    private fun say(msg: String) { toasts.tryEmit(msg) }
    fun toast(msg: String) = say(msg)
    internal val jsonCodec get() = json
    internal val appContext: Application get() = getApplication()
    /** Saves straight away (used before the app is closed and by sync). */
    internal fun saveNow() { saveJob?.cancel(); try { file.writeText(json.encodeToString(data.value)) } catch (_: Exception) {} }

    // ============================ RANKS, BADGES, UNLOCKS ============================
    /** All engine work runs here, one job at a time, off the main thread. */
    internal val engDispatcher = Dispatchers.Default.limitedParallelism(1)
    /** Engine snapshot for the screens, rebuilt in the background shortly after data changes. */
    val engCtx: StateFlow<Ctx> by lazy {
        combine(data, crewState) { d, cs -> d to cs }.debounce(150)
            .map { (d, cs) -> ctx(d, cs).also { it.eng } }.flowOn(engDispatcher)
            .stateIn(viewModelScope, SharingStarted.Eagerly, ctx())
    }
    private var ctxMemo: Ctx? = null
    /** Engine snapshot for the current data (cached until data or crew changes). */
    @Synchronized
    fun ctx(d: AppData = data.value, cs: CrewState? = crewState.value): Ctx {
        val c = ctxMemo
        if (c != null && c.d === d && c.cs === cs && c.now == LocalDate.now()) return c
        return Ctx(d, cs, myId(d)).also { ctxMemo = it }
    }
    fun myId(d: AppData = data.value): String? = if (d.settings.crew != null) d.settings.crewMe?.id else null
    fun later(ms: Long, f: () -> Unit) { viewModelScope.launch { delay(ms); f() } }
    fun sayLater(ms: Long, msg: String) = later(ms) { say(msg) }
    fun confetti(n: Int = 70) = ui.update { it.copy(confetti = it.confetti + 1, confettiN = n) }

    fun haptic(kind: String = "tap") {
        if (!data.value.settings.vibe) return
        try {
            val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(when (kind) {
                "heavy" -> VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 70), -1)
                "success" -> VibrationEffect.createWaveform(longArrayOf(0, 14, 40, 22), -1)
                else -> VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            })
        } catch (_: Exception) {}
    }

    fun addBell(icon: String, text: String) =
        setSettings { it.copy(bellLocal = (listOf(BellItem(System.currentTimeMillis(), icon, text)) + it.bellLocal).take(60)) }

    /** Marks a rest-day challenge action as done today (prog, plan, walk). */
    fun setFlag(k: String) {
        val d = iso(today()); val f = data.value.flags[d] ?: emptyMap()
        if ((f[k] ?: 0) > 0) return
        update { dd ->
            val all = java.util.TreeMap(dd.flags); all[d] = f + (k to 1)
            while (all.size > 90) all.remove(all.firstKey())
            dd.copy(flags = all)
        }
        checkUnlocks()
    }

    private fun checkFeatures(silent: Boolean): List<String> {
        val n = ctx().rs.size; val u = data.value.settings.unl
        val fresh = FEATS.filter { n >= it[1].toInt() && it[0] !in u }
        if (fresh.isEmpty()) return u
        val nu = u + fresh.map { it[0] }
        setSettings { it.copy(unl = nu) }
        if (!silent) fresh.forEachIndexed { i, f -> later(700L + i * 2200L) { say("🔓 New: ${f[2]}"); addBell("🔓", "${f[2]} unlocked. ${f[3]}") } }
        return nu
    }

    /** Hands out season pass rewards up to the current level. Returns labels of new ones. */
    private fun grantPass(e: Eng): List<String> {
        val cos = data.value.settings.cos; val got = mutableListOf<String>()
        var plate = cos.plate; var aura = cos.aura; var frame = cos.frame; var tag = cos.tag
        PASS.forEach { (r, label) -> val (lv, t, k) = r
            if (e.lvl < lv) return@forEach
            when (t) {
                "plate" -> if (k !in plate) { plate = plate + k; got += label }
                "aura" -> if (k !in aura) { aura = aura + k; got += label }
                "frame" -> if (k !in frame) { frame = frame + k; got += label }
                else -> if (k !in tag) { tag = tag + k; got += label }
            }
        }
        if (got.isNotEmpty()) setSettings { it.copy(cos = Cosmetics(plate, aura, frame, tag)) }
        return got
    }

    private fun goalCheck() {
        val c = ctx(); var ch = false
        val goals = data.value.settings.goals.map { g ->
            if (g.doneAt != null) return@map g
            val i = c.goalInfo(g)
            if (i.done != null) {
                ch = true
                later(600) { say("🎯 Goal crushed: ${c.exName(g.ex)} ${fmtN(g.w)}"); confetti(110); haptic("success"); addBell("🎯", "Goal crushed: ${c.exName(g.ex)} ${fmtN(g.w)} ${c.unit}") }
                g.copy(doneAt = i.done)
            } else g
        }
        if (ch) setSettings { it.copy(goals = goals) }
    }

    /** Starts new quest chains and celebrates finished ones. */
    fun chainsCheck() {
        if (!ctx().feat("quests")) return
        val (_, map, finished) = ctx().chains()
        val st = data.value.settings
        val newDone = finished.filter { it !in st.chainsDone }
        if (map == st.chains && newDone.isEmpty()) return
        setSettings { it.copy(chains = map, chainsDone = it.chainsDone + newDone) }
        newDone.forEach { nm -> later(500) { say("🗺️ Quest complete: $nm"); confetti(80); haptic("success"); addBell("🗺️", "Quest complete: $nm") } }
    }

    private var unlockJob: Job? = null
    /** Debounced unlock check (e.g. while typing a note). */
    fun checkUnlocksSoon(ms: Long = 1500) { unlockJob?.cancel(); unlockJob = viewModelScope.launch { delay(ms); checkUnlocks() } }

    fun checkUnlocks() { viewModelScope.launch(engDispatcher) { checkUnlocksNow(); grant14() } }

    /** Season pass plate finishes are granted and kept forever (0.14). */
    private fun grant14() {
        if (data.value.plan == null) return
        val c = ctx(); val got = FIN.values.filter { f -> f.season != null && c.eng.lvl >= f.season && f.k !in c.cos14("plate") }
        if (got.isEmpty()) return
        setSettings { it.copy(cos14 = it.cos14 + ("plate" to ((it.cos14["plate"] ?: emptyList()) + got.map { f -> f.k }))) }
        got.forEachIndexed { i, f -> sayLater(600L + i * 1800L, "🎁 Season reward: ${f.name} plate finish") }
    }

    // ============================ 0.14-0.17 ============================
    /** Ghost badge: 5 opens in a day without logging a set. */
    private fun ghostCount() {
        val d = data.value; if (d.plan == null) return; val t = iso(today())
        val g = d.settings.ghostD?.takeIf { it.d == t } ?: GhostDay(t, 0); val n = g.n + 1
        val logged = d.sessions.values.any { it.date == t && it.ex.values.any { a -> a.any(::full) } }
        setSettings { it.copy(ghostD = GhostDay(t, n), fb = if (n >= 5 && !logged) it.fb + ("ghost" to 1) else it.fb) }
    }
    /** The 5-step tour shows once for new lifters (0.16). */
    fun tourCheck() {
        val d = data.value; if (d.settings.tour16 != 0) return
        if (d.plan == null) { later(1500) { tourCheck() }; return }
        if (ctx().rs.size >= 3) { setSettings { it.copy(tour16 = 1) }; return }
        if (ui.value.sheet != null || ui.value.news) { later(1500) { tourCheck() }; return }
        setSettings { it.copy(tour16 = 1) }; openSheet(Sheet.Tour(0))
    }
    /** Equip something from Customize. */
    fun equipLook(t: String, k: String) {
        val c = ctx(); val it0 = LOCK[t]?.items?.get(k) ?: return
        if (t == "react") { say(if (c.owns(t, k)) "You can use this reaction" else "🔒 ${it0.how}"); return }
        if (!c.owns(t, k)) { say("🔒 ${it0.how}"); return }
        setSettings { it.copy(look = it.look + (t to k)) }
        when (t) { "sound" -> playRestSound(k, true); "setfx" -> setFx(k); "prfx" -> prFx(k, ""); "nameFx", "card", "pbanner" -> crew.sync(true) }
    }
    /** Opens the next mystery box (earned only, never bought). */
    fun openBox() {
        val c = ctx(); val src = c.boxesLeft().firstOrNull() ?: return; val got = c.boxPrize(src)
        setSettings { s0 -> s0.copy(boxes = s0.boxes + (src to (got?.let { "${it.first}:${it.second}" } ?: "dupe")),
            cos14 = if (got != null) s0.cos14 + (got.first to ((s0.cos14[got.first] ?: emptyList()) + got.second)) else s0.cos14) }
        openSheet(Sheet.BoxOpen(null, true)); haptic("heavy")
        later(900) { openSheet(Sheet.BoxOpen(got, false)); if (got != null) confetti(80) }
    }
    fun saveProfile(p: Prof, av: Av?, flex: List<String>, rim: String) {
        setSettings { it.copy(prof = p, av = av?.takeIf { a -> a.t != null || a.c != null }, flex = flex, rim = rim.take(24)) }
        openSheet(Sheet.ProfilePage(myId() ?: "")); crew.sync(true); say("Profile saved")
    }
    fun founderUnlock(code: String) {
        val a = data.value.settings.acct ?: run { say("Sign in first, then try again"); return }
        viewModelScope.launch {
            val (st, j) = postJson("$SERVER/vault/founder", buildJsonObject { put("acct", a.id); put("token", a.token); put("code", code) })
            if (st in 200..299) { setSettings { it.copy(founder = true) }; closeSheet(); confetti(140); say("👑 Welcome, Founder"); crew.sync(true) }
            else say(if (j.err == "wrong code") "That code isn’t right" else "Sign in first, then try again")
        }
    }
    fun loadDash() {
        dash.value = null; val a = data.value.settings.acct
        viewModelScope.launch {
            if (a == null) { dash.value = buildJsonObject { put("error", "Only the founder account can open this.") }; return@launch }
            val (st, j) = postJson("$SERVER/stats/dash", buildJsonObject { put("acct", a.id); put("token", a.token) })
            dash.value = if (st == 0) buildJsonObject { put("error", "Could not load. Check your connection.") } else if (j.err != null) buildJsonObject { put("error", if (j.err == "owner only") "Only the founder account can open this." else j.err!!) } else j
        }
    }
    private var pingAt = 0L
    /** Anonymous usage ping to Overload's own server (0.15, same as the web app; feeds the owner dashboard). */
    fun statsPing() {
        if (System.currentTimeMillis() - pingAt < 6 * 3600_000L || data.value.plan == null) return; pingAt = System.currentTimeMillis()
        val p = appContext.getSharedPreferences("overload", Context.MODE_PRIVATE); var dev = p.getString("dev", "") ?: ""
        if (!Regex("^[\\w-]{8,40}$").matches(dev)) { dev = "d-" + java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString((Math.random() * 1e12).toLong(), 36); p.edit().putString("dev", dev).apply() }
        val c = ctx(); val wk = c.rs.count { it.date >= crew.curWk() }
        viewModelScope.launch { postJson("$SERVER/stats/ping", buildJsonObject { put("dev", dev); put("name", data.value.settings.name); put("plat", "Android"); put("sa", true); put("ver", APP_VER)
            put("crew", crewState.value?.crew?.name ?: ""); put("sync", data.value.settings.acct != null); put("wk", wk); put("tot", c.rs.size) }) }
    }
    fun setFx(k: String = ctx().look("setfx")) { if (k != "pulse") ui.update { it.copy(setFx = it.setFx + 1, setFxKind = k) } }
    fun prFx(k: String = ctx().look("prfx"), name: String) {
        ui.update { it.copy(confettiKind = k) }; confetti(if (k == "goldrain") 60 else 90); haptic("heavy")
        say("🏆 ${if (name.isNotEmpty()) "$name PR. " else ""}${PR_LINES.random()}")
    }
    fun plateTalk() { haptic("tap"); ui.update { it.copy(plateTalk = (it.plateTalk.first + 1) to PLATE_LINES.random()) } }
    fun playRestSound(k: String = ctx().look("sound"), preview: Boolean = false) {
        if (k == "classic") { if (preview || data.value.settings.sound) runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 80).startTone(ToneGenerator.TONE_PROP_BEEP2, 500) }; return }
        SoundPack.play(k)
    }

    /** Port of the web app's unlock engine: badges, rank-ups, divisions, prestige, season rewards. */
    private fun checkUnlocksNow() {
        if (data.value.plan == null) return
        var e = ctx().eng; val st0 = data.value.settings
        if (st0.seen == null || st0.v12 == 0) {
            val held = st0.held ?: e.elig
            setSettings { it.copy(seen = e.keys.toList(), held = held, rankSeen = max(it.rankSeen, held), v12 = 1) }
            e = ctx().eng
            setSettings { it.copy(divSeen = e.rank * 10 + (4 - (if (e.div == 0) 1 else e.div)), presSeen = e.prestige, curSeason = e.season) }
            grantPass(e); checkFeatures(true); setSettings { it.copy(v13 = 1) }
            return
        }
        if (st0.v13 == 0) { checkFeatures(true); setSettings { it.copy(seen = e.keys.toList(), v13 = 1) }; return }
        checkFeatures(false); goalCheck(); chainsCheck()
        e = ctx().eng
        if (data.value.settings.held == null) setSettings { it.copy(held = e.elig) }
        if (e.trial?.passed == true) { val nr = e.rank + 1; setSettings { it.copy(held = nr) }; e = ctx().eng }
        val st = data.value.settings
        if (st.curSeason != e.season) setSettings { s2 -> s2.copy(seasons = if (s2.curSeason != null) s2.seasons + (s2.curSeason.toString() to s2.lastLvl) else s2.seasons, curSeason = e.season) }
        setSettings { it.copy(lastLvl = e.lvl) }
        val newCos = grantPass(e)
        val s1 = data.value.settings
        val seen = (s1.seen ?: emptyList()).toSet(); val fresh = e.keys.filter { it !in seen }
        val up = e.rank > s1.rankSeen; val pres = e.prestige > s1.presSeen
        val dkey = e.rank * 10 + (4 - (if (e.div == 0) 1 else e.div)); val divUp = !up && e.rank < 7 && dkey > s1.divSeen
        setSettings { it.copy(seen = e.keys.toList(), rankSeen = max(e.rank, it.rankSeen), presSeen = max(e.prestige, it.presSeen), divSeen = max(dkey, it.divSeen)) }
        newCos.forEachIndexed { i, c -> sayLater(400L + i * 2000L, "🎁 Season reward: $c") }
        if (up || pres) {
            addBell("🏅", "You reached ${e.rname}"); haptic("heavy")
            val unlock = if (up) UNLOCKS[e.rank] ?: "" else ""
            val en = e
            later(250) { ui.update { it.copy(cine = Cine(en.rank, en.rname, unlock, if (pres && !up) "Prestige unlocked" else "You ranked up to")) }; confetti(120) }
            if (myId() != null) crew.post(kotlinx.serialization.json.buildJsonObject { put("type", "rank"); put("data", kotlinx.serialization.json.buildJsonObject { put("rank", en.rank); put("rname", en.rname) }) })
            crew.sync(true); return
        }
        if (divUp) { sayLater(300, "📈 Division up: ${e.rname}"); addBell("📈", "Division up: ${e.rname}") }
        if (fresh.isEmpty()) { if (divUp) crew.sync(true); return }
        val items = fresh.mapNotNull { k -> e.badge(k.substringBefore("@")) }
        val s = session(); val mid = s.start != null && s.end == null
        crew.sync(true)
        if (mid) { items.forEachIndexed { i, b -> later(i * 2100L) { say("${b.icon} Badge unlocked: ${b.name}"); confetti(40) } }; return }
        if (items.isEmpty()) return
        openSheet(Sheet.Badges(items.distinctBy { it.k })); confetti(70)
    }

    /** On Mondays: last week's report card. Returns true if it was shown. */
    fun weekReport(): Boolean {
        val tw = iso(weekStart(today())); val st = data.value.settings
        if (st.lastReport == tw || data.value.plan == null) return false
        setSettings { it.copy(lastReport = tw) }
        val r = ctx().weekReport() ?: return false
        openSheet(Sheet.ReportCard(r)); return true
    }

    // ---------- screens and sheets ----------
    fun openSheet(s: Sheet?) = ui.update { it.copy(sheet = s, confirm = null) }
    fun closeSheet() = openSheet(null)
    fun setConfirm(k: String?) = ui.update { it.copy(confirm = k) }
    fun openProfile(on: Boolean = true) { if (on) setTab("me") else setTab("log") }
    fun closeCine() = ui.update { it.copy(cine = null) }
    fun setSub(t: String, k: String) {
        ui.update { it.copy(sub = it.sub + (t to k), editNote = if (t == "plan" && k == "notes") null else it.editNote) }
        if (t == "stats" && k == "lifts") setFlag("prog")
    }
    fun openYear(on: Boolean) {
        if (on && ctx().yearData(LocalDate.now().year) == null) { say("Log a workout this year to unlock your review"); return }
        ui.update { it.copy(yir = on) }
    }
    fun yearSeen() { setSettings { it.copy(yirSeen = LocalDate.now().year) }; ui.update { it.copy(yir = false) } }
    fun togglePin(k: String) {
        if (k !in data.value.settings.showcase && data.value.settings.showcase.size >= 3) say("Unpinned the oldest to make room")
        setSettings { s2 -> val a = s2.showcase.toMutableList(); if (k in a) a.remove(k) else { if (a.size >= 3) a.removeAt(0); a.add(k) }; s2.copy(showcase = a) }
        crew.sync(true)
    }
    fun setLook(frame: String? = null, aura: String? = null, tag: String? = null) {
        setSettings { it.copy(frame = frame ?: it.frame, aura = aura ?: it.aura, tag = tag ?: it.tag) }; crew.sync(true)
    }
    fun addGoal(ex: String, w: Double?, date: String) {
        if (ex.isEmpty() || w == null || w <= 0 || date.isEmpty()) { say("Pick a lift, a weight and a date"); return }
        if (date <= iso(today())) { say("Pick a date in the future"); return }
        setSettings { it.copy(goals = it.goals + Goal(ex, w, date, iso(today()))) }; closeSheet(); say("Goal set 🎯")
    }
    fun removeGoal(i: Int) {
        if (ui.value.confirm != "g$i") { setConfirm("g$i"); say("Tap ✕ again to remove this goal"); return }
        data.value.settings.goals.getOrNull(i)?.let { g -> trash.put("goal", "Goal: ${exName(g.ex)} ${fmtN(g.w)} $unit", json.encodeToJsonElement(g)) }
        setConfirm(null); setSettings { it.copy(goals = it.goals.filterIndexed { j, _ -> j != i }) }
    }
    fun saveBw(v: Double?) {
        if (v == null || v <= 40 || v >= 1000) { say("Enter your bodyweight"); return }
        setSettings { it.copy(bw = v) }; crew.sync(true)
    }
    fun setPlateColor(di: Int, k: String) {
        val lock = ctx().plateLocked(k)
        if (lock.isNotEmpty()) { say("Locked. Unlocks at $lock"); return }
        editDay(di) { it.copy(color = k) }
    }
    fun toggleWalk() { setFlag("walk"); haptic("success") }

    // ---------- derived helpers ----------
    val unit get() = data.value.settings.unit
    private fun dayKey(d: LocalDate) = DAYS[dayIdx(d)]
    fun curKey(u: UiState = ui.value) = sid(u.date.toString(), dayKey(u.date))
    fun session(d: AppData = data.value, u: UiState = ui.value): Session =
        d.sessions[curKey(u)] ?: Session(u.date.toString(), dayKey(u.date))
    fun todayPlan(d: AppData = data.value, u: UiState = ui.value): DayPlan? = d.plan?.getOrNull(dayIdx(u.date))
    fun todayGroups(d: AppData = data.value, u: UiState = ui.value): List<List<Exercise>> {
        val sw = session(d, u).swap
        return todayPlan(d, u)?.groups?.map { gr -> gr.map { sw[it.id] ?: it } } ?: emptyList()
    }
    private fun exMap(d: AppData = data.value): Map<String, Exercise> {
        val m = LinkedHashMap<String, Exercise>()
        d.plan?.forEach { day -> day.groups.forEach { gr -> gr.forEach { if (it.id !in m) m[it.id] = it } } }
        return m
    }
    fun exName(id: String, d: AppData = data.value): String =
        exMap(d)[id]?.name ?: d.sessions.values.firstNotNullOfOrNull { it.names[id] } ?: id
    fun exMuscle(id: String, d: AppData = data.value): String =
        exMap(d)[id]?.m ?: LIB.firstOrNull { slug(it.name) == id }?.m ?: "Other"
    fun allIds(d: AppData = data.value): List<String> =
        (exMap(d).keys + d.sessions.values.flatMap { it.ex.keys }).distinct()

    private fun editSession(f: (Session) -> Session) {
        val u = ui.value; val k = curKey(u)
        update { d -> d.copy(sessions = d.sessions + (k to f(d.sessions[k] ?: Session(u.date.toString(), dayKey(u.date))))) }
    }

    // ---------- navigation / date ----------
    fun setDate(d: LocalDate, manual: Boolean) =
        ui.update { it.copy(date = d, pday = dayIdx(d), picker = null, auto = !manual || d == LocalDate.now()) }
    fun shiftWeek(n: Int) = setDate(ui.value.date.plusWeeks(n.toLong()), true)
    fun goToday() = setDate(LocalDate.now(), false)
    fun setTab(t: String) {
        val u = ui.value
        val again = u.tab == t && !u.settings && !u.profile
        haptic("tap")
        ui.update {
            it.copy(tab = t, settings = false, profile = false, picker = null, editNote = null, confirm = null,
                pday = if (t == "plan") dayIdx(it.date) else it.pday,
                prog = if (t == "stats" && again) null else it.prog)
        }
        if (t == "log" && again) goToday()
        if (t == "plan") setFlag("plan")
        if (t == "crew") crew.sync(true)
        if (t == "me") ui.update { it.copy(profile = false) }
    }
    fun toggleSettings() = ui.update { it.copy(settings = !it.settings, profile = false, set17 = null) }
    fun setProg(id: String?) = ui.update { it.copy(prog = id) }
    fun setPFilter(f: String) = ui.update { it.copy(pfilter = f) }
    fun setPDay(i: Int) = ui.update { it.copy(pday = i, picker = null) }

    // ---------- onboarding / plan ----------
    fun startPlan(key: String, name: String) {
        val t = TEMPLATES.first { it.key == key }
        update { it.copy(plan = t.days(), settings = it.settings.copy(name = name.trim(), seenNews = NEWS_ID)) }
        checkUnlocks(); later(1400) { tourCheck() }
        say("You're set. Let's lift")
    }
    fun replacePlan(key: String) {
        val t = TEMPLATES.first { it.key == key }
        data.value.plan?.let { trash.put("plan", "Your plan before switching templates", json.encodeToJsonElement(it)) }
        update { it.copy(plan = t.days()) }; say("Plan replaced")
    }
    fun editDay(idx: Int, f: (DayPlan) -> DayPlan) =
        update { d -> d.copy(plan = d.plan?.mapIndexed { i, p -> if (i == idx) f(p) else p }) }
    fun renameToday(name: String) { if (name.isNotBlank()) editDay(dayIdx(ui.value.date)) { it.copy(name = name.trim()) } }

    private fun uniqueId(name: String): String {
        val m = exMap(); val base = slug(name); var id = base; var n = 2
        while (m[id] != null && !m[id]!!.name.equals(name, true)) id = "$base-${n++}"
        return id
    }
    fun openPicker(p: Picker?) = ui.update { it.copy(picker = p) }
    fun pickExercise(name: String, m: String, lo: Int, hi: Int) {
        val p = ui.value.picker ?: return
        val d = data.value
        if (p.ctx == "swap") {
            val (gi, ei) = p.key.split("-").map { it.toInt() }
            val orig = d.plan!![dayIdx(ui.value.date)].groups[gi][ei]
            val ex = E(name, m, lo, hi, orig.sets, slug(name))
            editSession { it.copy(swap = it.swap + (orig.id to ex), names = it.names + (ex.id to name)) }
            say("Swapped to $name for today")
        } else {
            val di = if (p.ctx == "today") dayIdx(ui.value.date) else ui.value.pday
            val ex = E(name, m, lo, hi, 2, uniqueId(name))
            editDay(di) { day ->
                val g = day.groups.toMutableList()
                if (p.gi >= 0 && p.gi < g.size) g[p.gi] = g[p.gi] + ex else g.add(listOf(ex))
                day.copy(groups = g, type = if (day.type == "off") "other" else day.type)
            }
            say("$name added")
        }
        ui.update { it.copy(picker = null) }
    }
    fun removeEx(dayI: Int, gi: Int, ei: Int) {
        data.value.plan?.getOrNull(dayI)?.let { p -> p.groups.getOrNull(gi)?.getOrNull(ei)?.let { e ->
            trash.put("exercise", "${e.name} (from ${p.name})", buildJsonObject { put("day", dayI); put("gi", gi); put("ex", json.encodeToJsonElement(e)) }) } }
        removeEx0(dayI, gi, ei)
    }
    private fun removeEx0(dayI: Int, gi: Int, ei: Int) = editDay(dayI) { day ->
        val g = day.groups.map { it.toMutableList() }.toMutableList()
        g[gi].removeAt(ei); if (g[gi].isEmpty()) g.removeAt(gi)
        day.copy(groups = g)
    }
    fun removeGroup(dayI: Int, gi: Int) {
        data.value.plan?.getOrNull(dayI)?.let { p -> p.groups.getOrNull(gi)?.let { g ->
            trash.put("group", "${g.joinToString(" + ") { it.name }} (from ${p.name})", buildJsonObject { put("day", dayI); put("gi", gi); put("group", json.encodeToJsonElement(g)) }) } }
        removeGroup0(dayI, gi)
    }
    private fun removeGroup0(dayI: Int, gi: Int) = editDay(dayI) { it.copy(groups = it.groups.filterIndexed { i, _ -> i != gi }) }
    fun moveGroupUp(dayI: Int, gi: Int) = editDay(dayI) { day ->
        if (gi <= 0) day else day.copy(groups = day.groups.toMutableList().also { java.util.Collections.swap(it, gi, gi - 1) })
    }
    fun editEx(dayI: Int, gi: Int, ei: Int, f: (Exercise) -> Exercise) = editDay(dayI) { day ->
        day.copy(groups = day.groups.mapIndexed { g, lst -> if (g == gi) lst.mapIndexed { e, x -> if (e == ei) f(x) else x } else lst })
    }

    // ---------- logging ----------
    fun setField(e: Exercise, i: Int, field: Char, text: String) = editSession { s ->
        val list = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (list.size <= i) list.add(SetEntry())
        val x = list[i]
        list[i] = when (field) {
            'w' -> x.copy(w = text.toDoubleOrNull())
            'r' -> x.copy(r = text.toIntOrNull())
            else -> x.copy(q = text.toIntOrNull())
        }
        s.copy(ex = s.ex + (e.id to list), names = s.names + (e.id to e.name))
    }

    fun toggleSet(e: Exercise, gi: Int, ei: Int, i: Int, phW: Double?, phR: Int?) {
        val d = data.value; val u = ui.value; val s = session(d, u)
        val list = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (list.size <= i) list.add(SetEntry())
        var x = list[i]
        if (x.d) { list[i] = x.copy(d = false); editSession { it.copy(ex = it.ex + (e.id to list)) }; return }
        if (x.w == null && phW != null) x = x.copy(w = phW)
        if (x.r == null && phR != null) x = x.copy(r = phR)
        if (!full(x)) { say("Enter weight and reps first"); return }
        x = x.copy(d = true); list[i] = x
        haptic("tap")
        val t = System.currentTimeMillis()
        editSession { s2 ->
            var n = s2
            if (n.start == null) n = n.copy(start = t, paused = 0, pauseAt = null, end = null)
            val pa = n.pauseAt
            if (pa != null) n = n.copy(paused = n.paused + (t - pa), pauseAt = null)
            n.copy(ex = n.ex + (e.id to list), names = n.names + (e.id to e.name))
        }
        val pb = bestBefore(d.sessions, e.id, s.date)
        val isPr = pb > 0 && e1(x) > pb
        setFx()
        if (isPr && s.prShown[e.id] == null) { editSession { it.copy(prShown = it.prShown + (e.id to 1)) }; prFx(name = e.name) }
        else if (isPr) { say("New PR on ${e.name}"); haptic("success") }
        later(50) { checkUnlocks(); crew.sendLive() }
        val groups = todayGroups(d, u); val gr = groups.getOrNull(gi) ?: emptyList()
        if (ei < gr.size - 1) {
            if (!isPr) say("Next: ${gr[ei + 1].name}")
        } else if (d.settings.autoRest) {
            val first = gr.firstOrNull()
            val next = if (first != null && i + 1 < max(first.sets, s.ex[first.id]?.size ?: 0)) "${first.name} · set ${i + 2}"
            else groups.getOrNull(gi + 1)?.firstOrNull()?.name ?: "Last round. Finish strong"
            startRest(todayPlan(d, u)?.rest ?: 90, next)
        }
    }
    fun addSet(e: Exercise) = editSession { s ->
        val l = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (l.size < e.sets) l.add(SetEntry())
        l.add(SetEntry()); s.copy(ex = s.ex + (e.id to l))
    }
    fun removeSet(e: Exercise) {
        val s = session(); val x = s.ex[e.id]?.lastOrNull()
        if (x != null && full(x)) trash.put("set", "${e.name} ${fmtN(x.w!!)}×${x.r} (${s.date})", buildJsonObject { put("key", curKey()); put("id", e.id); put("set", json.encodeToJsonElement(x)) })
        removeSet0(e)
    }
    private fun removeSet0(e: Exercise) = editSession { s ->
        val l = (s.ex[e.id] ?: emptyList()).dropLast(1); s.copy(ex = s.ex + (e.id to l))
    }
    fun unswap(orig: Exercise) { editSession { it.copy(swap = it.swap - orig.id) }; say("Back to ${orig.name}") }
    fun toggleWarm(id: String) = ui.update { it.copy(warm = if (id in it.warm) it.warm - id else it.warm + id) }
    fun setSessionNote(t: String) = editSession { it.copy(notes = t) }
    fun setExNote(id: String, t: String) { editSession { it.copy(exNotes = it.exNotes + (id to t)) }; checkUnlocksSoon() }

    // ---------- workout clock ----------
    fun startWorkout() { editSession { if (it.start == null) it.copy(start = System.currentTimeMillis(), paused = 0, pauseAt = null, end = null) else it }; say("Clock is running"); later(50) { crew.sendLive() } }
    fun togglePause() = editSession { s ->
        val t = System.currentTimeMillis(); val pa = s.pauseAt
        if (pa != null) s.copy(paused = s.paused + (t - pa), pauseAt = null) else s.copy(pauseAt = t)
    }
    fun finish() {
        editSession { s ->
            val t = System.currentTimeMillis(); val pa = s.pauseAt
            (if (pa != null) s.copy(paused = s.paused + (t - pa), pauseAt = null) else s).copy(end = t)
        }
        restStop(); crew.liveEnd(); haptic("success")
        later(400) { checkUnlocks() }
    }
    fun resetWorkout() { editSession { it.copy(start = null, end = null, pauseAt = null, paused = 0) }; say("Workout clock reset") }
    fun reopen() = editSession { s -> if (s.end != null) s.copy(pauseAt = s.end, end = null) else s }

    // ---------- rest timer ----------
    fun startRest(sec: Int, next: String = "") =
        ui.update { it.copy(rest = RestState(System.currentTimeMillis() + sec * 1000L, sec, sec, next), restLine = if (data.value.settings.restJokes) REST_LINES.random() else "") }
    fun restPlus() = ui.update { u -> u.copy(rest = u.rest?.let { it.copy(endAt = it.endAt + 15000, dur = it.dur + 15, buzzed = false) }) }
    fun restMinus() = ui.update { u -> u.copy(rest = u.rest?.let { it.copy(endAt = it.endAt - 15000) }) }
    fun restReset() { ui.value.rest?.let { startRest(it.base, it.next) } }
    fun restStop() {
        // Rest Timer Rebel: stopping the timer early counts as a skip
        val r = ui.value.rest; if (r != null && r.endAt > System.currentTimeMillis()) editSession { it.copy(skips = it.skips + 1) }
        ui.update { it.copy(rest = null) }
    }
    private fun tickRest() {
        val r = ui.value.rest ?: return
        if (!r.buzzed && System.currentTimeMillis() >= r.endAt) {
            ui.update { it.copy(rest = it.rest?.copy(buzzed = true)) }
            if (alertPending) return // app is in the background: the notification does the alerting
            val st = data.value.settings
            if (st.sound) playRestSound()
            if (st.vibe) try {
                val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
            } catch (_: Exception) {}
        }
    }

    // ---------- lock-screen rest alerts ----------
    private val appCtx get() = getApplication<Application>()
    private var alertPending = false
    fun enableRestAlerts(granted: Boolean) {
        if (!granted) { say("Notifications are blocked. Allow them in your phone settings"); return }
        setSettings { it.copy(restAlerts = true) }
        RestAlerts.show(appCtx, "Rest alerts are on", "You'll get a ping here when rest is over 💪")
        say("Rest alerts on. Test ping sent")
    }
    fun disableRestAlerts() { setSettings { it.copy(restAlerts = false) }; RestAlerts.cancel(appCtx); say("Rest alerts off") }
    /** App left the screen: if rest is running, set an alarm for when it ends. */
    fun onBackground() {
        crew.onBackground(); saveNow()
        val r = ui.value.rest ?: return
        if (!data.value.settings.restAlerts || r.endAt - System.currentTimeMillis() <= 3000) return
        RestAlerts.schedule(appCtx, r.endAt, r.next); alertPending = true
    }
    fun onForeground() {
        crew.onForeground(); sync.onForeground(); statsPing()
        if (!alertPending) return
        alertPending = false; RestAlerts.cancel(appCtx)
        // rest ended while away: the notification already pinged, so don't beep again on return
        ui.update { u -> u.copy(rest = u.rest?.let { if (System.currentTimeMillis() >= it.endAt) it.copy(buzzed = true) else it }) }
    }

    // ---------- heart rate (Health Connect) ----------
    private val hrLoading = mutableSetOf<String>()
    fun enableHeartRate(granted: Boolean) {
        if (!granted) { say("Heart rate access wasn't allowed"); return }
        setSettings { it.copy(heartRate = true) }
        say("Heart rate on. In Samsung Health, turn on syncing with Health Connect")
    }
    /**
     * Reads heart rate for a finished workout. Samsung Health syncs late, so it re-reads for 2 hours
     * after the workout ends (at most every 2 minutes). force = the refresh button.
     */
    fun loadHeartRate(key: String, force: Boolean = false) {
        val d = data.value
        if (!d.settings.heartRate) return
        val s = d.sessions[key] ?: return
        val start = s.start ?: return; val end = s.end ?: return
        val h = s.hr; val t = System.currentTimeMillis()
        val stale = h == null || h.forEnd != end || (t < end + 2 * 3_600_000L && t - h.fetchedAt > 120_000L)
        if (!force && !stale) return
        if (!hrLoading.add(key)) return
        viewModelScope.launch {
            val r = Health.read(appCtx, start, end)
            hrLoading.remove(key)
            if (r == null) { if (force || h == null) say("Couldn't read heart rate. Check Overload's access in Health Connect"); return@launch }
            update { dd ->
                val ss = dd.sessions[key]
                if (ss == null || ss.end != end) dd else dd.copy(sessions = dd.sessions + (key to ss.copy(hr = r)))
            }
            if (force) say(if (r.avg > 0) "Heart rate updated" else "No heart rate yet. Samsung Health may still be syncing")
        }
    }

    // ---------- notes / settings ----------
    fun editNote(n: Note?) = ui.update { it.copy(editNote = n) }
    fun newNote() = editNote(Note(id = ""))
    fun saveNote(n: Note) {
        if (n.title.isBlank() && n.text.isBlank()) { say("Write something first"); return }
        val saved = n.copy(id = n.id.ifEmpty { "n" + System.currentTimeMillis().toString(36) }, ts = System.currentTimeMillis())
        update { d -> d.copy(notes = if (d.notes.any { it.id == saved.id }) d.notes.map { if (it.id == saved.id) saved else it } else d.notes + saved) }
        editNote(null); say("Note saved"); checkUnlocks()
    }
    fun deleteNote(id: String) { data.value.notes.firstOrNull { it.id == id }?.let { n -> trash.put("note", (n.title.ifBlank { n.text }.ifBlank { "Note" }).take(40), json.encodeToJsonElement(n)) }; update { d -> d.copy(notes = d.notes.filter { it.id != id }) }; editNote(null) }
    fun showNews() = ui.update { it.copy(news = true) }
    fun closeNews() {
        ui.update { it.copy(news = false) }; if (data.value.settings.seenNews != NEWS_ID) setSettings { it.copy(seenNews = NEWS_ID) }
        later(300) { if (!weekReport()) checkUnlocks() }
    }
    fun setSettings(f: (Settings) -> Settings) = update { it.copy(settings = f(it.settings)) }
    fun wipe() { RestAlerts.cancel(appCtx); crew.stop(); chat.reset(); update { AppData(settings = Settings(theme = it.settings.theme, seenNews = NEWS_ID)) }; crewState.value = null; ui.update { UiState() } }

    // ---------- backup ----------
    fun exportJson(): String {
        setSettings { it.copy(lastBackup = System.currentTimeMillis()) }
        return json.encodeToString(data.value)
    }

    /** Accepts this app's backup, and the web version's backup file (plan wrapped in {days}, "" for empty boxes, d as 0/1). */
    fun importJson(text: String): Boolean = try {
        val root = Json.parseToJsonElement(text).jsonObject
        val fixed = JsonObject(root.mapValues { (k, v) ->
            if (k == "plan" && v is JsonObject && v["days"] != null) v["days"]!! else v
        })
        val c = clean(fixed).jsonObject
        val sessions = (c["sessions"] as? JsonObject)?.mapNotNull { (k, v) -> runCatching { k to json.decodeFromJsonElement<Session>(v) }.getOrNull() }?.toMap() ?: emptyMap()
        val plan = c["plan"]?.let { runCatching { json.decodeFromJsonElement<List<DayPlan>>(it) }.getOrNull() }
        val notes = c["notes"]?.let { runCatching { json.decodeFromJsonElement<List<Note>>(it) }.getOrNull() }
        val flags = c["flags"]?.let { runCatching { json.decodeFromJsonElement<Map<String, Map<String, Int>>>(it) }.getOrNull() }
        if (sessions.isEmpty() && plan == null) throw IllegalArgumentException("empty")
        trash.put("undo-restore", "Your data from before the restore", json.encodeToJsonElement(data.value))
        update { d ->
            d.copy(
                sessions = d.sessions + sessions,
                plan = plan ?: d.plan,
                notes = notes ?: d.notes,
                settings = (c["settings"] as? JsonObject)?.let { decodeSettingsLenient(it, d.settings).copy(acct = d.settings.acct) } ?: d.settings,
                flags = flags ?: d.flags
            )
        }
        say("Backup restored"); true
    } catch (e: Exception) { say("That file could not be read"); false }

    /** Web-app JSON -> this app's shapes ("" for empty boxes, 0/1 for done). */
    internal fun cleanWeb(el: JsonElement): JsonElement = clean(el)

    /**
     * Reads settings that may come from the web app. Keys whose type doesn't match are skipped
     * instead of failing the whole thing. Missing keys keep the defaults.
     */
    internal fun decodeSettingsLenient(o: JsonObject, fallback: Settings): Settings {
        val fixed = JsonObject(o.mapValues { (k, v) ->
            if (k == "bw" && v is JsonPrimitive && v.isString) JsonPrimitive(v.content.toDoubleOrNull() ?: 0.0)
            else if (k == "held" && v is JsonPrimitive && !v.isString) JsonPrimitive(v.doubleOrNull?.toInt()) else v
        })
        runCatching { return json.decodeFromJsonElement<Settings>(fixed) }
        val ok = LinkedHashMap<String, JsonElement>()
        fixed.forEach { (k, v) -> if (runCatching { json.decodeFromJsonElement<Settings>(JsonObject(ok + (k to v))) }.isSuccess) ok[k] = v }
        return runCatching { json.decodeFromJsonElement<Settings>(JsonObject(ok)) }.getOrDefault(fallback)
    }

    private fun clean(el: JsonElement): JsonElement = when (el) {
        is JsonObject -> JsonObject(el.mapValues { (k, v) ->
            when {
                v is JsonPrimitive && v.isString && v.content.isEmpty() && k in setOf("w", "r", "q", "lo", "hi", "sets") -> JsonNull
                k == "d" && v is JsonPrimitive && !v.isString && v.content in setOf("0", "1") -> JsonPrimitive(v.content == "1")
                else -> clean(v)
            }
        })
        is JsonArray -> JsonArray(el.map { clean(it) })
        else -> el
    }
}
