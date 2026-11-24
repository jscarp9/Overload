package com.overload.app

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

val DAYS = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
val DN = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val MON = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

typealias Sessions = Map<String, Session>

fun dayIdx(d: LocalDate) = d.dayOfWeek.value - 1
fun sid(date: String, day: String) = "${date}_$day"
fun md(d: LocalDate) = "${d.monthValue}/${d.dayOfMonth}"
fun mdS(s: String) = md(LocalDate.parse(s))
fun fmtN(w: Double): String = if (w % 1.0 == 0.0) w.toLong().toString() else w.toString()

fun e1(x: SetEntry) = (x.w ?: 0.0) * (1 + (x.r ?: 0) / 30.0)
fun full(x: SetEntry) = (x.w ?: 0.0) > 0 && (x.r ?: 0) > 0
fun has(a: List<SetEntry>?) = a?.any { (it.w ?: 0.0) > 0 || (it.r ?: 0) > 0 } == true
fun better(a: SetEntry?, b: SetEntry?) =
    a != null && full(a) && b != null && ((a.w!! > (b.w ?: 0.0)) || (a.w == b.w && a.r!! > (b.r ?: 0)))

fun fmtSets(a: List<SetEntry>?) = (a ?: emptyList()).filter(::full).joinToString(", ") { "${fmtN(it.w!!)}×${it.r}" }
fun kfmt(n: Double): String = if (n >= 10000) String.format("%.1fk", n / 1000) else String.format("%,d", n.roundToLong())

fun mins(ms0: Long): String {
    val ms = max(0L, ms0); val m = ms / 60000; val s = ms % 60000 / 1000
    return if (m >= 60) "${m / 60}h ${m % 60}m" else "$m:${s.toString().padStart(2, '0')}"
}

fun elapsed(s: Session, now: Long): Long {
    val st = s.start ?: return 0
    val end = s.end ?: (s.pauseAt ?: now)
    return end - st - s.paused
}

fun sortedDesc(ss: Sessions) = ss.values.sortedByDescending { it.date }

fun lastSession(ss: Sessions, id: String, curKey: String, curDate: String): Session? =
    sortedDesc(ss).firstOrNull { sid(it.date, it.day) != curKey && it.date <= curDate && has(it.ex[id]) }

fun lastNote(ss: Sessions, id: String, curKey: String, curDate: String): String =
    sortedDesc(ss).firstOrNull {
        sid(it.date, it.day) != curKey && it.date <= curDate && !it.exNotes[id].isNullOrBlank()
    }?.exNotes?.get(id)?.trim() ?: ""

fun bestBefore(ss: Sessions, id: String, date: String): Double =
    ss.values.filter { it.date < date }.flatMap { it.ex[id] ?: emptyList() }.filter(::full).maxOfOrNull(::e1) ?: 0.0

data class Sug(val w: Double, val r: Int)

fun inc(w: Double, unit: String) = if (unit == "kg") (if (w >= 40) 2.5 else 1.0) else (if (w >= 60) 5.0 else 2.5)

/** Double progression: hit the top of the rep range on every top-weight set -> add weight, else add a rep. */
fun suggest(e: Exercise, last: Session?, unit: String): Sug? {
    val a = (last?.ex?.get(e.id) ?: return null).filter(::full)
    if (a.isEmpty()) return null
    val top = a.reduce { m, x -> if (x.w!! > m.w!! || (x.w == m.w && x.r!! > m.r!!)) x else m }
    val atTop = a.filter { it.w == top.w }
    if (e.hi > 0 && atTop.all { it.r!! >= e.hi }) return Sug(top.w!! + inc(top.w!!, unit), if (e.lo > 0) e.lo else top.r!!)
    val minR = atTop.minOf { it.r!! }
    return Sug(top.w!!, if (e.hi > 0) min(e.hi, minR + 1) else minR + 1)
}

data class Pt(val s: Session, val e: Int, val b: SetEntry, val vol: Double)

fun points(ss: Sessions, id: String): List<Pt> = ss.values.sortedBy { it.date }.mapNotNull { s ->
    val v = (s.ex[id] ?: emptyList()).filter(::full)
    if (v.isEmpty()) null else {
        val b = v.maxByOrNull(::e1)!!
        Pt(s, e1(b).roundToInt(), b, v.sumOf { it.w!! * it.r!! })
    }
}

fun stalled(ss: Sessions, id: String, date: String): Boolean {
    val pts = points(ss, id).filter { it.s.date < date }
    if (pts.size < 4) return false
    val base = pts[pts.size - 4].e
    return pts.takeLast(3).all { it.e <= base }
}

data class SS(val sets: Int, val vol: Double, val prs: List<String>)

fun sessStats(ss: Sessions, s: Session): SS {
    var sets = 0; var vol = 0.0; val prs = mutableListOf<String>()
    s.ex.forEach { (id, a) ->
        val pb = bestBefore(ss, id, s.date); var hit = false
        a.forEach { x -> if (full(x)) { sets++; vol += x.w!! * x.r!!; if (pb > 0 && e1(x) > pb) hit = true } }
        if (hit) prs += id
    }
    return SS(sets, vol, prs)
}

fun weekDates(base: LocalDate): List<LocalDate> {
    val mon = base.minusDays(dayIdx(base).toLong())
    return (0..6).map { mon.plusDays(it.toLong()) }
}

fun doneOn(ss: Sessions, date: LocalDate, k: String) = ss["${date}_$k"]?.ex?.values?.any { has(it) } == true

data class WA(val done: Int, val planned: Int, val sets: Int, val vol: Double, val prs: Int, val avg: Long)

fun weekAgg(d: AppData, wd: List<LocalDate>): WA {
    val plan = d.plan ?: return WA(0, 0, 0, 0.0, 0, 0)
    var done = 0; var planned = 0
    plan.forEachIndexed { i, p -> if (p.type != "off") { planned++; if (doneOn(d.sessions, wd[i], DAYS[i])) done++ } }
    var sets = 0; var vol = 0.0; var prs = 0; var dur = 0L; var dn = 0
    val a = wd[0].toString(); val b = wd[6].toString()
    d.sessions.values.forEach { s ->
        if (s.date in a..b) {
            val ss = sessStats(d.sessions, s); sets += ss.sets; vol += ss.vol; prs += ss.prs.size
            val el = elapsed(s, 0)
            if (s.start != null && s.end != null && el > 0) { dur += el; dn++ }
        }
    }
    return WA(done, planned, sets, vol, prs, if (dn > 0) dur / dn else 0L)
}

fun weekStreak(d: AppData, today: LocalDate): Int {
    var streak = 0
    for (i in 0 until 104) {
        val ws = weekDates(weekDates(today)[0].minusWeeks(i.toLong()))
        val any = d.sessions.values.any { it.date in ws[0].toString()..ws[6].toString() && it.ex.values.any(::has) }
        if (any) streak++ else if (i > 0) break
    }
    return streak
}

// ---- plate calculator + warm-ups ----
class PlateDef(val w: Double, val color: Long, val h: Float)

fun plateDefs(unit: String) = if (unit == "kg") listOf(
    PlateDef(25.0, 0xFFEF2B36, 1f), PlateDef(20.0, 0xFF2563FF, 1f), PlateDef(15.0, 0xFFFFBE0B, .9f),
    PlateDef(10.0, 0xFF16B65B, .8f), PlateDef(5.0, 0xFFE8E8EC, .58f), PlateDef(2.5, 0xFF23252B, .46f),
    PlateDef(1.25, 0xFF9AA0AE, .38f)
) else listOf(
    PlateDef(45.0, 0xFFEF2B36, 1f), PlateDef(35.0, 0xFFFFBE0B, .9f), PlateDef(25.0, 0xFF16B65B, .78f),
    PlateDef(10.0, 0xFFE8E8EC, .58f), PlateDef(5.0, 0xFF2563FF, .46f), PlateDef(2.5, 0xFF23252B, .38f)
)

/** Plates per side (heaviest first) and what is left over per side. */
fun plateCalc(total: Double, bar: Double, unit: String): Pair<List<PlateDef>, Double> {
    var per = (total - bar) / 2
    val out = mutableListOf<PlateDef>()
    if (per < 0) return out to per
    for (p in plateDefs(unit)) while (per >= p.w - 1e-9) { out += p; per = Math.round((per - p.w) * 1000) / 1000.0 }
    return out to per
}

fun r5(w: Double, unit: String) = if (unit == "kg") (w / 2.5).roundToInt() * 2.5 else (w / 5).roundToInt() * 5.0

fun warmRows(t: Double, unit: String): List<Pair<Double, Int>> {
    val minW = if (unit == "kg") 20.0 else 45.0
    val rows = listOf(.4 to 10, .6 to 6, .75 to 3, .85 to 1).map { (p, r) -> max(r5(t * p, unit), minW) to r }
    return rows.filterIndexed { i, v -> v.first < t && (i == 0 || v.first != rows[i - 1].first) }
}
