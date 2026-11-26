package com.overload.app

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * Ranks, badges, challenges, quests, seasons, lift ranks, mastery, quest chains and goals.
 * Ported from the web app (0.10–0.13) so both give the same results for the same data,
 * including the "random" daily challenges and weekly quests (same seeds, same maths).
 */

// ============================ CONSTANTS ============================
class RankDef(val name: String, val weeks: Int, val badges: Int, val dark: Long, val light: Long)
val RANKS = listOf(
    RankDef("Iron", 0, 0, 0xFF5B6069, 0xFFB3B8C1), RankDef("Bronze", 2, 3, 0xFF8A4F1F, 0xFFF0B27A),
    RankDef("Silver", 6, 8, 0xFF7D838D, 0xFFF4F6F9), RankDef("Gold", 12, 15, 0xFFA97A05, 0xFFFFE07A),
    RankDef("Platinum", 20, 25, 0xFF2F948F, 0xFFC6F5F0), RankDef("Diamond", 32, 35, 0xFF2468D6, 0xFFCFE6FF),
    RankDef("Titan", 45, 45, 0xFF6D2FE0, 0xFFE6C4FF), RankDef("Olympian", 52, 60, 0xFFB88600, 0xFFFFF3B0)
)
val UNLOCKS = mapOf(1 to "Pink and teal plate colors", 2 to "Shiny badges", 3 to "Gold plate color", 4 to "Chrome plate color",
    5 to "Neon welcome screen", 6 to "Animated rank emblem", 7 to "Gold welcome plate")
val PLATE_LOCK = mapOf("pink" to 1, "teal" to 1, "gold" to 3, "chrome" to 4)
val SEASON_PLATES = mapOf("ember" to 2, "ice" to 8, "toxic" to 16, "galaxy" to 22)
val TIERS = listOf("Bronze" to 0xFFC07A3E, "Silver" to 0xFF8FB4DC, "Gold" to 0xFFE8B923, "Platinum" to 0xFF4FD1C5, "Diamond" to 0xFF5AA9FF, "Legend" to 0xFFD14DFF)
val LADDER = linkedMapOf("Workouts" to ("Iron Regular" to "🏋️"), "Streaks" to ("Streak Keeper" to "🔥"), "Volume" to ("Mover" to "🏗️"), "Sets" to ("Grinder" to "✅"), "PRs" to ("PR Machine" to "🏆"))
val ROMAN = listOf("", "I", "II", "III", "IV", "V", "VI")
/** Season pass: level, reward type, key, label. */
val PASS = listOf(
    Triple(2, "plate", "ember") to "Ember plate color", Triple(4, "aura", "spark") to "Spark aura", Triple(6, "frame", "neon") to "Neon frame",
    Triple(8, "plate", "ice") to "Ice plate color", Triple(10, "tag", "Grinder") to "\"Grinder\" nameplate", Triple(12, "aura", "frost") to "Frost aura",
    Triple(14, "frame", "circuit") to "Circuit frame", Triple(16, "plate", "toxic") to "Toxic plate color", Triple(18, "aura", "ember") to "Ember aura",
    Triple(20, "frame", "laurel") to "Laurel frame", Triple(22, "plate", "galaxy") to "Galaxy plate color", Triple(24, "aura", "prism") to "Prism aura",
    Triple(26, "tag", "Unbreakable") to "\"Unbreakable\" nameplate", Triple(28, "frame", "crown") to "Crown frame", Triple(30, "aura", "gold") to "Gold aura + season badge"
)
/** Progressive unlocks: key, workouts needed, name, description. */
val FEATS = listOf(
    listOf("mastery", "1", "Muscle mastery", "Your body map now levels up as you train"),
    listOf("goals", "2", "Goals", "Set a lift target with a date in Stats"),
    listOf("season", "3", "Season pass", "Earn levels and rewards all season"),
    listOf("quests", "3", "Quest chains", "Step-by-step roads to your next big lift"),
    listOf("liftranks", "5", "Lift ranks", "See how strong each lift is for your bodyweight")
)
val MASTER = listOf(25, 75, 150, 300, 600, 1000)
class LiftStd(val k: String, val name: String, val re: Regex, val std: List<Double>)
val LIFTS = listOf(
    LiftStd("incline", "Incline press", Regex("incline", RegexOption.IGNORE_CASE), listOf(.4, .6, .85, 1.05, 1.25, 1.45)),
    LiftStd("bench", "Bench press", Regex("bench|chest press", RegexOption.IGNORE_CASE), listOf(.5, .75, 1.0, 1.25, 1.5, 1.75)),
    LiftStd("hack", "Hack squat", Regex("hack", RegexOption.IGNORE_CASE), listOf(.75, 1.1, 1.5, 1.85, 2.2, 2.6)),
    LiftStd("legpress", "Leg press", Regex("leg press", RegexOption.IGNORE_CASE), listOf(1.25, 1.75, 2.25, 2.75, 3.25, 3.75)),
    LiftStd("squat", "Squat", Regex("squat", RegexOption.IGNORE_CASE), listOf(.75, 1.0, 1.25, 1.5, 1.75, 2.0)),
    LiftStd("rdl", "RDL", Regex("rdl|romanian", RegexOption.IGNORE_CASE), listOf(.6, .85, 1.1, 1.35, 1.6, 1.85)),
    LiftStd("dead", "Deadlift", Regex("deadlift", RegexOption.IGNORE_CASE), listOf(1.0, 1.25, 1.5, 1.85, 2.2, 2.5)),
    LiftStd("ohp", "Overhead press", Regex("overhead|shoulder press|ohp|military", RegexOption.IGNORE_CASE), listOf(.35, .5, .65, .8, .95, 1.1)),
    LiftStd("pull", "Pulldown", Regex("pulldown|pull-down|pull down|lat pull", RegexOption.IGNORE_CASE), listOf(.5, .65, .8, .95, 1.1, 1.25)),
    LiftStd("row", "Row", Regex("row", RegexOption.IGNORE_CASE), listOf(.5, .7, .9, 1.1, 1.3, 1.5)),
    LiftStd("curl", "Curl", Regex("curl", RegexOption.IGNORE_CASE), listOf(.2, .3, .4, .5, .6, .7))
)
/** Muscle group -> muscle-map regions. */
val MKEY = mapOf("Chest" to listOf("chest"), "Back" to listOf("lat", "mid"), "Shoulders" to listOf("fdelt"), "Side delts" to listOf("sdelt"),
    "Rear delts" to listOf("rdelt"), "Biceps" to listOf("bic"), "Triceps" to listOf("tri"), "Forearms" to listOf("fore"), "Traps" to listOf("trap"),
    "Quads" to listOf("quad"), "Hamstrings" to listOf("ham"), "Glutes" to listOf("glute"), "Calves" to listOf("calf"), "Abs" to listOf("abs"), "Lower back" to listOf("lowb"))
val OBJS = listOf(Triple(250, "a fridge", "🧊"), Triple(1500, "a grand piano", "🎹"), Triple(4000, "a pickup truck", "🛻"), Triple(9000, "an elephant", "🐘"),
    Triple(12000, "a school bus", "🚌"), Triple(18000, "a T-rex", "🦖"), Triple(33000, "a loaded semi truck", "🚛"), Triple(60000, "a fighter jet", "✈️"), Triple(150000, "a blue whale", "🐋"))

// ============================ SMALL HELPERS ============================
fun today(): LocalDate = LocalDate.now()
fun iso(d: LocalDate) = d.toString()
fun toD(s: String): LocalDate = LocalDate.parse(s.take(10))
fun weekStart(d: LocalDate): LocalDate = weekDates(d)[0]
fun hourOf(ms: Long?): Int? = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).hour }
fun loc(n: Number): String = String.format("%,d", n.toDouble().roundToLong())
/** JS Math.round (halves round up). */
fun jsRound(x: Double): Long = floor(x + 0.5).toLong()
fun lifeFmt(v: Double) = if (v >= 1e6) String.format("%.2fM", v / 1e6) else kfmt(v)

/** FNV-1a, same as the web app's hashS. */
fun hashS(str: String): Long {
    var h = 2166136261L.toInt()
    str.codePoints().forEach { c -> h = h xor c; h *= 16777619 }
    return h.toLong() and 0xFFFFFFFFL
}
/** Seeded pick of n items, matching the web app's floating-point maths exactly. */
fun <T> pick(pool: List<T>, n: Int, seed: Long): List<T> {
    val a = pool.toMutableList(); val out = mutableListOf<T>(); var s = if (seed == 0L) 1L else seed
    while (out.size < n && a.isNotEmpty()) {
        s = (s.toDouble() * 1103515245.0 + 12345.0).toLong() and 0xFFFFFFFFL
        out += a.removeAt((s % a.size).toInt())
    }
    return out
}
fun isHoliday(d: LocalDate): Boolean {
    val m = d.monthValue; val day = d.dayOfMonth
    if ((m == 1 && day == 1) || (m == 7 && day == 4) || (m == 12 && (day == 25 || day == 31))) return true
    return m == 11 && d.dayOfWeek == DayOfWeek.THURSDAY && day in 22..28
}
fun seasonOf(d: LocalDate) = (d.year - 2026) * 4 + (d.monthValue - 1) / 3 - 2
fun seasonRange(n: Int): Pair<LocalDate, LocalDate> {
    val q = n + 2; val y = 2026 + Math.floorDiv(q, 4); val m = Math.floorMod(q, 4) * 3
    val a = LocalDate.of(y, m + 1, 1); return a to a.plusMonths(3).minusDays(1)
}
fun sVol(s: Session) = s.ex.values.sumOf { a -> a.filter(::full).sumOf { it.w!! * it.r!! } }
fun sSets(s: Session) = s.ex.values.sumOf { a -> a.count(::full) }
fun plannedSetsFor(d: DayPlan?) = d?.groups?.sumOf { g -> g.sumOf { it.sets } } ?: 0
fun toMine(v: Double, unit: String?, mine: String) = if (unit == null || unit == mine) v else if (unit == "kg") v * 2.2046 else v / 2.2046

// ============================ DATA TYPES ============================
/** A badge. Repeatable weekly badges have count/list instead of cur/tgt. */
data class Bdg(
    val k: String, val name: String, val icon: String, val cat: String, val desc: String,
    val cur: Double = 0.0, val tgt: Double = 1.0, val hid: Boolean = false, val earned: Boolean = false,
    val count: Int? = null, val list: List<String> = emptyList()
) { val got get() = earned || (count ?: 0) > 0 }
data class Chal(val k: String, val t: String, val done: Boolean, val man: Boolean = false)
data class Quest(val k: String, val t: String, val c: Double, val n: Double, val done: Boolean)
data class Trial(val tier: Int, val days: Int, val need: Int, val beat: Int, val needBeat: Int, val passed: Boolean)
class Eng(
    val B: List<Bdg>, val R: List<Bdg>, val bcount: Int, val active: Int, val rank: Int, val elig: Int, val trial: Trial?,
    val div: Int, val frac: Double, val prestige: Int, val rname: String, val power: Int, val season: Int, val xp: Int, val lvl: Int,
    val streak: Int, val best: Int, val freeze: Int, val keys: Set<String>, val lifeSets: Int, val lifeVol: Double, val lifePR: Int,
    val workouts: Int, val since: String?, private val weekDatesOf: Map<String, List<String>>
) {
    /** Weekly badges earned on this day (for the post-workout breakdown). */
    fun earnedOn(d: String): List<Bdg> {
        val w = iso(weekStart(toD(d)))
        return R.filter { r -> r.list.contains(d) || (r.list.contains(w) && r.k != "sweep" && d == weekDatesOf[w]?.maxOrNull()) }.map { it.copy(count = 1) }
    }
    fun badge(k: String) = B.firstOrNull { it.k == k } ?: R.firstOrNull { it.k == k }
}
data class LiftRank(val k: String, val name: String, val e: Double, val w: Double, val r: Int, val n: String, val ratio: Double, val t: Int, val nextW: Int)
data class Mastery(val k: String, val n: Int, val t: Int, val next: Int?)
data class ChainStep(val w: Int, val r: Int, val done: Boolean)
data class ChainView(val id: String, val name: String, val m: Int, val steps: List<ChainStep>, val done: Boolean)
data class GoalInfo(val cur: Double, val target: Double, val done: String?, val proj: String?, val pct: Int, val status: String)
data class YearData(val y: Int, val n: Int, val days: Int, val vol: Double, val sets: Int, val prs: Int, val mk: List<String>, val levels: Map<String, Int>,
    val best: Triple<String, String, Double>?, val fav: Int, val am: Int, val pm: Int, val hours: Int)
data class Report(val grade: String, val a: WA, val b: WA, val pw: List<LocalDate>, val earned: List<String>)

// ============================ CONTEXT ============================
/**
 * Everything computed from one snapshot of the app's data. Built fresh after each change
 * (cheap enough for years of workouts) and cached by the view model.
 */
class Ctx(val d: AppData, val cs: CrewState?, val myId: String?, val now: LocalDate = today()) {
    val st = d.settings
    val unit = st.unit
    val kg = unit == "kg"
    val plan = d.plan ?: emptyList()
    private val exMap: Map<String, Exercise> by lazy {
        val m = LinkedHashMap<String, Exercise>()
        plan.forEach { day -> day.groups.forEach { g -> g.forEach { if (it.id !in m) m[it.id] = it } } }
        m
    }
    fun exName(id: String) = exMap[id]?.name ?: d.sessions.values.firstNotNullOfOrNull { it.names[id] } ?: id
    fun exMuscle(id: String) = exMap[id]?.m ?: LIB.firstOrNull { slug(it.name) == id }?.m ?: "Other"
    fun exById(id: String): Exercise? = exMap[id] ?: d.sessions.values.firstNotNullOfOrNull { s -> s.swap.values.firstOrNull { it.id == id } }

    /** Sessions with at least one complete set, oldest first. */
    val rs: List<Session> by lazy { d.sessions.values.filter { s -> s.ex.values.any { a -> a.any(::full) } }.sortedBy { it.date } }
    private val statsCache = HashMap<String, SS>()
    fun stats(s: Session): SS {
        val k = sid(s.date, s.day)
        return if (d.sessions[k] === s) statsCache.getOrPut(k) { sessStats(d.sessions, s) } else sessStats(d.sessions, s)
    }
    fun sessOn(date: LocalDate) = d.sessions[sid(iso(date), DAYS[dayIdx(date)])]

    fun muscleSets(s: Session): Map<String, Int> {
        val m = LinkedHashMap<String, Int>()
        s.ex.forEach { (id, a) -> val n = a.count(::full); if (n > 0) { val k = exMuscle(id); m[k] = (m[k] ?: 0) + n } }
        return m
    }
    fun mLevels(m: Map<String, Int>): Map<String, Int> {
        val l = HashMap<String, Int>()
        m.forEach { (k, n) -> MKEY[k]?.forEach { x -> l[x] = max(l[x] ?: 0, if (n >= 4) 2 else 1) } }
        return l
    }
    fun lastBefore(id: String, date: String): Session? = rs.lastOrNull { it.date < date && it.ex[id]?.any(::full) == true }
    fun beatCount(s: Session): Int = s.ex.count { (id, a) ->
        val l = lastBefore(id, s.date) ?: return@count false
        val la = l.ex[id] ?: emptyList()
        a.withIndex().any { (i, x) -> better(x, la.getOrNull(i) ?: la.lastOrNull()) }
    }

    // ---------- daily challenges ----------
    fun dayChallenges(date: LocalDate): List<Chal> {
        val ds = iso(date)
        val pl = plan.getOrNull(dayIdx(date))
        val rest = pl == null || pl.type == "off" || pl.groups.isEmpty()
        val seed = hashS("c$ds"); val f = d.flags[ds] ?: emptyMap()
        if (rest) {
            val pool = listOf(
                Chal("rnote", "Write something on your Notes board", d.notes.any { it.ts > 0 && iso(Instant.ofEpochMilli(it.ts).atZone(ZoneId.systemDefault()).toLocalDate()) == ds }),
                Chal("rprog", "Check how a lift is trending in Progress", (f["prog"] ?: 0) > 0),
                Chal("rplan", "Look over your next workout in Plan", (f["plan"] ?: 0) > 0),
                Chal("rbk", "Save a backup", st.lastBackup?.let { iso(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()) == ds } == true),
                Chal("rwalk", "Go for a recovery walk (tap to check off)", (f["walk"] ?: 0) > 0, man = true)
            )
            return pick(pool, 3, seed)
        }
        val s = sessOn(date) ?: Session(ds, DAYS[dayIdx(date)])
        val ps = plannedSetsFor(pl)
        val prev = rs.lastOrNull { it.day == DAYS[dayIdx(date)] && it.date < ds }
        val tv = if (prev != null) jsRound(sVol(prev) / 100) * 100 else 0L
        val pool = mutableListOf(
            Chal("beat", "Beat last time on at least 1 lift", beatCount(s) >= 1),
            Chal("sets", "Log ${if (ps > 0) ps else 12} sets", sSets(s) >= (if (ps > 0) ps else 12)),
            Chal("fast", "Finish in under 60 minutes", s.end != null && elapsed(s, System.currentTimeMillis()) < 3_600_000),
            Chal("pr", "Hit a new PR", stats(s).prs.isNotEmpty()),
            Chal("note", "Add a note to an exercise", s.exNotes.values.any { it.isNotBlank() })
        )
        if (tv > 0) pool += Chal("vol", "Move ${loc(tv)} $unit or more", sVol(s) >= tv)
        return pick(pool, 3, seed)
    }

    // ---------- weekly quests ----------
    fun weekQuests(ws: LocalDate): List<Quest> {
        val wd = weekDates(ws); val a = iso(wd[0]); val b = iso(wd[6])
        val seed = hashS("q$a")
        val planned = plan.count { it.type != "off" }
        val psw = plan.sumOf { if (it.type == "off") 0 else plannedSetsFor(it) }
        val inW = rs.filter { it.date in a..b }
        val pwd = weekDates(wd[0].minusDays(7))
        val prevV = rs.filter { it.date in iso(pwd[0])..iso(pwd[6]) }.sumOf(::sVol)
        val tv = if (prevV > 0) jsRound(prevV * 1.05 / 500) * 500 else 20000L
        val prLifts = HashSet<String>(); inW.forEach { prLifts.addAll(stats(it).prs) }
        val days = inW.map { it.date }.toSet().size
        val sets = inW.sumOf(::sSets); val vol = inW.sumOf(::sVol)
        val fast = inW.count { it.start != null && it.end != null && elapsed(it, 0) < 3_600_000 }
        val planM = HashSet<String>(); plan.forEach { x -> x.groups.forEach { g -> g.forEach { planM += it.m } } }
        val hitM = HashSet<String>(); inW.forEach { hitM.addAll(muscleSets(it).keys) }
        val pool = listOf(
            listOf("pr2", "PR on 2 different lifts", prLifts.size.toDouble(), 2.0),
            listOf("every", "Train every planned day", days.toDouble(), max(1, planned).toDouble()),
            listOf("sets", "Log ${if (psw > 0) psw else 40} sets", sets.toDouble(), (if (psw > 0) psw else 40).toDouble()),
            listOf("vol", "Move ${loc(tv)} $unit", jsRound(vol).toDouble(), tv.toDouble()),
            listOf("fast3", "3 workouts under 60 minutes", fast.toDouble(), 3.0),
            listOf("musc", "Train every muscle in your plan", planM.count { it in hitM }.toDouble(), max(1, planM.size).toDouble())
        )
        return pick(pool, 3, seed).map { q -> val c = q[2] as Double; val n = q[3] as Double; Quest(q[0] as String, q[1] as String, min(c, n), n, c >= n) }
    }

    // ---------- the engine ----------
    val eng: Eng by lazy { buildEngine() }

    private fun buildEngine(): Eng {
        val planned = max(1, plan.count { it.type != "off" })
        val wk = LinkedHashMap<String, MutableList<Session>>()
        rs.forEach { s -> wk.getOrPut(iso(weekStart(toD(s.date)))) { mutableListOf() }.add(s) }
        val tw = iso(weekStart(now))
        val weeks = mutableListOf<String>()
        var w0 = if (rs.isNotEmpty()) weekStart(toD(rs[0].date)) else weekStart(now)
        while (iso(w0) <= tw) { weeks += iso(w0); w0 = w0.plusDays(7) }
        var streak = 0; var best = 0; var freeze = 0; var since = 0; var active = 0
        weeks.forEach { w ->
            val n = (wk[w] ?: emptyList()).map { it.date }.toSet().size
            if (n >= 2) active++
            if (n >= 1) { streak++; since++; best = max(best, streak); if (since >= 4) { since = 0; freeze = min(2, freeze + 1) } }
            else if (w != tw) { if (freeze > 0) freeze-- else { streak = 0; since = 0 } }
        }
        val lifeSets = rs.sumOf(::sSets); val lifeVol = rs.sumOf(::sVol); val lifePR = rs.sumOf { stats(it).prs.size }
        var maxW = 0.0; rs.forEach { s -> s.ex.values.forEach { a -> a.forEach { x -> if (full(x)) maxW = max(maxW, x.w!!) } } }
        val exIds = HashSet<String>(); rs.forEach { s -> s.ex.forEach { (id, a) -> if (a.any(::full)) exIds += id } }
        val noteN = d.sessions.values.sumOf { s -> s.exNotes.values.count { it.isNotBlank() } }
        val planM = HashSet<String>(); plan.forEach { x -> x.groups.forEach { g -> g.forEach { planM += it.m } } }

        // weekly repeatables
        val qd = HashMap<String, Int>()
        val rep = linkedMapOf<String, MutableList<String>>("fullweek" to mutableListOf(), "overload" to mutableListOf(), "prhunt" to mutableListOf(),
            "speed" to mutableListOf(), "dawnwk" to mutableListOf(), "quest" to mutableListOf(), "sweep" to mutableListOf())
        fun wBest(w: String): Map<String, Double> {
            val m = HashMap<String, Double>()
            (wk[w] ?: emptyList()).forEach { s -> s.ex.forEach { (id, a) -> a.forEach { x -> if (full(x)) m[id] = max(m[id] ?: 0.0, e1(x)) } } }
            return m
        }
        var fullRun = 0; var perfectMonth = 0; var balanced = 0; var century = 0
        weeks.forEachIndexed { i, w ->
            val ss = wk[w] ?: emptyList(); val days = ss.map { it.date }.toSet().size
            if (days >= planned) { rep["fullweek"]!! += w; fullRun++; if (fullRun >= 4) perfectMonth = 1 } else if (w != tw) fullRun = 0
            if (i > 0) { val a = wBest(w); val b = wBest(weeks[i - 1]); if (a.keys.count { id -> b[id] != null && a[id]!! > b[id]!! } >= 5) rep["overload"]!! += w }
            if (ss.sumOf { stats(it).prs.size } >= 3) rep["prhunt"]!! += w
            val timed = ss.filter { it.start != null && it.end != null }
            if (timed.size >= 2 && timed.all { elapsed(it, 0) < 3_600_000 }) rep["speed"]!! += w
            if (ss.count { s -> hourOf(s.start)?.let { it < 6 } == true } >= 3) rep["dawnwk"]!! += w
            run { val q = weekQuests(toD(w)); qd[w] = q.count { it.done }; if (q.all { it.done } && ss.isNotEmpty()) rep["quest"]!! += w }
            if (ss.sumOf(::sSets) >= 100) century = 1
            val hit = HashSet<String>(); ss.forEach { hit.addAll(muscleSets(it).keys) }
            if (planM.isNotEmpty() && planM.all { it in hit }) balanced = 1
        }
        val fdays = (rs.map { it.date } + d.flags.keys).toSortedSet()
        fdays.forEach { ds -> if (ds > iso(now)) return@forEach; val c = dayChallenges(toD(ds)); if (c.isNotEmpty() && c.all { it.done }) rep["sweep"]!! += ds }

        // hidden facts
        var comeback = false
        for (i in 1 until rs.size) if (ChronoUnit.DAYS.between(toD(rs[i - 1].date), toD(rs[i].date)) >= 14) comeback = true
        val weekend = weeks.any { w -> val dd = weekDates(toD(w)); val ds = (wk[w] ?: emptyList()).map { it.date }.toSet(); iso(dd[5]) in ds && iso(dd[6]) in ds }
        val perfect = rs.any { s -> var n = 0; var ok = true
            s.ex.forEach { (id, a) -> val e = exById(id); a.filter(::full).forEach { x -> n++; if (e == null || e.hi <= 0 || x.r!! < e.hi) ok = false } }
            ok && n >= 4 }
        val speedrun = rs.any { s -> val p = plan.getOrNull(DAYS.indexOf(s.day))
            s.start != null && s.end != null && elapsed(s, 0) < 1_800_000 && p != null && plannedSetsFor(p) > 0 && sSets(s) >= plannedSetsFor(p) }

        val B = mutableListOf<Bdg>()
        fun T(k: String, name: String, icon: String, cat: String, cur: Double, tgt: Double, desc: String, hid: Boolean = false) =
            B.add(Bdg(k, name, icon, cat, desc, min(cur, tgt), tgt, hid, cur >= tgt))
        listOf(1 to "First Rep", 10 to "Getting Going", 25 to "Regular", 50 to "Committed", 100 to "Centurion", 250 to "Iron Legend").forEach { (n, nm) ->
            T("w$n", nm, "🏋️", "Workouts", rs.size.toDouble(), n.toDouble(), "Log $n workout${if (n > 1) "s" else ""}") }
        listOf(2, 4, 8, 12, 26, 52).forEachIndexed { i, n ->
            T("s$n", listOf("Spark", "On Fire", "Blazing", "Inferno", "Unstoppable", "Eternal Flame")[i], "🔥", "Streaks", best.toDouble(), n.toDouble(), "$n week streak") }
        val vt = if (kg) listOf(25e3, 5e4, 1e5, 25e4, 5e5, 1e6) else listOf(5e4, 1e5, 25e4, 5e5, 1e6, 25e5)
        vt.forEachIndexed { i, n -> T("v$i", listOf("Mover", "Hauler", "Heavy Duty", "Freight Train", "Millionaire", "Mountain Mover")[i], "🏗️", "Volume", lifeVol, n,
            "Move ${if (n >= 1e6) fmtN(n / 1e6) + "M" else fmtN(n / 1e3) + "k"} $unit total") }
        listOf(100, 500, 1000, 2500, 5000).forEachIndexed { i, n -> T("t$n", listOf("Set Starter", "Grinder", "Thousand Club", "Workhorse", "Machine")[i], "✅", "Sets", lifeSets.toDouble(), n.toDouble(), "Log ${loc(n)} sets") }
        listOf(1, 10, 25, 50, 100).forEachIndexed { i, n -> T("p$n", listOf("First PR", "PR Machine", "Record Breaker", "Limit Pusher", "Legend")[i], "🏆", "PRs", lifePR.toDouble(), n.toDouble(), "Hit $n PR${if (n > 1) "s" else ""}") }
        (if (kg) listOf(60, 100, 140, 180) else listOf(135, 225, 315, 405)).forEachIndexed { i, n -> T("c$i", "$n Club", "🥇", "Lift clubs", maxW, n.toDouble(), "Lift $n $unit on any exercise") }
        T("pm", "Perfect Month", "🗓️", "Consistency", perfectMonth.toDouble(), 1.0, "4 full weeks in a row")
        T("nt", "Note Taker", "📝", "Consistency", noteN.toDouble(), 10.0, "Write 10 exercise notes")
        T("ex", "Explorer", "🧭", "Consistency", exIds.size.toDouble(), 20.0, "Log 20 different exercises")
        T("bal", "Balanced", "⚖️", "Consistency", balanced.toDouble(), 1.0, "Train every muscle in your plan in one week")
        listOf(
            listOf("dawn", "Dawn Patrol", "🌅", "Start a workout before 6 AM", rs.any { s -> hourOf(s.start)?.let { it < 6 } == true }),
            listOf("owl", "Night Owl", "🌙", "Start a workout after 10 PM", rs.any { s -> hourOf(s.start)?.let { it >= 22 } == true }),
            listOf("back", "Comeback", "🔁", "Return after 2+ weeks off", comeback),
            listOf("wknd", "Weekend Warrior", "🛡️", "Train Saturday and Sunday", weekend),
            listOf("hol", "No Days Off", "🦃", "Train on a holiday", rs.any { isHoliday(toD(it.date)) }),
            listOf("mara", "Marathon", "⏳", "A workout over 2 hours", rs.any { it.start != null && it.end != null && elapsed(it, 0) >= 7_200_000 }),
            listOf("spd", "Speedrun", "⚡", "Finish a full plan day in under 30 min", speedrun),
            listOf("cent", "Century", "💯", "100 sets in one week", century == 1),
            listOf("perf", "Perfectionist", "🎯", "Top of the rep range on every set (4+ sets)", perfect)
        ).forEach { h -> T(h[0] as String, h[1] as String, h[2] as String, "Hidden", if (h[4] as Boolean) 1.0 else 0.0, 1.0, h[3] as String, true) }
        // 0.14: funny badges (hidden until earned)
        funnyBadges(weeks, wk).forEach { f -> T(f[0] as String, f[1] as String, f[2] as String, "Funny", if (f[4] as Boolean) 1.0 else 0.0, 1.0, f[3] as String, true) }
        val R = listOf(
            listOf("fullweek", "Full Week", "🔥", "Train every planned day in a week"), listOf("overload", "Overloader", "📈", "Beat last week on 5+ lifts"),
            listOf("prhunt", "PR Hunter", "🏹", "3+ PRs in a week"), listOf("speed", "Speed Demon", "⚡", "Every timed workout under an hour (2+)"),
            listOf("dawnwk", "Early Bird", "🐓", "3 workouts before 6 AM in a week"), listOf("quest", "Quest Master", "🗺️", "Finish all 3 weekly quests"),
            listOf("sweep", "Daily Sweep", "🧹", "Finish all 3 daily challenges")
        ).map { (k, name, icon, desc) -> Bdg(k, name, icon, "Weekly", desc, count = rep[k]!!.size, list = rep[k]!!) }
        B.addAll(crewBadges()); B.addAll(crewExtraBadges())
        fun tierOf(c: String) = B.count { it.cat == c && it.earned }
        fun hasK(k: String) = B.any { it.k == k && it.earned }
        listOf(
            listOf("col_club", "Club Collector", "🏛️", "Earn all 4 lift clubs", listOf("c0", "c1", "c2", "c3").all(::hasK)),
            listOf("col_clock", "Around the Clock", "🕛", "Earn Dawn Patrol and Night Owl", hasK("dawn") && hasK("owl")),
            listOf("col_secret", "Secret Keeper", "🗝️", "Find every hidden badge", B.filter { it.hid && it.cat == "Hidden" }.all { it.earned }),
            listOf("col_week", "Weekly Grinder", "📆", "Earn every weekly badge once (Early Bird optional)", R.filter { it.k != "dawnwk" }.all { (it.count ?: 0) > 0 }),
            listOf("col_ms", "Milestone Master", "👑", "Reach Gold in every milestone", LADDER.keys.all { tierOf(it) >= 3 })
        ).forEach { c -> T(c[0] as String, c[1] as String, c[2] as String, "Collections", if (c[4] as Boolean) 1.0 else 0.0, 1.0, c[3] as String) }
        val bcount = B.count { it.earned } + R.sumOf { it.count ?: 0 }
        var elig = 0; RANKS.forEachIndexed { i, r -> if (active >= r.weeks && bcount >= r.badges) elig = i }
        val rank = st.held?.coerceIn(0, 7) ?: elig
        val twd = weekDates(now); val tws = rs.filter { it.date in iso(twd[0])..iso(twd[6]) }; val pwd = weekDates(twd[0].minusDays(7))
        fun bestIn(list: List<Session>): Map<String, Double> { val m = HashMap<String, Double>(); list.forEach { s -> s.ex.forEach { (id, a) -> a.forEach { x -> if (full(x)) m[id] = max(m[id] ?: 0.0, e1(x)) } } }; return m }
        val wbm = bestIn(tws); val lbm = bestIn(rs.filter { it.date in iso(pwd[0])..iso(pwd[6]) })
        val beat = wbm.keys.count { id -> lbm[id] != null && wbm[id]!! > lbm[id]!! }
        val tdays = tws.map { it.date }.toSet().size; val need = max(2, planned - 1)
        val trial = if (elig > rank && rank < 7) Trial(rank + 1, tdays, need, beat, 3, tdays >= need && beat >= 3) else null
        var div = 0; var frac = 1.0
        if (rank < 7) {
            val c = RANKS[rank]; val n = RANKS[rank + 1]
            fun f(v: Int, a: Int, b: Int) = if (b > a) (v - a).toDouble() / (b - a) else 1.0
            frac = if (elig > rank) 1.0 else max(0.0, min(1.0, min(f(active, c.weeks, n.weeks), f(bcount, c.badges, n.badges))))
            div = if (frac < 1.0 / 3) 3 else if (frac < 2.0 / 3) 2 else 1
        }
        var prestige = 0
        if (rank == 7) while (active >= 52 + 13 * (prestige + 1) && bcount >= 60 + 15 * (prestige + 1)) prestige++
        val rname = RANKS[rank].name + if (rank < 7) " " + ROMAN[div] else if (prestige > 0) " " + "★".repeat(min(prestige, 5)) else ""
        val power = jsRound(rank * 120.0 + (if (rank < 7) (3 - div) * 40 else 0) + prestige * 100 + active * 6 + bcount * 3 + lifePR * 2 + streak * 4).toInt()
        val season = seasonOf(now); val (s0, s1) = seasonRange(season); val sw0 = iso(weekStart(s0)); val s0s = iso(s0); val s1s = iso(s1)
        fun inS(ds: String) = ds >= s0s && ds <= s1s && ds <= iso(now)
        var xp = 0
        (rs.map { it.date } + d.flags.keys).filter(::inS).toSet().forEach { ds -> val n = dayChallenges(toD(ds)).count { it.done }; xp += n * 10 + if (n == 3) 20 else 0 }
        rs.forEach { s -> if (inS(s.date)) xp += 15 + stats(s).prs.size * 5 }
        qd.forEach { (w, n) -> if (w >= sw0 && w <= s1s) xp += n * 30 }
        R.forEach { r -> r.list.forEach { x -> if (x >= sw0 && x <= s1s) xp += 25 } }
        xp += (cs?.buddyDates ?: emptyList()).count(::inS) * 50
        val lvl = min(30, 1 + xp / 200)
        val keys = B.filter { it.earned }.map { it.k }.toMutableSet().apply { R.forEach { r -> r.list.forEach { x -> add(r.k + "@" + x) } } }
        return Eng(B, R, bcount, active, rank, elig, trial, div, frac, prestige, rname, power, season, xp, lvl, streak, best, freeze, keys,
            lifeSets, lifeVol, lifePR, rs.size, rs.firstOrNull()?.date, wk.mapValues { (_, v) -> v.map { it.date } })
    }

    // ---------- funny badges (port of the web app's funnyBadges) ----------
    private fun funnyBadges(weeks: List<String>, wk: Map<String, List<Session>>): List<List<Any>> {
        val legM = Regex("Quads|Hamstrings|Glutes|Calves")
        fun legSets(s: Session) = s.ex.entries.sumOf { (id, a) -> if (legM.containsMatchIn(exMuscle(id))) a.count(::full) else 0 }
        var run = 0; var dodger = false
        weeks.forEachIndexed { i, w -> if (i == weeks.size - 1) return@forEachIndexed
            val ss = wk[w] ?: emptyList(); val days = ss.map { it.date }.toSet().size
            if (days >= 2 && ss.all { legSets(it) == 0 }) { run++; if (run >= 3) dodger = true } else run = 0 }
        var ego = false; val lastW = HashMap<String, Pair<Double, String>>()
        rs.forEach { s -> s.ex.forEach { (id, a) -> val m = a.filter(::full).maxOfOrNull { it.w!! } ?: 0.0; if (m <= 0) return@forEach
            val p = lastW[id]; if (p != null && m - p.first >= (if (kg) 9.0 else 20.0) && ChronoUnit.DAYS.between(toD(p.second), toD(s.date)) <= 10) ego = true
            lastW[id] = m to s.date } }
        var cardio = false
        weeks.forEachIndexed { i, w -> if (i == 0) return@forEachIndexed
            val a = (wk[weeks[i - 1]] ?: emptyList()).map { it.date }.toSet().size; val b = (wk[w] ?: emptyList()).map { it.date }.toSet().size
            if (a == 0 && b >= 5) cardio = true }
        var rest = false; for (i in 1 until rs.size) if (ChronoUnit.DAYS.between(toD(rs[i - 1].date), toD(rs[i].date)) >= 8) rest = true
        return listOf(
            listOf("f_legs", "Leg Day Dodger", "🦵", "Skip legs 3 weeks in a row (we see you)", dodger),
            listOf("f_ego", "Ego Lifter", "🦚", "Jump 20+ lb on a lift in a week", ego),
            listOf("f_ghost", "Ghost", "👻", "Open the app 5 times in a day without logging a set", (st.fb["ghost"] ?: 0) > 0),
            listOf("f_night", "Night Shift", "🌃", "Start a workout after midnight", rs.any { s -> hourOf(s.start)?.let { it < 4 } == true }),
            listOf("f_cardio", "Cardio Is Hard", "🫠", "Zero workouts one week, then 5+ the next", cardio),
            listOf("f_rest", "Rest Day Champion", "🛋", "Take 7 rest days in a row", rest),
            listOf("f_rebel", "Rest Timer Rebel", "⏭", "Skip the rest timer 10 times in one workout", rs.any { it.skips >= 10 })
        )
    }

    // ---------- crew badges ----------
    private fun crewBadges(): List<Bdg> {
        val cb = st.crewB; val mem = cs?.members ?: emptyList(); val out = mutableListOf<Bdg>()
        fun T(k: String, name: String, icon: String, cur: Double, tgt: Double, desc: String) =
            out.add(Bdg(k, name, icon, "Crew", desc, if (cb[k] != null) tgt else min(cur, tgt), tgt, false, cb[k] != null || cur >= tgt))
        val lifeV = mem.sumOf { m -> m.profile?.let { toMine(it.lifeVol, it.unit, unit) } ?: 0.0 }
        T("squad", "Full Squad", "🫂", 0.0, 1.0, "Everyone in your crew trains on the same day")
        T("cmil", "Crew Million", "🌋", jsRound(lifeV).toDouble(), if (kg) 450000.0 else 1000000.0, "Move ${if (kg) "450k kg" else "1M lb"} as a crew")
        T("hype", "Hype Man", "📣", st.reactN.toDouble(), 50.0, "React to 50 crew posts")
        T("rival", "Rival", "⚔️", 0.0, 1.0, "Beat your rival over a full week")
        T("recruit", "Recruiter", "🤝", if (cs != null && cs.crew.owner == myId) (mem.size - 1).toDouble() else 0.0, 1.0, "Start a crew and get someone to join")
        T("team", "Team Player", "🏁", 0.0, 1.0, "Finish a crew challenge")
        return out
    }
    private fun crewExtraBadges(): List<Bdg> {
        val out = mutableListOf<Bdg>(); val n = cs?.buddyDates?.size ?: 0
        listOf(1 to "Gym Buddies", 5 to "Spotter", 15 to "Partners in Iron").forEach { (t, nm) ->
            out += Bdg("buddy$t", nm, "👯", "Crew", "Train at the same time as a crew member $t time${if (t > 1) "s" else ""}", min(n, t).toDouble(), t.toDouble(), false, n >= t) }
        val aw = st.crewB.keys.count { it.startsWith("award_") }
        out += Bdg("awards", "Crew Award", "🎖️", "Crew", "Win a monthly crew award", min(aw, 1).toDouble(), 1.0, false, aw > 0)
        st.chainsDone.forEachIndexed { i, nm -> out += Bdg("qc$i", nm, "🗺️", "Quests", "Finished a quest chain", 1.0, 1.0, false, true) }
        val gd = st.goals.count { it.doneAt != null }
        listOf(1 to "Goal Getter", 3 to "Goal Crusher", 10 to "Target Locked").forEach { (t, nm) ->
            out += Bdg("goal$t", nm, "🎯", "Quests", "Crush $t goal${if (t > 1) "s" else ""}", min(gd, t).toDouble(), t.toDouble(), false, gd >= t) }
        return out
    }

    /** Rarity label for a badge: crew-based when you have a crew, otherwise by type. */
    fun rarity(b: Bdg): Pair<String, String> {
        val mem = cs?.members?.filter { it.profile?.keys?.isNotEmpty() == true } ?: emptyList()
        if (cs != null && cs.members.size > 1 && b.cat != "Crew" && mem.size > 1) {
            val n = mem.count { it.profile!!.keys.contains(b.k) }; val f = n.toDouble() / mem.size
            return (if (f <= .25) "Legendary" else if (f <= .5) "Epic" else if (f <= .75) "Rare" else "Common") to "$n of ${mem.size} in your crew"
        }
        if (b.cat == "Collections") return "Legendary" to ""
        if (b.hid) return "Epic" to ""
        if (b.cat == "Lift clubs") return listOf("Common", "Rare", "Epic", "Legendary")[b.k.drop(1).toIntOrNull()?.coerceIn(0, 3) ?: 0] to ""
        if (b.cat == "Crew") return "Rare" to ""
        val l = eng.B.filter { it.cat == b.cat }.indexOfFirst { it.k == b.k }
        return (if (l <= 1) "Common" else if (l <= 2) "Rare" else if (l <= 4) "Epic" else "Legendary") to ""
    }

    // ---------- cosmetics ----------
    fun cosOwn(t: String, k: String) = when (t) { "plate" -> st.cos.plate; "aura" -> st.cos.aura; "frame" -> st.cos.frame; else -> st.cos.tag }.contains(k)
    fun plateLocked(c: String): String {
        SEASON_PLATES[c]?.let { return if (cosOwn("plate", c)) "" else "Season pass level $it" }
        PLATE_LOCK[c]?.let { if (eng.rank < it) return "${RANKS[it].name} rank" }
        return ""
    }
    fun frameOK(k: String) = FRAMES[k]?.let { f -> f.test?.invoke(eng) ?: cosOwn("frame", k) } ?: false
    fun auraOK(k: String) = AURAS[k]?.let { a -> a.test?.invoke(eng) ?: cosOwn("aura", k) } ?: false

    fun feat(k: String) = st.unl.contains(k)

    // ---------- muscle mastery ----------
    fun mastery(): List<Mastery> {
        val tot = HashMap<String, Int>(); rs.forEach { s -> muscleSets(s).forEach { (k, n) -> tot[k] = (tot[k] ?: 0) + n } }
        return tot.map { (k, n) -> val t = MASTER.count { n >= it }; Mastery(k, n, t, MASTER.getOrNull(t)) }.sortedByDescending { it.n }
    }

    // ---------- lift ranks ----------
    fun liftRanks(): List<LiftRank> {
        val best = HashMap<String, LiftRank>()
        rs.forEach { s -> s.ex.forEach { (id, a) ->
            val n = exName(id)
            if (Regex("leg curl|hamstring curl", RegexOption.IGNORE_CASE).containsMatchIn(n)) return@forEach
            val l = LIFTS.firstOrNull { it.re.containsMatchIn(n) } ?: return@forEach
            a.forEach { x -> if (full(x)) { val v = e1(x); if (best[l.k] == null || v > best[l.k]!!.e) best[l.k] = LiftRank(l.k, l.name, v, x.w!!, x.r!!, n, 0.0, 0, 0) } }
        } }
        val bw = st.bw
        return LIFTS.filter { best[it.k] != null }.map { l ->
            val b = best[l.k]!!; val ratio = if (bw > 0) b.e / bw else 0.0; val t = if (bw > 0) l.std.count { ratio >= it } else 0
            val nx = l.std.getOrNull(t)
            b.copy(name = l.name, ratio = ratio, t = t, nextW = if (nx != null && bw > 0) (ceil(nx * bw / (1 + 1.0 / 30) / 5) * 5).toInt() else 0)
        }
    }

    // ---------- quest chains ----------
    private fun miles() = if (kg) listOf(40, 60, 80, 100, 120, 140, 160, 180, 200, 220, 240, 260) else listOf(95, 135, 185, 225, 275, 315, 365, 405, 455, 495, 545, 585, 635)
    private fun chainSteps(m: Int): List<Pair<Int, Int>> { val big = if (kg) 10 else 20; val small = if (kg) 5 else 10; return listOf(m - big to 5, m - small to 3, m to 1) }
    /** Returns the chains to show, the updated chain map and names of chains just finished. */
    fun chains(): Triple<List<ChainView>, Map<String, Chain>, List<String>> {
        val cnt = HashMap<String, Int>(); val bestW = HashMap<String, Double>()
        rs.forEach { s -> s.ex.forEach { (id, a) -> val v = a.filter(::full); if (v.isEmpty()) return@forEach; cnt[id] = (cnt[id] ?: 0) + 1; v.forEach { x -> bestW[id] = max(bestW[id] ?: 0.0, x.w!!) } } }
        val c = st.chains.toMutableMap()
        cnt.keys.filter { id -> val n = exName(id); LIFTS.any { it.k != "curl" && it.re.containsMatchIn(n) } && !Regex("leg curl|raise|fly|extension", RegexOption.IGNORE_CASE).containsMatchIn(n) }
            .sortedByDescending { cnt[it] }.take(3).forEach { id ->
                if (c[id] != null) return@forEach
                val m = miles().firstOrNull { it > bestW[id]!! && it - bestW[id]!! <= (if (kg) 40 else 90) }
                if (m != null) c[id] = Chain(m, iso(now))
            }
        val out = mutableListOf<ChainView>()
        c.forEach { (id, ch) ->
            if (cnt[id] == null) return@forEach
            val steps = chainSteps(ch.m).map { (w, r) -> ChainStep(w, r, rs.any { s -> s.ex[id]?.any { x -> full(x) && x.w!! >= w && x.r!! >= r } == true }) }
            out += ChainView(id, exName(id), ch.m, steps, steps.all { it.done })
        }
        val finished = mutableListOf<String>()
        out.filter { it.done }.forEach { v -> finished += "Road to ${v.m} · ${v.name}"; c.remove(v.id) }
        return Triple(out.filter { !it.done }, c, finished)
    }

    // ---------- goals ----------
    fun goalInfo(g: Goal): GoalInfo {
        val t = g.w * (1 + 1.0 / 30); var cur = 0.0; var done: String? = null; val wkBest = java.util.TreeMap<String, Double>()
        rs.forEach { s -> (s.ex[g.ex] ?: emptyList()).forEach { x ->
            if (!full(x)) return@forEach
            val v = e1(x); cur = max(cur, v); val w = iso(weekStart(toD(s.date))); wkBest[w] = max(wkBest[w] ?: 0.0, v)
            if (done == null && s.date >= g.created && x.w!! >= g.w) done = s.date
        } }
        val pts = wkBest.entries.toList().takeLast(8).map { (w, v) -> ChronoUnit.DAYS.between(now, toD(w)) / 7.0 to v }
        var slope = 0.0
        if (pts.size >= 3) {
            val n = pts.size; val mx = pts.sumOf { it.first } / n; val my = pts.sumOf { it.second } / n
            val num = pts.sumOf { (it.first - mx) * (it.second - my) }; val den = pts.sumOf { (it.first - mx) * (it.first - mx) }
            slope = if (den != 0.0) num / den else 0.0
        }
        var proj: String? = null
        if (done == null && cur < t && slope > 0) proj = iso(now.plusDays((ceil((t - cur) / slope) * 7).toLong()))
        else if (cur >= t && done == null) proj = iso(now)
        return GoalInfo(cur, t, done, proj, min(100, jsRound(cur / t * 100).toInt()),
            if (done != null) "done" else if (proj != null) (if (proj <= g.date) "on" else "behind") else "flat")
    }

    // ---------- post-workout breakdown ----------
    fun volCompare(v: Double): Pair<String, String> {
        val lb = if (kg) v * 2.2046 else v
        var o = OBJS[0]; for (x in OBJS) if (lb >= x.first) o = x
        val n = lb / o.first; val r = jsRound(n)
        val what = o.second.replace(Regex("^an? "), "")
        return o.third to "You moved ${loc(v)} $unit, " + if (n >= 1.8) "about ${r}× $what${if (r > 1) "s" else ""}" else "about the weight of ${o.second}"
    }
    fun funStats(s: Session): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>(); val ss = stats(s); val p = plan.getOrNull(DAYS.indexOf(s.day))
        val dn = DN[DAYS.indexOf(s.day).coerceAtLeast(0)]
        if (ss.vol > 0) out += volCompare(ss.vol)
        if (ss.prs.isNotEmpty()) { val id = ss.prs[0]; val b = (s.ex[id] ?: emptyList()).filter(::full).maxByOrNull(::e1)
            if (b != null) out += "🏆" to "Heaviest set ever on ${exName(id)}: ${fmtN(b.w!!)} × ${b.r}" }
        val prev = rs.lastOrNull { it.day == s.day && it.date < s.date }
        if (prev != null) {
            val dv = ss.vol - sVol(prev); if (dv > 0) out += "📈" to "You beat last $dn by +${loc(dv)} $unit"
            val ds = sSets(s) - sSets(prev); if (dv <= 0 && ds > 0) out += "➕" to "$ds more set${if (ds > 1) "s" else ""} than last $dn"
        }
        if (s.start != null && s.end != null) {
            val mine = elapsed(s, 0); val others = rs.filter { it.day == s.day && sid(it.date, it.day) != sid(s.date, s.day) && it.start != null && it.end != null }.map { elapsed(it, 0) }
            if (others.isNotEmpty() && mine < others.min()) out += "⚡" to "Fastest ${p?.name ?: "workout"} yet, ${max(1, jsRound((others.min() - mine) / 6e4).toInt())} min quicker"
        }
        val n = rs.count { it.date <= s.date }; val next = listOf(10, 25, 50, 100, 250, 500).firstOrNull { it > n }
        out += "🗓️" to "Workout #$n logged" + if (next != null && next - n <= 3) " · ${next - n} more to hit $next" else ""
        val life = rs.sumOf(::sVol); if (life > ss.vol) out += "🌍" to "Lifetime total: ${lifeFmt(life)} $unit"
        return out.take(4)
    }

    // ---------- weekly report card ----------
    fun weekReport(): Report? {
        val tw = weekStart(now); val pw = weekDates(tw.minusDays(7)); val a = weekAgg(d, pw)
        if (a.sets == 0) return null
        val b = weekAgg(d, weekDates(pw[0].minusDays(7))); val up = a.vol > b.vol; val ratio = a.done.toDouble() / max(1, a.planned)
        val g = if (ratio >= 1 && a.prs >= 1 && up) "A+" else if (ratio >= 1) "A" else if (ratio >= .6 || a.prs >= 3) "B" else "C"
        return Report(g, a, b, pw, eng.R.filter { it.list.contains(iso(pw[0])) }.map { it.icon + " " + it.name })
    }

    // ---------- year in review ----------
    fun yearData(y: Int): YearData? {
        val ys = rs.filter { it.date.startsWith("$y") }; if (ys.isEmpty()) return null
        val m = HashMap<String, Int>(); ys.forEach { s -> muscleSets(s).forEach { (k, n) -> m[k] = (m[k] ?: 0) + n } }
        val mk = m.keys.sortedByDescending { m[it] }
        val l = HashMap<String, Int>(); mk.forEachIndexed { i, k -> MKEY[k]?.forEach { x -> l[x] = max(l[x] ?: 0, if (i < 3) 2 else 1) } }
        var best: Triple<String, String, Double>? = null
        ys.forEach { s -> s.ex.forEach { (id, a) -> a.forEach { x -> if (full(x) && (best == null || e1(x) > best!!.third)) best = Triple(exName(id), "${fmtN(x.w!!)}×${x.r}", e1(x)) } } }
        val dow = IntArray(7); ys.forEach { dow[dayIdx(toD(it.date))]++ }
        val mins = ys.filter { it.start != null && it.end != null }.sumOf { elapsed(it, 0) / 6e4 }
        return YearData(y, ys.size, ys.map { it.date }.toSet().size, ys.sumOf(::sVol), ys.sumOf(::sSets), ys.sumOf { stats(it).prs.size }, mk, l, best,
            dow.indices.maxByOrNull { dow[it] } ?: 0, ys.count { hourOf(it.start)?.let { h -> h < 12 } == true }, ys.count { hourOf(it.start)?.let { h -> h >= 12 } == true }, (mins / 60).roundToInt())
    }
}

// ============================ COSMETIC DEFINITIONS ============================
class Cos(val label: String, val how: String, val test: ((Eng) -> Boolean)? = null)
fun hasB(e: Eng, k: String) = e.B.any { it.k == k && it.earned }
val FRAMES = linkedMapOf(
    "bronze" to Cos("Bronze ring", "Reach Bronze") { it.rank >= 1 }, "silver" to Cos("Silver ring", "Reach Silver") { it.rank >= 2 },
    "gold" to Cos("Gold ring", "Reach Gold") { it.rank >= 3 }, "plat" to Cos("Platinum ring", "Reach Platinum") { it.rank >= 4 },
    "diamond" to Cos("Diamond ring", "Reach Diamond") { it.rank >= 5 }, "titan" to Cos("Titan ring", "Reach Titan") { it.rank >= 6 },
    "olymp" to Cos("Olympian ring", "Reach Olympian") { it.rank >= 7 }, "iron" to Cos("Iron Club", "Club Collector badge") { hasB(it, "col_club") },
    "shadow" to Cos("Shadow", "Secret Keeper badge") { hasB(it, "col_secret") }, "neon" to Cos("Neon", "Season pass level 6"),
    "circuit" to Cos("Circuit", "Season pass level 14"), "laurel" to Cos("Laurel", "Season pass level 20"), "crown" to Cos("Crown", "Season pass level 28")
)
val AURAS = linkedMapOf(
    "glow" to Cos("Rank glow", "Reach Diamond") { it.rank >= 5 }, "spark" to Cos("Spark", "Season pass level 4"),
    "frost" to Cos("Frost", "Season pass level 12"), "ember" to Cos("Ember", "Season pass level 18"),
    "prism" to Cos("Prism", "Season pass level 24"), "gold" to Cos("Season gold", "Season pass level 30")
)
