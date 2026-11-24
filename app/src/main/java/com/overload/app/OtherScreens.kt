package com.overload.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
private fun Tile(v: String, l: String, modifier: Modifier, vColor: Color = ov.ink) =
    Column(modifier.background(ov.card, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Title(v, 28, vColor); Sub(l)
    }

// ============================ PROGRESS ============================
@Composable
fun ProgressScreen(vm: OverloadViewModel, d: AppData, u: UiState) {
    val unit = d.settings.unit
    Title("Progress"); Sub("Every lift you've logged. Tap one for the full story.")
    if (u.prog != null) { LiftDetail(vm, d, u.prog, unit); return }
    val ids = vm.allIds(d)
    val muscles = listOf("All") + MUSCLES.filter { m -> ids.any { vm.exMuscle(it, d) == m } }
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        muscles.forEach { Seg(it, u.pfilter == it, { vm.setPFilter(it) }) }
    }
    val list = ids.filter { u.pfilter == "All" || vm.exMuscle(it, d) == u.pfilter }
        .map { it to points(d.sessions, it) }.sortedByDescending { it.second.lastOrNull()?.s?.date ?: "" }
    OvCard {
        if (list.isEmpty()) Sub("Nothing logged yet.")
        list.forEachIndexed { i, (id, pts) ->
            if (i > 0) HorizontalDivider(color = ov.line)
            val b = pts.lastOrNull()?.e ?: 0; val a = pts.firstOrNull()?.e ?: 0
            Row(Modifier.fillMaxWidth().clickable { vm.setProg(id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    OvText(vm.exName(id, d), 15, FontWeight.Bold)
                    Row {
                        Sub(vm.exMuscle(id, d))
                        if (pts.size > 1) Sub("  ·  ${if (b - a >= 0) "+" else ""}${b - a}", color = if (b - a >= 0) Green else Red)
                    }
                }
                Spark(pts, muscleColor(vm.exMuscle(id, d)), Modifier.size(80.dp, 30.dp))
                Title(if (b > 0) "$b" else "–", 22, modifier = Modifier.width(56.dp).padding(start = 10.dp))
            }
        }
    }
}

@Composable
private fun Spark(pts: List<Pt>, c: Color, modifier: Modifier) = Canvas(modifier) {
    if (pts.size < 2) { drawLine(Color(0xFFD7D9E0), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 2f); return@Canvas }
    val v = pts.map { it.e }; val mn = v.min(); val mx = v.max(); val r = max(1, mx - mn)
    val path = Path()
    pts.forEachIndexed { i, p ->
        val x = i / (pts.size - 1f) * size.width
        val y = size.height * (.87f - (p.e - mn) / r.toFloat() * .73f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, c, style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun LiftDetail(vm: OverloadViewModel, d: AppData, id: String, unit: String) {
    val pts = points(d.sessions, id); val c = muscleColor(vm.exMuscle(id, d))
    Spacer(Modifier.height(12.dp))
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Title(vm.exName(id, d), 20, modifier = Modifier.weight(1f)); OvButton("All lifts", { vm.setProg(null) }, small = true)
        }
        Sub(vm.exMuscle(id, d))
        if (pts.isEmpty()) { Sub("No sets logged yet."); return@OvCard }
        val a = pts.first().e; val b = pts.last().e; val best = pts.maxByOrNull { it.e }!!; val tv = pts.sumOf { it.vol }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("$b", "Est. max $unit", Modifier.weight(1f))
            Tile("${if (b - a >= 0) "+" else ""}${b - a}", "Since first log", Modifier.weight(1f), if (b - a > 0) Green else if (b - a < 0) Red else ov.ink)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Tile("${fmtN(best.b.w!!)}×${best.b.r}", "Best set · ${mdS(best.s.date)}", Modifier.weight(1f))
            Tile(kfmt(tv), "Total $unit · ${pts.size} sessions", Modifier.weight(1f))
        }
        if (pts.size > 1) LineChart(pts, c, Modifier.fillMaxWidth().height(200.dp).padding(vertical = 12.dp))
        pts.reversed().forEach { p ->
            HorizontalDivider(color = ov.line)
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Column(Modifier.weight(1f)) {
                    OvText("${DN[dayIdx(LocalDate.parse(p.s.date))]} ${mdS(p.s.date)}", 15)
                    Sub(d.plan?.getOrNull(DAYS.indexOf(p.s.day))?.name ?: "Old split")
                }
                OvText(fmtSets(p.s.ex[id]), 14, FontWeight.Bold, modifier = Modifier.weight(1.2f))
                OvText("${p.e}", 15, FontWeight.Bold)
            }
        }
    }
    Sub("Est. max uses your best set each session: weight × (1 + reps ÷ 30).")
}

@Composable
private fun LineChart(pts: List<Pt>, c: Color, modifier: Modifier) = Canvas(modifier) {
    val l = 8f; val r = 12f; val t = 10f; val b = 10f
    val v = pts.map { it.e }; var mn = v.min().toFloat(); var mx = v.max().toFloat()
    val pad = max(5f, (mx - mn) * .2f)
    mn = floor((mn - pad) / 5) * 5; mx = ceil((mx + pad) / 5) * 5
    val X = { i: Int -> l + (size.width - l - r) * (i / (pts.size - 1f)) }
    val Y = { x: Float -> t + (size.height - t - b) * (1 - (x - mn) / (mx - mn)) }
    listOf(mn, mx).forEach { drawLine(Color.Gray.copy(.25f), Offset(l, Y(it)), Offset(size.width - r, Y(it)), 2f) }
    val line = Path(); pts.forEachIndexed { i, p -> if (i == 0) line.moveTo(X(i), Y(p.e.toFloat())) else line.lineTo(X(i), Y(p.e.toFloat())) }
    val area = Path().apply { addPath(line); lineTo(X(pts.size - 1), size.height - b); lineTo(X(0), size.height - b); close() }
    drawPath(area, Brush.verticalGradient(listOf(c.copy(.3f), Color.Transparent)))
    drawPath(line, c, style = Stroke(9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    pts.forEachIndexed { i, p ->
        val last = i == pts.size - 1
        drawCircle(if (last) c else Color.White, if (last) 16f else 11f, Offset(X(i), Y(p.e.toFloat())))
        drawCircle(c, if (last) 16f else 11f, Offset(X(i), Y(p.e.toFloat())), style = Stroke(6f))
    }
}

// ============================ STATS ============================
@Composable
fun StatsScreen(vm: OverloadViewModel, d: AppData) {
    val today = LocalDate.now(); val unit = d.settings.unit; val wd = weekDates(today); val w = weekAgg(d, wd)
    Title("Stats"); Sub("This week · ${MON[wd[0].monthValue - 1]} ${wd[0].dayOfMonth} to ${MON[wd[6].monthValue - 1]} ${wd[6].dayOfMonth}")
    Spacer(Modifier.height(10.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Tile("${w.done}/${w.planned}", "Sessions", Modifier.weight(1f)); Tile("${w.sets}", "Hard sets", Modifier.weight(1f))
        Tile(kfmt(w.vol), "$unit moved", Modifier.weight(1f)); Tile("${w.prs}", "PRs", Modifier.weight(1f), if (w.prs > 0) Red else ov.ink)
    }
    if (w.avg > 0) Sub("Average workout this week: ${mins(w.avg)}", Modifier.padding(top = 8.dp))
    Spacer(Modifier.height(18.dp))

    // sets by muscle
    val a = wd[0].toString(); val b = wd[6].toString()
    val done = HashMap<String, Int>(); val planned = HashMap<String, Int>()
    d.sessions.values.filter { it.date in a..b }.forEach { s -> s.ex.forEach { (id, l) -> val k = vm.exMuscle(id, d); done[k] = (done[k] ?: 0) + l.count(::full) } }
    d.plan?.forEach { day -> day.groups.forEach { gr -> gr.forEach { planned[it.m] = (planned[it.m] ?: 0) + it.sets } } }
    val keys = MUSCLES.filter { (planned[it] ?: 0) > 0 || (done[it] ?: 0) > 0 }
    val mx = max(1, keys.maxOfOrNull { max(planned[it] ?: 0, done[it] ?: 0) } ?: 1)
    OvCard {
        Title("Sets by muscle", 20); Sub("Logged this week against what your plan calls for.")
        if (keys.isEmpty()) Sub("Add exercises to your plan to see this.")
        keys.forEach { k ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText(k, 14, FontWeight.SemiBold, modifier = Modifier.width(96.dp))
                Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(ov.soft)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth((done[k] ?: 0) / mx.toFloat()).background(muscleColor(k), RoundedCornerShape(6.dp)))
                }
                OvText("${done[k] ?: 0}/${planned[k] ?: 0}", 14, FontWeight.Bold, modifier = Modifier.width(52.dp).padding(start = 8.dp))
            }
        }
    }

    // weekly volume
    val wk = (7 downTo 0).map { i -> val ws = weekDates(wd[0].minusWeeks(i.toLong())); ws[0] to weekAgg(d, ws) }
    val vmax = max(1.0, wk.maxOf { it.second.vol })
    OvCard {
        Title("Weekly volume", 20); Sub("Total $unit moved, last 8 weeks. This week in blue.")
        Row(Modifier.fillMaxWidth().height(150.dp).padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
            wk.forEachIndexed { i, (start, agg) ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Sub(if (agg.vol > 0) kfmt(agg.vol) else "")
                    Box(Modifier.fillMaxWidth().fillMaxHeight(max(.03f, (agg.vol / vmax).toFloat() * .72f))
                        .background(if (i == 7) Blue else Red, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)))
                    Sub(md(start))
                }
            }
        }
    }

    // consistency heatmap
    val streak = weekStreak(d, today)
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { Title("Consistency", 20, modifier = Modifier.weight(1f)); Title("$streak", 22); Sub(" week streak") }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) { Spacer(Modifier.width(44.dp)); DN.forEach { OvText(it.take(1), 11, FontWeight.SemiBold, ov.mute, Modifier.weight(1f), androidx.compose.ui.text.style.TextAlign.Center) } }
        val start = wd[0].minusWeeks(11)
        for (wi in 0 until 12) {
            val ws = start.plusWeeks(wi.toLong())
            Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Sub(md(ws), Modifier.width(44.dp))
                for (i in 0 until 7) {
                    val x = ws.plusDays(i.toLong())
                    val s = d.sessions.values.firstOrNull { it.date == x.toString() && it.ex.values.any(::has) }
                    val bg = s?.let { ss -> d.plan?.getOrNull(DAYS.indexOf(ss.day))?.let { pc(colorOf(it)).base } ?: Red } ?: ov.soft
                    Box(Modifier.weight(1f).padding(horizontal = 2.5.dp).aspectRatio(1f).clip(RoundedCornerShape(6.dp)).background(bg))
                }
            }
        }
    }

    // PR board
    class Pr(val id: String, val best: SetEntry, val date: String)
    val prs = vm.allIds(d).mapNotNull { id ->
        var best: SetEntry? = null; var bd = ""
        d.sessions.values.forEach { s -> (s.ex[id] ?: emptyList()).filter(::full).forEach { x -> if (best == null || e1(x) > e1(best!!)) { best = x; bd = s.date } } }
        best?.let { Pr(id, it, bd) }
    }.sortedByDescending { e1(it.best) }
    val wk7 = today.minusDays(7).toString()
    OvCard {
        Title("PR board", 20); Sub("Best estimated max for every lift.")
        if (prs.isEmpty()) Sub("Log a session to start your board.")
        prs.forEach { p ->
            HorizontalDivider(Modifier.padding(top = 10.dp), color = ov.line)
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    OvText(vm.exName(p.id, d), 15, FontWeight.Bold)
                    Sub("${fmtN(p.best.w!!)}×${p.best.r} · ${mdS(p.date)}" + if (p.date >= wk7) " · new" else "")
                }
                Title("${e1(p.best).roundToInt()}", 24)
            }
        }
    }
}

// ============================ NOTES ============================
val NOTE_COLORS = listOf("red", "yellow", "blue", "green", "white")
fun noteBg(c: String, dark: Boolean): Color = if (dark) when (c) {
    "red" -> Color(0xFFFF3B3F).copy(.18f); "yellow" -> Color(0xFFFFBE0B).copy(.16f); "blue" -> Color(0xFF2563FF).copy(.2f)
    "green" -> Color(0xFF16B65B).copy(.18f); else -> Color.White.copy(.08f)
} else when (c) {
    "red" -> Color(0xFFFFE1E3); "yellow" -> Color(0xFFFFF1C2); "blue" -> Color(0xFFDFE8FF); "green" -> Color(0xFFD9F6E5); else -> Color.White
}

@Composable
fun NotesScreen(vm: OverloadViewModel, d: AppData, u: UiState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Title("Notes", modifier = Modifier.weight(1f)); OvButton("+ New note", { vm.newNote() }, small = true, bg = Red, fg = Color.White)
    }
    Sub("Goals, form cues, PR targets. Pinned notes stay on top.")
    val en = u.editNote
    if (en != null) {
        var n by remember(en.id) { mutableStateOf(en) }
        Spacer(Modifier.height(12.dp))
        OvCard {
            TextBox(n.title, { n = n.copy(title = it) }, "Title", bold = true)
            Spacer(Modifier.height(8.dp))
            TextBox(n.text, { n = n.copy(text = it) }, "Write it down…", singleLine = false, minH = 120.dp)
            Row(Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NOTE_COLORS.forEach { c ->
                    Box(Modifier.size(40.dp).clip(CircleShape).background(noteBg(c, ov.dark))
                        .border(if (n.color == c) 3.dp else 1.dp, if (n.color == c) ov.ink else ov.line, CircleShape).clickable { n = n.copy(color = c) },
                        contentAlignment = Alignment.Center) { if (n.color == c) OvText("✓", 16, FontWeight.Black) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OvButton("Save", { vm.saveNote(n) }, bg = ov.ink, fg = ov.bg)
                OvButton(if (n.pin) "Unpin" else "Pin", { n = n.copy(pin = !n.pin) })
                if (n.id.isNotEmpty()) { var c by remember { mutableStateOf(false) }; OvButton(if (c) "Tap to delete" else "Delete", { if (c) vm.deleteNote(n.id) else c = true }) }
                OvButton("Cancel", { vm.editNote(null) })
            }
        }
        return
    }
    Spacer(Modifier.height(12.dp))
    val list = d.notes.sortedWith(compareByDescending<Note> { it.pin }.thenByDescending { it.ts })
    if (list.isEmpty()) OvCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Title("Your board is empty", 20); Sub("Try a goal like “Incline 245×6 by January” or a cue like “Elbows tucked on JM press”.")
        }
    }
    list.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { n ->
                Column(Modifier.weight(1f).heightIn(min = 124.dp).clip(RoundedCornerShape(22.dp)).background(noteBg(n.color, ov.dark)).clickable { vm.editNote(n) }.padding(14.dp)) {
                    if (n.pin) Sub("PINNED")
                    Title(n.title.ifEmpty { "Untitled" }, 18)
                    Text(n.text, color = ov.ink, fontSize = 14.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    val sn = sortedDesc(d.sessions).filter { it.notes.isNotBlank() || it.exNotes.values.any { v -> v.isNotBlank() } }.take(20)
    if (sn.isNotEmpty()) OvCard {
        Title("Session notes", 20)
        sn.forEach { s ->
            HorizontalDivider(Modifier.padding(top = 12.dp), color = ov.line)
            Sub("${DN[dayIdx(LocalDate.parse(s.date))]} ${mdS(s.date)} · ${d.plan?.getOrNull(DAYS.indexOf(s.day))?.name ?: ""}", Modifier.padding(top = 12.dp))
            if (s.notes.isNotBlank()) OvText(s.notes, 15)
            s.exNotes.filter { it.value.isNotBlank() }.forEach { (id, v) -> OvText("${vm.exName(id, d)}: $v", 15) }
        }
    }
}
