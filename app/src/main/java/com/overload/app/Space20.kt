package com.overload.app

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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/*
 * 0.20 founder banners: three gold space scenes (Gold Eclipse, The Constellation, Golden Galaxy),
 * each with three drifting star layers, soft nebula clouds and flare stars. Founder only.
 * t counts 0..10 over a minute (one unit = 6 seconds), so sec = t * 6.
 */
val SPACE20 = setOf("feclipse", "fconst", "fgalaxy")
private const val TAU = (2 * PI).toFloat()

private class Kit(val u: String, val sec: Float) {
    fun R(n: String, a: Float, b: Float) = rnd(u + n, a, b)
    /** 0..1 position in a loop of [d] seconds, offset by [b] seconds. */
    fun ph(d: Float, b: Float = 0f) = (((sec + b) / d) % 1f + 1f) % 1f
    fun tw(d: Float, b: Float, lo: Float = .35f) = lo + (1 - lo) * (.5f + .5f * sin((sec + b) / d * TAU))
}

private fun DrawScope.stars(k: Kit) {
    fun layer(tag: String, n: Int, r0: Float, r1: Float, drift: Float, cols: List<String>) {
        val per = 60f + drift * 4f; val dx = -drift * (.5f - .5f * cos(k.sec / per * TAU))
        translate(dx, 0f) { repeat(n) { i -> drawCircle(hex(cols[i % cols.size]), k.R("$tag r$i", r0, r1), Offset(k.R("$tag x$i", -20f, 400f), k.R("$tag y$i", 0f, 130f)),
            alpha = k.tw(k.R("$tag d$i", 1.6f, 5.5f), k.R("$tag b$i", 0f, 5f), k.R("$tag lo$i", .25f, .55f))) } }
    }
    layer("a", 90, .2f, .6f, 4f, listOf("#ffffff", "#d6dcff", "#ffe9b8"))
    layer("b", 45, .5f, 1f, 9f, listOf("#ffffff", "#ffe7a8"))
    layer("c", 14, .9f, 1.5f, 16f, listOf("#fff8e6", "#ffd77a"))
}
private fun DrawScope.flare(k: Kit, x: Float, y: Float, s: Float, c: String, d: Float) = translate(x, y) {
    drawCircle(hex(c), 3 * s, Offset.Zero, alpha = .25f); drawCircle(hex("#fffdf2"), 1.1f * s, Offset.Zero)
    fillP("M0 -${7 * s} L${.45f * s} 0 L0 ${7 * s} L-${.45f * s} 0Z M-${7 * s} 0 L0 ${.45f * s} L${7 * s} 0 L0 -${.45f * s}Z", hex("#fffdf2"), .4f + .6f * k.tw(d, 0f, 0f))
}
private fun DrawScope.nebula(k: Kit, tag: String, n: Int, cols: List<String>, cx: Float, cy: Float, spread: Float, op: Float) = repeat(n) { i ->
    val x = cx + k.R("$tag nx$i", -spread, spread); val y = cy + k.R("$tag ny$i", -40f, 40f); val rx = k.R("$tag rx$i", 40f, 120f); val ry = k.R("$tag ry$i", 14f, 40f)
    rotate(k.R("$tag nr$i", -30f, 30f), Offset(190f, 65f)) {
        translate(x, y) { scale(1f, ry / rx, Offset.Zero) { drawCircle(Brush.radialGradient(listOf(hex(cols[i % cols.size], k.R("$tag no$i", op * .6f, op)), Color.Transparent), Offset.Zero, rx), rx, Offset.Zero) } } }
}
private val GOLD get() = Brush.linearGradient(listOf(hex("#fff4c4"), hex("#e8c55c"), hex("#8a6a1f"), hex("#ffe08a")), Offset(-40f, -40f), Offset(40f, 40f))
/** The black and gold 001 plate at the middle of each scene. */
private fun DrawScope.plate001(r: Float) {
    drawCircle(Brush.radialGradient(listOf(hex("#26241f"), hex("#0b0a08"), hex("#020202")), Offset(-r * .2f, -r * .3f), r * 1.5f), r, Offset.Zero)
    drawCircle(GOLD, r, Offset.Zero, style = Stroke(r * .06f)); drawCircle(GOLD, r * .86f, Offset.Zero, alpha = .7f, style = Stroke(r * .015f))
    repeat(8) { i -> val a = i * PI.toFloat() / 4 + PI.toFloat() / 8; val p = Offset(r * .62f * cos(a), r * .62f * sin(a))
        drawCircle(hex("#6e5014"), r * .045f, p); drawCircle(hex("#fff6cf"), r * .018f, p - Offset(r * .012f, r * .012f)) }
    drawCircle(Brush.radialGradient(listOf(hex("#fff3c0"), hex("#d9b04a"), hex("#6e5014")), Offset(-r * .1f, -r * .14f), r * .5f), r * .42f, Offset.Zero)
    drawCircle(hex("#5a410f"), r * .42f, Offset.Zero, style = Stroke(r * .015f))
    drawCircle(hex("#030303"), r * .2f, Offset.Zero); drawCircle(GOLD, r * .2f, Offset.Zero, style = Stroke(r * .035f))
    svgText("001", 0f, -r * .5f, r * .17f, hex("#e8c55c"), spacing = r * .03f)
}

fun DrawScope.drawSpace(k: String, uid: String, t: Float) {
    val K = Kit("sp$k$uid", t * 6f)
    when (k) {
        "feclipse" -> {
            drawRect(Brush.radialGradient(listOf(hex("#3d2708"), hex("#140c04"), hex("#010102")), Offset(274f, 65f), 300f), size = Size(380f, 130f))
            nebula(K, "n", 5, listOf("#5a3608", "#2a1a40", "#7a4e10"), 260f, 65f, 140f, .35f); stars(K)
            flare(K, 48f, 26f, 1f, "#ffd77a", 3.4f); flare(K, 150f, 104f, .7f, "#cfd8ff", 4.6f)
            translate(272f, 64f) {
                rotate(K.sec / 140f * 360f, Offset.Zero) { repeat(48) { i -> val len = K.R("ra$i", 60f, 150f); val w = K.R("rw$i", .8f, 2.6f)
                    rotate(i * 7.5f + K.R("rr$i", -2f, 2f), Offset.Zero) { fillP("M0 -40 L$w -$len L-$w -$len Z", hex("#ffd77a"), K.R("ro$i", .04f, .2f)) } } }
                rotate(-K.sec / 90f * 360f, Offset.Zero) { repeat(24) { i -> val len = K.R("qa$i", 50f, 95f)
                    rotate(i * 15f, Offset.Zero) { strokeP("M0 -40 Q${K.R("qx$i", -6f, 6f)} -${len * .6f} 0 -$len", hex("#fff0c0"), K.R("qw$i", .4f, 1.2f), K.R("qo$i", .08f, .25f)) } } }
                val cr = 72f + 10f * (.5f - .5f * cos(K.sec / 6f * TAU))
                drawCircle(Brush.radialGradient(0f to Color.Transparent, .5f to hex("#fff6d0"), .6f to hex("#ffc94a", .95f), .74f to hex("#e08a12", .35f), 1f to Color.Transparent, center = Offset.Zero, radius = cr), cr, Offset.Zero)
                drawCircle(Brush.radialGradient(0f to hex("#fff6d0"), .8f to hex("#ffc94a", .6f), 1f to Color.Transparent, center = Offset.Zero, radius = 50f), 50f, Offset.Zero)
                repeat(7) { i -> val a = K.R("pa$i", 0f, 360f); val h = K.R("ph$i", 8f, 18f); val f = K.ph(K.R("pd$i", 3f, 7f), K.R("pb$i", 0f, 7f))
                    rotate(a, Offset.Zero) { strokeP("M-4 -40 C-6 -${40 + h} 6 -${40 + h} 4 -40", hex("#ff9a2a"), K.R("pw$i", 1.2f, 2.6f), .9f * (if (f < .5f) f * 2 else (1 - f) * 2)) } }
                drawCircle(hex("#fffbe6"), 41.4f, Offset.Zero, style = Stroke(2.4f))
                val dr = .5f - .5f * cos(K.sec / 12f * TAU); translate(-2.5f * dr, 1f * dr) { plate001(41f) }
                val fl = K.ph(8f); val fa = if (fl in .86f..1f) (if (fl < .9f) (fl - .86f) / .04f else (1 - fl) / .1f) else 0f
                if (fa > 0) { drawCircle(hex("#fffdf0"), 3.4f, Offset(-29f, -29f), alpha = fa); drawLine(hex("#fffdf0"), Offset(-29f, -41f), Offset(-29f, -17f), 1.2f, alpha = fa); drawLine(hex("#fffdf0"), Offset(-41f, -29f), Offset(-17f, -29f), 1.2f, alpha = fa) }
            }
            repeat(34) { i -> val d = K.R("dd$i", 7f, 16f); val p = K.ph(d, K.R("db$i", 0f, 10f))
                drawCircle(hex("#ffe7a0"), K.R("dr$i", .4f, 1.3f), Offset(K.R("dx$i", 120f, 390f) + K.R("dmx$i", -40f, -12f) * p, K.R("dy$i", 0f, 130f) + K.R("dmy$i", -12f, 12f) * p), alpha = .9f * (if (p < .5f) p * 2 else (1 - p) * 2)) }
            svgText("TOTALITY · FOUNDER 001", 16f, 20f, 7f, hex("#e8c55c", .8f), align = 0, spacing = 4.5f)
        }
        "fconst" -> {
            drawRect(hex("#02030a"), size = Size(380f, 130f))
            rotate(8f * (.5f - .5f * cos(K.sec / 40f * TAU)), Offset(200f, 65f)) { nebula(K, "m", 8, listOf("#8a5a12", "#4a2f7a", "#b8862b", "#22164a", "#6a3a10"), 200f, 65f, 190f, .42f) }
            strokeP("M-10 100 Q120 60 240 70 T400 30", hex("#ffe7b0"), 22f, .05f); stars(K)
            flare(K, 210f, 22f, 1f, "#cfd8ff", 3.8f); flare(K, 168f, 110f, .8f, "#ffd77a", 4.4f)
            val T = 16f; val tt = K.sec % T
            val crown = listOf(70f to 72f, 62f to 48f, 80f to 60f, 94f to 36f, 108f to 60f, 126f to 48f, 118f to 72f); val pc = 288f to 62f
            val ring = List(12) { i -> (pc.first + 36 * cos(i * PI / 6 - PI / 2)).toFloat() to (pc.second + 36 * sin(i * PI / 6 - PI / 2)).toFloat() }
            val inner = List(6) { i -> (pc.first + 16 * cos(i * PI / 3)).toFloat() to (pc.second + 16 * sin(i * PI / 3)).toFloat() }
            /** How much of a line drawn from t0 is showing (draws over half a second, holds until 93% of the loop). */
            fun prog(t0: Float) = if (tt / T > .93f) 1 - (tt / T - .93f) / .07f else ((tt - t0) / .5f).coerceIn(0f, 1f)
            fun line(a: Pair<Float, Float>, b: Pair<Float, Float>, t0: Float) { val p = prog(t0); if (p <= 0f) return
                drawLine(hex("#ffd77a"), Offset(a.first, a.second), Offset(a.first + (b.first - a.first) * p, a.second + (b.second - a.second) * p), .9f, StrokeCap.Round, alpha = .55f + .45f * K.tw(2.4f, 0f, 0f)) }
            listOf(0, 1, 2, 3, 4, 5, 6, 0).zipWithNext().forEachIndexed { i, (x, y) -> line(crown[x], crown[y], .6f + i * .35f) }
            ring.forEachIndexed { i, p -> line(p, ring[(i + 1) % 12], 3.6f + i * .28f) }
            inner.forEachIndexed { i, p -> line(p, inner[(i + 1) % 6], 7.2f + i * .25f) }
            repeat(6) { i -> line(inner[i], ring[i * 2], 9f + i * .22f) }
            fun node(p: Pair<Float, Float>, s: Float, t0: Float) { val on = if (tt >= t0 && tt / T <= .93f) 1f else .35f
                translate(p.first, p.second) { drawCircle(hex("#ffd77a"), 5 * s, Offset.Zero, alpha = .2f * on); drawCircle(hex("#fff8e0"), 1.6f * s, Offset.Zero, alpha = on)
                    fillP("M0 -${5.5f * s} L.45 0 L0 ${5.5f * s} L-.45 0Z M-${5.5f * s} 0 L0 .45 L${5.5f * s} 0 L0 -.45Z", hex("#fff8e0"), .8f * on) } }
            crown.forEachIndexed { i, p -> node(p, if (i == 3) 1.3f else 1f, .6f + i * .35f) }; ring.forEachIndexed { i, p -> node(p, .85f, 3.6f + i * .28f) }
            inner.forEachIndexed { i, p -> node(p, .7f, 7.2f + i * .25f) }; node(pc, 1.5f, 10.4f)
            if (tt / T in .72f..0.93f) svgText("001", pc.first, pc.second + 3f, 8.5f, hex("#e8c55c"), spacing = 1.5f)
            fun shoot(t0: Float, x0: Float, y0: Float, x1: Float, y1: Float, len: Float) { val p = (tt - t0) / 1.1f; if (p !in 0f..1f) return
                val x = x0 + (x1 - x0) * p; val y = y0 + (y1 - y0) * p; val a = if (p < .2f) p / .2f else 1 - (p - .2f) / .8f
                drawLine(Brush.linearGradient(listOf(hex("#fff6d6"), Color.Transparent), Offset(x, y), Offset(x - len, y - len * .28f)), Offset(x, y), Offset(x - len, y - len * .28f), 1.5f, StrokeCap.Round, alpha = a)
                drawCircle(hex("#fffdf0"), 1.4f, Offset(x, y), alpha = a) }
            shoot(2.4f, -40f, 8f, 420f, 120f, 70f); shoot(12.2f, 60f, -10f, 330f, 110f, 55f)
            svgText("CONSTELLATION · FOUNDER 001", 16f, 20f, 7f, hex("#e8c55c", .8f), align = 0, spacing = 4.5f)
        }
        else -> {
            drawRect(hex("#020207"), size = Size(380f, 130f))
            nebula(K, "g", 4, listOf("#2a1a4a", "#3d2a08"), 120f, 60f, 140f, .4f); stars(K)
            flare(K, 40f, 22f, 1f, "#cfd8ff", 4f); flare(K, 120f, 108f, .8f, "#ffd77a", 3.2f)
            translate(270f, 64f) { rotate(-16f, Offset.Zero) {
                repeat(4) { arm -> repeat(10) { i -> val f = i / 10f; val a = arm * PI.toFloat() / 2 + f * 5.4f; val r = 14 + f * 140
                    drawCircle(Brush.radialGradient(listOf(hex(if (i % 3 != 0) "#c99a35" else "#5a3a8a", .14f), Color.Transparent), Offset(r * cos(a), r * sin(a) * .46f), 24f - f * 12f), 24f - f * 12f, Offset(r * cos(a), r * sin(a) * .46f)) } }
                rotate(K.sec / 160f * 360f, Offset.Zero) { scale(1f, .46f, Offset.Zero) { repeat(4) { arm -> repeat(120) { i -> val f = i / 120f; val a = arm * PI.toFloat() / 2 + f * 5.4f; val r = 10 + f * 150
                    val x = r * cos(a) + K.R("ax$arm-$i", -7f, 7f) * f * 2; val y = (r * sin(a) * .46f + K.R("ay$arm-$i", -4f, 4f) * f) / .46f
                    drawCircle(hex(if (f < .3f) "#fffaf0" else if (f < .6f) "#f3d77a" else if (f < .85f) "#c99a35" else "#8aa0d6"), K.R("ar$arm-$i", .35f, 1.7f) * (1.15f - f * .6f), Offset(x, y), alpha = .98f - f * .5f) } } } }
                scale(1f, 26f / 62f, Offset.Zero) { drawCircle(Brush.radialGradient(0f to hex("#fffbe8"), .3f to hex("#ffd77a", .9f), 1f to Color.Transparent, center = Offset.Zero, radius = 62f), 62f, Offset.Zero) }
                rotate(16f, Offset.Zero) { plate001(17f); val p = K.ph(4.5f); drawCircle(hex("#fff6d6"), 17f + 17f * p, Offset.Zero, alpha = .6f * (1 - p), style = Stroke(1f)) }
                fun planet(rx: Float, ry: Float, dur: Float, r: Float, c1: String, c2: String, ringed: Boolean, b: Float) { val a = K.ph(dur, b) * TAU; val p = Offset(rx * cos(a), ry * sin(a))
                    translate(p.x, p.y) { if (ringed) rotate(-14f, Offset.Zero) { drawOval(hex("#e8c55c"), Offset(-r * 2.2f, -r * .55f), Size(r * 4.4f, r * 1.1f), alpha = .8f, style = Stroke(1f)) }
                        drawCircle(hex(c1), r, Offset.Zero); drawCircle(Brush.radialGradient(0f to Color.White.copy(.4f), .6f to Color.Black.copy(.1f), 1f to Color.Black.copy(.65f), center = Offset(-r * .36f, -r * .4f), radius = r * 1.5f), r, Offset.Zero)
                        strokeP("M-$r 0 A$r ${r * .3f} 0 0 0 $r 0", hex(c2), r * .25f, .5f) } }
                planet(84f, 36f, 26f, 5f, "#c9873a", "#7a4a1a", true, 4f); planet(124f, 52f, 44f, 3.2f, "#8c95b8", "#4a5270", false, 22f); planet(58f, 24f, 16f, 2.3f, "#e8c55c", "#8a6a1f", false, 8f)
            } }
            val cp = K.ph(13f); if (cp in .55f..0.76f) { val f = cp; val x = -40f + 460f * f; val y = 120f - 130f * f; val a = if (f < .58f) (f - .55f) / .03f else if (f > .72f) (.76f - f) / .04f else 1f
                drawCircle(hex("#fff8e0"), 2.6f, Offset(x, y), alpha = a); drawLine(hex("#ffe7a0"), Offset(x, y), Offset(x - 44f, y + 13f), 1.8f, StrokeCap.Round, alpha = .7f * a)
                drawLine(hex("#cfe0ff"), Offset(x, y), Offset(x - 30f, y + 4f), 1f, StrokeCap.Round, alpha = .5f * a) }
            svgText("GALAXY 001 · THE ORIGIN", 16f, 20f, 7f, hex("#e8c55c", .8f), align = 0, spacing = 4.5f)
        }
    }
}

