package com.overload.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/*
 * 0.18 crew crests: a shape, a two-colour pattern, an emblem, a metal, a border and a ribbon.
 * Metals unlock with crew rank, and the crest gains stars (Silver to Platinum), wings (Diamond),
 * a crown (Titan) and laurels (Olympian). "Founders gold" is only for the founder's crew.
 * Same keys and drawing as the web app's crestSVG, so a crest saved on one shows the same on the other.
 */

class CrMetal(val name: String, val c: List<String>, val req: Int) // req -1 = founder crew only
val CR_METAL = linkedMapOf(
    "steel" to CrMetal("Steel", listOf("#f4f6f9", "#b8bec8", "#6b717c", "#e3e6eb"), 0),
    "bronze" to CrMetal("Bronze", listOf("#ffd9b0", "#c8834a", "#6e3c16", "#f0b783"), 1),
    "silver" to CrMetal("Silver", listOf("#ffffff", "#cfd4dc", "#7d838e", "#eef1f5"), 2),
    "gold" to CrMetal("Gold", listOf("#fff4c4", "#e3b84a", "#7a5410", "#ffe08a"), 3),
    "black" to CrMetal("Black chrome", listOf("#7a7d86", "#2a2c31", "#050506", "#4a4d55"), 5),
    "founders" to CrMetal("Founders gold", listOf("#fff4c4", "#e3b84a", "#7a5410", "#ffe08a"), -1)
)
val CR_SHAPE = linkedMapOf(
    "shield" to ("Shield" to "M0 -58 L46 -44 L46 4 C46 34 24 52 0 62 C-24 52 -46 34 -46 4 L-46 -44 Z"),
    "kite" to ("Kite" to "M0 -60 C24 -60 46 -50 46 -30 C46 10 26 40 0 64 C-26 40 -46 10 -46 -30 C-46 -50 -24 -60 0 -60 Z"),
    "seal" to ("Seal" to "M0 -56 A56 56 0 1 1 0 56 A56 56 0 1 1 0 -56 Z"),
    "hex" to ("Hex" to "M0 -60 L52 -30 L52 30 L0 60 L-52 30 L-52 -30 Z")
)
val CR_PAT = linkedMapOf("plain" to "Plain", "chevron" to "Chevron", "stripes" to "Stripes", "split" to "Split", "quarter" to "Quartered")
val CR_BORDER = linkedMapOf("plain" to "Clean", "rope" to "Rope", "studs" to "Studded")

/** One piece of an emblem. kind 0 = body (metal), 1 = eye detail (filled), 2 = eye detail (stroked). */
class EmbPart(val d: String, val kind: Int = 0, val w: Float = 1.6f)
private fun rp(x: Float, y: Float, w: Float, h: Float) = "M$x ${y}h${w}v${h}h${-w}z"
private fun cp(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"
private val DB = listOf(rp(-40f, -3f, 80f, 6f), rp(-36f, -14f, 7f, 28f), rp(-28f, -10f, 5f, 20f), rp(29f, -14f, 7f, 28f), rp(23f, -10f, 5f, 20f))
val CR_EMB: LinkedHashMap<String, Pair<String, List<EmbPart>>> = linkedMapOf(
    "barbells" to ("Crossed dumbbells" to emptyList()), // drawn as two rotated dumbbells
    "bull" to ("Bull" to listOf(EmbPart("M-34 -30 C-30 -16 -22 -12 -14 -14 L-10 -18 L10 -18 L14 -14 C22 -12 30 -16 34 -30 C36 -14 30 -4 18 -4 L16 6 L12 22 C8 30 4 34 0 34 C-4 34 -8 30 -12 22 L-16 6 L-18 -4 C-30 -4 -36 -14 -34 -30 Z"),
        EmbPart("M-10 0 L-5 4 L-10 7 Z M10 0 L5 4 L10 7 Z", 1), EmbPart(cp(0f, 29f, 5f), 2, 1.6f))),
    "wolf" to ("Wolf" to listOf(EmbPart("M-26 -34 L-14 -14 L-6 -16 L0 -12 L6 -16 L14 -14 L26 -34 L28 -6 L22 8 L12 18 L6 30 L0 34 L-6 30 L-12 18 L-22 8 L-28 -6 Z"),
        EmbPart("M-14 -2 L-5 2 L-12 5 Z M14 -2 L5 2 L12 5 Z M-4 22 L0 26 L4 22 Z", 1))),
    "eagle" to ("Eagle" to listOf(EmbPart("M0 -26 C5 -26 8 -22 8 -17 L14 -12 L8 -10 C8 -4 6 2 4 6 C18 0 34 -10 46 -24 C44 -8 36 4 24 10 C32 10 38 8 42 6 C36 16 24 22 12 22 L8 34 L0 30 L-8 34 L-12 22 C-24 22 -36 16 -42 6 C-38 8 -32 10 -24 10 C-36 4 -44 -8 -46 -24 C-34 -10 -18 0 -4 6 C-6 2 -8 -4 -8 -10 C-8 -20 -5 -26 0 -26 Z"),
        EmbPart(cp(3f, -19f, 1.6f), 1))),
    "skull" to ("Skull" to listOf(EmbPart("M0 -30 C18 -30 28 -18 28 -2 C28 8 22 14 18 16 L18 26 L-18 26 L-18 16 C-22 14 -28 8 -28 -2 C-28 -18 -18 -30 0 -30 Z"),
        EmbPart("M-16 -4 C-16 -12 -4 -12 -4 -2 C-4 4 -16 6 -16 -4 Z M16 -4 C16 -12 4 -12 4 -2 C4 4 16 6 16 -4 Z M0 6 L-4 13 L4 13 Z", 1),
        EmbPart("M-10 18 V26 M-3 18 V26 M4 18 V26 M11 18 V26", 2, 2f))),
    "kettle" to ("Kettlebell" to listOf(EmbPart("M-16 -8 L-16 -18 C-16 -34 16 -34 16 -18 L16 -8 L9 -8 L9 -18 C9 -26 -9 -26 -9 -18 L-9 -8 Z"),
        EmbPart("M-22 8 C-22 -8 -10 -12 0 -12 C10 -12 22 -8 22 8 C22 22 12 30 0 30 C-12 30 -22 22 -22 8 Z"), EmbPart(rp(-14f, 28f, 28f, 5f)))),
    "crown" to ("Crown" to listOf(EmbPart("M-26 5.2 L-31.2 -23.4 L-14.3 -10.4 L0 -33.8 L14.3 -10.4 L31.2 -23.4 L26 5.2 Z"), EmbPart(rp(-26f, 7.8f, 52f, 7.8f)))),
    "bolt" to ("Lightning" to listOf(EmbPart("M8 -34 L-20 4 L-2 4 L-10 34 L20 -6 L2 -6 Z"))),
    "flame" to ("Flame" to listOf(EmbPart("M2 -34 C10 -20 26 -10 24 10 C22 26 10 34 0 34 C-12 34 -24 24 -22 8 C-20 -4 -10 -8 -8 -20 C-2 -14 0 -8 0 -2 C6 -8 8 -20 2 -34 Z"))),
    "star" to ("Star" to listOf(EmbPart("M0 -32 L9 -10 L32 -10 L14 4 L20 28 L0 14 L-20 28 L-14 4 L-32 -10 L-9 -10 Z")))
)
private val CR_ICONMAP = mapOf("barbell" to "barbells", "skull" to "skull", "crown" to "crown", "bolt" to "bolt", "wolf" to "wolf", "flame" to "flame", "star" to "star", "none" to "barbells")
private val HEX6 = Regex("^#[0-9a-fA-F]{6}$")

/** Fills in a crest from any version: old crests (shape/icon/txt) convert to the 0.18 layout. */
fun crNorm(c0: Crest?): Crest {
    val c = c0 ?: Crest()
    val shape = if (CR_SHAPE[c.shape] != null) c.shape else if (c.shape == "plate" || c.shape == "circle") "seal" else "shield"
    return c.copy(shape = shape, emb = c.emb?.takeIf { CR_EMB[it] != null } ?: CR_ICONMAP[c.icon] ?: "barbells",
        pat = c.pat?.takeIf { CR_PAT[it] != null } ?: "plain", metal = c.metal?.takeIf { CR_METAL[it] != null } ?: "steel",
        border = c.border?.takeIf { CR_BORDER[it] != null } ?: "plain",
        c1 = if (HEX6.matches(c.c1)) c.c1 else "#b8222c", c2 = if (HEX6.matches(c.c2)) c.c2 else "#14161c", rib = c.rib ?: c.txt)
}

/** Darken (amt < 0) or lighten a hex colour, like the web app's shade(). */
fun shade(h: String, amt: Double): String {
    val n = runCatching { h.removePrefix("#").toLong(16) }.getOrDefault(0x888888L)
    fun f(v: Long) = Math.round(v + if (amt < 0) v * amt else (255 - v) * amt).coerceIn(0, 255)
    return "#%02x%02x%02x".format(f(n shr 16 and 255), f(n shr 8 and 255), f(n and 255))
}

/**
 * The crew crest. [emoji] is the seasonal touch the crew's own crest gets during a holiday it unlocked (0.19).
 * Small crests (under 48dp) leave out the wings, crown and ribbon so they stay readable.
 */
@Composable
fun CrestView(cr: Crest?, rank: Int, modifier: Modifier = Modifier, emoji: String? = null) = Canvas(modifier.aspectRatio(1f)) {
    val small = size.width / density < 48f
    if (small) viewBox(124f, 132f) { translate(62f, 66f) { drawCrest(cr, rank, true, emoji) } }
    else viewBox(220f, 200f) { translate(110f, 105f) { drawCrest(cr, rank, false, emoji) } }
}

fun DrawScope.drawCrest(cr0: Crest?, rank: Int, small: Boolean, emoji: String? = null) {
    val cr = crNorm(cr0); val fc = cr.metal == "founders"; val r = if (fc) 7 else rank
    val M = CR_METAL.getValue(cr.metal!!).c.map(::hex)
    val metal = Brush.linearGradient(0f to M[0], .35f to M[1], .65f to M[2], 1f to M[3], start = Offset(-60f, -60f), end = Offset(60f, 60f))
    val c1 = if (fc) hex("#08080a") else hex(cr.c1); val c2 = if (fc) hex("#2a2418") else hex(cr.c2)
    val shp = CR_SHAPE.getValue(cr.shape).second; val path = svgPath(shp)
    if (!small) {
        if (r >= 6) drawCircle(Brush.radialGradient(listOf(hex("#ffd76a", .4f), Color.Transparent), Offset.Zero, 100f), 100f, Offset.Zero)
        if (r >= 5) listOf(-1f, 1f).forEach { q -> scale(q, 1f, Offset.Zero) { fillP("M48 -30 C78 -40 96 -24 100 -4 C88 -14 76 -14 66 -8 C80 -6 88 4 90 16 C78 6 66 4 56 8 C64 12 68 20 68 28 C58 18 50 14 46 14 Z", metal) } }
        if (r >= 7) listOf(-1f, 1f).forEach { q -> repeat(7) { i ->
            val a = Math.toRadians(118.0 + i * 15); val x = (cos(a) * 66 * q).toFloat(); val y = (sin(a) * 66 + 8).toFloat()
            rotate(q * (i * 15 + 28), Offset(x, y)) { val d = "M$x ${y - 9} Q${x + 4} $y $x ${y + 9} Q${x - 4} $y $x ${y - 9} Z"; fillP(d, metal); drawPath(svgPath(d), M[2], style = Stroke(.5f)) } } }
    }
    translate(0f, 4f) { scale(1.1f, 1.1f, Offset.Zero) { drawPath(path, Color.Black, alpha = .45f) } }
    scale(1.1f, 1.1f, Offset.Zero) { drawPath(path, metal) }
    scale(1.04f, 1.04f, Offset.Zero) { drawPath(path, M[2]) }
    clipPath(path) {
        when (cr.pat) {
            "chevron" -> { drawRect(c1, Offset(-80f, -80f), Size(160f, 160f))
                var ty = -72f; while (ty < 72f) { var tx = -72f; while (tx < 72f) { translate(tx, ty) { fillP("M0 18 L12 6 L24 18 L24 24 L12 12 L0 24 Z", c2, .45f) }; tx += 24f }; ty += 24f } }
            "stripes" -> { drawRect(c1, Offset(-80f, -80f), Size(160f, 160f))
                rotate(30f, Offset.Zero) { var x = -112f; while (x < 112f) { drawRect(c2, Offset(x, -112f), Size(6f, 224f), alpha = .45f); x += 14f } } }
            "quarter" -> { drawRect(c1, Offset(-80f, -80f), Size(160f, 160f)); drawRect(c2, Offset(-60f, -60f), Size(60f, 60f)); drawRect(c2, Offset(0f, 0f), Size(60f, 60f)) }
            "split" -> { drawRect(c1, Offset(-80f, -80f), Size(80f, 160f)); drawRect(c2, Offset(0f, -80f), Size(80f, 160f)) }
            else -> drawRect(Brush.radialGradient(listOf(c1, c2), Offset(0f, -18f), 84f), Offset(-80f, -80f), Size(160f, 160f))
        }
        fillP("M-60 -70 L60 -70 L60 -10 Q0 10 -60 -10 Z", Brush.verticalGradient(0f to Color.White.copy(.28f), .45f to Color.White.copy(0f), startY = -70f, endY = 10f))
    }
    when (cr.border) {
        "rope" -> scale(.9f, .9f, Offset.Zero) { drawPath(path, metal, style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 2f)))) }
        "studs" -> scale(.9f, .9f, Offset.Zero) { drawPath(path, metal, style = Stroke(3.6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(.1f, 9f)))) }
        else -> scale(.88f, .88f, Offset.Zero) { drawPath(path, metal, style = Stroke(1.2f)) }
    }
    // emblem: a soft shadow, then metal (or dark on black chrome) with an outline
    val eye = c1; val dark = cr.metal == "black"
    val parts = CR_EMB.getValue(cr.emb!!).second
    fun body(fill: Brush?, solid: Color?, stroke: Color?) {
        fun one(d: String) { if (fill != null) drawPath(svgPath(d), fill) else drawPath(svgPath(d), solid!!); if (stroke != null) drawPath(svgPath(d), stroke, style = Stroke(.8f)) }
        if (cr.emb == "barbells") listOf(-35f, 35f).forEach { a -> rotate(a, Offset.Zero) { DB.forEach(::one) } }
        else parts.filter { it.kind == 0 }.forEach { one(it.d) }
    }
    translate(0f, -2f) {
        translate(0f, 2.5f) { body(null, Color.Black.copy(.45f), null) }
        if (fc) body(metal, null, hex("#7a5410")) else if (dark) body(null, hex("#15161a"), M[2]) else body(metal, null, M[2])
        parts.forEach { p -> when (p.kind) { 1 -> drawPath(svgPath(p.d), eye); 2 -> drawPath(svgPath(p.d), eye, style = Stroke(p.w)) } }
    }
    if (!small) {
        if (r >= 6) translate(0f, -70f) {
            val crown = "M-22 6 L-26 -14 L-12 -4 L0 -22 L12 -4 L26 -14 L22 6 Z"; fillP(crown, metal); drawPath(svgPath(crown), M[2], style = Stroke(.8f))
            drawCircle(metal, 3f, Offset(0f, -22f)); drawCircle(metal, 2.4f, Offset(-26f, -14f)); drawCircle(metal, 2.4f, Offset(26f, -14f))
        } else if (r >= 2) { val n = if (r >= 4) 3 else if (r >= 3) 2 else 1
            repeat(n) { i -> translate((i - (n - 1) / 2f) * 16f, -70f) { fillP("M0 -7 L2 -2 L7 -2 L3 1.5 L4.5 7 L0 3.6 L-4.5 7 L-3 1.5 L-7 -2 L-2 -2 Z", metal) } } }
        val t = (cr.rib ?: "").uppercase().take(26)
        if (t.isNotEmpty()) translate(0f, if (cr.shape == "seal") 50f else 56f) {
            fillP("M-70 -2 L-56 -10 L-56 12 L-70 4 L-62 1 Z M70 -2 L56 -10 L56 12 L70 4 L62 1 Z", if (fc) Color.Black else hex(shade(cr.c1, -.45)))
            val band = "M-58 -12 Q0 -2 58 -12 L58 10 Q0 20 -58 10 Z"; fillP(band, if (fc) hex("#121216") else c1); drawPath(svgPath(band), metal, style = Stroke(1.6f))
            curvedText(t, minOf(9.5f, 150f / t.length), if (fc) hex("#e3b84a") else Color.White, if (t.length > 14) 1f else 1.6f)
        }
    }
    if (emoji != null) svgText(emoji, if (small) 30f else 34f, if (small) -36f else -38f, if (small) 24f else 26f, Color.White)
}

/** The ribbon text, bent along the ribbon (path M-54 3 Q0 13 54 3). */
private fun DrawScope.curvedText(t: String, sizePx: Float, c: Color, spacing: Float) = drawIntoCanvas { cv ->
    val p = android.graphics.Path().apply { moveTo(-54f, 3f); quadTo(0f, 13f, 54f, 3f) }
    val len = android.graphics.PathMeasure(p, false).length
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = c.toArgb(); textSize = sizePx; isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD); letterSpacing = spacing / sizePx
    }
    cv.nativeCanvas.drawTextOnPath(t, p, len / 2f, sizePx * .35f, paint)
}

// ============================ THE CREST EDITOR ============================
@Composable
fun CrestSheet(vm: OverloadViewModel, start: Crest) {
    var c by remember { mutableStateOf(crNorm(start)) }; var sec by remember { mutableStateOf<String?>(null) }
    val R = vm.crew.crewRankInfo().r; val fcrew = vm.crewState.value?.crew?.fcrew == true
    if (sec == null) {
        SheetHead("Crew crest", "Cancel") { vm.closeSheet() }
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) { CrestView(c, R, Modifier.size(190.dp)) }
        Sub("Stars, wings, a crown and laurels get added as your crew ranks up.", Modifier.fillMaxWidth().padding(bottom = 6.dp))
        OvCard {
            Row16("🛡", "Shape", CR_SHAPE[c.shape]?.first ?: "") { sec = "shape" }
            Row16("▦", "Pattern", CR_PAT[c.pat] ?: "") { sec = "pat" }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { sec = "col" }.padding(vertical = 11.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText("🎨", 20, modifier = Modifier.width(34.dp)); OvText("Colors", 16, FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Box(Modifier.size(16.dp).background(hex(c.c1), CircleShape)); Spacer(Modifier.width(4.dp)); Box(Modifier.size(16.dp).background(hex(c.c2), CircleShape)); Spacer(Modifier.width(6.dp))
                OvText("›", 20, FontWeight.Bold, ov.mute) }
            Row16("🐂", "Emblem", CR_EMB[c.emb]?.first ?: "") { sec = "emb" }
            Row16("⚙️", "Metal", CR_METAL[c.metal]?.name ?: "") { sec = "metal" }
            Row16("◎", "Border", CR_BORDER[c.border] ?: "") { sec = "border" }
            Row16("🎗", "Ribbon", c.rib?.ifEmpty { null } ?: "None") { sec = "rib" }
        }
        OvButton("Save crest", { vm.crew.saveCrest(crNorm(c)) }, bg = Red, fg = Color.White, wide = true)
        return
    }
    val label = mapOf("shape" to "Shape", "pat" to "Pattern", "col" to "Colors", "emb" to "Emblem", "metal" to "Metal", "border" to "Border", "rib" to "Ribbon")[sec] ?: ""
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        OvButton("‹ Back", { sec = null }, small = true); Title(label, 20, modifier = Modifier.weight(1f).padding(horizontal = 10.dp)); OvButton("Done", { sec = null }, small = true, bg = Red, fg = Color.White)
    }
    Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) { CrestView(c, R, Modifier.size(130.dp)) }
    @Composable fun tiles(opts: List<Triple<String, String, String>>, pick: (String) -> Crest, cur: String?) =
        opts.chunked(3).forEach { row -> Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (v, l, lock) -> val on = cur == v
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(ov.soft).then(if (on) Modifier.border(2.dp, Red, RoundedCornerShape(16.dp)) else Modifier)
                    .clickable { if (lock.isNotEmpty()) vm.toast("🔒 $lock") else { c = pick(v); vm.haptic("tap") } }.padding(vertical = 8.dp).alpha(if (lock.isNotEmpty()) .55f else 1f),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    CrestView(pick(v).copy(rib = ""), 0, Modifier.size(54.dp)); OvText(l + if (lock.isNotEmpty()) " 🔒" else "", 12, FontWeight.Bold) } }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } }
    when (sec) {
        "shape" -> tiles(CR_SHAPE.map { (k, v) -> Triple(k, v.first, "") }, { c.copy(shape = it) }, c.shape)
        "pat" -> tiles(CR_PAT.map { (k, v) -> Triple(k, v, "") }, { c.copy(pat = it) }, c.pat)
        "emb" -> tiles(CR_EMB.map { (k, v) -> Triple(k, v.first, "") }, { c.copy(emb = it) }, c.emb)
        "border" -> tiles(CR_BORDER.map { (k, v) -> Triple(k, v, "") }, { c.copy(border = it) }, c.border)
        "metal" -> tiles(CR_METAL.filter { it.key != "founders" || fcrew }.map { (k, m) ->
            Triple(k, m.name, if (m.req < 0) (if (fcrew) "" else "Founder crew only") else if (R >= m.req) "" else "${RANKS[m.req].name} crew rank") }, { c.copy(metal = it) }, c.metal)
        "col" -> { val cols = (AV_COLS + listOf("#14161c", "#f2f3f6")).distinct()
            Lbl("MAIN COLOR"); FlowRowC { cols.forEach { x -> Box(Modifier.size(34.dp).clip(CircleShape).background(hex(x)).then(if (c.c1 == x) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { c = c.copy(c1 = x) }) } }
            Lbl("SECOND COLOR"); FlowRowC { cols.forEach { x -> Box(Modifier.size(34.dp).clip(CircleShape).background(hex(x)).then(if (c.c2 == x) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { c = c.copy(c2 = x) }) } } }
        "rib" -> { TextBox(c.rib ?: "", { c = c.copy(rib = it.uppercase().take(26)) }, (vm.crewState.value?.crew?.name ?: "").uppercase().take(26).ifEmpty { "IRON CREW" })
            Sub("Your crew name or motto, up to 26 letters. Leave empty for no ribbon.", Modifier.padding(top = 6.dp)) }
    }
}
