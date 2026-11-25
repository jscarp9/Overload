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

class PlateColor(val base: Color, val light: Color, val dark: Color, val grad: List<Color>)

val PC: Map<String, PlateColor> = mapOf(
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
fun pc(name: String?) = PC[name] ?: PC.getValue("red")
fun colorOf(d: DayPlan): String = d.color ?: TYPES[d.type]?.second ?: "purple"

val Red = Color(0xFFEF2B36); val Green = Color(0xFF16B65B); val Blue = Color(0xFF2563FF)

fun muscleColor(m: String): Color = Color(when (m) {
    "Chest" -> 0xFFEF2B36; "Back" -> 0xFF2563FF; "Shoulders" -> 0xFFFF7A3D; "Side delts" -> 0xFFFF9F1C
    "Rear delts" -> 0xFFFFBE0B; "Biceps" -> 0xFF7C4DFF; "Triceps" -> 0xFFD14DFF; "Forearms" -> 0xFFA98BFF
    "Traps" -> 0xFF00B7FF; "Quads" -> 0xFF16B65B; "Hamstrings" -> 0xFF0FA958; "Glutes" -> 0xFF00C2A8
    "Calves" -> 0xFF4FDC8C; "Abs" -> 0xFF23252B; "Lower back" -> 0xFF6B7080; else -> 0xFF6B7080
})

class OvColors(val bg: Color, val ink: Color, val mute: Color, val line: Color, val card: Color, val soft: Color, val dark: Boolean)

val LightOv = OvColors(Color(0xFFF7F5F1), Color(0xFF14161C), Color(0xFF6B7080), Color(0xFFE7E7EC), Color(0xCCFFFFFF), Color(0xFFF2F3F6), false)
val DarkOv = OvColors(Color(0xFF07080C), Color(0xFFF2F3F7), Color(0xFF9AA0AE), Color(0xFF242632), Color(0xBD12141C), Color(0xFF1A1C26), true)
val LocalOv = staticCompositionLocalOf { LightOv }

@Composable
fun OverloadTheme(theme: String, content: @Composable () -> Unit) {
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
            }) { content() }
        }
    }
}
