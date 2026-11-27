package com.overload.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/*
 * 0.14-0.16 artwork, drawn from the web app's SVG: animated scene banners (crew + profile), the crew
 * crest, weekly bosses and trophies.
 */

// ============================ SCENES ============================
val SCENES = linkedMapOf(
    "forge" to Triple("Forge", "Crew level 1", 1), "city" to Triple("Night City", "Crew level 2", 2), "ocean2" to Triple("Ocean Storm", "Crew level 3", 3),
    "gym" to Triple("Iron Gym", "Crew level 4", 4), "aurora" to Triple("Northern Lights", "Crew level 6", 6), "volcano" to Triple("Volcano", "Crew level 8", 8),
    "colosseum" to Triple("Colosseum", "Crew rank Gold", -3), "space" to Triple("Deep Space", "Crew rank Diamond", -5), "thunder" to Triple("Thunder", "Million Pound Club trophy", 0),
    "olympus" to Triple("Olympus", "Crew rank Olympian", -7), "founder" to Triple("Founder", "Founder only", 0)
).apply { HOL19.values.forEach { h -> put("hb_${h.k}", Triple(h.n, "3 crew lifts on ${h.n}", 0)) } }
val LEGACY_SCENE = mapOf("steel" to "gym", "ember" to "forge", "ocean" to "ocean2", "toxic" to "volcano", "royal" to "aurora", "gold" to "olympus",
    "vault" to "feclipse", "fblue" to "fconst", "throne" to "fgalaxy")
/** Can the crew use this banner? (level, crew rank, trophy, or the founder's crew) */
fun sceneOK(cs: CrewState?, k: String): Boolean {
    val c = cs?.crew ?: return false; val r = SCENES[k]?.third ?: return false
    return when {
        k == "founder" -> c.fcrew
        k.startsWith("hb_") -> crewHol(cs, k.drop(3))
        k == "thunder" -> c.trophies.containsKey("mpc")
        r > 0 -> c.level >= r
        r < 0 -> c.rank >= -r
        else -> false
    }
}

@Composable
fun Scene(k0: String, modifier: Modifier = Modifier, level: Int = 8, uid: String = k0) {
    val k = LEGACY_SCENE[k0] ?: k0
    val tr = rememberInfiniteTransition(label = "scene")
    // the founder space scenes move slowly, so they run on a 60 second loop (t still counts 0..10 "6 second" beats)
    val slow = k in SPACE20
    val t0 by tr.animateFloat(0f, 1f, infiniteRepeatable(tween(if (slow) 60000 else 6000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    val t = if (slow) t0 * 10f else t0
    Canvas(modifier.aspectRatio(380f / 130f)) { viewBox(380f, 130f, slice = true) { drawScene(k, level, uid, t) } }
}

private fun DrawScope.drawScene(k: String, lvl: Int, u: String, t: Float) {
    val L = maxOf(1, lvl)
    fun R(n: String, a: Float, b: Float) = rnd(u + n, a, b)
    fun tw(i: Int, d: Float) = (.35f + .65f * abs(sin((t * 2 + d / 3f + i * .37f) * PI.toFloat())))
    fun dots(n: Int, cls: String, y0: Float, y1: Float, col: Color, r0: Float, r1: Float, rise: Float = 0f) = repeat(n) { i ->
        val d = R("d$cls$i", 0f, 3f); val y = R("y$cls$i", y0, y1) - rise * ((t + d / 3f) % 1f)
        drawCircle(col, R("r$cls$i", r0, r1), Offset(R("x$cls$i", 10f, 370f), y), alpha = tw(i, d)) }
    fun bg(c: String) = drawRect(hex(c), size = Size(380f, 130f))
    when (k) {
        "forge" -> { bg("#140b07"); drawOval(hex("#7a2a0c"), Offset(160f, 80f), Size(260f, 84f), alpha = .7f + .3f * tw(0, 0f)); drawOval(hex("#e0571b"), Offset(226f, 102f), Size(128f, 40f))
            fillP("M232 92h108l-14 12h-28v14h20v8h-64v-8h20v-14h-30z", hex("#0b0605")); drawRoundRect(hex("#1c1210"), Offset(250f, 66f), Size(72f, 7f), CornerRadius(3f))
            drawRoundRect(hex("#0b0605"), Offset(238f, 52f), Size(12f, 34f), CornerRadius(3f)); drawRoundRect(hex("#0b0605"), Offset(322f, 52f), Size(12f, 34f), CornerRadius(3f))
            dots(6 + minOf(10, L), "em", 104f, 118f, hex("#ffb347"), 1.2f, 2.4f, rise = 40f) }
        "city" -> { bg("#0a0f24"); dots(8 + minOf(12, L), "tw", 4f, 40f, Color.White, .6f, 1.3f); drawCircle(hex("#e8e6d9"), 12f, Offset(330f, 30f)); drawCircle(hex("#0a0f24"), 11f, Offset(335f, 26f))
            fillP("M0 130V92h22V70h18v22h14V58h26v34h12V78h20v14h16V48h24v44h14V66h22v26h12V74h18v18h20V54h28v38h14V70h20v22h16V60h24v70z", hex("#141a38"))
            repeat(14) { i -> drawRect(hex("#f5c46b"), Offset(R("wx$i", 20f, 360f), R("wy$i", 62f, 100f)), Size(3f, 3f), alpha = tw(i, R("wd$i", 0f, 3f))) }
            drawRect(hex("#070a18"), Offset(0f, 118f), Size(380f, 12f))
            val cx = -t * 560f; listOf(380f to "#ff4d4d", 470f to "#ffd27a", 540f to "#ff4d4d").forEach { (x, c) -> drawRoundRect(hex(c), Offset(x + cx + 180f, 122f), Size(10f, 2f), CornerRadius(1f)) } }
        "ocean2" -> { bg("#0b1a2e"); drawOval(hex("#22344d"), Offset(-20f, -6f), Size(220f, 44f)); drawOval(hex("#22344d"), Offset(170f, -10f), Size(240f, 44f))
            if (t % .5f > .45f) fillP("M300 20l-12 26h10l-8 24 22-32h-11l8-18z", hex("#eaf4ff"))
            translate(-20f * sin(t * 2 * PI.toFloat()), 0f) { fillP("M-40 96 Q-10 78 20 96 T80 96 T140 96 T200 96 T260 96 T320 96 T380 96 T440 96 V130 H-40Z", hex("#12406b")) }
            translate(20f * sin(t * 2 * PI.toFloat()), 0f) { fillP("M-40 108 Q-10 92 20 108 T80 108 T140 108 T200 108 T260 108 T320 108 T380 108 T440 108 V130 H-40Z", hex("#0c2c4c")) }
            dots(minOf(10, 2 + L), "spray", 84f, 100f, hex("#cfe8ff"), .7f, 1.4f) }
        "gym" -> { bg("#121316"); val fl = if (t % .3f < .02f) .4f else 1f
            drawRoundRect(hex("#f4f1e6"), Offset(40f, 6f), Size(120f, 5f), CornerRadius(2f), alpha = fl); drawRoundRect(hex("#f4f1e6"), Offset(220f, 6f), Size(120f, 5f), CornerRadius(2f))
            fillP("M40 11 L10 130 H190 L160 11Z", hex("#f4f1e6"), .06f * fl); fillP("M220 11 L190 130 H370 L340 11Z", hex("#f4f1e6"), .06f)
            drawRect(hex("#0a0a0c"), Offset(0f, 112f), Size(380f, 18f))
            listOf(floatArrayOf(250f, 92f, 100f, 5f), floatArrayOf(244f, 74f, 10f, 40f), floatArrayOf(346f, 74f, 10f, 40f), floatArrayOf(232f, 80f, 10f, 28f), floatArrayOf(358f, 80f, 10f, 28f)).forEach { r -> drawRoundRect(hex("#2a2c31"), Offset(r[0], r[1]), Size(r[2], r[3]), CornerRadius(2.5f)) }
            dots(6 + minOf(12, L), "dust", 40f, 110f, hex("#e8e3d6"), .6f, 1.2f, rise = 10f) }
        "aurora" -> { bg("#06121c"); dots(10, "tw", 4f, 50f, Color.White, .5f, 1.1f)
            translate(10f * sin(t * 2 * PI.toFloat()), 0f) { fillP("M-20 60 C60 20 120 70 200 36 S330 30 400 50 L400 70 C330 52 270 84 200 62 S60 54 -20 84Z", hex("#2ee6a6"), .45f) }
            translate(-10f * sin(t * 2 * PI.toFloat()), 0f) { fillP("M-20 44 C80 10 140 54 220 26 S340 20 400 36 L400 48 C330 34 280 64 210 44 S70 36 -20 60Z", hex("#7a5cff"), .35f) }
            fillP("M0 130 L70 92 L120 108 L190 80 L260 104 L320 86 L380 100 V130Z", hex("#e8f1f6")); fillP("M0 130 L70 104 L120 116 L190 96 L260 114 L320 100 L380 110 V130Z", hex("#c4d4de")) }
        "volcano" -> { bg("#1a0c0a"); dots(6 + minOf(10, L), "ash", 10f, 70f, hex("#6b5b57"), .8f, 1.8f); fillP("M150 130 L240 40 L262 40 L360 130Z", hex("#2a1512"))
            fillP("M246 40 C244 60 236 70 238 86 C240 100 230 112 226 130 L236 130 C240 110 250 100 248 84 C246 68 254 58 256 40Z", hex("#ff5a1f"), .8f + .2f * tw(1, 0f))
            drawOval(hex("#ffb347"), Offset(235f, 34f), Size(32f, 12f), alpha = tw(2, 1f)); dots(4 + minOf(8, L), "em", 30f, 44f, hex("#ffcf6b"), .8f, 1.6f, rise = 26f) }
        "colosseum" -> { bg("#2a1408"); drawCircle(hex("#ff8a3d"), 44f, Offset(300f, 96f), alpha = .55f); drawCircle(hex("#ffc16b"), 28f, Offset(300f, 96f), alpha = .8f)
            drawRect(hex("#3b2213"), Offset(120f, 62f), Size(220f, 68f))
            repeat(7) { i -> fillP("M${132 + i * 30} 130v-30a10 10 0 0 1 20 0v30z", hex("#1a0e07")); fillP("M${132 + i * 30} 92v-14a10 10 0 0 1 20 0v14z", hex("#1a0e07")) }
            drawRect(hex("#4a2c18"), Offset(120f, 58f), Size(220f, 6f)); drawRect(hex("#2a1508"), Offset(104f, 80f), Size(4f, 40f)); drawRect(hex("#2a1508"), Offset(352f, 80f), Size(4f, 40f))
            drawOval(hex("#ffb347"), Offset(101f, 68f), Size(10f, 16f), alpha = tw(3, 0f)); drawOval(hex("#ffb347"), Offset(349f, 68f), Size(10f, 16f), alpha = tw(4, 1f)) }
        "space" -> { bg("#05040f"); dots(18, "tw", 4f, 126f, Color.White, .4f, 1.2f)
            rotate(t * 30f, Offset(290f, 64f)) { drawOval(hex("#5b2bd6"), Offset(220f, 46f), Size(140f, 36f), alpha = .45f); drawOval(hex("#ff4fd8"), Offset(246f, 53f), Size(88f, 22f), alpha = .35f) }
            drawOval(Color.White, Offset(276f, 58f), Size(28f, 12f), alpha = .8f)
            val sp = (t * 3) % 1f; if (sp < .3f) drawLine(Color.White, Offset(40f + sp * 200, 20f + sp * 70), Offset(80f + sp * 200, 34f + sp * 70), 1.4f) }
        "thunder" -> { bg("#12141c"); val fl = (t % .5f) > .46f; if (fl) drawRect(hex("#5b6390"), size = Size(380f, 130f), alpha = .35f)
            drawOval(hex("#262a3a"), Offset(-10f, -4f), Size(180f, 44f)); drawOval(hex("#262a3a"), Offset(110f, -12f), Size(220f, 48f)); drawOval(hex("#262a3a"), Offset(260f, -2f), Size(160f, 44f))
            if (fl) fillP("M262 30l-16 34h14l-12 32 30-42h-15l12-24z", hex("#f4f0ff"))
            fillP("M0 130l60-52 34 26 52-48 46 40 40-30 64 50 38-22 46 36z", hex("#0b0c12")); drawCircle(hex("#9ba3c9", .25f), 16f, Offset(330f, 104f), style = Stroke(5f)); drawCircle(hex("#9ba3c9", .25f), 5f, Offset(330f, 104f)) }
        "olympus" -> { bg("#f3ead6"); drawOval(hex("#ffe7a8"), Offset(180f, -40f), Size(240f, 140f), alpha = .7f); dots(10, "tw", 6f, 60f, hex("#fff7d6"), .8f, 1.6f)
            val cl = 12f * sin(t * 2 * PI.toFloat()); drawOval(hex("#fffaf0"), Offset(-20f + cl, 92f), Size(160f, 40f)); drawOval(hex("#fffaf0"), Offset(240f - cl, 96f), Size(180f, 40f))
            fillP("M200 38 L300 18 L400 38Z", hex("#e8dcc0")); drawRect(hex("#d8c79f"), Offset(210f, 38f), Size(180f, 6f))
            repeat(6) { i -> drawRect(hex("#efe6d0"), Offset(218f + i * 28, 46f), Size(10f, 56f)); drawRect(hex("#cdb98c"), Offset(216f + i * 28, 44f), Size(14f, 3f)) }
            drawRect(hex("#d8c79f"), Offset(206f, 102f), Size(188f, 8f)) }
        // 0.16 banners
        "founder", "cofounder" -> { val sv = k == "cofounder"; fun g(a: String, b: String) = hex(if (sv) b else a)
            drawRect(Brush.radialGradient(listOf(g("#4a3510", "#2c3038"), g("#16110a", "#101216"), hex("#050505")), Offset(304f, 13f), 380f), size = Size(380f, 130f))
            repeat(6) { i -> fillP("M360 -20 L${120 + i * 46} 140 L${142 + i * 46} 140 Z", g("#f3d77a", "#eef1f5"), .02f + .07f * tw(i, 0f)) }
            val gold = Brush.horizontalGradient(listOf(g("#9c7424", "#7d838e"), g("#fff2bd", "#ffffff"), g("#9c7424", "#7d838e")))
            translate(312f, 66f) { rotate(t * 60f, Offset.Zero) { drawCircle(gold, 70f, Offset.Zero, alpha = .55f, style = Stroke(1.4f)); drawCircle(g("#e8c55c", "#d5dae2"), 58f, Offset.Zero, alpha = .55f, style = Stroke(.8f))
                repeat(12) { i -> val a = i * PI.toFloat() / 6; drawLine(g("#e8c55c", "#d5dae2").copy(.33f), Offset(cos(a) * 30, sin(a) * 30), Offset(cos(a) * 56, sin(a) * 56), .6f) } }
                drawCircle(hex("#0b0906"), 26f, Offset.Zero, alpha = .55f); fillP("M-12 8 L-14 -6 L-6 0 L0 -11 L6 0 L14 -6 L12 8 Z", gold, .55f) }
            repeat(26) { i -> val d = R("d$i", 5f, 11f); val ph = ((t * 6f / d) + R("b$i", 0f, 1f)) % 1f
                drawCircle(g("#f3d77a", "#eef1f5"), R("r$i", .6f, 1.9f), Offset(R("x$i", 8f, 372f), 140f - ph * 150f), alpha = if (ph < .15f) ph / .15f else if (ph > .7f) (1 - ph) / .3f else 1f) }
            drawRect(gold, Offset(0f, 0f), Size(380f, 2f)); drawRect(gold, Offset(0f, 128f), Size(380f, 2f)) }
        "feclipse", "fconst", "fgalaxy" -> drawSpace(k, u, t)
        "sunrise" -> { drawRect(Brush.verticalGradient(listOf(hex("#2b1d4f"), hex("#e2557a"), hex("#ffb35c")), 0f, 130f), size = Size(380f, 130f))
            drawCircle(hex("#ffe08a"), 30f, Offset(280f, 98f - 6f * sin(t * 2 * PI.toFloat())))
            fillP("M0 130 L0 96 Q60 70 120 92 T250 88 T380 84 V130Z", hex("#5a2846")); fillP("M0 130 L0 108 Q80 90 170 110 T380 104 V130Z", hex("#2c1530"))
            repeat(3) { i -> translate(40f * sin(t * PI.toFloat() + i), 0f) { strokeP("M${60 + i * 24} ${34 + i * 8} q4 -4 8 0 q4 -4 8 0", hex("#2c1530"), 1.4f) } } }
        "neon" -> { bg("#08060f"); repeat(13) { i -> drawLine(hex("#7a2cff", .55f), Offset(190f, 70f), Offset(-120f + i * 52, 130f), .8f) }
            listOf(78f, 88f, 102f, 122f).forEach { y -> drawLine(hex("#7a2cff", .55f), Offset(0f, y), Offset(380f, y), .8f) }
            val ph = (t * 4) % 1f; val on = ph !in .6f..0.68f; svgText("LIFT", 240f, 52f, 34f, hex("#ff3d8b", if (on) 1f else .3f), spacing = 6f)
            drawRoundRect(hex("#22e3ff"), Offset(200f, 62f), Size(80f, 2f), CornerRadius(1f), alpha = tw(0, 0f)) }
        "summit" -> { bg("#0c1426"); repeat(16) { i -> drawCircle(Color.White, R("sr$i", .4f, 1.1f), Offset(R("sx$i", 5f, 375f), R("sy$i", 4f, 60f)), alpha = tw(i, R("sd$i", 0f, 3f))) }
            fillP("M0 130 L90 70 L140 96 L230 30 L300 88 L380 60 V130Z", hex("#26324d")); fillP("M230 30 L214 46 L224 44 L232 52 L240 44 L248 48Z", hex("#eef3fb"))
            fillP("M0 130 L70 100 L160 118 L260 96 L380 112 V130Z", hex("#141d33")); drawLine(hex("#c9cfdb"), Offset(230f, 30f), Offset(230f, 12f), 1.2f); fillP("M230 12 l14 4 -14 4z", hex("#ef2b36")) }
        "chalk" -> { bg("#15161b"); repeat(14) { i -> drawCircle(hex("#e9e9ef"), R("cr$i", 8f, 26f), Offset(R("cx$i", 20f, 360f) + R("tx$i", -14f, 14f) * sin(t * 2 * PI.toFloat()), R("cy$i", 20f, 110f)), alpha = .05f) }
            listOf(floatArrayOf(200f, 74f, 150f, 5f), floatArrayOf(214f, 54f, 12f, 46f), floatArrayOf(324f, 54f, 12f, 46f), floatArrayOf(200f, 60f, 10f, 34f), floatArrayOf(340f, 60f, 10f, 34f)).forEach { r -> drawRoundRect(hex("#2a2c33"), Offset(r[0], r[1]), Size(r[2], r[3]), CornerRadius(2.5f)) }
            repeat(18) { i -> drawCircle(Color.White, .9f, Offset(R("dx$i", 180f, 370f), R("dy$i", 40f, 100f)), alpha = .5f * tw(i, R("db$i", 0f, 3f))) } }
        "blueprint" -> { bg("#0b2a4a"); repeat(20) { i -> drawLine(hex("#3a6ea5", .5f), Offset(i * 20f, 0f), Offset(i * 20f, 130f), .4f) }; repeat(7) { i -> drawLine(hex("#3a6ea5", .5f), Offset(0f, i * 20f), Offset(380f, i * 20f), .4f) }
            rotate(t * 30f, Offset(300f, 64f)) { drawCircle(hex("#cfe3ff", .9f), 40f, Offset(300f, 64f), style = Stroke(1.2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 3f)))) }
            drawCircle(hex("#cfe3ff", .9f), 12f, Offset(300f, 64f), style = Stroke(1.2f)); strokeP("M260 64h-60M200 54v20M120 30h60v68h-60z", hex("#cfe3ff"), 1.2f, .9f)
            strokeP("M130 44h40M130 56h30M130 68h36", hex("#cfe3ff"), 1.2f, .55f); svgText("OVERLOAD · BLUEPRINT · 001", 22f, 24f, 10f, hex("#cfe3ff", .8f), align = 0, spacing = 3f) }
        else -> if (k.startsWith("hb_")) drawHolScene(k.drop(3), u, t) else drawScene("forge", lvl, u, t)
    }
}

// ============================ BOSSES ============================
@Composable
fun BossView(k: String, modifier: Modifier = Modifier) = Canvas(modifier.aspectRatio(1f)) {
    val g = hex("#2a1640"); val l = hex("#8b5cf6"); val e = hex("#ffd23f")
    viewBox(100f, 100f) { when (k) {
        "golem" -> { drawCircle(g, 30f, Offset(50f, 56f)); drawCircle(hex("#120a1c"), 10f, Offset(50f, 56f)); drawCircle(l, 20f, Offset(50f, 56f), style = Stroke(3f))
            drawRoundRect(g.copy(.9f), Offset(34f, 18f), Size(32f, 20f), CornerRadius(6f)); drawCircle(e, 3f, Offset(43f, 28f)); drawCircle(e, 3f, Offset(57f, 28f)) }
        "hydra" -> listOf(28f, 50f, 72f).forEach { x -> strokeP("M50 90Q$x 60 $x 30", g, 10f); drawCircle(g, 9f, Offset(x, 26f)); drawCircle(e, 2f, Offset(x + 3, 24f)) }
        "kraken" -> { drawOval(g, Offset(28f, 16f), Size(44f, 40f)); listOf(22f, 36f, 50f, 64f, 78f).forEachIndexed { i, x -> strokeP("M${40 + i * 5} 52Q$x 74 ${x + (if (i % 2 == 1) 6 else -6)} 90", g, 6f) }
            drawCircle(e, 4f, Offset(42f, 36f)); drawCircle(e, 4f, Offset(58f, 36f)) }
        "dragon" -> { fillP("M50 30L12 14l14 30-10 10 34-8 34 8-10-10 14-30z", g); fillP("M40 46q10 30 10 44 0-14 10-44", l, .7f); drawCircle(e, 3f, Offset(44f, 34f)); drawCircle(e, 3f, Offset(56f, 34f)); strokeP("M42 20l-4-10M58 20l4-10", g, 4f) }
        "minotaur" -> { fillP("M30 30Q14 26 12 10q14 8 24 12M70 30q16-4 18-20-14 8-24 12", hex("#d8c9a8")); fillP("M26 30h48v30q0 26-24 30-24-4-24-30z", g)
            drawCircle(e, 4f, Offset(40f, 46f)); drawCircle(e, 4f, Offset(60f, 46f)); drawCircle(hex("#d8c9a8"), 6f, Offset(50f, 72f), style = Stroke(2.4f)) }
        "yeti" -> { fillP("M18 90q-4-50 32-66 36 16 32 66z", hex("#d9e4f2")); drawRoundRect(hex("#8ea4c2"), Offset(34f, 44f), Size(32f, 20f), CornerRadius(6f))
            drawCircle(hex("#1a2540"), 3f, Offset(43f, 52f)); drawCircle(hex("#1a2540"), 3f, Offset(57f, 52f)); strokeP("M42 60l4 3 4-3 4 3 4-3", Color.White, 1.6f) }
        "phoenix" -> { fillP("M50 88q-8-20 0-40 8 20 0 40z", hex("#ff7a1f")); fillP("M48 50Q30 40 12 16q22 6 36 28zM52 50q18-10 36-34-22 6-36 28z", hex("#ffb347")); drawCircle(hex("#ff7a1f"), 8f, Offset(50f, 40f)); drawCircle(Color.White, 2f, Offset(52f, 38f)) }
        else -> { fillP("M20 86V44q0-24 30-26 30 2 30 26v42z", g); drawRect(l.copy(.6f), Offset(30f, 40f), Size(40f, 10f)); drawRect(e, Offset(34f, 42f), Size(10f, 5f)); drawRect(e, Offset(56f, 42f), Size(10f, 5f)); strokeP("M38 64h24M40 70h20", l, 3f) }
    } }
}

// ============================ TROPHIES ============================
@Composable
fun TrophyView(t: Trophy, modifier: Modifier = Modifier) = Canvas(modifier.aspectRatio(1f)) {
    val c = if (t.earned) Color(t.color) else hex("#6b7080")
    val g = Brush.linearGradient(listOf(Color.White.copy(if (t.earned) .9f else .15f), c, if (t.earned) hex("#3a2a05") else hex("#2a2c31")), Offset(-34f, -34f), Offset(34f, 34f))
    val a = if (t.earned) 1f else .35f
    viewBox(68f, 68f) { translate(34f, 34f) { when (t.type) {
        "statue" -> { fillP("M0-30a7 7 0 1 1 0 14a7 7 0 1 1 0-14zM-10-14h20l-4 24h-12z", g, a); drawRoundRect(g, Offset(-14f, 10f), Size(28f, 7f), CornerRadius(2f), alpha = a); drawRoundRect(g, Offset(-18f, 17f), Size(36f, 8f), CornerRadius(2f), alpha = a) }
        "cup" -> { fillP("M-16-26h32v8c0 14-8 22-16 22s-16-8-16-22z", g, a); drawPath(svgPath("M-16-22h-6v4c0 6 4 10 9 10M16-22h6v4c0 6-4 10-9 10"), g, alpha = a, style = Stroke(3f))
            drawRect(g, Offset(-3f, 4f), Size(6f, 10f), alpha = a); drawRoundRect(g, Offset(-12f, 14f), Size(24f, 10f), CornerRadius(2f), alpha = a) }
        "medal" -> { fillP("M-10-30l10 16 10-16h8l-12 22h-12l-12-22z", g, a * .75f); drawCircle(g, 16f, Offset(0f, 8f), alpha = a); drawCircle(Color.White.copy(.5f), 10f, Offset(0f, 8f), alpha = a, style = Stroke(1.5f)) }
        "plaque" -> { drawRoundRect(g, Offset(-20f, -26f), Size(40f, 50f), CornerRadius(4f), alpha = a); drawRoundRect(Color.White.copy(.35f), Offset(-14f, -18f), Size(28f, 20f), CornerRadius(2f), alpha = a)
            drawRoundRect(Color.White.copy(.5f), Offset(-12f, 8f), Size(24f, 3f), CornerRadius(1f), alpha = a); drawRoundRect(Color.White.copy(.4f), Offset(-9f, 14f), Size(18f, 3f), CornerRadius(1f), alpha = a) }
        else -> { drawRoundRect(g, Offset(-28f, -3f), Size(56f, 5f), CornerRadius(2f), alpha = a); listOf(-24f to 7f, 17f to 7f).forEach { (x, w) -> drawRoundRect(g, Offset(x, -15f), Size(w, 30f), CornerRadius(2f), alpha = a) }
            listOf(-30f, 25f).forEach { x -> drawRoundRect(g, Offset(x, -10f), Size(5f, 20f), CornerRadius(2f), alpha = a) }; drawRoundRect(g, Offset(-14f, 18f), Size(28f, 8f), CornerRadius(2f), alpha = a) }
    } } }
}
