package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Thin rounded progress bar. */
@Composable
fun Bar(frac: Double, color: Color = Green, modifier: Modifier = Modifier, h: Int = 8) =
    Box(modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(h.dp)).background(ov.soft)) {
        Box(Modifier.fillMaxWidth(frac.toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(color, RoundedCornerShape(h.dp)))
    }

/** Label + "a/b" + bar, used on the profile and quests. */
@Composable
fun LabeledBar(label: String, a: Number, b: Number, color: Color = Green) = Column(Modifier.padding(vertical = 5.dp)) {
    Row { OvText(label, 14, FontWeight.SemiBold, modifier = Modifier.weight(1f)); OvText("${loc(minOf(a.toDouble(), b.toDouble()))}/${loc(b)}", 14, FontWeight.ExtraBold) }
    Spacer(Modifier.height(4.dp)); Bar(if (b.toDouble() > 0) a.toDouble() / b.toDouble() else 0.0, color)
}

/** Segmented sub-tabs (Overview / Lifts etc.). */
@Composable
fun SegTabs(opts: List<Pair<String, String>>, cur: String, onPick: (String) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(ov.card, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        opts.forEach { (k, l) ->
            Box(Modifier.weight(1f).height(40.dp).background(if (cur == k) ov.ink else Color.Transparent, RoundedCornerShape(12.dp)).clickable { onPick(k) }, contentAlignment = Alignment.Center) {
                OvText(l, 14, FontWeight.Bold, if (cur == k) ov.bg else ov.mute)
            }
        }
    }

@Composable
fun StatTile(v: String, l: String, modifier: Modifier = Modifier, vColor: Color = ov.ink) =
    Column(modifier.background(ov.soft, RoundedCornerShape(16.dp)).padding(vertical = 12.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Title(v, 22, vColor); Sub(l)
    }

/** A tappable card with an icon, title, subtitle and chevron (smart card, nudges). */
@Composable
fun CardButton(icon: String, title: String, sub: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp)).background(ov.card).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(icon, 28); Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { OvText(title, 16, FontWeight.Bold); Sub(sub) }
        OvText("›", 24, FontWeight.Bold, ov.mute)
    }

@Composable
fun SheetHead(title: String, btn: String = "Done", onBtn: () -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Title(title, 24, modifier = Modifier.weight(1f)); OvButton(btn, onBtn, small = true)
    }

@Composable
fun H3(t: String, modifier: Modifier = Modifier) = OvText(t, 18, FontWeight.ExtraBold, modifier = modifier.padding(bottom = 4.dp))

/** A badge in a badge case. */
@Composable
fun BadgeTile(b: Bdg, shine: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val got = b.got; val hidden = b.hid && !got
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(if (got) ov.soft else ov.soft.copy(alpha = .5f))
        .then(if (shine) Modifier.border(1.5.dp, Color(0xFFFFD75E).copy(.7f), RoundedCornerShape(16.dp)) else Modifier)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BadgeMedal(b, 54.dp, dim = !got, text = false)
        OvText(if (hidden) "???" else b.name, 12, FontWeight.Bold, align = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
        OvText(when { b.count != null -> if (b.count > 0) "×${b.count}" else "Not yet"; got -> "Earned"; b.hid -> "Hidden"; else -> "${loc(b.cur)}/${loc(b.tgt)}" }, 11, color = ov.mute)
        if (!got && b.tgt > 1 && !b.hid && b.count == null) Bar(b.cur / b.tgt, modifier = Modifier.padding(top = 4.dp), h = 4)
    }
}

/** Badges in a 3-column grid. */
@Composable
fun BadgeGrid(list: List<Bdg>, shine: Boolean, onClick: ((Bdg) -> Unit)? = null) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    list.chunked(3).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { b -> BadgeTile(b, shine && b.got, Modifier.weight(1f), onClick?.let { { it(b) } }) }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Round initials avatar coloured from the member id. */
@Composable
fun Avatar(id: String, name: String, size: Int = 40) {
    val h = hue(id).toFloat()
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color.hsl(h, .7f, .88f)), contentAlignment = Alignment.Center) {
        OvText(initials(name), (size * .38f).toInt(), FontWeight.ExtraBold, Color.hsl(h, .6f, .26f))
    }
}

@Composable
fun Chip(t: String, bg: Color = ov.soft, fg: Color = ov.ink) =
    OvText(t, 12, FontWeight.Bold, fg, Modifier.padding(end = 4.dp).background(bg, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp))

/** Bottom sheet wrapper with a scrollable body. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = ov.bg) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 28.dp).navigationBarsPadding(), content = content)
    }
}
