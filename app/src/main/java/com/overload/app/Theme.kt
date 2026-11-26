package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

class PlateColor(val base: Color, val light: Color, val dark: Color, val grad: List<Color>)

private val PC0: Map<String, PlateColor> = mapOf(
    "red" to PlateColor(Color(0xFFEF2B36), Color(0xFFFF6B73), Color(0xFF9C0F18), listOf(Color(0xFFFF3D4A), Color(0xFFEF2B36), Color(0xFFFF7A3D))),
    "blue" to PlateColor(Color(0xFF2563FF), Color(0xFF6B95FF), Color(0xFF0F2F9E), listOf(Color(0xFF2563FF), Color(0xFF3B82F6), Color(0xFF00B7FF))),
    "yellow" to PlateColor(Color(0xFFFFBE0B), Color(0xFFFFD75E), Color(0xFFB07A00), listOf(Color(0xFFFF9F1C), Color(0xFFFFBE0B), Color(0xFFFFD23F))),
    "green" to PlateColor(Color(0xFF16B65B), Color(0xFF4FDC8C), Color(0xFF0A6E35), listOf(Color(0xFF0FA958), Color(0xFF16B65B), Color(0xFF00C2A8))),
    "purple" to PlateColor(Color(0xFF7C4DFF), Color(0xFFA98BFF), Color(0xFF4520B8), listOf(Color(0xFF6A3DFF), Color(0xFF7C4DFF), Color(0xFFD14DFF))),
    "black" to PlateColor(Color(0xFF23252B), Color(0xFF4A4D56), Color(0xFF0C0D10), listOf(Color(0xFF23252B), Color(0xFF3A3D46), Color(0xFF5A5E69))),
    // unlocked by rank
    "pink" to PlateColor(Color(0xFFFF3D8B), Color(0xFFFF86B8), Color(0xFFA3104F), listOf(Color(0xFFFF3D8B), Color(0xFFFF5FA2), Color(0xFFFF9F6B))),
    "teal" to PlateColor(Color(0xFF00B3A4), Color(0xFF4DE0D3), Color(0xFF00665D), listOf(Color(0xFF00A896), Color(0xFF00C2B2), Color(0xFF3AD0FF))),
    "gold" to PlateColor(Color(0xFFD4A20B), Color(0xFFFFE27A), Color(0xFF8A6400), listOf(Color(0xFFB8860B), Color(0xFFE8B923), Color(0xFFFFE27A))),
    "chrome" to PlateColor(Color(0xFF8C939E), Color(0xFFF4F6F9), Color(0xFF454A52), listOf(Color(0xFF5D636D), Color(0xFFAEB5BF), Color(0xFFEEF1F5))),
    // unlocked by the season pass
    "ember" to PlateColor(Color(0xFFFF5A1F), Color(0xFFFFB36B), Color(0xFF8A2100), listOf(Color(0xFFFF2D2D), Color(0xFFFF6A00), Color(0xFFFFC14D))),
    "ice" to PlateColor(Color(0xFF5EC8FF), Color(0xFFD4F1FF), Color(0xFF1A6C99), listOf(Color(0xFF8BE0FF), Color(0xFF5EC8FF), Color(0xFFE9F8FF))),
    "toxic" to PlateColor(Color(0xFF7DDC1F), Color(0xFFD2FF8A), Color(0xFF336600), listOf(Color(0xFF3DDC84), Color(0xFFA8FF1A), Color(0xFFF0FF6B))),
    "galaxy" to PlateColor(Color(0xFF5B2BD6), Color(0xFFB892FF), Color(0xFF21105C), listOf(Color(0xFF1B0F4A), Color(0xFF5B2BD6), Color(0xFFFF4FD8)))
)
/** Every plate: the colours above plus the special finishes and themed plates (Locker.kt). */
val PC: Map<String, PlateColor> = LinkedHashMap(PC0).apply {
    FIN.values.forEach { f -> put(f.k, PlateColor(Color(f.base), Color(f.light), Color(f.dark), listOf(Color(f.dark), Color(f.base), Color(f.light)))) }
}
fun pc(name: String?) = PC[name] ?: PC.getValue("red")
fun colorOf(d: DayPlan): String = d.color ?: TYPES[d.type]?.second ?: "purple"

/** App accent (Customize → App theme). Read through [Red] so screens recompose when it changes. */
val accentState = mutableStateOf(Color(0xFFEF2B36))
val Red: Color get() = accentState.value
val Green = Color(0xFF16B65B); val Blue = Color(0xFF2563FF)

fun muscleColor(m: String): Color = Color(when (m) {
    "Chest" -> 0xFFEF2B36; "Back" -> 0xFF2563FF; "Shoulders" -> 0xFFFF7A3D; "Side delts" -> 0xFFFF9F1C
    "Rear delts" -> 0xFFFFBE0B; "Biceps" -> 0xFF7C4DFF; "Triceps" -> 0xFFD14DFF; "Forearms" -> 0xFFA98BFF
    "Traps" -> 0xFF00B7FF; "Quads" -> 0xFF16B65B; "Hamstrings" -> 0xFF0FA958; "Glutes" -> 0xFF00C2A8
    "Calves" -> 0xFF4FDC8C; "Abs" -> 0xFF23252B; "Lower back" -> 0xFF6B7080; else -> 0xFF6B7080
})

class OvColors(val bg: Color, val ink: Color, val mute: Color, val line: Color, val card: Color, val soft: Color, val dark: Boolean)

// 0.17 glass look: cards are see-through so the background art shows through
val LightOv = OvColors(Color(0xFFF7F5F1), Color(0xFF14161C), Color(0xFF6B7080), Color(0xFFE7E7EC), Color(0xA6FFFFFF), Color(0x99EEF0F4), false)
val DarkOv = OvColors(Color(0xFF07080C), Color(0xFFF2F3F7), Color(0xFF9AA0AE), Color(0xFF242632), Color(0x8C12141C), Color(0x991A1C26), true)
val LocalOv = staticCompositionLocalOf { LightOv }

@Composable
fun OverloadTheme(theme: String, art: String = "log", bg: String = "classic", content: @Composable () -> Unit) {
    val dark = when (theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val c = if (dark) DarkOv else LightOv
    val scheme = if (dark) darkColorScheme(primary = Red, background = c.bg, surface = c.bg, onSurface = c.ink, onBackground = c.ink)
    else lightColorScheme(primary = Red, background = c.bg, surface = Color.White, onSurface = c.ink, onBackground = c.ink)
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalOv provides c) {
            Box(Modifier.fillMaxSize().background(c.bg).drawBehind {
                val a = if (dark) 1.4f else 1f
                drawRect(Brush.radialGradient(listOf(Color(0xFFEF2B36).copy(alpha = .20f * a), Color.Transparent), Offset(0f, 0f), size.width * .8f))
                drawRect(Brush.radialGradient(listOf(Color(0xFF2563FF).copy(alpha = .18f * a), Color.Transparent), Offset(size.width, size.height * .08f), size.width * .8f))
                drawRect(Brush.radialGradient(listOf(Color(0xFFFFBE0B).copy(alpha = .18f * a), Color.Transparent), Offset(size.width * .85f, size.height), size.width * .9f))
                backgroundArt(art, dark, bg)
            }) { content() }
        }
    }
}

// ============================ BACKGROUND ART (0.17: always on) ============================
private sealed class Sh { class R(val x: Float, val y: Float, val w: Float, val h: Float, val rx: Float) : Sh(); class C(val x: Float, val y: Float, val r: Float) : Sh(); class P(val d: String) : Sh() }
private val ICONS: Map<String, List<Sh>> = mapOf(
    "dumbbell" to listOf(Sh.R(4f, 34f, 9f, 32f, 3f), Sh.R(13f, 24f, 12f, 52f, 4f), Sh.R(25f, 45f, 50f, 10f, 3f), Sh.P("M33 45v10M38 45v10M62 45v10M67 45v10"), Sh.R(75f, 24f, 12f, 52f, 4f), Sh.R(87f, 34f, 9f, 32f, 3f)),
    "kettlebell" to listOf(Sh.P("M34 46 C30 14 70 14 66 46"), Sh.P("M40 44 C38 24 62 24 60 44"), Sh.P("M27 66 C27 46 42 40 50 40 C58 40 73 46 73 66 C73 80 64 88 50 88 C36 88 27 80 27 66Z"), Sh.P("M34 88h32"), Sh.P("M38 62 a12 12 0 0 1 10 -10")),
    "plate" to listOf(Sh.C(50f, 50f, 44f), Sh.C(50f, 50f, 38f), Sh.C(50f, 50f, 27f), Sh.C(50f, 50f, 16f), Sh.C(50f, 50f, 7f), Sh.C(50f, 31f, 1.6f), Sh.C(66.5f, 40.5f, 1.6f), Sh.C(66.5f, 59.5f, 1.6f), Sh.C(50f, 69f, 1.6f), Sh.C(33.5f, 59.5f, 1.6f), Sh.C(33.5f, 40.5f, 1.6f)),
    "stopwatch" to listOf(Sh.C(50f, 56f, 34f), Sh.C(50f, 56f, 28f), Sh.R(43f, 10f, 14f, 8f, 2f), Sh.P("M50 18v4M76 28l5-5M50 56l12-14M50 32v4M74 56h-4M50 80v-4M26 56h4"), Sh.C(50f, 56f, 3f)),
    "rack" to listOf(Sh.P("M18 92V8M82 92V8M10 92h16M74 92h16"), Sh.P("M18 36h6v6M82 36h-6v6"), Sh.P("M2 40h96"), Sh.R(6f, 22f, 7f, 36f, 2f), Sh.R(13f, 28f, 5f, 24f, 2f), Sh.R(87f, 22f, 7f, 36f, 2f), Sh.R(82f, 28f, 5f, 24f, 2f), Sh.P("M40 37v6M60 37v6")),
    "rope" to listOf(Sh.P("M20 20 C20 70 80 30 80 80"), Sh.R(14f, 6f, 12f, 20f, 5f), Sh.R(74f, 78f, 12f, 20f, 5f))
)
private val ART = mapOf("log" to listOf("dumbbell", "plate", "#ff3d4a", "#ff9f1c"), "prog" to listOf("kettlebell", "dumbbell", "#2563ff", "#00b7ff"),
    "stats" to listOf("plate", "kettlebell", "#ff9f1c", "#ffbe0b"), "notes" to listOf("stopwatch", "rope", "#16b65b", "#00c2a8"),
    "plan" to listOf("rack", "plate", "#7c4dff", "#d14dff"), "set" to listOf("plate", "stopwatch", "#23252b", "#6b7080"))

private fun DrawScope.backgroundArt(key: String, dark: Boolean, bg: String) {
    val a = ART[key] ?: ART.getValue("log"); val dim = if (bg == "minimal") .35f else 1f
    val c1 = if (dark && key == "set") hex("#9aa0ae") else hex(a[2]); val c2 = if (dark && key == "set") Color.White else hex(a[3])
    val W = size.width; val H = size.height; val K = if (dark) 1.7f else 1f
    if (bg == "grid") { val step = 24.dp.toPx(); var x = 0f; while (x < W) { drawLine(c1.copy(.07f * K), Offset(x, 0f), Offset(x, H)); x += step }; var y = 0f; while (y < H) { drawLine(c1.copy(.07f * K), Offset(0f, y), Offset(W, y)); y += step } }
    if (bg == "stars") repeat(70) { i -> drawCircle(if (dark) Color.White else c1, rnd("st$i", .6f, 1.8f) * density, Offset(rnd("sx$i") * W, rnd("sy$i") * H), alpha = rnd("so$i", .15f, .6f)) }
    val lg = Brush.horizontalGradient(listOf(c1.copy(0f), c1, c2, c2.copy(0f)))
    listOf(floatArrayOf(.3f, .1f, 1f, 0f), floatArrayOf(.62f, .13f, .8f, 2.2f)).forEach { w ->
        fun wave(shift: Float) = Path().apply { for (k in 0..120) { val t = k / 120f; val x = -W * .1f + W * 1.2f * t
            val y = H * w[0] + H * w[1] * kotlin.math.sin(t * Math.PI.toFloat() * 2 * w[2] + w[3]) + shift - (t - .5f) * H * .1f; if (k == 0) moveTo(x, y) else lineTo(x, y) } }
        drawPath(wave(0f), lg, alpha = .1f * K * dim, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
        drawPath(wave(0f), lg, alpha = minOf(.9f, .38f * K) * dim, style = Stroke(1.5f * density))
        drawPath(wave(13 * density), lg, alpha = .16f * dim, style = Stroke(1f * density))
    }
    fun icon(name: String, cx: Float, cy: Float, sz: Float, rot: Float, op: Float) {
        val sc = sz / 100f; val g = Brush.linearGradient(listOf(c1, c2), Offset.Zero, Offset(100f, 100f))
        withTransform({ translate(cx - sz / 2, cy - sz / 2); rotate(rot, Offset(sz / 2, sz / 2)); scale(sc, sc, Offset.Zero) }) {
            listOf(Triple(g, 9f * density / sc, op * .25f), Triple(g, 2f * density / sc, op), Triple(SolidColor(Color.White), .8f * density / sc, op * .9f)).forEach { (b, sw, al) ->
                val st = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
                ICONS[name]?.forEach { s -> when (s) {
                    is Sh.R -> drawRoundRect(b, Offset(s.x, s.y), Size(s.w, s.h), CornerRadius(s.rx, s.rx), alpha = al * dim, style = st)
                    is Sh.C -> drawCircle(b, s.r, Offset(s.x, s.y), alpha = al * dim, style = st)
                    is Sh.P -> drawPath(svgPath(s.d), b, alpha = al * dim, style = st)
                } }
            }
        }
    }
    val M = minOf(W, H) / density; val big = maxOf(260f, minOf(560f, M * .95f)) * density
    icon(a[0], W * .86f, H * .17f, big * .82f, -20f, minOf(.9f, .55f * K))
    icon(a[1], W * .1f, H * .74f, big * .6f, 16f, minOf(.8f, .42f * K))
    icon(a[0], W * .92f, H * .92f, big * .38f, 24f, minOf(.6f, .3f * K))
}
