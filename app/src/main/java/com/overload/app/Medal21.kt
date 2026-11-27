package com.overload.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * 0.21 medals: pinned badges are minted coins with a reeded edge, the badge name around the rim,
 * the tier along the bottom, a laurel wreath, sunburst lines and a raised icon. The metal shows
 * how far you've gone (Bronze to Legend), hidden badges are dark "Secret" coins with a gold icon,
 * crew badges are platinum, collections are Legend and weekly badges level up with how often you
 * earned them. Same rules as the web app's medal21.
 */

/** Metal colours, light to dark, and the tier name. */
private val MT21 = mapOf(
    "bronze" to listOf("#ffe6cc", "#e8ab72", "#b5703a", "#6e3c17", "#2e1606"), "silver" to listOf("#ffffff", "#e6ebf1", "#adb7c3", "#677382", "#262d36"),
    "gold" to listOf("#fffbe2", "#f7d97c", "#d6a63a", "#8a6212", "#3c2905"), "plat" to listOf("#f5fffd", "#cdf3ee", "#88d1c8", "#3d8880", "#123532"),
    "diam" to listOf("#f3f9ff", "#c8e2ff", "#82b6f2", "#3965b5", "#0f2252"), "legend" to listOf("#fdf2ff", "#ebc6ff", "#ba7cea", "#682a98", "#24083a"),
    "obs" to listOf("#8d95a3", "#545b68", "#2c3139", "#15181d", "#050607"))
private val MNAME = mapOf("bronze" to "Bronze", "silver" to "Silver", "gold" to "Gold", "plat" to "Platinum", "diam" to "Diamond", "legend" to "Legend", "obs" to "Secret")
private val TIER21 = listOf("bronze", "silver", "gold", "plat", "diam", "legend")
/** Icons in a 24 x 24 box, stroked. */
val IC21 = mapOf(
    "dumbbell" to "M3 10v4M6 7.5v9M18 7.5v9M21 10v4M6 12h12", "flame" to "M12 3c1 3 5 5 5 10a5 5 0 0 1-10 0c0-3 2-4.5 2-6.5 1.2 1 2 2.2 2 3.5 1-2 1-5 1-7z",
    "stack" to "M4 19h16M6 15h12M8 11h8M10 7h4", "check" to "M5 12.5l4.5 4.5L19 7.5", "trophy" to "M8 4h8v5a4 4 0 0 1-8 0zM8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4",
    "calendar" to "M5 6h14v13H5zM5 10h14M9 4v4M15 4v4M9 14h2M13 14h2", "pencil" to "M5 19l1-4L16 5l3 3L9 18zM14 7l3 3",
    "compass" to "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM15.5 8.5l-2 5-5 2 2-5z", "scale" to "M12 4v16M8 20h8M5 7h14M5 7l-2.5 6h5zM19 7l-2.5 6h5z",
    "sunrise" to "M3 17h18M7 17a5 5 0 0 1 10 0M12 6v3M5.6 10.6l2 1.6M18.4 10.6l-2 1.6M8 20.5h8", "moon" to "M16 4a8 8 0 1 0 4 12A7 7 0 0 1 16 4z",
    "loop" to "M5 12a7 7 0 0 1 12-5l2 2M19 4v5h-5M19 12a7 7 0 0 1-12 5l-2-2M5 20v-5h5", "shield" to "M12 3l7 3v5c0 5-3 8-7 10-4-2-7-5-7-10V6zM12 7v10",
    "leaf" to "M5 19c0-9 5-14 14-14 0 9-5 14-14 14zM5 19l8-8", "hourglass" to "M7 3h10M7 21h10M8 3c0 5 8 6 8 9s-8 4-8 9M16 3c0 5-8 6-8 9s8 4 8 9",
    "bolt" to "M13 3L5 13h6l-1 8 8-10h-6z", "target" to "M12 4a8 8 0 1 0 0 16a8 8 0 1 0 0-16zM12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8zM12 11.2a.8.8 0 1 0 0 1.6a.8.8 0 1 0 0-1.6z",
    "people" to "M8 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM16 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM3 19c0-3 2-5 5-5s5 2 5 5M11 19c0-3 2-5 5-5s5 2 5 5",
    "mountain" to "M2.5 19l6.5-11 4 6.5 3-4 5.5 8.5zM7.5 10.5l1.5 1.5 1.5-1", "megaphone" to "M4 10v4h3l8 4.5V5.5L7 10zM18 9.5a3.5 3.5 0 0 1 0 5M7 14l1.2 5",
    "swords" to "M4 4l11 11M20 4L9 15M12.5 17.5l5-5M6.5 12.5l5 5M4 20l3-3M20 20l-3-3",
    "hands" to "M2.5 11l4-4 4 2 2.5-1.5L16 9l5.5 2M6.5 12l4 4a1.4 1.4 0 0 0 2-2M10 15.5l1.6 1.6a1.4 1.4 0 0 0 2-2M12.4 17.4l.8.8a1.4 1.4 0 0 0 2-2l-4-4M16 9l-3.5 3",
    "flag" to "M6 21V4M6 4h11l-2.5 4L17 12H6", "columns" to "M4 20h16M5 9h14M12 4l8 5H4zM7 9v11M12 9v11M17 9v11", "clock" to "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 7v5l3.5 2",
    "key" to "M15.5 3.5a5 5 0 1 0 0 10 5 5 0 0 0 0-10zM12 12l-8.5 8.5M5.5 18.5l2 2M8.5 15.5l2 2", "crown" to "M4 17l-1-9 5 4 4-7 4 7 5-4-1 9zM4 20h16",
    "chart" to "M4 20h16M6 16l4-5 3 3 6-7M15 7h4v4", "arrow" to "M4 20L19 5M13 5h6v6M4 20l3.5-.5M4 20l.5-3.5", "map" to "M3 6l6-2 6 2 6-2v14l-6 2-6-2-6 2zM9 4v14M15 6v14",
    "broom" to "M19 3l-7.5 7.5M8 10l6 6-3 5H4v-7z", "star" to "M12 3l2.6 5.6 6 .7-4.5 4.1 1.2 6-5.3-3-5.3 3 1.2-6-4.5-4.1 6-.7z")
private val MK21 = mapOf('w' to (listOf(1, 10, 25, 50, 100, 250) to "dumbbell"), 's' to (listOf(2, 4, 8, 12, 26, 52) to "flame"), 't' to (listOf(100, 500, 1000, 2500, 5000) to "check"), 'p' to (listOf(1, 10, 25, 50, 100) to "trophy"))
private val MFIX21 = mapOf("pm" to ("gold" to "calendar"), "nt" to ("silver" to "pencil"), "ex" to ("silver" to "compass"), "bal" to ("silver" to "scale"), "dawn" to ("obs" to "sunrise"),
    "owl" to ("obs" to "moon"), "back" to ("obs" to "loop"), "wknd" to ("obs" to "shield"), "hol" to ("obs" to "leaf"), "mara" to ("obs" to "hourglass"), "spd" to ("obs" to "bolt"),
    "cent" to ("obs" to "#100"), "perf" to ("obs" to "target"), "squad" to ("plat" to "people"), "cmil" to ("plat" to "mountain"), "hype" to ("plat" to "megaphone"),
    "rival" to ("plat" to "swords"), "recruit" to ("plat" to "hands"), "team" to ("plat" to "flag"), "col_club" to ("legend" to "columns"), "col_clock" to ("legend" to "clock"),
    "col_secret" to ("legend" to "key"), "col_week" to ("legend" to "calendar"), "col_ms" to ("legend" to "crown"))
private val MWK21 = mapOf("fullweek" to "flame", "overload" to "chart", "prhunt" to "arrow", "speed" to "bolt", "dawnwk" to "sunrise", "quest" to "map", "sweep" to "broom")

/** Which metal and icon a badge's medal gets. Icon "#123" = a number, null = the badge's emoji. */
fun medalOf(k: String, name: String, count: Int): Pair<String, String?> {
    Regex("^([wstp])(\\d+)$").find(k)?.let { m -> val d = MK21[m.groupValues[1][0]]; val i = d?.first?.indexOf(m.groupValues[2].toInt()) ?: -1; if (d != null && i >= 0) return TIER21[minOf(5, i)] to d.second }
    Regex("^v(\\d)$").find(k)?.let { return TIER21[minOf(5, it.groupValues[1].toInt())] to "stack" }
    Regex("^c(\\d)$").find(k)?.let { return TIER21[minOf(5, it.groupValues[1].toInt())] to "#" + (Regex("^\\d+").find(name)?.value ?: "") }
    MFIX21[k]?.let { return it }
    MWK21[k]?.let { return (if (count >= 25) "plat" else if (count >= 10) "gold" else if (count >= 3) "silver" else "bronze") to it }
    return "silver" to null
}
fun tierName(k: String, name: String, count: Int) = MNAME[medalOf(k, name, count).first] ?: "Silver"

/** A minted coin for a badge. text = false drops the rim engraving (small sizes always do). */
@Composable
fun Medal(k: String, name: String, count: Int = 0, emoji: String = "⭐", size: Dp = 64.dp, dim: Boolean = false, text: Boolean = true, modifier: Modifier = Modifier) {
    val (m, ic) = medalOf(k, name, count)
    val shine = !dim && m in setOf("gold", "plat", "diam", "legend")
    val sw = if (shine) { val tr = rememberInfiniteTransition(label = "md"); val v by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(3600, easing = LinearEasing)), label = "mv"); v } else -1f
    Canvas(modifier.size(size).alpha(if (dim) .42f else 1f)) { viewBox(120f, 120f) { drawMedal(m, ic, name, count, emoji, size.value >= 56 && text, sw) } }
}
@Composable fun BadgeMedal(b: Bdg, size: Dp, dim: Boolean = false, text: Boolean = true) =
    if (b.hid && !b.got) Medal("zz", "", 0, "❔", size, dim = true, text = false) else Medal(b.k, b.name, b.count ?: 0, b.icon, size, dim, text)

private val C60 = Offset(60f, 60f)
private fun DrawScope.drawMedal(m: String, ic: String?, name: String, count: Int, emoji: String, band: Boolean, sweep: Float) {
    val C = MT21.getValue(m).map(::hex); val ink = if (m == "obs") listOf(hex("#fff1b8"), hex("#e8b923"), hex("#8a6209")) else listOf(C[0], C[2], C[4])
    drawCircle(C[4], 54f, Offset(60f, 63f))
    drawCircle(Brush.linearGradient(0f to C[0], .18f to C[1], .38f to C[3], .52f to C[1], .7f to C[2], .86f to C[3], 1f to C[4], start = Offset(6f, 6f), end = Offset(114f, 114f)), 54f, C60)
    repeat(90) { i -> val t = i * Math.PI.toFloat() / 45; drawLine(C[4].copy(.45f), Offset(60 + 51.2f * cos(t), 60 + 51.2f * sin(t)), Offset(60 + 54f * cos(t), 60 + 54f * sin(t)), .9f) }
    drawCircle(Brush.verticalGradient(listOf(C[0], C[2], C[4]), 10f, 110f), 49.5f, C60, style = Stroke(2f))
    if (band) {
        drawCircle(Brush.radialGradient(0f to C[2], .7f to C[3], 1f to C[4], center = Offset(48f, 42f), radius = 70f), 48.5f, C60)
        val nm = name.uppercase(); val fs = if (nm.length > 16) 5.4f else if (nm.length > 13) 6.2f else 7.2f
        arcText(nm, 42f, true, fs, C[0].copy(.55f), .7f); arcText(nm, 42f, true, fs, C[4], 0f)
        val tier = "★ ${(MNAME[m] ?: "").uppercase()} ★"; arcText(tier, 46.5f, false, 6f, C[0].copy(.55f), -.4f); arcText(tier, 46.5f, false, 6f, C[4], -1f)
    }
    drawCircle(Brush.verticalGradient(listOf(C[4], C[3], C[0]), 20f, 100f), if (band) 37.5f else 48f, C60, style = Stroke(2.4f))
    drawCircle(Brush.radialGradient(0f to C[0], .35f to C[1], .75f to C[2], 1f to C[3], center = Offset(43f, 36f), radius = 60f), if (band) 36.3f else 46.8f, C60)
    val s = if (band) 1f else 1.28f
    scale(s, s, C60) {
        repeat(72) { i -> val t = i * Math.PI.toFloat() / 36; drawLine(C[0].copy(.16f), Offset(60 + 9f * cos(t), 60 + 9f * sin(t)), Offset(60 + 35f * cos(t), 60 + 35f * sin(t)), .5f) }
        var r = 12f; while (r < 35f) { drawCircle(C[3].copy(.14f), r, C60, style = Stroke(.45f)); r += 2.6f }
        fun leaf(a: Float, side: Int) { val t = Math.toRadians(a.toDouble()); val x = (60 + 29.5 * cos(t)).toFloat(); val y = (60 + 29.5 * sin(t)).toFloat()
            rotate(a + if (side > 0) 60f else -60f, Offset(x, y)) { drawOval(C[1].copy(.9f), Offset(x - 3.6f, y - 1.5f), Size(7.2f, 3f)); drawOval(C[3].copy(.9f), Offset(x - 3.6f, y - 1.5f), Size(7.2f, 3f), style = Stroke(.5f)) } }
        var a = 100f; while (a <= 215f) { leaf(a, 1); a += 13f }; a = 80f; while (a >= -35f) { leaf(a, -1); a -= 13f }
        when {
            ic != null && ic.startsWith("#") -> { val t = ic.drop(1); val f = if (t.length >= 3) 19f else 23f
                svgText(t, 61f, 68f, f, C[4].copy(.7f)); svgText(t, 59.5f, 66.5f, f, ink[0]); svgText(t, 60f, 67f, f, ink[1]) }
            ic != null && IC21[ic] != null -> { val p = IC21.getValue(ic)
                translate(60f, 60f) { scale(1.32f, 1.32f, Offset.Zero) { translate(-12f, -12f) {
                    translate(.8f, 1f) { drawPath(svgPath(p), C[4].copy(.7f), style = Stroke(3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                    translate(-.45f, -.45f) { drawPath(svgPath(p), ink[0], style = Stroke(2.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
                    drawPath(svgPath(p), Brush.verticalGradient(listOf(ink[0], ink[1], ink[2]), 2f, 22f), style = Stroke(2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) } } } }
            else -> svgText(emoji, 60f, 69f, 26f, Color.White)
        }
    }
    val clip = Path().apply { addOval(androidx.compose.ui.geometry.Rect(6f, 6f, 114f, 114f)) }
    clipPath(clip) {
        translate(46f, 38f) { scale(1f, 30f / 44f, Offset.Zero) { drawCircle(Brush.radialGradient(listOf(Color.White.copy(.55f), Color.Transparent), Offset.Zero, 44f), 44f, Offset.Zero) } }
        if (sweep >= 0f) rotate(20f, C60) { drawRect(Color.White.copy(.35f), Offset(-40f + sweep * 200f, -10f), Size(18f, 140f)) }
    }
    if (count > 1) { drawRoundRect(hex("#0b0c10"), Offset(80f, 94f), Size(34f, 18f), androidx.compose.ui.geometry.CornerRadius(9f)); drawRoundRect(C[1], Offset(80f, 94f), Size(34f, 18f), androidx.compose.ui.geometry.CornerRadius(9f), style = Stroke(1.2f))
        svgText("×${if (count > 99) "99+" else "$count"}", 97f, 107f, 11f, Color.White) }
}

/** Text around the coin: over the top (left to right) or along the bottom (left to right, upright). */
private fun DrawScope.arcText(t: String, r: Float, top: Boolean, sizePx: Float, c: Color, dy: Float) = drawIntoCanvas { cv ->
    val oval = android.graphics.RectF(60 - r, 60 - r, 60 + r, 60 + r)
    val p = android.graphics.Path().apply { if (top) addArc(oval, 180f, 180f) else { moveTo(60 - r, 60f); arcTo(oval, 180f, -180f) } }
    val len = android.graphics.PathMeasure(p, false).length
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = c.toArgb(); textSize = sizePx; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER; letterSpacing = 1.6f / sizePx * 1.2f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) }
    cv.nativeCanvas.drawTextOnPath(t, p, len / 2f, dy, paint)
}

// ============================ THE SHELF ============================
/** Your 3 pinned medals on a glass shelf (profile). Tapping one opens the badge; an empty slot opens the picker. */
@Composable
fun Shelf(items: List<Triple<String, String, Pair<String, Int>>>, onTap: ((String) -> Unit)?, onAdd: (() -> Unit)?, size: Dp = 84.dp) = Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        (0 until 3).forEach { i -> val x = items.getOrNull(i)
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).then(if (x != null && onTap != null) Modifier.clickable { onTap(x.first) } else if (x == null && onAdd != null) Modifier.clickable(onClick = onAdd) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally) {
                if (x != null) { Medal(x.first, x.second, x.third.second, x.third.first, size)
                    OvText(x.second, 12, FontWeight.Bold, align = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                    Sub(tierName(x.first, x.second, x.third.second) + if (x.third.second > 0) " · ×${x.third.second}" else "") }
                else if (onAdd != null) { Box(Modifier.size(size).padding(10.dp).background(ov.soft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { OvText("＋", 26, FontWeight.Bold, ov.mute) }
                    OvText("Pin", 12, FontWeight.Bold); Sub("Badge") }
            } }
    }
    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp).height(6.dp).background(Brush.verticalGradient(listOf(Color.White.copy(if (ov.dark) .22f else .7f), Color.White.copy(.05f))), RoundedCornerShape(3.dp)))
}
/** Shelf items from your showcase: key, name, (emoji, count). */
fun Ctx.shelfItems() = st.showcase.mapNotNull { eng.badge(it) }.map { Triple(it.k, it.name, it.icon to (it.count ?: 0)) }
/** Shelf items from a crew member's profile ([icon, name, key, count]). Older apps only send icon and name. */
fun memberShelf(p: MemberProfile) = p.show.take(3).map { Triple(it.getOrElse(2) { "" }, it.getOrElse(1) { "" }, it.getOrElse(0) { "⭐" } to (it.getOrElse(3) { "0" }.toDoubleOrNull()?.toInt() ?: 0)) }
