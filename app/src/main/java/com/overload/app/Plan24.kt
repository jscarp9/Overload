package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * 0.21 / 0.24 Plan page: one clean row per exercise ("3 × 6–8 · Chest"). Tap a row for the edit card,
 * drag the ≡ grip to reorder, swipe left to delete (Trash + Undo), tap the gold 🔗 to unpair a
 * superset or the faint one between two exercises to pair them (max 3 per superset).
 */

private val SS_GOLD = Color(0xFFE8B923)
fun fmtRest(v: Int) = "${v / 60}:${(v % 60).toString().padStart(2, '0')}"
private fun reps(e: Exercise) = "${e.sets} × ${e.lo}${if (e.hi > 0 && e.hi != e.lo) "–${e.hi}" else ""}"

// ---------- group surgery ----------
fun OverloadViewModel.pair24(di: Int, gi: Int) {
    val G = data.value.plan?.getOrNull(di)?.groups ?: return; val a = G.getOrNull(gi) ?: return; val b = G.getOrNull(gi + 1) ?: return
    if (a.size + b.size > 3) { toast("A superset can have up to 3 exercises"); return }
    editDay(di) { d -> d.copy(groups = d.groups.toMutableList().also { it[gi] = a + b; it.removeAt(gi + 1) }) }; haptic("tap"); toast("Superset made")
}
fun OverloadViewModel.unpair24(di: Int, gi: Int, ei: Int) {
    val g = data.value.plan?.getOrNull(di)?.groups?.getOrNull(gi) ?: return; if (ei >= g.size - 1) return
    editDay(di) { d -> d.copy(groups = d.groups.toMutableList().also { it[gi] = g.take(ei + 1); it.add(gi + 1, g.drop(ei + 1)) }) }; haptic("tap"); toast("Unpaired")
}
/** Takes one exercise out of its superset, leaving it where it was. */
fun OverloadViewModel.takeOut24(di: Int, gi: Int, ei: Int) {
    val g = data.value.plan?.getOrNull(di)?.groups?.getOrNull(gi) ?: return; val e = g.getOrNull(ei) ?: return
    editDay(di) { d -> val G = d.groups.toMutableList(); G.removeAt(gi)
        G.addAll(gi, listOfNotNull(g.take(ei).takeIf { it.isNotEmpty() }, listOf(e), g.drop(ei + 1).takeIf { it.isNotEmpty() })); d.copy(groups = G) }
    toast("Taken out of the superset")
}
/** Removes an exercise: copied to Trash first, and the Undo bar puts it back exactly where it was. */
fun OverloadViewModel.del24(di: Int, gi: Int, ei: Int) {
    val d = data.value.plan?.getOrNull(di) ?: return; val gr = d.groups.getOrNull(gi) ?: return; val ex = gr.getOrNull(ei) ?: return
    trash.put("exercise", "${ex.name} (from ${d.name.ifBlank { "plan" }})", buildJsonObject { put("day", di); put("gi", gi); put("ex", jsonCodec.encodeToJsonElement(Exercise.serializer(), ex)) })
    val whole = gr.size == 1
    editDay(di) { day -> val G = day.groups.map { it.toMutableList() }.toMutableList(); G[gi].removeAt(ei); if (G[gi].isEmpty()) G.removeAt(gi); day.copy(groups = G) }
    showUndo("${ex.name} removed") {
        editDay(di) { day -> val G = day.groups.map { it.toMutableList() }.toMutableList()
            if (whole || G.getOrNull(gi) == null) G.add(minOf(gi, G.size), mutableListOf(ex)) else G[gi].add(minOf(ei, G[gi].size), ex); day.copy(groups = G) }
        toast("${ex.name} is back")
    }
}
/** Moves an exercise to slot k (between groups). Dragging out of a superset makes it a normal exercise. */
fun OverloadViewModel.move24(di: Int, gi: Int, ei: Int, k: Int) = editDay(di) { day ->
    val G = day.groups.map { it.toMutableList() }.toMutableList(); val single = G[gi].size == 1
    if (single && (k == gi || k == gi + 1)) return@editDay day
    val ex = G[gi].removeAt(ei); var t = k; if (G[gi].isEmpty()) { G.removeAt(gi); if (gi < k) t-- }
    G.add(t.coerceIn(0, G.size), mutableListOf(ex)); day.copy(groups = G)
}
fun OverloadViewModel.copyDayTo(from: Int, to: Int) {
    val p = data.value.plan ?: return; if (from == to) return
    trash.put("plan", "Your plan before copying ${p[from].name}", jsonCodec.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(DayPlan.serializer()), p))
    editDay(to) { p[from].copy(color = it.color ?: p[from].color) }; toast("Copied ${p[from].name} to ${DN[to]}")
}

// ============================ THE PAGE ============================
@Composable
fun PlanScreen(vm: OverloadViewModel, d: AppData, u: UiState) {
    val plan = d.plan ?: return; val di = u.pday; val day = plan[di]; val col = colorOf(day); val off = day.type == "off"
    val ai by vm.ai.store.collectAsState(); val aiOn = ai.on
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        DN.forEachIndexed { i, n -> val dd = plan[i]
            Column(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(13.dp)).background(if (i == di) ov.ink else ov.card).clickable { vm.setPDay(i); vm.ui.value = vm.ui.value.copy(pd21 = false, pd21m = false) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                OvText(n, 13, FontWeight.ExtraBold, if (i == di) ov.bg else ov.ink)
                Box(Modifier.padding(top = 3.dp).size(6.dp).background(if (dd.type == "off") Color(0xFFC9CDD4) else pc(colorOf(dd)).base, CircleShape)) } }
    }
    if (d.settings.ptip21 == 0) Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(ov.card, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText("Set each day once. It repeats every week.", 14, FontWeight.SemiBold, modifier = Modifier.weight(1f)); OvButton("Got it", { vm.setSettings { it.copy(ptip21 = 1) } }, small = true) }
    val empty = plan.all { it.groups.isEmpty() }
    if (aiOn && (empty || d.settings.aiSeen23 == 0)) AiPlanBar(vm)
    // the day header: plate · name, focus, rest · ⋯
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clickable { vm.openSheet(Sheet.PlatePick(di)) }) { Plate(col, "done", Modifier.fillMaxSize()) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { vm.ui.value = vm.ui.value.copy(pd21 = !u.pd21, pd21m = false) }.padding(4.dp)) {
            Title(day.name.ifBlank { "Untitled day" }, 22)
            Sub(if (off) "Rest day" else "${day.focus.ifBlank { "Add a focus" }} · Rest ${fmtRest(day.rest)}" + (FIN[col]?.let { " · ${it.name}" } ?: ""))
        }
        Box(Modifier.size(40.dp).background(ov.card, RoundedCornerShape(12.dp)).clickable { vm.ui.value = vm.ui.value.copy(pd21m = !u.pd21m, pd21 = false) }, contentAlignment = Alignment.Center) { OvText("⋯", 18, FontWeight.Black) }
    }
    if (u.pd21m) OvCard {
        if (aiOn) { Row16("✦", "Ask Coach") { vm.ui.value = vm.ui.value.copy(pd21m = false); vm.ai.openCoach() }
            Row16("✦", "Build a whole new week with AI") { vm.ui.value = vm.ui.value.copy(pd21m = false); vm.ai.openBuilder() } }
        Row16("✎", "Edit day name, focus and rest") { vm.ui.value = vm.ui.value.copy(pd21 = true, pd21m = false) }
        var copying by remember { mutableStateOf(false) }
        Row16("📋", "Copy this day to other days") { copying = !copying }
        if (copying) { Sub("Tap a day to replace it with ${day.name}. The old plan goes to Trash.", Modifier.padding(bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { DN.forEachIndexed { i, n -> if (i != di) OvText(n, 13, FontWeight.Bold, modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(ov.soft).clickable { vm.copyDayTo(di, i) }.padding(vertical = 8.dp), align = androidx.compose.ui.text.style.TextAlign.Center) } } }
        Row16("🗂", "Templates") { vm.ui.value = vm.ui.value.copy(tpl21 = true, pd21m = false) }
    }
    if (u.pd21) OvCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextBox(day.name, { vm.editDay(di) { x -> x.copy(name = it) } }, "Day name", Modifier.weight(1f))
            Dropdown(TYPES[day.type]?.first ?: "Other", TYPES.values.map { it.first }, Modifier.weight(1f)) { sel -> val key = TYPES.entries.first { it.value.first == sel }.key; vm.editDay(di) { x -> x.copy(type = key, color = null) } }
        }
        if (!off) { Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextBox(day.focus, { vm.editDay(di) { x -> x.copy(focus = it) } }, "Focus, e.g. Chest and back", Modifier.weight(1f))
                val opts = listOf(30, 45, 60, 75, 90, 105, 120, 150, 180, 240)
                Dropdown("Rest ${fmtRest(day.rest)}", opts.map { "Rest ${fmtRest(it)}" }, Modifier.weight(1f)) { sel -> vm.editDay(di) { x -> x.copy(rest = opts.first { "Rest ${fmtRest(it)}" == sel }) } }
            } }
        OvButton("Done", { vm.ui.value = vm.ui.value.copy(pd21 = false) }, Modifier.padding(top = 10.dp), small = true, bg = Red, fg = Color.White)
    }
    if (off) Empty24("😴", "Rest day", "Tap the day name to change it to a training day.")
    else {
        PlanList(vm, di, day)
        if (day.groups.isEmpty()) Empty24("🏋️", "No exercises yet", if (aiOn) "Add one below, or ask Coach ✦ to build the whole week." else "Add your first one below.")
        OvButton("＋ Add exercise", { vm.openPicker(Picker("plan", gi = -1)) }, Modifier.padding(top = 4.dp), bg = ov.ink, fg = ov.bg, wide = true)
        if (day.groups.isNotEmpty() && d.settings.hint24 == 0) Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Sub("Tap to edit · drag ≡ to reorder · swipe left to delete · tap 🔗 to pair or unpair", Modifier.weight(1f)); OvButton("Got it", { vm.setSettings { it.copy(hint24 = 1) } }, small = true) }
    }
    if (u.tpl21) { Spacer(Modifier.height(22.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Title("Templates", 20, modifier = Modifier.weight(1f)); OvButton("Hide", { vm.ui.value = vm.ui.value.copy(tpl21 = false) }, small = true) }
        Sub("Replaces the whole week. Your logged history stays.")
        var confirm by remember { mutableStateOf<String?>(null) }
        Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { TEMPLATES.forEach { t ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ov.card).clickable { if (confirm == t.key) { vm.replacePlan(t.key); confirm = null } else confirm = t.key }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Plate(t.color, "done", Modifier.size(52.dp)); Spacer(Modifier.width(14.dp)); Column { Title(if (confirm == t.key) "Tap again to replace your plan" else t.label, 20); Sub(t.desc) } } } }
    }
    u.picker?.let { ExercisePicker(vm, it) }
}

@Composable private fun Empty24(ic: String, t: String, s: String) = Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
    OvText(ic, 36); OvText(t, 17, FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)); Sub(s) }

@Composable
private fun PlanList(vm: OverloadViewModel, di: Int, day: DayPlan) {
    val G = day.groups
    val bounds = remember(di) { mutableStateMapOf<Int, androidx.compose.ui.geometry.Rect>() }
    var drag by remember(di) { mutableStateOf<Pair<Int, Int>?>(null) }; var dy by remember(di) { mutableStateOf(0f) }; var startY by remember(di) { mutableStateOf(0f) }
    fun target(): Int { val y = startY + dy; val keys = (0 until G.size).mapNotNull { i -> bounds[i]?.let { i to it } }; return keys.firstOrNull { (_, r) -> y < r.top + r.height / 2 }?.first ?: G.size }
    val k = if (drag != null) target() else -1
    G.forEachIndexed { gi, g ->
        if (gi > 0) { val a = G[gi - 1]
            Box(Modifier.fillMaxWidth().height(if (a.size + g.size <= 3) 26.dp else 10.dp), contentAlignment = Alignment.Center) {
                if (k == gi) Box(Modifier.fillMaxWidth().height(3.dp).background(SS_GOLD, RoundedCornerShape(2.dp)))
                else if (a.size + g.size <= 3) LinkBtn(false) { vm.pair24(di, gi - 1) } } }
        else if (k == 0) Box(Modifier.fillMaxWidth().padding(bottom = 4.dp).height(3.dp).background(SS_GOLD, RoundedCornerShape(2.dp)))
        val ss = g.size > 1
        Column(Modifier.fillMaxWidth().onGloballyPositioned { bounds[gi] = it.boundsInRoot() }
            .then(if (ss) Modifier.clip(RoundedCornerShape(20.dp)).border(1.5.dp, SS_GOLD.copy(.55f), RoundedCornerShape(20.dp)).background(SS_GOLD.copy(.06f)).padding(6.dp) else Modifier)) {
            if (ss) OvText("SUPERSET", 11, FontWeight.ExtraBold, SS_GOLD, Modifier.padding(start = 8.dp, bottom = 4.dp))
            g.forEachIndexed { ei, e ->
                if (ei > 0) Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.Center) { LinkBtn(true) { vm.unpair24(di, gi, ei - 1) } }
                val me = drag == gi to ei
                Row24(vm, di, gi, ei, e, Modifier.zIndex(if (me) 1f else 0f).graphicsLayer { translationY = if (me) dy else 0f; shadowElevation = if (me) 12f else 0f },
                    onDragStart = { y -> drag = gi to ei; dy = 0f; startY = y; vm.haptic("tap") }, onDrag = { dy += it },
                    onDragEnd = { ok -> val d0 = drag; val t = target(); drag = null; dy = 0f; if (ok && d0 != null) vm.move24(di, d0.first, d0.second, t) })
            }
        }
    }
    if (k == G.size && G.isNotEmpty()) Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(3.dp).background(SS_GOLD, RoundedCornerShape(2.dp)))
}

@Composable private fun LinkBtn(on: Boolean, onClick: () -> Unit) =
    Box(Modifier.size(28.dp).clip(CircleShape).background(if (on) SS_GOLD else ov.soft).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        OvText("🔗", 13, modifier = Modifier.graphicsLayer { alpha = if (on) 1f else .5f }) }

@Composable
private fun Row24(vm: OverloadViewModel, di: Int, gi: Int, ei: Int, e: Exercise, modifier: Modifier, onDragStart: (Float) -> Unit, onDrag: (Float) -> Unit, onDragEnd: (Boolean) -> Unit) {
    var sx by remember(e.id, gi, ei) { mutableStateOf(0f) }; var top by remember { mutableStateOf(0f) }
    Box(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Red).onGloballyPositioned { top = it.boundsInRoot().center.y }) {
        OvText("Delete", 14, FontWeight.Black, Color.White, Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
        Row(Modifier.fillMaxWidth().graphicsLayer { translationX = sx }.background(if (ov.dark) Color(0xFF16181F) else Color.White, RoundedCornerShape(16.dp))
            .pointerInput(e.id, gi, ei) { detectHorizontalDragGestures(onDragEnd = { if (sx < -90 * density) vm.del24(di, gi, ei); sx = 0f }, onDragCancel = { sx = 0f }) { _, a -> sx = (sx + a).coerceIn(-160 * density, 0f) } }
            .clickable { vm.openSheet(Sheet.PlanEx(di, gi, ei)) }.padding(start = 14.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                OvText(e.name.ifBlank { "Exercise" }, 16, FontWeight.Bold)
                Sub("${reps(e)} · ${e.m}" + (if (e.cue.isNotEmpty()) " · 📝" else "") + (if (e.ai == "new") " · ✦ new" else ""))
            }
            Box(Modifier.size(44.dp).pointerInput(e.id, gi, ei) {
                detectDragGestures(onDragStart = { onDragStart(top) }, onDragEnd = { onDragEnd(true) }, onDragCancel = { onDragEnd(false) }) { ch, a -> ch.consume(); onDrag(a.y) } },
                contentAlignment = Alignment.Center) { OvText("≡", 22, FontWeight.Black, ov.mute) }
        }
    }
}

// ============================ THE EDIT CARD ============================
@Composable
fun PlanExSheet(vm: OverloadViewModel, c: Ctx, di: Int, gi: Int, ei: Int) {
    val d = c.plan.getOrNull(di); val g = d?.groups?.getOrNull(gi); val e = g?.getOrNull(ei)
    if (e == null) { SheetHead("Exercise") { vm.closeSheet() }; Sub("This exercise was moved or removed."); return }
    val next = d.groups.getOrNull(gi + 1); val ss = g.size > 1
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Sub(d.name, Modifier.weight(1f)); OvButton("Done", { vm.closeSheet() }, small = true, bg = Red, fg = Color.White) }
    TextBox(e.name, { v -> vm.editEx(di, gi, ei) { it.copy(name = v.take(40)) } }, "Exercise name", bold = true, size = 20)
    Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) { OvText("Muscle", 15, FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Dropdown(e.m, MUSCLES, Modifier.width(170.dp)) { m -> vm.editEx(di, gi, ei) { it.copy(m = m) } } }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("sets", "Sets", e.sets), Triple("lo", "Min reps", e.lo), Triple("hi", "Max reps", e.hi)).forEach { (f, l, v) ->
            Column(Modifier.weight(1f).background(ov.soft, RoundedCornerShape(16.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Sub(l)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepBtn("−") { vm.editEx(di, gi, ei) { x -> step(x, f, -1) } }
                    NumBox("$v", "0", { t -> t.toIntOrNull()?.takeIf { it > 0 }?.let { n -> vm.editEx(di, gi, ei) { x -> set(x, f, n) } } }, Modifier.width(44.dp), decimal = false, size = 18, h = 40.dp)
                    StepBtn("+") { vm.editEx(di, gi, ei) { x -> step(x, f, 1) } }
                } } } }
    TextBox(e.cue, { v -> vm.editEx(di, gi, ei) { it.copy(cue = v.take(80)) } }, "Form cue or note (optional)", Modifier.padding(top = 12.dp), size = 14)
    Column(Modifier.padding(top = 12.dp)) {
        if (vm.ai.on) Row16("✦", "Form tips", "AI cues for this lift") { vm.ai.openCue(e.name) }
        if (ss) Row16("🔗", "Take out of superset") { vm.takeOut24(di, gi, ei); vm.closeSheet() }
        else if (next != null && g.size + next.size <= 3) Row16("🔗", "Superset with ${next[0].name}") { vm.closeSheet(); vm.pair24(di, gi) }
        OvButton("Remove from this day", { vm.closeSheet(); vm.del24(di, gi, ei) }, Modifier.padding(top = 8.dp), fg = Red, wide = true)
    }
}
@Composable private fun StepBtn(t: String, onClick: () -> Unit) = Box(Modifier.size(32.dp).background(ov.card, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { OvText(t, 18, FontWeight.Black) }
private fun set(x: Exercise, f: String, v0: Int): Exercise { val v = v0.coerceIn(1, if (f == "sets") 20 else 100)
    return when (f) { "sets" -> x.copy(sets = v); "lo" -> x.copy(lo = v, hi = maxOf(x.hi, v)); else -> x.copy(hi = v, lo = minOf(x.lo, v)) } }
private fun step(x: Exercise, f: String, d: Int) = set(x, f, (when (f) { "sets" -> x.sets; "lo" -> x.lo; else -> x.hi }) + d)

