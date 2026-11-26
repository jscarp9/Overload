package com.overload.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ============================ MUSCLE MAP ============================
private class Muscle(val key: String, val path: Path, val fibers: List<Path>)
private fun p(d: String): Path = PathParser().parsePathString(d).toPath()

private val SIL = p("M100 58 L91 58 C84 64 68 67 56 71 C44 75 39 86 39 99 C37 114 37 127 39 138 C35 155 33 170 35 184 C34 194 37 202 42 203 C48 203 50 195 50 186 C54 168 58 152 62 139 C64 128 66 118 68 109 C70 130 70 150 68 168 C65 180 63 190 64 202 C61 230 62 255 67 272 C63 292 61 322 65 352 C64 362 60 372 70 375 L89 375 C91 365 90 354 90 342 C92 318 94 298 92 282 C96 262 98 232 98 204 L100 200 Z")
private val NECK = p("M90 46 L89 60 L111 60 L110 46 Z")
private val ANKLE = p("M78 328 L80 352 L86 352 L86 328 Z")

private fun m(k: String, d: String, vararg f: String) = Muscle(k, p(d), f.map(::p))

private val FRONT: List<Muscle> by lazy {
    val l = mutableListOf(
        m("trap", "M91 59 C84 63 74 67 66 70 L80 71 L92 66 Z"),
        m("sdelt", "M64 70 C52 70 44 78 42 92 C44 97 47 98 49 96 C50 86 54 78 60 74 Z", "M60 73 C52 78 47 85 46 93"),
        m("fdelt", "M66 71 C58 74 52 81 50 95 C54 99 59 98 62 94 C63 86 66 80 71 75 Z", "M64 75 C58 81 55 88 54 95"),
        m("chest", "M99 72 L99 107 C90 112 76 111 66 103 C61 97 62 87 70 77 C80 73 90 72 99 72 Z", "M98 76 C86 78 74 84 66 96", "M98 84 C86 86 75 92 68 100", "M98 93 C88 96 78 100 72 104", "M98 101 C90 104 82 106 76 106"),
        m("bic", "M49 98 C44 109 44 122 47 133 C52 138 58 136 60 130 C62 118 61 107 58 99 C55 96 51 96 49 98 Z", "M53 101 C51 112 51 124 53 133"),
        m("tri", "M45 101 C41 112 41 124 43 134 L47 134 C44 123 44 110 48 99 Z"),
        m("fore", "M45 138 C39 152 37 168 39 182 C42 186 46 186 48 183 C51 170 55 155 58 141 C55 136 49 135 45 138 Z", "M50 141 C46 154 43 168 42 181")
    )
    for (r in 0..3) { val y = 113 + r * 15; val h = if (r == 3) 20 else 13
        l += m("abs", "M99 $y L99 ${y + h} L${if (r == 3) 92 else 89} ${y + h} C88 ${y + h - 4} 88 ${y + 4} 89 $y Z") }
    l += m("abs", "M87 110 C79 116 74 130 75 148 C77 162 81 172 89 180 L89 112 Z", "M84 118 C80 128 79 140 80 152", "M86 140 C83 150 83 160 86 170")
    listOf(76 to 104, 77 to 113, 78 to 122).forEach { (x, y) -> l += m("abs", "M$x $y L${x + 8} ${y + 2} L${x + 7} ${y + 7} L$x ${y + 5} Z") }
    l += m("quad", "M73 186 C65 210 65 238 69 262 C73 268 79 266 81 258 C82 236 82 211 80 191 Z", "M75 195 C70 215 70 240 73 258")
    l += m("quad", "M82 188 C79 214 81 240 85 262 L91 262 C94 236 94 210 92 190 Z", "M87 194 C86 216 87 240 88 258")
    l += m("quad", "M89 234 C96 242 98 255 94 268 C88 268 85 260 85 251 Z")
    l += m("quad", "M92 189 L98 192 L98 228 C96 236 92 236 89 230 C90 216 91 202 92 189 Z")
    l += m("calf", "M71 281 C65 301 65 325 69 350 L77 352 C79 327 81 302 81 283 Z", "M73 290 C70 310 70 330 72 348")
    l += m("calf", "M83 283 C85 307 85 331 83 352 L89 352 C91 327 91 301 89 284 Z")
    l
}
private val BACK: List<Muscle> by lazy {
    listOf(
        m("trap", "M100 60 L100 120 C94 112 86 96 80 84 C74 78 68 74 62 72 C74 68 86 64 91 59 Z", "M99 66 C90 70 80 74 68 74", "M99 80 C93 82 87 84 81 86", "M99 96 C95 98 91 100 88 102"),
        m("rdelt", "M62 71 C52 71 44 78 42 92 C45 97 49 97 51 94 C53 84 58 78 66 75 Z", "M60 74 C53 79 49 86 47 93"),
        m("mid", "M67 76 C76 80 82 86 86 96 C80 100 72 100 66 96 C64 89 64 82 67 76 Z", "M70 82 C76 86 80 91 83 96"),
        m("lat", "M86 97 C82 108 80 120 80 128 C74 128 68 122 64 110 C63 104 64 100 66 97 C72 101 80 101 86 97 Z"),
        m("lat", "M64 102 C58 118 60 140 70 160 C78 166 86 166 92 162 C90 150 84 140 80 129 C72 128 66 120 64 102 Z", "M66 112 C68 130 74 146 84 158", "M64 124 C68 138 74 150 80 158"),
        m("lowb", "M99 124 L99 176 C95 176 92 172 91 166 C92 152 93 138 93 126 Z", "M96 130 L96 170"),
        m("tri", "M48 98 C43 109 43 122 46 134 C51 138 57 137 59 131 C61 118 60 107 57 99 C54 96 50 96 48 98 Z", "M52 101 C50 112 50 124 52 133", "M56 103 C55 113 55 123 57 130"),
        m("fore", "M45 138 C39 152 37 168 39 182 C42 186 46 186 48 183 C51 170 55 155 58 141 C55 136 49 135 45 138 Z"),
        m("glute", "M99 182 L99 218 C88 222 74 216 68 204 C66 194 68 186 74 180 C82 178 92 178 99 182 Z", "M96 186 C86 190 78 196 72 204"),
        m("ham", "M70 220 C65 240 66 258 70 270 L82 270 C82 252 81 236 80 220 Z", "M73 226 C70 242 70 256 72 266"),
        m("ham", "M81 220 C83 240 84 256 85 270 L95 270 C96 252 96 236 96 220 Z", "M88 226 C89 242 90 256 90 266"),
        m("calf", "M69 280 C62 296 63 314 70 326 C76 328 81 322 82 312 C83 300 81 288 78 280 Z", "M72 288 C68 300 68 312 71 322"),
        m("calf", "M80 280 C85 290 88 302 88 314 C87 322 84 326 81 324 C82 310 82 294 80 280 Z")
    )
}

private fun radial(b: Rect, stops: Array<Pair<Float, Color>>) =
    Brush.radialGradient(*stops, center = Offset(b.left + b.width * .35f, b.top + b.height * .3f), radius = max(1f, max(b.width, b.height) * .85f))

/**
 * Front or back of the body with muscles coloured by level (0 rested, 1 = 1–3 sets, 2 = 4+ sets),
 * or by a fixed colour per region (muscle mastery). Port of the web app's anatomy SVG.
 */
@Composable
fun Anatomy(side: String, levels: Map<String, Int>, modifier: Modifier = Modifier, dark: Boolean = ov.dark, colors: Map<String, Color>? = null) =
    Canvas(modifier.aspectRatio(140f / 380f)) {
        val sc = min(size.width / 140f, size.height / 380f)
        val skin = Brush.horizontalGradient(
            if (dark) listOf(Color(0xFF1D2027), Color(0xFF2C3039), Color(0xFF1D2027)) else listOf(Color(0xFFC3C7CF), Color(0xFFE4E6EA), Color(0xFFC3C7CF)),
            startX = 30f, endX = 170f)
        val line = if (dark) Color(0xFF4B505C) else Color(0xFF9AA0AB)
        val lv0 = if (dark) arrayOf(0f to Color(0xFF4A4F5B), .6f to Color(0xFF30343D), 1f to Color(0xFF1C1F26)) else arrayOf(0f to Color(0xFFF3F4F7), .6f to Color(0xFFD9DCE2), 1f to Color(0xFFB9BEC8))
        val lv1 = arrayOf(0f to Color(0xFFFFD0A8), .5f to Color(0xFFFF8A3D), 1f to Color(0xFFE0531C))
        val lv2 = arrayOf(0f to Color(0xFFFF9A9E), .45f to Color(0xFFF0303A), 1f to Color(0xFFA50D17))
        val list = if (side == "front") FRONT else BACK
        fun DrawScope.half() {
            drawPath(SIL, skin); drawPath(SIL, if (dark) Color(0xFF4B505C) else Color(0xFF8D939E), style = Stroke(.9f))
            list.forEach { mu ->
                val v = levels[mu.key] ?: 0; val b = mu.path.getBounds()
                if (v >= 2) drawPath(mu.path, Color(0x8CFF2A35), style = Stroke(3.5f))
                val custom = colors?.get(mu.key)
                if (custom != null) drawPath(mu.path, custom) else drawPath(mu.path, radial(b, when (v) { 0 -> lv0; 1 -> lv1; else -> lv2 }))
                drawPath(mu.path, if (v > 0) Color(0x8C780A0A) else line, style = Stroke(.7f, join = StrokeJoin.Round))
                val fc = if (v > 0) Color.White.copy(.38f) else if (dark) Color.White.copy(.1f) else Color.White.copy(.7f)
                mu.fibers.forEach { drawPath(it, fc, style = Stroke(.55f, cap = StrokeCap.Round)) }
            }
            if (side == "front") {
                drawOval(skin, Offset(76f, 268f), Size(14f, 10f)); drawOval(line, Offset(76f, 268f), Size(14f, 10f), style = Stroke(.6f))
            } else { drawPath(ANKLE, skin); drawPath(ANKLE, line, style = Stroke(.6f)) }
        }
        withTransform({ scale(sc, sc, Offset.Zero); translate(-30f, 0f) }) {
            half()
            withTransform({ translate(200f, 0f); scale(-1f, 1f, Offset.Zero) }) { half() }
            drawOval(skin, Offset(84f, 10f), Size(32f, 40f)); drawOval(line, Offset(84f, 10f), Size(32f, 40f), style = Stroke(.7f))
            drawPath(NECK, skin); drawPath(NECK, line, style = Stroke(.7f))
        }
    }

/** Front and back side by side with captions and a legend. */
@Composable
fun BodyMap(levels: Map<String, Int>, modifier: Modifier = Modifier, colors: Map<String, Color>? = null, legend: Boolean = true) =
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Anatomy("front", levels, Modifier.height(220.dp), colors = colors); Anatomy("back", levels, Modifier.height(220.dp), colors = colors)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { Sub("Front"); Sub("Back") }
        if (legend && colors == null) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
            listOf(Color(0xFFFF8A3D) to "1–3 sets", Color(0xFFF0303A) to "4+ sets", (if (ov.dark) Color(0xFF30343D) else Color(0xFFD9DCE2)) to "Rested").forEach { (c, t) ->
                Canvas(Modifier.padding(start = 10.dp, end = 4.dp).size(10.dp)) { drawCircle(c) }; Sub(t)
            }
        }
    }

// ============================ RANK EMBLEM ============================
private val STAR = p("M50 27 L56.5 43 L73 43.5 L60 53.5 L64.5 70 L50 60.5 L35.5 70 L40 53.5 L27 43.5 L43.5 43 Z")

/** The rank medal. Titan and Olympian spin slowly when spin = true. */
@Composable
fun Emblem(rank: Int, modifier: Modifier = Modifier, spin: Boolean = false) {
    val r = RANKS[rank.coerceIn(0, 7)]; val dk = Color(r.dark); val lt = Color(r.light)
    val angle = if (spin) rememberInfiniteTransition(label = "spin").animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "a").value else 0f
    Canvas(modifier.aspectRatio(1f).rotate(angle)) {
        val s = size.minDimension / 100f
        withTransform({ scale(s, s, Offset.Zero) }) {
            drawCircle(Brush.radialGradient(0f to lt, .55f to dk, 1f to Color(0xFF14161B), center = Offset(35f, 30f), radius = 85f), 48f, Offset(50f, 50f))
            val ring = Brush.linearGradient(0f to Color.White, .45f to lt, 1f to dk, start = Offset.Zero, end = Offset(100f, 100f))
            drawCircle(ring, 42f, Offset(50f, 50f), style = Stroke(3f))
            for (k in 0 until 12) { val a = k * PI / 6; drawCircle(lt, 1.5f, Offset((50 + 44.5 * cos(a)).toFloat(), (50 + 44.5 * sin(a)).toFloat())) }
            drawCircle(Color.Black.copy(.18f), 28f, Offset(50f, 50f)); drawCircle(Color.Black.copy(.3f), 28f, Offset(50f, 50f), style = Stroke(1f))
            drawPath(STAR, ring); drawPath(STAR, Color.Black.copy(.35f), style = Stroke(1f))
            if (rank >= 5) drawCircle(lt, 48f, Offset(50f, 50f), style = Stroke(2f))
            rotate(-32f, Offset(34f, 26f)) { drawOval(Color.White.copy(.28f), Offset(17f, 20f), Size(34f, 12f)) }
        }
    }
}

/** Emblem with a cosmetic frame (ring) and aura (glow) around it. */
@Composable
fun FramedEmblem(rank: Int, frame: String, aura: String, modifier: Modifier = Modifier, spin: Boolean = rank >= 6) =
    Box(modifier.aspectRatio(1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center; val rad = size.minDimension / 2
            val glow = when (aura) { "glow" -> Color(0xFF5AA9FF); "spark" -> Color(0xFFFFBE0B); "frost" -> Color(0xFF8BE0FF); "ember" -> Color(0xFFFF5A1F); "gold" -> Color(0xFFFFD75E); else -> null }
            if (glow != null) drawCircle(Brush.radialGradient(0f to glow.copy(.75f), .7f to glow.copy(.25f), 1f to Color.Transparent, center = c, radius = rad), rad, c)
            if (aura == "prism") drawCircle(Brush.sweepGradient(listOf(Color(0xFFFF4D4D), Color(0xFFFFBE0B), Color(0xFF16B65B), Color(0xFF2563FF), Color(0xFFD14DFF), Color(0xFFFF4D4D)), c), rad, c, alpha = .45f)
            val ringR = rad * .9f; val w = rad * .1f
            fun sweep(vararg cs: Long) = Brush.sweepGradient(cs.map { Color(it) }, c)
            fun stripes(a: Long, b: Long, deg: Int): Brush { val n = 360 / deg; val l = mutableListOf<Pair<Float, Color>>()
                for (i in 0 until n) { val f0 = i / n.toFloat(); val f1 = (i + 1) / n.toFloat(); val col = Color(if (i % 2 == 0) a else b); l += f0 to col; l += f1 - .0001f to col }
                return Brush.sweepGradient(*l.toTypedArray(), center = c) }
            val brush: Brush? = when (frame) {
                "bronze" -> sweep(0xFF8A4F1F, 0xFFF0B27A, 0xFF8A4F1F, 0xFFF0B27A, 0xFF8A4F1F); "silver" -> sweep(0xFF7D838D, 0xFFF4F6F9, 0xFF7D838D, 0xFFF4F6F9, 0xFF7D838D)
                "gold" -> sweep(0xFFA97A05, 0xFFFFE07A, 0xFFA97A05, 0xFFFFE07A, 0xFFA97A05); "plat" -> sweep(0xFF2F948F, 0xFFC6F5F0, 0xFF2F948F, 0xFFC6F5F0, 0xFF2F948F)
                "diamond" -> sweep(0xFF2468D6, 0xFFCFE6FF, 0xFF7FB6FF, 0xFFCFE6FF, 0xFF2468D6); "titan" -> sweep(0xFF6D2FE0, 0xFFE6C4FF, 0xFF6D2FE0, 0xFFE6C4FF, 0xFF6D2FE0)
                "olymp" -> sweep(0xFFB88600, 0xFFFFF3B0, 0xFFFFD75E, 0xFFFFF3B0, 0xFFB88600); "iron" -> stripes(0xFF3A3D46, 0xFF8C939E, 15)
                "shadow" -> sweep(0xFF000000, 0xFF4A2A7A, 0xFF000000, 0xFF4A2A7A, 0xFF000000); "circuit" -> stripes(0xFF00C2A8, 0x0000C2A8, 11)
                "neon" -> SolidColor(Color(0xFF00E5FF)); "laurel" -> SolidColor(Color(0xFF3B7D2A)); "crown" -> SolidColor(Color(0xFFE8B923)); else -> null
            }
            when (frame) {
                "olymp" -> drawCircle(Color(0x88FFD75E), ringR + w, c, style = Stroke(w))
                "shadow" -> drawCircle(Color(0x664A2A7A), ringR + w, c, style = Stroke(w))
                "neon" -> drawCircle(Color(0x66D14DFF), ringR + w * .8f, c, style = Stroke(w * 1.6f))
                "laurel" -> drawCircle(Color(0x559FD36A), ringR + w, c, style = Stroke(w))
            }
            if (brush != null) drawCircle(brush, ringR, c, style = Stroke(w))
        }
        Emblem(rank, Modifier.fillMaxSize(if (frame.isEmpty()) .86f else .74f), spin)
        if (frame == "crown") OvText("👑", 18, modifier = Modifier.align(Alignment.TopCenter).offset(y = (-10).dp))
        if (frame == "laurel") { OvText("🌿", 16, modifier = Modifier.align(Alignment.BottomStart)); OvText("🌿", 16, modifier = Modifier.align(Alignment.BottomEnd)) }
    }

// ============================ CONFETTI ============================
private class Bit(val x: Float, val delay: Float, val dur: Float, val rot: Float, val color: Color, val drift: Float)

/** Falling confetti. Re-runs whenever [trigger] changes (and is > 0). */
@Composable
fun Confetti(trigger: Int, count: Int = 70, kind: String = "confetti") {
    if (trigger <= 0) return
    if (kind == "fireworks" || kind == "storm") { FxBurst(trigger, kind); return }
    val bits = remember(trigger) {
        val cols = if (kind == "goldrain") listOf(0xFFE3C25A, 0xFFFFE07A, 0xFFC99A2E, 0xFFFFF3B0) else listOf(0xFFEF2B36, 0xFFFFBE0B, 0xFF2563FF, 0xFF16B65B, 0xFFD14DFF, 0xFFFF7A3D)
        List(count) { i -> Bit(Random.nextFloat(), Random.nextFloat() * .4f, 1.6f + Random.nextFloat() * 1.2f, Random.nextFloat() * 360f, Color(cols[i % cols.size]), Random.nextFloat() * 2 - 1) }
    }
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(3.4f, tween(3400, easing = LinearEasing)) }
    if (t.value >= 3.4f) return
    Canvas(Modifier.fillMaxSize()) {
        bits.forEach { b ->
            val f = ((t.value - b.delay) / b.dur).coerceIn(0f, 1f); if (f <= 0f || f >= 1f) return@forEach
            val x = b.x * size.width + b.drift * 40.dp.toPx() * f; val y = -20f + f * (size.height + 40f)
            rotate(b.rot + f * 720f, Offset(x, y)) { drawRect(b.color, Offset(x - 5f, y - 8f), Size(10f, 16f), alpha = 1f - f * .3f) }
        }
    }
}

/** Big flame emojis floating up when a crew mate sends hype. */
@Composable
fun HypeBurst(trigger: Int, from: String) {
    if (trigger <= 0) return
    val t = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) { t.snapTo(0f); t.animateTo(1f, tween(2600, easing = LinearEasing)) }
    if (t.value >= 1f) return
    val bits = remember(trigger) { List(22) { Triple(Random.nextFloat(), Random.nextFloat() * .25f, 22 + Random.nextInt(26)) } }
    Box(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            bits.forEach { (x, d, sz) ->
                val f = ((t.value - d) / .75f).coerceIn(0f, 1f)
                if (f > 0f && f < 1f) OvText("🔥", sz, modifier = Modifier.offset(x = maxWidth * x, y = maxHeight * (1f - f)))
            }
        }
        Box(Modifier.align(Alignment.Center).padding(20.dp)) {
            androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = Color(0xEE14161C)) {
                OvText("$from hyped you up 🔥", 18, androidx.compose.ui.text.font.FontWeight.Black, Color.White, Modifier.padding(horizontal = 20.dp, vertical = 14.dp))
            }
        }
    }
}
