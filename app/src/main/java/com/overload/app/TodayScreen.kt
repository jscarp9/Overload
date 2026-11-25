package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun TodayScreen(vm: OverloadViewModel, d: AppData, u: UiState, now: Long) {
    val plan = d.plan ?: return
    val day = plan[dayIdx(u.date)]
    val s = vm.session(d, u)
    val groups = vm.todayGroups(d, u)
    val col = colorOf(day)
    Column(Modifier.fillMaxWidth()) {
        Rack(vm, d, u)
        Hero(vm, d, u, day, s, groups, now)
        if (s.end != null) Summary(vm, d, s, now)
        if (s.end != null && s.start != null && d.settings.heartRate) HeartRateCard(vm, vm.curKey(u), s)
        if (day.type == "off") {
            OvCard {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Title("Rest day", 20)
                    Spacer(Modifier.height(6.dp))
                    Sub("Walk, eat, sleep. Growth happens today. Want to train anyway? Tap another day above or add an exercise.")
                    Spacer(Modifier.height(10.dp))
                    OvButton("+ Add exercise", { vm.openPicker(Picker("today")) })
                }
            }
        } else {
            groups.forEachIndexed { gi, gr ->
                OvCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OvText(if (gr.size > 1) "SUPERSET ${'A' + gi}" else "EXERCISE ${'A' + gi}", 13, FontWeight.ExtraBold, ov.mute, Modifier.weight(1f))
                        val r = day.rest
                        OvButton("⏱ Rest ${r / 60}:${(r % 60).toString().padStart(2, '0')}", { vm.startRest(r) }, small = true)
                    }
                    gr.forEachIndexed { ei, e ->
                        if (ei > 0) HorizontalDivider(Modifier.padding(top = 12.dp), color = ov.line)
                        ExerciseBlock(vm, d, u, s, e, gi, ei, gr.size, col)
                    }
                }
            }
            if (day.groups.isEmpty()) OvCard {
                Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Title("No exercises yet", 20); Spacer(Modifier.height(6.dp))
                    Sub("Add them now. They save to your plan for every ${DN[dayIdx(u.date)]}.")
                }
            }
            OvButton("+ Add exercise", { vm.openPicker(Picker("today")) }, Modifier.padding(bottom = 12.dp), bg = ov.ink, fg = ov.bg, wide = true)
            OvCard {
                OvText("SESSION NOTES", 13, FontWeight.ExtraBold, ov.mute)
                Spacer(Modifier.height(8.dp))
                TextBox(s.notes, { vm.setSessionNote(it) }, "Energy, sleep, what felt strong, what hurt…", singleLine = false, minH = 90.dp)
            }
        }
    }
    u.picker?.let { ExercisePicker(vm, it) }
}

@Composable
private fun Rack(vm: OverloadViewModel, d: AppData, u: UiState) {
    val wd = weekDates(u.date); val today = LocalDate.now(); val plan = d.plan!!
    val label = if (wd[0].monthValue == wd[6].monthValue) "${MON[wd[0].monthValue - 1]} ${wd[0].dayOfMonth} – ${wd[6].dayOfMonth}"
    else "${MON[wd[0].monthValue - 1]} ${wd[0].dayOfMonth} – ${MON[wd[6].monthValue - 1]} ${wd[6].dayOfMonth}"
    OvCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OvText(label, 16, FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            NavChip("‹") { vm.shiftWeek(-1) }
            if (u.date != today) { Spacer(Modifier.width(6.dp)); OvButton("Today", { vm.goToday() }, small = true) }
            Spacer(Modifier.width(6.dp))
            NavChip("›") { vm.shiftWeek(1) }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            wd.forEachIndexed { i, ds ->
                val p = plan[i]
                val state = if (p.type == "off") "rest" else if (doneOn(d.sessions, ds, DAYS[i])) "done" else "idle"
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (ds == u.date) ov.soft else Color.Transparent)
                        .clickable { vm.setDate(ds, true) }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Plate(colorOf(p), state, Modifier.fillMaxWidth(.9f))
                    Spacer(Modifier.height(4.dp))
                    OvText(if (ds == today) "Today" else DN[i], 12, FontWeight.Bold, if (ds == today) Red else ov.mute)
                    OvText("${ds.dayOfMonth}", 15, FontWeight.ExtraBold, if (ds == u.date) Red else ov.ink)
                }
            }
        }
    }
}

@Composable
private fun NavChip(t: String, onClick: () -> Unit) =
    Box(Modifier.size(36.dp).background(ov.soft, RoundedCornerShape(11.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        OvText(t, 18, FontWeight.ExtraBold)
    }

@Composable
private fun Hero(vm: OverloadViewModel, d: AppData, u: UiState, day: DayPlan, s: Session, groups: List<List<Exercise>>, now: Long) {
    val col = pc(colorOf(day)); val off = day.type == "off"
    var tot = 0; var dn = 0
    groups.forEach { gr -> gr.forEach { e -> val l = s.ex[e.id] ?: emptyList(); tot += maxOf(e.sets, l.size); dn += l.count { it.d } } }
    val ss = sessStats(d.sessions, s)
    val unit = d.settings.unit
    val today = LocalDate.now()
    val whenLbl = if (u.date == today) "Today" else if (u.date < today) "Past" else "Upcoming"
    var renaming by remember(u.date) { mutableStateOf(false) }
    var newName by remember(u.date, day.name) { mutableStateOf(day.name) }
    Box(
        Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(col.grad)).padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    OvText("$whenLbl · ${DN[dayIdx(u.date)]} ${MON[u.date.monthValue - 1]} ${u.date.dayOfMonth}", 14, FontWeight.SemiBold, Color.White.copy(.9f))
                    Spacer(Modifier.height(4.dp))
                    if (renaming) {
                        TextBox(newName, { newName = it }, "Day name", bold = true, size = 24)
                        Spacer(Modifier.height(6.dp))
                        OvButton("Save", { vm.renameToday(newName); renaming = false }, small = true, bg = Color.White, fg = Color(0xFF14161C))
                    } else Row(verticalAlignment = Alignment.CenterVertically) {
                        Title(day.name, 40, Color.White, Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(36.dp).background(Color.White.copy(.22f), RoundedCornerShape(12.dp)).clickable { renaming = true }, contentAlignment = Alignment.Center) { OvText("✎", 18, color = Color.White) }
                    }
                    OvText(day.focus, 14, FontWeight.SemiBold, Color.White.copy(.9f))
                }
                Plate(colorOf(day), if (off) "rest" else "idle", Modifier.size(112.dp), ring = if (off) null else if (tot > 0) dn.toFloat() / tot else 0f)
            }
            if (!off) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HeroStat(mins(elapsed(s, now)), if (s.pauseAt != null) "Paused" else "Time", Modifier.weight(1f))
                    HeroStat("$dn/$tot", "Sets", Modifier.weight(1f))
                    HeroStat(kfmt(ss.vol), "$unit moved", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    var confirm by remember(u.date) { mutableStateOf(false) }
                    if (s.start == null) OvButton("▶ Start workout", { vm.startWorkout() }, bg = Color.White, fg = Color(0xFF14161C))
                    else {
                        if (s.end == null) {
                            OvButton(if (s.pauseAt != null) "▶ Resume" else "❚❚ Pause", { vm.togglePause() }, bg = Color.White, fg = Color(0xFF14161C))
                            OvButton("Finish", { vm.finish() }, bg = Color.White.copy(.22f), fg = Color.White)
                        }
                        OvButton(if (confirm) "Tap to reset" else "↺ Reset", { if (confirm) { vm.resetWorkout(); confirm = false } else confirm = true }, bg = Color.White.copy(.22f), fg = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(v: String, l: String, modifier: Modifier) =
    Column(modifier.background(Color.White.copy(.18f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Title(v, 24, Color.White); OvText(l, 12, FontWeight.SemiBold, Color.White.copy(.9f))
    }

@Composable
private fun Summary(vm: OverloadViewModel, d: AppData, s: Session, now: Long) {
    val ss = sessStats(d.sessions, s)
    Box(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF16B65B), Color(0xFF00C2A8)))).padding(16.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Title("Workout complete", 20, Color.White, Modifier.weight(1f))
                OvButton("Keep logging", { vm.reopen() }, small = true, bg = Color.White.copy(.22f), fg = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat(mins(elapsed(s, now)), "Time", Modifier.weight(1f)); HeroStat("${ss.sets}", "Sets", Modifier.weight(1f))
                HeroStat(kfmt(ss.vol), "${d.settings.unit} moved", Modifier.weight(1f)); HeroStat("${ss.prs.size}", "PRs", Modifier.weight(1f))
            }
            if (ss.prs.isNotEmpty()) { Spacer(Modifier.height(8.dp)); OvText("New PRs: " + ss.prs.joinToString(", ") { vm.exName(it, d) }, 13, FontWeight.Bold, Color.White) }
        }
    }
}

@Composable
private fun ExerciseBlock(vm: OverloadViewModel, d: AppData, u: UiState, s: Session, e: Exercise, gi: Int, ei: Int, glen: Int, col: String) {
    val unit = d.settings.unit; val rir = d.settings.rir
    val key = "$gi-$ei"
    val sets = s.ex[e.id] ?: emptyList()
    val last = remember(d.sessions, e.id, u.date) { lastSession(d.sessions, e.id, vm.curKey(u), u.date.toString()) }
    val sg = remember(last, e, unit) { suggest(e, last, unit) }
    val n = maxOf(e.sets, sets.size)
    val letter = "${'A' + gi}" + if (glen > 1) "${ei + 1}" else ""
    val pb = bestBefore(d.sessions, e.id, s.date)
    val orig = vm.todayPlan(d, u)?.groups?.getOrNull(gi)?.getOrNull(ei)
    val isSw = orig != null && orig.id != e.id
    var menu by remember { mutableStateOf(false) }
    var confirmDel by remember { mutableStateOf(false) }
    var plates by remember { mutableStateOf(false) }

    Column(Modifier.padding(top = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(pc(col).base, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { OvText(letter, 14, FontWeight.Black, Color.White) }
            Spacer(Modifier.width(10.dp))
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Title(e.name, 20, modifier = Modifier.weight(1f, fill = false))
                if (isSw) OvText("SWAPPED", 11, FontWeight.ExtraBold, Blue, Modifier.padding(start = 6.dp).background(Blue.copy(.12f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 2.dp))
            }
            OvText("${e.sets} × ${if (e.lo > 0) e.lo else "?"}–${if (e.hi > 0) e.hi else "?"}", 13, FontWeight.Bold, ov.mute)
            Box {
                Box(Modifier.padding(start = 8.dp).height(34.dp).widthIn(min = 34.dp).background(ov.soft, RoundedCornerShape(10.dp)).clickable { menu = true }, contentAlignment = Alignment.Center) { OvText("⋯", 18, FontWeight.Black, ov.mute) }
                DropdownMenu(menu, { menu = false; confirmDel = false }) {
                    DropdownMenuItem({ Text("🏋️ Plate calculator") }, { menu = false; plates = true })
                    DropdownMenuItem({ Text("🔥 Warm-ups") }, { menu = false; vm.toggleWarm(e.id) })
                    if (isSw) DropdownMenuItem({ Text("↩︎ Undo swap (back to ${orig!!.name})") }, { menu = false; vm.unswap(orig!!) })
                    else DropdownMenuItem({ Text("🔄 Swap for today") }, { menu = false; vm.openPicker(Picker("swap", key = key)) })
                    DropdownMenuItem({ Text(if (confirmDel) "Tap again to remove" else "🗑 Remove from plan", color = Red) }, {
                        if (!confirmDel) confirmDel = true
                        else { vm.removeEx(dayIdx(u.date), gi, ei); menu = false; confirmDel = false }
                    })
                }
            }
        }
        val lastLine = if (last != null) "Last ${mdS(last.date)}: ${fmtSets(last.ex[e.id])}" + (sg?.let { " · Try ${fmtN(it.w)}×${it.r}" } ?: "") else "First time. Set your baseline"
        Sub("${e.m} · $lastLine" + if (pb > 0) " · Best ${pb.toInt()}" else "", Modifier.padding(vertical = 8.dp))

        if (e.id in u.warm) WarmBox(vm, e, sets, sg, unit)
        if (e.cue.isNotEmpty()) Tag(e.cue, Color(0xFFFFF6DA), Color(0xFF7A5A00))
        val ln = lastNote(d.sessions, e.id, vm.curKey(u), u.date.toString())
        if (ln.isNotEmpty()) Tag("Last note: $ln", Color(0xFFEEF2FF), Color(0xFF3B4252))
        tagFor(vm, d, u, e, sets, last, pb)?.let { (txt, bg, fg) -> Tag(txt, bg, fg) }

        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sub("Set", Modifier.width(34.dp)); Sub(unit, Modifier.weight(1f), ov.mute); Sub("Reps", Modifier.weight(1f))
            if (rir) Sub("RIR", Modifier.width(52.dp)); Spacer(Modifier.width(52.dp))
        }
        for (i in 0 until n) {
            val x = sets.getOrNull(i) ?: SetEntry()
            val la = last?.ex?.get(e.id) ?: emptyList()
            val lx = la.getOrNull(i) ?: la.lastOrNull()
            val phW = sg?.w ?: lx?.w?.takeIf { it > 0 }
            val phR = sg?.r ?: lx?.r?.takeIf { it > 0 }
            val done = x.d
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(34.dp), verticalAlignment = Alignment.CenterVertically) {
                    OvText("${i + 1}", 18, FontWeight.Black, if (done) Green else Color(0xFFB3B6C2))
                    if (better(x, la.getOrNull(i))) OvText("↑", 13, color = Green)
                }
                val doneBg = if (ov.dark) Green.copy(.16f) else Color(0xFFE8F8EF)
                val doneFg = if (ov.dark) Color(0xFF5FF0A0) else Color(0xFF0B6B35)
                NumBox(x.w?.let { fmtN(it) } ?: "", phW?.let { fmtN(it) } ?: "0", { vm.setField(e, i, 'w', it) }, Modifier.weight(1f), true, if (done) doneBg else ov.soft, if (done) doneFg else ov.ink)
                NumBox(x.r?.toString() ?: "", phR?.toString() ?: "0", { vm.setField(e, i, 'r', it) }, Modifier.weight(1f), false, if (done) doneBg else ov.soft, if (done) doneFg else ov.ink)
                if (rir) NumBox(x.q?.toString() ?: "", "RIR", { vm.setField(e, i, 'q', it) }, Modifier.width(52.dp), false, size = 16)
                Box(Modifier.size(52.dp, 50.dp).background(if (done) Green else ov.soft, RoundedCornerShape(14.dp)).clickable { vm.toggleSet(e, gi, ei, i, phW, phR) }, contentAlignment = Alignment.Center) {
                    OvText("✓", 22, FontWeight.Black, if (done) Color.White else Color(0xFFB4B7C2))
                }
            }
        }
        TextBox(s.exNotes[e.id] ?: "", { vm.setExNote(e.id, it) }, "Add a note (seat, grip, how it felt)", singleLine = false, minH = 40.dp, size = 14)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("+ Set", { vm.addSet(e) }, small = true)
            if (sets.isNotEmpty()) OvButton("− Set", { vm.removeSet(e) }, small = true)
        }
    }
    if (plates) PlateDialog(e.name, sets.firstOrNull(::full)?.w ?: sg?.w, unit) { plates = false }
}

@Composable
private fun Tag(t: String, bg: Color, fg: Color) =
    OvText(t, 13, FontWeight.Bold, fg, Modifier.padding(bottom = 8.dp).background(bg, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 4.dp))

private fun tagFor(vm: OverloadViewModel, d: AppData, u: UiState, e: Exercise, sets: List<SetEntry>, last: Session?, pb: Double): Triple<String, Color, Color>? {
    val f = sets.filter(::full)
    if (pb > 0 && f.any { e1(it) > pb }) return Triple("New PR", Color(0xFFFFE5E7), Color(0xFFD1121F))
    if (e.hi > 0 && f.size >= e.sets && f.all { (it.r ?: 0) >= e.hi }) return Triple("Top of range. Add weight next time", Color(0xFFE3F7EC), Color(0xFF0F8A45))
    val la = last?.ex?.get(e.id) ?: emptyList()
    if (last != null && f.indices.any { better(f[it], la.getOrNull(it)) }) return Triple("Beat last time", Color(0xFFFFE5E7), Color(0xFFD1121F))
    if (f.isEmpty() && stalled(d.sessions, e.id, u.date.toString())) return Triple("Stalled 3 sessions · try a 10% deload or swap it", Color(0xFFFFF1E0), Color(0xFFB35A00))
    return null
}

@Composable
private fun WarmBox(vm: OverloadViewModel, e: Exercise, sets: List<SetEntry>, sg: Sug?, unit: String) {
    val t = sets.firstOrNull(::full)?.w ?: sg?.w ?: 0.0
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(ov.soft, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvText(if (t > 0) "WARM-UPS FOR ${fmtN(t)} ${unit.uppercase()}" else "WARM-UPS", 13, FontWeight.ExtraBold, ov.mute, Modifier.weight(1f))
            OvButton("Hide", { vm.toggleWarm(e.id) }, small = true)
        }
        if (t <= 0) Sub("Log your first set or do this lift once, then warm-ups are worked out for you.")
        else {
            warmRows(t, unit).forEach { (w, r) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { OvText("${fmtN(w)} $unit", 14, modifier = Modifier.weight(1f)); OvText("× $r", 14, FontWeight.Bold) }
            }
            Sub("Easy and fast. Warm-ups don't count as working sets.", Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun PlateDialog(name: String, startW: Double?, unit: String, onClose: () -> Unit) {
    var w by remember { mutableStateOf(startW?.let { fmtN(it) } ?: "") }
    val bars = if (unit == "kg") listOf(20.0, 15.0, 0.0) else listOf(45.0, 35.0, 0.0)
    var bar by remember { mutableStateOf(bars[0]) }
    val total = w.toDoubleOrNull() ?: 0.0
    val (out, left) = plateCalc(total, bar, unit)
    AlertDialog(onDismissRequest = onClose, confirmButton = { OvButton("Done", onClose, small = true) },
        title = { Title("Plate calculator", 22) },
        text = {
            Column {
                Sub(name)
                Spacer(Modifier.height(10.dp))
                NumBox(w, "Total $unit", { w = it })
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    bars.forEach { b -> Seg(if (b > 0) "${fmtN(b)} bar" else "No bar", bar == b, { bar = b }) }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp, 14.dp).background(Color(0xFFB4B9C2), RoundedCornerShape(4.dp)))
                    out.reversed().forEach { Box(Modifier.padding(horizontal = 1.dp).size(14.dp, (110 * it.h).dp).background(Color(it.color), RoundedCornerShape(5.dp))) }
                    Box(Modifier.size(50.dp, 10.dp).background(Color(0xFF9AA0AA), RoundedCornerShape(3.dp)))
                    out.forEach { Box(Modifier.padding(horizontal = 1.dp).size(14.dp, (110 * it.h).dp).background(Color(it.color), RoundedCornerShape(5.dp))) }
                    Box(Modifier.size(20.dp, 14.dp).background(Color(0xFFB4B9C2), RoundedCornerShape(4.dp)))
                }
                Spacer(Modifier.height(8.dp))
                val msg = when {
                    total == 0.0 -> "Enter a total weight."
                    total < bar -> "That's less than the bar."
                    out.isEmpty() -> "Just the bar."
                    else -> "Per side: " + out.joinToString(" + ") { fmtN(it.w) } + if (left > .01) "  (${fmtN(left * 2)} $unit short of exact)" else ""
                }
                OvText(msg, 17, FontWeight.Bold, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        })
}

@Composable
fun ExercisePicker(vm: OverloadViewModel, p: Picker) {
    var q by remember { mutableStateOf("") }
    val ql = q.trim().lowercase()
    val list = LIB.filter { ql.isEmpty() || it.name.lowercase().contains(ql) || it.m.lowercase().contains(ql) }.take(40)
    AlertDialog(
        onDismissRequest = { vm.openPicker(null) },
        confirmButton = { OvButton("Close", { vm.openPicker(null) }, small = true) },
        title = { Title(if (p.ctx == "swap") "Swap for today" else if (p.gi >= 0) "Add to superset ${'A' + p.gi}" else "Add exercise", 20) },
        text = {
            Column {
                TextBox(q, { q = it }, "Search 60+ exercises or type your own")
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (ql.isNotEmpty() && LIB.none { it.name.lowercase() == ql })
                        item { PickRow("Add “${q.trim()}”", "Custom") { vm.pickExercise(q.trim(), "Other", 8, 12) } }
                    items(list) { PickRow(it.name, "${it.m} · ${it.lo}–${it.hi}") { vm.pickExercise(it.name, it.m, it.lo, it.hi) } }
                }
            }
        })
}

@Composable
private fun PickRow(a: String, b: String, onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().background(ov.soft, RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(a, 15, FontWeight.Bold, modifier = Modifier.weight(1f)); Sub(b)
    }
