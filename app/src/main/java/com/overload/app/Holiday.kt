package com.overload.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * 0.19 Holiday Drops: lift on a holiday and unlock a whole matching set: an animated plate with the
 * year, a profile banner, a name effect, a crew reaction, a set-complete burst and a dated medal.
 * When 3 people in a crew lift on the same holiday the crew unlocks a holiday crew banner, and the
 * crew crest gets a seasonal touch during that holiday's season. Keys match the web app.
 */

/** t = the holiday itself, w = its season (for the crest touch). */
class Hol(val k: String, val n: String, val p: String, val e: String, val fx: List<String>, val b: String, val c: String,
          val t: (LocalDate) -> Boolean, val w: (LocalDate) -> Boolean)

fun easterOf(y: Int): LocalDate {
    val a = y % 19; val b = y / 100; val c = y % 100; val d = b / 4; val e = b % 4; val f = (b + 8) / 25; val g = (b - f + 1) / 3
    val h = (19 * a + b - d - g + 15) % 30; val i = c / 4; val k = c % 4; val l = (32 + 2 * e + 2 * i - h - k) % 7; val m = (a + 11 * h + 22 * l) / 451
    return LocalDate.of(y, (h + l - 7 * m + 114) / 31, ((h + l - 7 * m + 114) % 31) + 1)
}
fun isThanksDay(t: LocalDate) = t.monthValue == 11 && t.dayOfWeek == DayOfWeek.THURSDAY && t.dayOfMonth in 22..28
private fun md(t: LocalDate, m: Int, d: Int) = t.monthValue == m && t.dayOfMonth == d

val HOL19: LinkedHashMap<String, Hol> = linkedMapOf(
    "thanks" to Hol("thanks", "Thanksgiving", "h_thanks", "🦃", listOf("🍂", "🍁", "🦃"), "Turkey Day Grind", "#e0903f", ::isThanksDay) { it.monthValue == 11 && it.dayOfMonth >= 15 },
    "xmas" to Hol("xmas", "Christmas", "h_xmas", "🎄", listOf("❄️", "🎄", "⭐"), "Christmas Grind", "#e8424e", { md(it, 12, 25) }) { it.monthValue == 12 && it.dayOfMonth < 31 },
    "newyear" to Hol("newyear", "New Year’s", "h_newyear", "🎆", listOf("🎉", "✨", "🎆"), "New Year Grind", "#e3c25a", { md(it, 1, 1) }) { md(it, 12, 31) || (it.monthValue == 1 && it.dayOfMonth <= 7) },
    "valentine" to Hol("valentine", "Valentine’s", "h_valentine", "💘", listOf("💘", "💗", "💕"), "Valentine Grind", "#ff6fa8", { md(it, 2, 14) }) { it.monthValue == 2 && it.dayOfMonth in 7..14 },
    "stpat" to Hol("stpat", "St. Patrick’s", "h_stpat", "☘️", listOf("☘️", "🍀", "✨"), "Lucky Grind", "#3fbf6a", { md(it, 3, 17) }) { it.monthValue == 3 && it.dayOfMonth in 10..17 },
    "easter" to Hol("easter", "Easter", "h_easter", "🐣", listOf("🐣", "🥚", "🌸"), "Easter Grind", "#f2a8d0", { it == easterOf(it.year) }) { ChronoUnit.DAYS.between(it, easterOf(it.year)) in 0..7 },
    "july4" to Hol("july4", "Fourth of July", "h_july4", "🎇", listOf("🎆", "⭐", "🎇"), "Independence Grind", "#4a6fd6", { md(it, 7, 4) }) { (it.monthValue == 6 && it.dayOfMonth >= 28) || (it.monthValue == 7 && it.dayOfMonth <= 4) },
    "halloween" to Hol("halloween", "Halloween", "h_halloween", "🎃", listOf("🎃", "🦇", "👻"), "Halloween Grind", "#ff8a2a", { md(it, 10, 31) }) { it.monthValue == 10 && it.dayOfMonth >= 15 }
)
val PLATE2HOL: Map<String, String> by lazy { HOL19.values.associate { it.p to it.k } }

/** Years you lifted on this holiday (any saved workout with a filled set on that date). */
fun Ctx.holYears(k: String): List<Int> { val h = HOL19[k] ?: return emptyList(); return rs.map { toD(it.date) }.filter(h.t).map { it.year }.distinct().sorted() }
fun Ctx.holHas(k: String) = holYears(k).isNotEmpty()
fun hexL(h: String): Long = 0xFF000000L or (runCatching { h.removePrefix("#").toLong(16) }.getOrDefault(0x888888L))

/** Did the crew unlock this holiday (3 lifters)? */
fun crewHol(cs: CrewState?, k: String): Boolean {
    val v = cs?.crew?.holU?.get(k) ?: return false
    if (v is JsonNull) return false
    if (v is JsonPrimitive) { if (v.booleanOrNull == false) return false; if (v.content == "0" || v.content.isEmpty()) return false }
    return true
}
/** The crew crest's seasonal touch: 🎅 in December, 🦃 in late November, and so on. */
fun holEmoji(cs: CrewState?): String? {
    val t = today(); val k = HOL19.values.firstOrNull { crewHol(cs, it.k) && it.w(t) } ?: return null
    return if (k.k == "xmas") "🎅" else k.e
}

// ============================ THE CREW HOLIDAY BONUS + THE REVEAL ============================
private var holBusy = false
/** Tells the crew you lifted on a holiday (3 lifters unlock it for everyone). Once per holiday per year. */
fun OverloadViewModel.holCheck() {
    if (!crew.on || crewState.value == null || holBusy) return
    val t = today(); val y = t.year; val h = HOL19.values.firstOrNull { it.t(t) } ?: return
    if (data.value.settings.holSent["${h.k}$y"] != null) return
    if (!ctx().rs.any { it.date == iso(t) }) return
    holBusy = true
    viewModelScopeLaunch {
        val r = crew.call("holiday", buildJsonObject { put("hk", h.k); put("year", y) })
        if (r.err == null) setSettings { it.copy(holSent = it.holSent + ("${h.k}$y" to 1)) }
        holBusy = false
    }
}
/** Shows the "Drop unlocked" screen once per holiday per year. Waits until you're not mid-workout. */
fun OverloadViewModel.holReveal() {
    val s = session(); if (s.start != null && s.end == null) return
    val c = ctx()
    for (h in HOL19.values) { val y = c.holYears(h.k).lastOrNull() ?: continue
        if (c.st.holSeen["${h.k}$y"] == null) { setSettings { it.copy(holSeen = it.holSeen + ("${h.k}$y" to 1)) }; autoSheet(Sheet.Holiday(h.k, y)); return } }
}
fun OverloadViewModel.wearHoliday(k: String) {
    setSettings { it.copy(look = it.look + mapOf("pbanner" to "hb_$k", "nameFx" to "h$k", "setfx" to "h$k")) }
    confetti(120); toast("${HOL19[k]?.e ?: ""} Holiday set on"); closeSheet(); crew.sync(true)
}

@Composable
fun HolidaySheet(vm: OverloadViewModel, k: String, y: Int) {
    val h = HOL19[k] ?: return
    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(20.dp))) { Scene("hb_$k", Modifier.matchParentSize(), 8, "rv$k") }
    Column(Modifier.fillMaxWidth().offset(y = (-48).dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Plate(h.p, "done", Modifier.size(120.dp), label = h.n.uppercase(), year = "$y")
        Title("${h.e} ${h.n} Drop unlocked", 22, modifier = Modifier.padding(top = 6.dp))
        Sub("You lifted on ${h.n} $y. Here’s your set:")
    }
    Column(Modifier.offset(y = (-36).dp)) {
        OvCard {
            Row16("💿", "Animated plate", "Plan → Plate → Holiday") { vm.closeSheet(); vm.setTab("plan") }
            Row16("🖼", "Profile banner", "Edit profile → Banner") { vm.openSheet(Sheet.ProfEdit("banner", direct = true)) }
            Row16("✨", "Name effect", "Customize → Name effect") { vm.openSheet(Sheet.Locker("nameFx")) }
            Row16(h.e, "Crew reaction", "Shows on posts") { vm.openSheet(Sheet.Locker("react")) }
            Row16("💥", "Set complete burst", "Customize → Set complete") { vm.openSheet(Sheet.Locker("setfx")) }
            Row16("🏅", "${h.b} $y", "In your trophy room") { vm.openSheet(Sheet.TrophyRoom(null)) }
        }
        Sub("If 3 people in your crew lift on ${h.n}, the whole crew unlocks a holiday crew banner and a seasonal crest.", Modifier.fillMaxWidth().padding(bottom = 10.dp))
        OvButton("Wear the whole set", { vm.wearHoliday(k) }, bg = Red, fg = Color.White, wide = true)
        OvButton("Later", { vm.closeSheet() }, Modifier.padding(top = 8.dp), wide = true)
    }
}

// ============================ ANIMATED HOLIDAY PLATES ============================
/** Draws the moving bits over a holiday plate, in the plate's 100 x 100 box. t runs 0..1 every 6 seconds. */
fun DrawScope.holPlateFx(k: String, uid: String, t: Float, big: Boolean) {
    fun R(n: String, a: Float, b: Float) = rnd("hx$uid$n", a, b)
    fun ph(d: Float, b: Float) = (((t * 6f) / d + b / d) % 1f + 1f) % 1f
    fun fall(n: Int, draw: DrawScope.(Int) -> Unit) = repeat(n) { i ->
        val x = R("x$i", 14f, 86f); val d = R("d$i", 3f, 6f); val p = ph(d, R("b$i", 0f, 6f)); val dx = R("w$i", -8f, 8f) * p
        translate(x + dx, 6f + 90f * p) { draw(i) }
    }
    fun burst(x: Float, y: Float, c: String, d: Float, b: Float) { val p = ph(d, b); val s = 1.3f * p.coerceAtMost(1f); val a = if (p < .5f) p * 2 else (1 - p) * 2
        translate(x, y) { scale(s, s, Offset.Zero) { repeat(10) { i -> rotate(i * 36f, Offset.Zero) { drawLine(hex(c), Offset.Zero, Offset(0f, -9f), 1.2f, StrokeCap.Round, alpha = a) } } } } }
    fun blink(d: Float, b: Float) = .25f + .75f * abs(cos(((t * 6f / d) + b / d) * PI.toFloat()))
    when (k) {
        "thanks" -> { drawCircle(hex("#ff9a3c"), 46f, Offset(50f, 50f), alpha = .06f + .14f * blink(4f, 0f))
            fall(if (big) 7 else 4) { i -> rotate(t * 720f, Offset.Zero) { fillP("M0 -3 C3 -3 4 0 0 4 C-4 0 -3 -3 0 -3Z", hex(listOf("#e8650c", "#c2410c", "#f2b13c")[i % 3])) } } }
        "xmas" -> { val cols = listOf("#ff3b47", "#34d27a", "#ffd24a", "#ffffff")
            repeat(16) { i -> val a = i * PI.toFloat() / 8; drawCircle(hex(cols[i % 4]), 1.7f, Offset(50f + 45.5f * cos(a), 50f + 45.5f * sin(a)), alpha = blink(1.4f, (i % 3) * .45f)) }
            fall(if (big) 10 else 6) { i -> drawCircle(Color.White, if (i % 3 != 0) 1f else 1.5f, Offset.Zero) } }
        "newyear" -> { burst(28f, 30f, "#ffd24a", 2.2f, 0f); burst(72f, 34f, "#ffffff", 2.6f, .8f); burst(66f, 72f, "#e3c25a", 2.4f, 1.5f)
            fall(if (big) 8 else 4) { i -> rotate(t * 1800f, Offset.Zero) { drawRect(hex(listOf("#ffd24a", "#ffffff", "#e3c25a")[i % 3]), Offset(-1f, -1.5f), Size(2f, 3f)) } } }
        "valentine" -> { drawCircle(hex("#ff6fa8"), 46f, Offset(50f, 50f), alpha = .12f)
            repeat(if (big) 6 else 4) { i -> val x = R("h$i", 16f, 84f); val p = ph(R("hd$i", 3f, 5f), R("hb$i", 0f, 5f))
                translate(x, 92f - 84f * p) { fillP("M0 2 C-5 -2 -3 -6 0 -3.5 C3 -6 5 -2 0 2Z", hex(if (i % 2 == 1) "#ff4f8b" else "#ffb3cf"), (if (p < .5f) p * 2 else (1 - p) * 2)) } } }
        "stpat" -> { rotate(t * 360f * 6f / 14f, Offset(50f, 50f)) { repeat(6) { i -> rotate(i * 60f, Offset(50f, 50f)) { translate(50f, 6f) {
                listOf(0f, 120f, 240f).forEach { r -> rotate(r, Offset.Zero) { drawCircle(hex("#5ed47f"), 1.6f, Offset(0f, -1.6f)) } } } } } }
            repeat(6) { i -> drawCircle(hex("#ffd24a"), 1.3f, Offset(R("cx$i", 20f, 80f), R("cy$i", 20f, 80f)), alpha = blink(1.8f, i * .3f)) } }
        "easter" -> repeat(5) { i -> val a = i * PI.toFloat() * 2 / 5 - PI.toFloat() / 2; val x = 50f + 36f * cos(a); val y = 50f + 36f * sin(a)
            rotate(12f * sin((t * 6f / 1.6f + i * .2f) * 2 * PI.toFloat()), Offset(x, y)) {
                drawOval(hex(listOf("#ffd1e8", "#c7f0ff", "#fff3b0", "#d8ffd1", "#e8d1ff")[i]), Offset(x - 3.4f, y - 4.4f), Size(6.8f, 8.8f))
                drawOval(Color.White, Offset(x - 3.4f, y - 4.4f), Size(6.8f, 8.8f), style = Stroke(.6f)) } }
        "july4" -> { burst(28f, 30f, "#ff3b47", 2.2f, 0f); burst(72f, 30f, "#ffffff", 2.5f, .7f); burst(50f, 76f, "#4a8cff", 2.3f, 1.4f)
            repeat(8) { i -> translate(R("sx$i", 16f, 84f), R("sy$i", 16f, 84f)) { fillP("M0 -2l.6 1.4 1.5 .1-1.1 1 .3 1.5-1.3-.8-1.3 .8 .3-1.5-1.1-1 1.5-.1z", Color.White, blink(1.6f, i * .2f)) } } }
        "halloween" -> { drawCircle(hex("#ff8a2a"), 25f, Offset(50f, 50f), alpha = .2f + .7f * blink(1.3f, 0f), style = Stroke(2.5f))
            rotate(-t * 360f * 6f / 7f, Offset(50f, 50f)) { repeat(3) { i -> rotate(i * 120f, Offset(50f, 50f)) { translate(50f, 10f) {
                fillP("M-5 0 Q-3 -3 -1 -.5 Q0 -2 1 -.5 Q3 -3 5 0 Q3 -.6 1 1 Q0 .3 -1 1 Q-3 -.6 -5 0Z", hex("#1b0f24")) } } } }
            translate(6f * sin(t * 6f / 5f * 2 * PI.toFloat()), 0f) { drawOval(hex("#9b6bd6"), Offset(20f, 78f), Size(60f, 12f), alpha = .25f) } }
    }
}

/** A plate with its holiday animation (used by Plate() when the plate is a holiday one). */
@Composable
fun holT(): Float { val tr = rememberInfiniteTransition(label = "hol"); val t by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ht"); return t }

// ============================ HOLIDAY BANNERS ============================
fun DrawScope.drawHolScene(k: String, uid: String, t: Float) {
    val u = "hs$uid"
    fun R(n: String, a: Float, b: Float) = rnd(u + n, a, b)
    fun ph(d: Float, b: Float) = (((t * 6f) / d + b / d) % 1f + 1f) % 1f
    fun blink(d: Float, b: Float) = .3f + .7f * abs(cos(((t * 6f / d) + b / d) * PI.toFloat()))
    fun sky(a: String, b: String) = drawRect(Brush.verticalGradient(listOf(hex(a), hex(b)), 0f, 130f), size = Size(380f, 130f))
    fun fall(n: Int, y0: Float = -10f, y1: Float = 140f, draw: DrawScope.(Int) -> Unit) = repeat(n) { i ->
        val x = R("x$i", 0f, 380f); val p = ph(R("d$i", 4f, 9f), R("b$i", 0f, 9f)); translate(x + R("w$i", -30f, 30f) * p, y0 + (y1 - y0) * p) { draw(i) } }
    fun fw(x: Float, y: Float, c: String, d: Float, b: Float, r: Float = 22f) { val p = ph(d, b); val s = 1.15f * p; val a = if (p < .5f) p * 2 else (1 - p) * 2
        translate(x, y) { scale(s, s, Offset.Zero) { repeat(14) { i -> rotate(i * 360f / 14, Offset.Zero) { drawCircle(hex(c), 1.6f, Offset(0f, -r), alpha = a) } } } } }
    fun city(c: String) = fillP("M0 130 V96 H24 V80 H44 V100 H62 V70 H80 V92 H104 V76 H122 V100 H146 V64 H160 V58 H170 V64 H184 V96 H206 V82 H230 V100 H250 V72 H268 V90 H292 V78 H312 V98 H334 V84 H352 V100 H380 V130 Z", hex(c))
    when (k) {
        "thanks" -> { sky("#3a1d0e", "#c2601c"); drawCircle(hex("#ffb347"), 34f, Offset(300f, 96f), alpha = .55f); fillP("M0 130 V104 Q80 86 170 102 T380 96 V130 Z", hex("#4a2410"))
            fall(18) { i -> rotate(t * 360f * (3 - i % 3), Offset.Zero) { fillP("M0 -5 C5 -5 7 0 0 7 C-7 0 -5 -5 0 -5Z", hex(listOf("#e8650c", "#c2410c", "#f2b13c", "#8a2b0c")[i % 4])) } }
            svgText("GIVING THANKS · LIFTING ANYWAY", 18f, 22f, 9f, hex("#ffd9a8", .8f), align = 0, spacing = 5f) }
        "xmas" -> { sky("#071226", "#1b2f5a"); fillP("M300 112 L272 112 L300 38 L328 112 Z", hex("#0f3d24"))
            fillP("M300 30 l3 7 7 .5-5.5 4.5 2 7-6.5-4-6.5 4 2-7-5.5-4.5 7-.5z", hex("#ffd24a"), .6f + .4f * blink(1.5f, 0f)); fillP("M0 130 V112 Q190 100 380 112 V130 Z", hex("#e9f1ff"))
            strokeP("M0 8 Q95 26 190 8 T380 8", hex("#2a2a2a"), 1f)
            repeat(20) { i -> val x = i * 19f + 8; val y = 8 + sin(i * 19 / 380f * PI.toFloat() * 2) * -9 + 9; drawCircle(hex(listOf("#ff3b47", "#34d27a", "#ffd24a", "#4a8cff")[i % 4]), 3f, Offset(x, y), alpha = blink(1.2f, (i % 4) * .3f)) }
            fall(30) { i -> drawCircle(Color.White, if (i % 3 != 0) 1.2f else 2f, Offset.Zero, alpha = .9f) } }
        "newyear" -> { sky("#05050c", "#141433"); fw(90f, 40f, "#ffd24a", 2.4f, 0f); fw(200f, 34f, "#ffffff", 2.8f, .9f); fw(310f, 44f, "#e3c25a", 2.5f, 1.7f); fw(250f, 60f, "#ff6fa8", 2.6f, .4f, 16f)
            city("#0b0b16"); val d = today(); svgText("${if (d.monthValue == 1) d.year else d.year + 1}", 190f, 118f, 22f, hex("#e3c25a", .5f + .5f * blink(2f, 0f)), spacing = 8f) }
        "valentine" -> { sky("#3d0a24", "#d6457f")
            repeat(16) { i -> val x = R("vx$i", 10f, 370f); val p = ph(R("vd$i", 5f, 9f), R("vb$i", 0f, 9f)); val s = R("vs$i", .6f, 1.6f)
                translate(x, 140f - 154f * p) { scale(s, s, Offset.Zero) { fillP("M0 6 C-12 -4 -7 -14 0 -8 C7 -14 12 -4 0 6Z", hex(if (i % 2 == 1) "#ff4f8b" else "#ffc2d8")) } } } }
        "stpat" -> { sky("#0b3d1f", "#2a8a4a")
            listOf("#ff3b47", "#ff9a3c", "#ffd24a", "#34d27a", "#4a8cff", "#8a5cff").forEachIndexed { i, c -> translate(0f, i * 7f) { strokeP("M60 140 A140 120 0 0 1 340 140", hex(c), 6f, .5f, round = false) } }
            fillP("M0 130 V108 Q190 92 380 108 V130 Z", hex("#0f5a2a"))
            repeat(10) { i -> drawCircle(hex("#ffd24a"), 3f, Offset(R("gx$i", 150f, 250f), R("gy$i", 96f, 118f)), alpha = blink(1.4f, i * .15f)) } }
        "easter" -> { sky("#bfe6ff", "#ffe1f0"); fillP("M0 130 V100 Q100 88 190 100 T380 96 V130 Z", hex("#8fd48a"))
            repeat(7) { i -> val x = 40f + i * 48; val y = 104f + (i % 2) * 6; rotate(8f * sin((t * 3f + i * .25f) * 2 * PI.toFloat()), Offset(x, y + 10)) {
                drawOval(hex(listOf("#ffd1e8", "#c7f0ff", "#fff3b0", "#d8ffd1", "#e8d1ff")[i % 5]), Offset(x - 9, y - 12), Size(18f, 24f)); drawOval(Color.White, Offset(x - 9, y - 12), Size(18f, 24f), style = Stroke(2f)) } }
            fall(10, -10f, 90f) { i -> drawCircle(hex(listOf("#ffb3cf", "#ffffff")[i % 2]), 2f, Offset.Zero) } }
        "july4" -> { sky("#050a1f", "#16224d"); fw(80f, 40f, "#ff3b47", 2.4f, 0f); fw(190f, 30f, "#ffffff", 2.7f, .8f); fw(300f, 42f, "#4a8cff", 2.5f, 1.5f); city("#070b1a")
            repeat(7) { i -> drawRect(hex(if (i % 2 == 1) "#ffffff" else "#c8202b"), Offset(i * 55f, 118f + (i % 2)), Size(55f, 12f), alpha = .5f) } }
        "halloween" -> { sky("#12051f", "#3d1a5c"); drawCircle(hex("#ffe9a8"), 26f, Offset(300f, 40f)); drawCircle(hex("#3d1a5c"), 26f, Offset(292f, 34f), alpha = .12f)
            repeat(5) { i -> val p = ph(8f + i * 2, i * 2f); translate(-40f + i * 30 + (460f - i * 30) * p, 30f + i * 12 - (20f - 2f * i) * p) {
                fillP("M-10 0 Q-6 -6 -2 -1 Q0 -4 2 -1 Q6 -6 10 0 Q6 -1 2 2 Q0 .5 -2 2 Q-6 -1 -10 0Z", hex("#0b0410")) } }
            fillP("M0 130 V112 Q190 100 380 112 V130 Z", hex("#1a0b26"))
            listOf(60f, 120f, 330f).forEachIndexed { i, x -> translate(x, 110f) { drawOval(hex("#ff7a1a"), Offset(-16f, -12f), Size(32f, 24f))
                fillP("M-6 -3 l3 -4 3 4z M3 -3 l3 -4 3 4z M-8 4 Q0 10 8 4", hex("#ffe14a"), blink(1f + i * .3f, 0f)); drawRect(hex("#2f5a1a"), Offset(-1.5f, -15f), Size(3f, 5f)) } }
            translate(20f * sin(t * 6f / 7f * 2 * PI.toFloat()), 0f) { drawOval(hex("#b48cff"), Offset(-10f, 110f), Size(400f, 20f), alpha = .15f) } }
    }
}

/** The holiday set-complete burst: the holiday's emoji fly out in a ring. */
fun DrawScope.holBurst(k: String, c: Offset, f: Float) {
    val h = HOL19[k] ?: return; val px = density
    repeat(9) { i -> rotate(i * 40f, c) { svgText(h.fx[i % h.fx.size], c.x, c.y - 46f * px * f, 18f * px * (.6f + .5f * f), Color.White.copy(1 - f)) } }
}
