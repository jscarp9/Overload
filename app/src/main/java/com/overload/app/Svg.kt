package com.overload.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.max
import kotlin.math.min

/*
 * Small helpers so the web app's SVG artwork (scenes, crests, bosses, trophies, plate designs) can be
 * drawn in Compose from the same path strings.
 */

private val pathCache = HashMap<String, Path>()

/** Parses an SVG path ("M0 0L10 10z") once and caches it. Don't mutate the result. */
fun svgPath(d: String): Path = synchronized(pathCache) { pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() } }

/** Hex colour like "#ef2b36" (or "#abc"). Falls back to grey. */
fun hex(s: String?, alpha: Float = 1f): Color = try {
    var h = (s ?: "").trim().removePrefix("#")
    if (h.length == 3) h = h.map { "$it$it" }.joinToString("")
    Color(("FF$h").toLong(16)).copy(alpha = alpha)
} catch (e: Exception) { Color(0xFF8C939E).copy(alpha = alpha) }

/**
 * Draws [block] in a [vw] x [vh] viewBox scaled to this canvas.
 * slice = true covers the whole area (CSS "xMidYMid slice"), false fits inside it ("meet").
 */
inline fun DrawScope.viewBox(vw: Float, vh: Float, slice: Boolean = false, block: DrawScope.() -> Unit) {
    val sx = size.width / vw; val sy = size.height / vh
    val s = if (slice) max(sx, sy) else min(sx, sy)
    val dx = (size.width - vw * s) / 2f; val dy = (size.height - vh * s) / 2f
    withTransform({ translate(dx, dy); scale(s, s, Offset.Zero) }) { block() }
}

fun DrawScope.fillP(d: String, c: Color, alpha: Float = 1f) = drawPath(svgPath(d), c, alpha = alpha)
fun DrawScope.fillP(d: String, b: Brush, alpha: Float = 1f) = drawPath(svgPath(d), b, alpha = alpha)
fun DrawScope.strokeP(d: String, c: Color, w: Float, alpha: Float = 1f, round: Boolean = true) =
    drawPath(svgPath(d), c, alpha = alpha, style = Stroke(w, cap = if (round) StrokeCap.Round else StrokeCap.Butt, join = if (round) StrokeJoin.Round else StrokeJoin.Miter))

/** Text in viewBox units. align: 0 = left, 1 = centre. */
fun DrawScope.svgText(t: String, x: Float, y: Float, sizePx: Float, c: Color, bold: Boolean = true, align: Int = 1, spacing: Float = 0f) =
    drawIntoCanvas { cv ->
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = c.toArgb(); textSize = sizePx; isFakeBoldText = bold
            textAlign = if (align == 1) android.graphics.Paint.Align.CENTER else android.graphics.Paint.Align.LEFT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
            if (spacing > 0) letterSpacing = spacing / sizePx
        }
        cv.nativeCanvas.drawText(t, x, y, p)
    }

/** Text running around a circle (the plate rim). Starts at the left and goes clockwise over the top. */
fun DrawScope.rimText(t: String, cx: Float, cy: Float, r: Float, sizePx: Float, c: Color, spacing: Float = 0f) =
    drawIntoCanvas { cv ->
        val path = android.graphics.Path().apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) }
        val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = c.toArgb(); textSize = sizePx; isFakeBoldText = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            if (spacing > 0) letterSpacing = spacing / sizePx
        }
        // addCircle starts at 3 o'clock; offset half way round to start on the left like the web app
        val circ = (2 * Math.PI * r).toFloat()
        cv.nativeCanvas.drawTextOnPath(t, path, circ * .5f, 0f, p)
    }

/** Deterministic 0..1 value from a string, like the web app's R() helper (uses hashS). */
fun rnd(seed: String): Float = (Math.floorMod(hashS(seed), 1000L)) / 1000f
fun rnd(seed: String, a: Float, b: Float) = a + (b - a) * rnd(seed)
