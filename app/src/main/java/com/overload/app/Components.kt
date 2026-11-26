package com.overload.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ov: OvColors @Composable get() = LocalOv.current

@Composable
fun OvCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(24.dp), color = ov.card,
        border = BorderStroke(1.dp, if (ov.dark) Color.White.copy(.07f) else Color.White.copy(.7f)), shadowElevation = 2.dp
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
fun OvText(t: String, size: Int = 16, weight: FontWeight = FontWeight.Normal, color: Color = ov.ink,
           modifier: Modifier = Modifier, align: TextAlign? = null) =
    Text(t, modifier, color = color, fontSize = size.sp, fontWeight = weight, textAlign = align, lineHeight = (size * 1.25).sp)

@Composable
fun Title(t: String, size: Int = 32, color: Color = ov.ink, modifier: Modifier = Modifier) =
    OvText(t, size, FontWeight.Black, color, modifier)

@Composable
fun Sub(t: String, modifier: Modifier = Modifier, color: Color = ov.mute) =
    OvText(t, 13, FontWeight.Normal, color, modifier)

@Composable
fun OvButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    bg: Color = ov.soft, fg: Color = ov.ink, small: Boolean = false, wide: Boolean = false
) {
    Box(
        modifier.then(if (wide) Modifier.fillMaxWidth() else Modifier)
            .heightIn(min = if (small) 36.dp else 46.dp)
            .background(bg, RoundedCornerShape(if (small) 11.dp else 14.dp))
            .clickable(onClick = onClick).padding(horizontal = if (small) 12.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) { OvText(label, if (small) 14 else 15, FontWeight.Bold, fg, align = TextAlign.Center) }
}

/** A numeric text box. Keeps its own text so "12." survives while typing. */
@Composable
fun NumBox(
    value: String, placeholder: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    decimal: Boolean = true, bg: Color = ov.soft, fg: Color = ov.ink, size: Int = 20, h: Dp = 50.dp
) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (text.toDoubleOrNull() != value.toDoubleOrNull() || (text.isEmpty() != value.isEmpty())) text = value }
    BasicTextField(
        value = text, onValueChange = { text = it; onChange(it) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        textStyle = TextStyle(color = fg, fontSize = size.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Red),
        modifier = modifier.height(h).background(bg, RoundedCornerShape(14.dp)),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (text.isEmpty()) OvText(placeholder, size, FontWeight.Bold, ov.mute.copy(alpha = .5f))
                inner()
            }
        }
    )
}

@Composable
fun TextBox(
    value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, minH: Dp = 48.dp, bold: Boolean = false, size: Int = 16
) {
    var text by remember { mutableStateOf(value) }
    LaunchedEffect(value) { if (value != text) text = value }
    BasicTextField(
        value = text, onValueChange = { text = it; onChange(it) }, singleLine = singleLine,
        textStyle = TextStyle(color = ov.ink, fontSize = size.sp, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal),
        cursorBrush = SolidColor(Red),
        modifier = modifier.fillMaxWidth().heightIn(min = minH).background(ov.soft, RoundedCornerShape(14.dp)),
        decorationBox = { inner ->
            Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) OvText(placeholder, size, color = ov.mute.copy(alpha = .6f))
                inner()
            }
        }
    )
}

/** Chip row item used by the muscle filter, theme/unit pickers, and the plan day picker. */
@Composable
fun Seg(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = 38.dp)
            .background(if (selected) ov.ink else ov.card, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) { OvText(label, 14, FontWeight.Bold, if (selected) ov.bg else ov.ink) }
}

@Composable
fun Toggle(on: Boolean, onChange: (Boolean) -> Unit) {
    Box(
        Modifier.width(52.dp).height(32.dp).background(if (on) Green else Color(0xFFD7D9E0), RoundedCornerShape(20.dp))
            .clickable { onChange(!on) }.padding(3.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
    ) { Box(Modifier.size(26.dp).background(Color.White, CircleShape)) }
}

/**
 * Bumper plate. state: "done" = solid colour, "idle" = black with a coloured rim, "rest" = bare steel.
 * ring (0..1) draws a progress arc around the edge (the big hero plate). label = rim engraving on big plates.
 * Special plates (Locker.kt FIN) draw their artwork over the face when solid.
 */
@Composable
fun Plate(color: String, state: String, modifier: Modifier = Modifier, ring: Float? = null, label: String? = null, og: Int = 0) {
    val p = pc(color)
    val base = when (state) { "rest" -> Color(0xFFC9CDD4); "idle" -> Color(0xFF25272D); else -> p.base }
    val light = when (state) { "rest" -> Color.White; "idle" -> Color(0xFF4A4D55); else -> p.light }
    val dark = when (state) { "rest" -> Color(0xFF7D828C); "idle" -> Color(0xFF0D0E11); else -> p.dark }
    Canvas(modifier.aspectRatio(1f)) {
        if (color == "founder" && state != "idle" && state != "rest") { viewBox(100f, 100f) { founderPlate(label ?: "", ring) }; return@Canvas }
        val r = size.minDimension / 2; val c = center
        if (state == "rest") {
            val rr = r * .7f
            drawCircle(Brush.radialGradient(listOf(light, base, dark), Offset(size.width * .38f, size.height * .32f), rr * 1.6f), rr, c)
            drawCircle(Color.White.copy(.7f), rr * .97f, c, style = Stroke(r * .012f))
        } else {
            drawCircle(Brush.radialGradient(listOf(light, base, dark), Offset(size.width * .38f, size.height * .32f), r * 1.5f), r, c)
            if (state == "done" && FIN[color] != null) viewBox(100f, 100f) { plateOverlay(color, color, og) }
            drawCircle(Color.White.copy(.3f), r * .98f, c, style = Stroke(r * .012f))
            drawCircle(Color.Black.copy(.22f), r * .93f, c, style = Stroke(r * .025f))
            if (state == "idle") drawCircle(p.base, r * .86f, c, style = Stroke(r * .05f))
            drawCircle(Color.Black.copy(.16f), r * .75f, c, style = Stroke(r * .018f))
            if (label != null) viewBox(100f, 100f) { rimText(label, 50f, 50f, 40f, 5.6f, Color.White.copy(.85f), 1.4f) }
        }
        val hub = r * if (state == "rest") .26f else .47f
        drawCircle(Brush.linearGradient(listOf(Color.White, Color(0xFF9AA0AA), Color(0xFFF4F6F9), Color(0xFF6F7580)), c - Offset(hub, hub), c + Offset(hub, hub)), hub, c)
        drawCircle(Color(0xFF0A0A0C), hub * .42f, c)
        drawCircle(Color(0xFFC9CDD4), hub * .42f, c, style = Stroke(r * .03f))
        if (ring != null) {
            drawCircle(Color.White.copy(.25f), r * .98f, c, style = Stroke(r * .04f))
            drawArc(Color.White, -90f, 360f * ring.coerceIn(0f, 1f), false,
                topLeft = Offset(c.x - r * .98f, c.y - r * .98f), size = Size(r * 1.96f, r * 1.96f),
                style = Stroke(r * .05f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
    }
}
