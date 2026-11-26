package com.overload.app

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.cos
import kotlin.math.sin

/*
 * Artwork drawn over special plates (port of the web app's finishOverlay / themedOverlay).
 * Everything is in a 100 x 100 box with the plate centred at 50,50, radius ~47.
 */

private val C50 = Offset(50f, 50f)
private fun pol(r: Float, deg: Float) = Offset(50f + r * cos(Math.toRadians(deg.toDouble())).toFloat(), 50f + r * sin(Math.toRadians(deg.toDouble())).toFloat())

/** Silhouettes for lift legend plates. */
private fun liftSil(k: String) = when {
    Regex("bench|incline").containsMatchIn(k) -> "M28 58h44M34 58v8M66 58v8M40 50c4-3 16-3 20 0M50 47v-8M44 39h12M22 52v12M78 52v12"
    Regex("squat|hack|legpress").containsMatchIn(k) -> "M30 34h40M24 28v12M76 28v12M50 36a5 5 0 1 1 0-1M50 41v12l-8 6 4 10M50 53l8 6-4 10"
    Regex("dead|rdl").containsMatchIn(k) -> "M26 70h48M20 63v14M80 63v14M50 34a5 5 0 1 1 0-1M50 40l-2 14 4 14M48 54l-8 8"
    k == "ohp" -> "M28 26h44M22 20v12M78 20v12M50 44a5 5 0 1 1 0-1M44 30l6 9 6-9M50 49v14l-6 9M50 63l6 9"
    else -> "M28 58h44M34 58v8M66 58v8M40 50c4-3 16-3 20 0M50 47v-8M44 39h12M22 52v12M78 52v12"
}

/** Draws the design for plate [k] (no-op for plain colours). [seed] varies the random bits per plate. */
fun DrawScope.plateOverlay(k: String, seed: String = k, og: Int = 0) {
    fun R(n: String, a: Float, b: Float) = rnd(seed + k + n, a, b)
    val white = Color.White
    if (k.startsWith("l_")) {
        drawCircle(hex("#e3c25a"), 42f, C50, style = Stroke(1.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 2f))))
        translate(35f, 4f) { scale(.3f, .3f, Offset.Zero) { strokeP(liftSil(k.drop(2)), hex("#e3c25a"), 4.4f) } }
        svgText("LEGEND", 50f, 86f, 6f, hex("#e3c25a"), spacing = 1.6f); return
    }
    when (k) {
        // ---------- finishes ----------
        "mirror" -> drawCircle(Brush.linearGradient(listOf(white.copy(0f), white.copy(.9f), hex("#5c626c", .7f), white.copy(.6f), white.copy(0f)), Offset(3f, 3f), Offset(97f, 97f)), 47f, C50)
        "goldleaf" -> repeat(16) { i -> val a = Math.floorMod(hashS(seed + i), 360L).toFloat(); val r = 12f + Math.floorMod(hashS(seed + "r" + i), 32L); val p = pol(r, a)
            fillP("M${p.x} ${p.y}l${3 + i % 3} 1l-1 ${3 + i % 2}l-${2 + i % 3}-1z", hex("#fff3c2"), .75f) }
        "carbon" -> { var y = 3f; var row = 0; while (y < 97f) { var x = 3f + (row % 2) * 2.5f; while (x < 97f) { if ((x - 50) * (x - 50) + (y - 50) * (y - 50) < 47 * 47) drawRect(hex("#34363c"), Offset(x, y), Size(2.5f, 2.5f)); x += 5f }; y += 2.5f; row++ } }
        "lava" -> listOf("M10 40 Q22 44 30 36 T48 30", "M60 88 Q66 74 80 72 T92 56", "M20 76 Q30 70 34 80").forEach { strokeP(it, hex("#ffb347"), 1.6f, .85f) }
        "holo" -> drawCircle(Brush.sweepGradient(listOf(hex("#ff7ad9"), hex("#7ab8ff"), hex("#7affc4"), hex("#ffe27a"), hex("#c47aff"), hex("#ff7ad9")), C50), 47f, C50, alpha = .55f)
        "nebula" -> { rotate(-20f, C50) { drawOval(hex("#ff4fd8", .35f), Offset(14f, 28f), Size(60f, 24f)) }; rotate(25f, C50) { drawOval(hex("#4fb3ff", .3f), Offset(32f, 50f), Size(52f, 20f)) }
            repeat(14) { i -> drawCircle(white, .7f, Offset(10f + Math.floorMod(hashS(seed + "x" + i), 80L), 10f + Math.floorMod(hashS(seed + "y" + i), 80L)), alpha = .7f) } }
        "neonring" -> { drawCircle(hex("#00e5ff"), 42f, C50, style = Stroke(2f)); drawCircle(hex("#00e5ff", .18f), 42f, C50, style = Stroke(5f))
            drawCircle(hex("#ff2bd6"), 31f, C50, style = Stroke(1.6f)); drawCircle(hex("#ff2bd6", .18f), 31f, C50, style = Stroke(4.5f)) }
        "diamondp" -> { var y = 6f; while (y < 94f) { var x = 6f; while (x < 94f) { if ((x - 50) * (x - 50) + (y - 50) * (y - 50) < 44 * 44) rotate(45f, Offset(x + 2, y + .7f)) { drawRoundRect(hex("#eef1f4", .8f), Offset(x, y), Size(4f, 1.4f), androidx.compose.ui.geometry.CornerRadius(.7f)) }; x += 7f }; y += 7f } }
        "frost" -> repeat(6) { i -> rotate(i * 60f, C50) { strokeP("M50 50V8M50 20l-5-5M50 20l5-5M50 32l-6-6M50 32l6-6", white, .9f, .85f) } }
        "radioactive" -> { drawCircle(hex("#e7ff7a"), 40f, C50, style = Stroke(1.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3f, 5f))))
            fillP("M22 20c2 6 4 10 2 14-2-3-4-8-2-14z", hex("#d8ff5a"), .8f); fillP("M76 24c2 7 3 11 1 15-2-4-3-9-1-15z", hex("#d8ff5a"), .8f); drawCircle(hex("#c8ff5a"), 46f, C50, alpha = .12f) }
        "camo" -> { fillP("M14 30c8-10 22-6 20 4s-16 8-20-4z", hex("#2f3a1c"), .9f); fillP("M58 18c10-4 20 4 14 12s-18 2-14-12z", hex("#8a9a5b"), .9f)
            fillP("M60 64c12-4 22 6 14 14s-20 2-14-14z", hex("#3d2f1c"), .9f); fillP("M20 66c6-8 18-4 16 6s-14 6-16-6z", hex("#8a9a5b"), .9f); fillP("M40 44c6-4 12 0 10 6s-10 2-10-6z", hex("#3d2f1c"), .9f) }
        "marble" -> { strokeP("M8 34c14 4 18 18 34 16s20 10 34 4", hex("#8f8a82"), 1f, .55f); strokeP("M20 78c10-8 22-4 30-14s18-8 30-20", hex("#8f8a82"), .7f, .55f); strokeP("M30 14c4 8 12 10 14 20", hex("#8f8a82"), .5f, .55f) }
        "bloodmoon" -> { listOf(Triple(34f, 34f, 6f), Triple(66f, 70f, 8f), Triple(72f, 30f, 3.5f), Triple(28f, 70f, 4f)).forEach { (x, y, r) -> drawCircle(hex("#3a0408"), r, Offset(x, y), alpha = .55f) }
            drawCircle(hex("#ff3d4a"), 46.5f, C50, style = Stroke(1.4f)) }
        "laurel" -> repeat(18) { i -> val p = pol(41f, i * 20f); rotate(i * 20f, p) { drawOval(hex("#c9a640"), Offset(p.x - 2.2f, p.y - 4.6f), Size(4.4f, 9.2f)) } }
        // ---------- rank plates ----------
        "r_bronze" -> { repeat(22) { i -> drawCircle(if (i % 2 == 1) hex("#e0a066") else hex("#4a2a0e"), R("r$i", 2f, 5f), Offset(R("x$i", 18f, 82f), R("y$i", 18f, 82f)), alpha = .35f) }
            repeat(10) { i -> val p = pol(42f, i * 36f); drawCircle(hex("#f0c088"), 1.9f, p); drawCircle(hex("#5a3210"), 1.9f, p, style = Stroke(.6f)) } }
        "r_silver" -> { listOf(44f, 40f, 36f, 32f, 28f).forEach { drawCircle(white, it, C50, alpha = .6f, style = Stroke(.35f)) }
            drawCircle(hex("#5b6370"), 43f, C50, style = Stroke(1.4f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(1f, 2.2f))))
            repeat(12) { i -> rotate(i * 30f, C50) { fillP("M50 9l2 4-2 2-2-2z", hex("#5b6370")) } } }
        "r_gold" -> { repeat(24) { i -> rotate(i * 15f, C50) { fillP("M50 50L49 8h2z", hex("#fff3b0"), .25f) } }
            fillP("M50 22l4 9 9 1-7 6 2 9-8-5-8 5 2-9-7-6 9-1z", hex("#fff3b0")); strokeP("M50 22l4 9 9 1-7 6 2 9-8-5-8 5 2-9-7-6 9-1z", hex("#6b4a0c"), .8f); drawCircle(hex("#fff3b0"), 43f, C50, style = Stroke(1f)) }
        "r_plat" -> repeat(12) { i -> val a = pol(47f, i * 30f); val b = pol(47f, (i + 1) * 30f); fillP("M50 50L${a.x} ${a.y}L${b.x} ${b.y}z", if (i % 2 == 1) white else hex("#7d8a9b"), if (i % 2 == 1) .35f else .25f) }
        "r_diamond" -> { repeat(8) { i -> val a = pol(47f, i * 45f); val b = pol(30f, (i + .5f) * 45f); fillP("M50 50L${a.x} ${a.y}L${b.x} ${b.y}z", if (i % 2 == 1) white else hex("#1f6fa8"), .35f) }
            listOf(30f to 28f, 70f to 34f, 64f to 72f).forEach { (x, y) -> fillP("M$x ${y - 5}l1.2 3.8 3.8 1.2-3.8 1.2-1.2 3.8-1.2-3.8-3.8-1.2 3.8-1.2z", white) } }
        "r_titan" -> { strokeP("M14 40l12 4 6 10 10 2 6 8M70 14l-4 14 8 10 10 2M60 86l-4-12 6-8 12-2", hex("#ff6a1f"), 1.3f, .8f); drawCircle(Color.Black, 5f, Offset(30f, 70f), alpha = .25f); drawCircle(Color.Black, 4f, Offset(74f, 56f), alpha = .25f) }
        "r_olympian" -> { plateOverlay("marble", seed); plateOverlay("laurel", seed) }
        // ---------- crew plates ----------
        "c_mpc" -> { repeat(12) { i -> drawCircle(hex("#bfc3ca"), 1.6f, pol(43f, i * 30f)) }; svgText("1,000,000", 50f, 22f, 7f, hex("#d6d9de")); svgText("LB CLUB", 50f, 84f, 7f, hex("#d6d9de"), spacing = 2f) }
        "c_storm" -> { drawOval(hex("#11152a", .85f), Offset(14f, 13f), Size(36f, 14f)); drawOval(hex("#11152a", .85f), Offset(42f, 9f), Size(40f, 16f)); fillP("M76 30l-8 14h6l-5 14 13-19h-7l5-9z", hex("#e8ecff")) }
        "c_phoenix" -> { fillP("M50 72c-4-10-4-20 0-28 4 8 4 18 0 28z", hex("#ffcf6b"), .9f); fillP("M48 50c-10-4-20-14-28-30 12 4 22 12 28 22zM52 50c10-4 20-14 28-30-12 4-22 12-28 22z", hex("#ffcf6b"), .9f)
            fillP("M50 72c-6 6-8 12-6 18 2-4 4-6 6-8 2 2 4 4 6 8 2-6 0-12-6-18z", hex("#ff7a1f")) }
        "c_raid" -> { strokeP("M30 30l40 40M70 30l-40 40", hex("#e6d6ff"), 2.6f); strokeP("M64 70l6 6M36 70l-6 6", hex("#8b5cf6"), 5f); drawCircle(hex("#12061a"), 7f, C50); drawCircle(hex("#e6d6ff"), 7f, C50, style = Stroke(1f)) }
        // ---------- funny plates ----------
        "f_chicken" -> listOf(-1, 1).forEach { sd -> translate(20f * sd, -4f) { rotate(20f * sd, C50) { val x = if (sd < 0) 40f else 60f
            drawOval(hex("#c8742b"), Offset(x - 9, 32f), Size(18f, 24f)); drawRoundRect(hex("#fff6e0"), Offset(x - 2, 54f), Size(4f, 16f), androidx.compose.ui.geometry.CornerRadius(2f))
            drawCircle(hex("#fff6e0"), 3f, Offset(x - 3, 72f)); drawCircle(hex("#fff6e0"), 3f, Offset(x + 3, 72f)) } } }
        "f_ghost" -> { fillP("M34 70V44a16 16 0 0 1 32 0v26l-5-4-5 4-6-4-6 4-5-4z", white, .9f); drawCircle(hex("#2a2f45"), 2.6f, Offset(44f, 46f)); drawCircle(hex("#2a2f45"), 2.6f, Offset(56f, 46f)); drawOval(hex("#2a2f45"), Offset(47f, 51f), Size(6f, 8f)) }
        "f_moon" -> { drawCircle(hex("#ffe9a8"), 16f, Offset(56f, 44f)); drawCircle(hex("#141b3d"), 15f, Offset(63f, 38f)); repeat(10) { i -> drawCircle(white, .9f, Offset(R("sx$i", 16f, 84f), R("sy$i", 16f, 84f))) }
            svgText("NIGHT SHIFT", 50f, 82f, 5.4f, hex("#ffe9a8"), spacing = 1.4f) }
        "f_couch" -> translate(0f, -28f) { drawRoundRect(hex("#7a3a2a"), Offset(24f, 50f), Size(52f, 16f), androidx.compose.ui.geometry.CornerRadius(4f)); drawRoundRect(hex("#8a4a3a"), Offset(20f, 44f), Size(10f, 22f), androidx.compose.ui.geometry.CornerRadius(3f))
            drawRoundRect(hex("#8a4a3a"), Offset(70f, 44f), Size(10f, 22f), androidx.compose.ui.geometry.CornerRadius(3f)); drawOval(hex("#c9954f"), Offset(38f, 38f), Size(24f, 16f)); drawCircle(hex("#3a2410"), 1.2f, Offset(46f, 44f)); drawCircle(hex("#3a2410"), 1.2f, Offset(54f, 44f)) }
        "f_peacock" -> repeat(9) { i -> rotate(-80f + i * 20f, Offset(50f, 78f)) { drawLine(hex("#3cc9b5"), Offset(50f, 78f), Offset(50f, 24f), 1f); drawOval(hex("#1a8f7a"), Offset(45f, 19f), Size(10f, 14f)); drawOval(hex("#1d3a8a"), Offset(47.4f, 23.4f), Size(5.2f, 7.2f)); drawCircle(hex("#ffd23f"), 1.2f, Offset(50f, 27.5f)) } }
        // ---------- holiday plates ----------
        "h_xmas" -> { repeat(7) { i -> translate(R("x$i", 18f, 82f), R("y$i", 18f, 82f)) { scale(.5f, .5f, Offset.Zero) { strokeP("M0-6V6M-5-3l10 6M-5 3l10-6", white, 1.4f, .75f) } } }
            fillP("M50 26l-14 26h8l-10 18h32l-10-18h8z", hex("#0f6b2f")); drawRect(hex("#5a3210"), Offset(47f, 70f), Size(6f, 6f)); drawCircle(hex("#ffd23f"), 2.4f, Offset(50f, 26f)); drawCircle(hex("#ffd23f"), 1.6f, Offset(44f, 52f)); drawCircle(white, 1.6f, Offset(56f, 60f)) }
        "h_newyear" -> { listOf(Triple(34f, 34f, "#ffd23f"), Triple(64f, 40f, "#ff4fd8"), Triple(50f, 64f, "#4fb3ff")).forEach { (cx, cy, c) -> repeat(12) { i -> rotate(i * 30f, Offset(cx, cy)) { drawLine(hex(c), Offset(cx, cy), Offset(cx, cy - 12), 1.4f, StrokeCap.Round) } } }
            svgText("NEW YEAR", 50f, 84f, 6f, hex("#e3c25a"), spacing = 1.8f) }
        "h_valentine" -> repeat(7) { i -> translate(R("x$i", 22f, 78f), R("y$i", 22f, 78f)) { val s2 = R("s$i", .5f, 1.1f); scale(s2, s2, Offset.Zero) { fillP("M0 6C-8 0-8-6-4-7c2 0 3 1 4 3 1-2 2-3 4-3 4 1 4 7-4 13z", white, .85f) } } }
        "h_stpat" -> repeat(5) { i -> translate(R("x$i", 24f, 76f), R("y$i", 24f, 76f)) { val s2 = R("s$i", .6f, 1f); scale(s2, s2, Offset.Zero) { val g = hex("#c9f5d6"); drawCircle(g, 4f, Offset(0f, -4f)); drawCircle(g, 4f, Offset(-4f, 1f)); drawCircle(g, 4f, Offset(4f, 1f)); strokeP("M0 2q1 6 3 9", g, 1.4f) } } }
        "h_july4" -> { val clip = androidx.compose.ui.graphics.Path().apply { addOval(androidx.compose.ui.geometry.Rect(3f, 3f, 97f, 97f)) }
            clipPath(clip) { repeat(7) { i -> drawRect(hex("#d62839"), Offset(0f, 50f + i * 7), Size(100f, 3.5f)); drawRect(white, Offset(0f, 53.5f + i * 7), Size(100f, 3.5f)) } }
            repeat(9) { i -> fillP("M${28 + (i % 3) * 22} ${20 + (i / 3) * 10}l1.4 3 3.2.4-2.4 2.2.6 3.2-2.8-1.6-2.8 1.6.6-3.2-2.4-2.2 3.2-.4z", white) } }
        "h_halloween" -> { fillP("M38 38l6 6-6 2zM62 38l-6 6 6 2z", hex("#2a1200")); fillP("M34 58q16 12 32 0l-4 4-4-3-4 4-4-4-4 4-4-4-4 3z", hex("#2a1200")); strokeP("M50 18q4-6 8-6", hex("#2f5d1a"), 2.4f) }
        "h_thanks" -> { listOf("#c0392b", "#e67e22", "#f1c40f", "#8e5b2b", "#e67e22", "#c0392b", "#f1c40f").forEachIndexed { i, c -> rotate(-60f + i * 20f, Offset(50f, 70f)) { drawLine(hex(c, .85f), Offset(50f, 70f), Offset(50f, 30f), 9f, StrokeCap.Round) } }
            drawCircle(hex("#7a4a24"), 9f, Offset(50f, 66f)); drawCircle(hex("#8e5b2b"), 5f, Offset(50f, 58f)); drawCircle(Color.Black, .9f, Offset(48f, 57f)); fillP("M50 60l3 1-3 1z", hex("#f1c40f")) }
        // ---------- special ----------
        "og" -> { drawCircle(hex("#2a2a2e"), 43f, C50, style = Stroke(1.2f)); svgText("OG", 50f, 38f, 12f, hex("#2a2a2e"))
            if (og > 0) rimText("OG #${og.toString().padStart(3, '0')} · EARLY LIFTER · OG #${og.toString().padStart(3, '0')}", 50f, 50f, 40f, 5.6f, hex("#2a2a2e"), 1.4f) }
    }
}

/** The Founder 001 plate (0.16): black and gold, crown, laurels, gold hub, one rim engraving. */
fun DrawScope.founderPlate(label: String = "", ring: Float? = null) {
    val gold = Brush.linearGradient(listOf(hex("#fff6cf"), hex("#e8c55c"), hex("#9c7424"), hex("#f3d77a")), Offset(0f, 0f), Offset(100f, 100f))
    drawCircle(Brush.radialGradient(listOf(hex("#2c2a2f"), hex("#0f0f13"), hex("#030304")), Offset(40f, 35f), 75f), 48f, C50)
    drawCircle(gold, 47.4f, C50, style = Stroke(1.8f)); drawCircle(gold, 40.4f, C50, style = Stroke(.9f))
    val lbl = label.uppercase().replace(Regex("[^A-Z0-9 ]"), "").trim().take(12)
    rimText("FOUNDER · 001 · OVERLOAD${if (lbl.isNotEmpty()) " · $lbl" else ""} · EST 2026 ·", 50f, 50f, 42.6f, 4.5f, hex("#e8c55c"), 1.15f)
    drawCircle(hex("#e8c55c"), 37.6f, C50, alpha = .7f, style = Stroke(.7f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(.5f, 2.4f))))
    for (i in 0 until 7) { val a = 124f + i * 13.5f; val r = if (i % 2 == 1) 34.6f else 31.6f
        listOf(a to -1, 180f - a to 1).forEach { (aa, side) -> val p = pol(r, aa); val rot = aa + (if (side < 0) 90f else -90f) + (if (side < 0) -28f else 28f)
            rotate(rot, p) { drawOval(gold, Offset(p.x - 1.35f, p.y - 3.1f), Size(2.7f, 6.2f)) } } }
    fillP("M41.5 25 L40 16.5 L44.8 20.2 L50 13.6 L55.2 20.2 L60 16.5 L58.5 25 Z", gold); strokeP("M41.5 25 L40 16.5 L44.8 20.2 L50 13.6 L55.2 20.2 L60 16.5 L58.5 25 Z", hex("#6e5014"), .4f)
    drawCircle(hex("#fff6cf"), 1.1f, Offset(50f, 13.4f)); drawRoundRect(gold, Offset(41.6f, 25.2f), Size(16.8f, 1.6f), androidx.compose.ui.geometry.CornerRadius(.8f))
    svgText("001", 50f, 87.4f, 8.2f, hex("#e8c55c"), spacing = 1.4f)
    drawCircle(Color.Black.copy(.45f), 23.2f, C50); drawCircle(Brush.radialGradient(listOf(hex("#fff3c0"), hex("#d9b04a"), hex("#6e5014")), Offset(38f, 32f), 30f), 22f, C50)
    drawCircle(hex("#5a410f"), 22f, C50, style = Stroke(.7f)); drawCircle(hex("#7a5a18"), 15.6f, C50, style = Stroke(.7f))
    repeat(6) { i -> val p = pol(18.8f, i * 60f + 30f); drawCircle(hex("#5a410f"), 1.3f, p); drawCircle(hex("#fff6cf"), .55f, p - Offset(.35f, .35f)) }
    drawCircle(hex("#050506"), 9.5f, C50); drawCircle(gold, 9.5f, C50, style = Stroke(1.5f))
    if (ring != null) {
        drawCircle(hex("#e8c55c", .3f), 49f, C50, style = Stroke(2f))
        drawArc(hex("#f3d77a"), -90f, 360f * ring.coerceIn(0f, 1f), false, Offset(1f, 1f), Size(98f, 98f), style = Stroke(2.4f, cap = StrokeCap.Round))
    }
}
