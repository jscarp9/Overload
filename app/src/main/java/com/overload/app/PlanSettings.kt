package com.overload.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.launch

// ============================ PLAN ============================
@Composable
fun PlanScreen(vm: OverloadViewModel, d: AppData, u: UiState) {
    val plan = d.plan ?: return
    val di = u.pday; val day = plan[di]
    var confirm by remember(di) { mutableStateOf<String?>(null) }
    Title("Plan"); Sub("Set up each day once. It repeats every week.")
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        DN.forEachIndexed { i, n ->
            val dd = plan[i]
            Column(Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(13.dp))
                .background(if (i == di) ov.ink else ov.card).clickable { vm.setPDay(i) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                OvText(n, 13, FontWeight.ExtraBold, if (i == di) ov.bg else ov.ink)
                Box(Modifier.padding(top = 3.dp).size(6.dp).background(if (dd.type == "off") Color(0xFFC9CDD4) else pc(colorOf(dd)).base, androidx.compose.foundation.shape.CircleShape))
            }
        }
    }
    OvCard {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextBox(day.name, { vm.editDay(di) { x -> x.copy(name = it) } }, "Day name", Modifier.weight(1f))
            Dropdown(TYPES[day.type]?.first ?: "Other", TYPES.values.map { it.first }, Modifier.weight(1f)) { sel ->
                val key = TYPES.entries.first { it.value.first == sel }.key
                vm.editDay(di) { x -> x.copy(type = key, color = null) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextBox(day.focus, { vm.editDay(di) { x -> x.copy(focus = it) } }, "Focus, e.g. Chest and back", Modifier.weight(1f))
            val opts = listOf(30, 45, 60, 75, 90, 105, 120, 150, 180, 240)
            Dropdown("Rest ${day.rest / 60}:${(day.rest % 60).toString().padStart(2, '0')}", opts.map { "Rest ${it / 60}:${(it % 60).toString().padStart(2, '0')}" }, Modifier.weight(1f)) { sel ->
                vm.editDay(di) { x -> x.copy(rest = opts[opts.indexOfFirst { "Rest ${it / 60}:${(it % 60).toString().padStart(2, '0')}" == sel }]) }
            }
        }
        Sub("Colour", Modifier.padding(top = 10.dp))
        val cx by vm.engCtx.collectAsState()
        PC.keys.chunked(7).forEach { row -> Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { k -> val lock = cx.plateLocked(k)
                Box(Modifier.size(38.dp).clip(androidx.compose.foundation.shape.CircleShape).background(PC.getValue(k).base)
                    .then(if (colorOf(day) == k) Modifier.background(Color.White.copy(.35f)) else Modifier)
                    .clickable { vm.setPlateColor(di, k) }, contentAlignment = Alignment.Center) {
                    if (colorOf(day) == k) OvText("✓", 16, FontWeight.Black, Color.White) else if (lock.isNotEmpty()) OvText("🔒", 13)
                }
            }
        } }
    }
    if (day.type != "off") {
        day.groups.forEachIndexed { gi, gr ->
            OvCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OvText("${if (gr.size > 1) "SUPERSET" else "EXERCISE"} ${'A' + gi}", 13, FontWeight.ExtraBold, ov.mute, Modifier.weight(1f))
                    OvButton("↑", { vm.moveGroupUp(di, gi) }, small = true); Spacer(Modifier.width(6.dp))
                    OvButton(if (confirm == "g$gi") "Tap to confirm" else "Delete", { if (confirm == "g$gi") { vm.removeGroup(di, gi); confirm = null } else confirm = "g$gi" }, small = true)
                }
                gr.forEachIndexed { ei, e ->
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextBox(e.name, { vm.editEx(di, gi, ei) { x -> x.copy(name = it) } }, "Exercise name", Modifier.weight(1f), bold = true)
                        OvButton("✕", { vm.removeEx(di, gi, ei) })
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                        Dropdown(e.m, MUSCLES, Modifier.weight(1.4f)) { m -> vm.editEx(di, gi, ei) { x -> x.copy(m = m) } }
                        LabeledNum("Sets", e.sets, Modifier.weight(1f)) { n -> vm.editEx(di, gi, ei) { x -> x.copy(sets = n) } }
                        LabeledNum("Min reps", e.lo, Modifier.weight(1f)) { n -> vm.editEx(di, gi, ei) { x -> x.copy(lo = n) } }
                        LabeledNum("Max reps", e.hi, Modifier.weight(1f)) { n -> vm.editEx(di, gi, ei) { x -> x.copy(hi = n) } }
                    }
                    Spacer(Modifier.height(6.dp))
                    TextBox(e.cue, { vm.editEx(di, gi, ei) { x -> x.copy(cue = it) } }, "Cue, e.g. Elbows tucked", size = 14)
                }
                Spacer(Modifier.height(8.dp))
                OvButton("+ Superset another exercise", { vm.openPicker(Picker("plan", gi = gi)) }, small = true)
            }
        }
        OvButton("+ Add exercise", { vm.openPicker(Picker("plan", gi = -1)) }, bg = ov.ink, fg = ov.bg, wide = true)
    }
    Spacer(Modifier.height(22.dp))
    Title("Templates", 20); Sub("Replaces the whole week. Your logged history stays.")
    TemplateList(Modifier.padding(top = 10.dp), confirm) { t -> if (confirm == "t${t.key}") { vm.replacePlan(t.key); confirm = null } else confirm = "t${t.key}" }
    u.picker?.let { ExercisePicker(vm, it) }
}

@Composable
private fun TemplateList(modifier: Modifier, confirm: String?, onPick: (Template) -> Unit) = Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    TEMPLATES.forEach { t ->
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ov.card).clickable { onPick(t) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Plate(t.color, "done", Modifier.size(52.dp)); Spacer(Modifier.width(14.dp))
            Column {
                Title(if (confirm == "t${t.key}") "Tap again to replace your plan" else t.label, 20)
                Sub(t.desc)
            }
        }
    }
}

@Composable
private fun Dropdown(current: String, options: List<String>, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(ov.soft, RoundedCornerShape(14.dp)).clickable { open = true }.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            OvText(current, 14, FontWeight.SemiBold)
        }
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem({ Text(o) }, { onPick(o); open = false }) } }
    }
}

@Composable
private fun LabeledNum(label: String, v: Int, modifier: Modifier, onChange: (Int) -> Unit) =
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Sub(label)
        NumBox(if (v > 0) "$v" else "", "0", { onChange(it.toIntOrNull()?.coerceAtLeast(0) ?: 0) }, decimal = false, size = 16, h = 44.dp)
    }

// ============================ SETTINGS ============================
@Composable
fun SettingsScreen(vm: OverloadViewModel, d: AppData) {
    val st = d.settings; val ctx = LocalContext.current
    var confirmWipe by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
    }
    val scope = rememberCoroutineScope()
    val hrPerm = rememberLauncherForActivityResult(Health.permissionContract()) { granted -> vm.enableHeartRate(granted.containsAll(Health.PERMS)) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.enableRestAlerts(it) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) ctx.contentResolver.openInputStream(uri)?.use { vm.importJson(it.readBytes().decodeToString()) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Title("Settings", modifier = Modifier.weight(1f)); OvButton("Done", { vm.toggleSettings() }, small = true, bg = ov.ink, fg = ov.bg)
    }
    SecLabel("LOOK")
    OvCard {
        OvText("YOUR NAME", 13, FontWeight.ExtraBold, ov.mute); Spacer(Modifier.height(8.dp))
        TextBox(st.name, { vm.setSettings { s -> s.copy(name = it.trim()) } }, "Name")
        Spacer(Modifier.height(14.dp)); OvText("THEME", 13, FontWeight.ExtraBold, ov.mute)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("light" to "☀️ Light", "dark" to "🌙 Dark", "auto" to "Auto").forEach { (k, l) -> Seg(l, st.theme == k, { vm.setSettings { s -> s.copy(theme = k) } }) }
        }
        OvText("UNITS", 13, FontWeight.ExtraBold, ov.mute)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("lb", "kg").forEach { k -> Seg(k, st.unit == k, { vm.setSettings { s -> s.copy(unit = k) } }) }
        }
    }
    SecLabel("WORKOUT")
    OvCard {
        TogRow("Auto rest timer", "Starts when you finish the last exercise in a round", st.autoRest) { v -> vm.setSettings { it.copy(autoRest = v) } }
        TogRow("Sound when rest ends", null, st.sound) { v -> vm.setSettings { it.copy(sound = v) } }
        TogRow("Vibrate when rest ends", null, st.vibe) { v -> vm.setSettings { it.copy(vibe = v) } }
        TogRow("Track RIR", "Adds a reps in reserve box to each set", st.rir) { v -> vm.setSettings { it.copy(rir = v) } }
        TogRow("Keep screen on during workouts", "So the rest timer stays on screen", st.awake) { v -> vm.setSettings { it.copy(awake = v) } }
        TogRow("Rest alerts on lock screen", "Get a ping when rest is over, even with your phone locked", st.restAlerts) { v ->
            when {
                !v -> vm.disableRestAlerts()
                RestAlerts.canNotify(ctx) -> vm.enableRestAlerts(true)
                else -> notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    SecLabel("HEALTH")
    OvCard {
        TogRow("Heart rate from Samsung Health", "Average, max and a heart rate graph after each workout, through Health Connect", st.heartRate) { v ->
            if (!v) vm.setSettings { it.copy(heartRate = false) }
            else when (Health.status(ctx)) {
                "AVAILABLE" -> scope.launch { if (Health.hasPerms(ctx)) vm.enableHeartRate(true) else hrPerm.launch(Health.PERMS) }
                "NEEDS_INSTALL" -> { vm.toast("Install or update Health Connect first"); Health.openStore(ctx) }
                else -> vm.toast("Heart rate needs Android 9 or newer")
            }
        }
        if (st.heartRate) Sub("In Samsung Health: Settings → Health Connect, and allow heart rate. Starting a Weights workout on your watch gives a much more detailed graph.", Modifier.padding(bottom = 6.dp))
    }
    SecLabel("DATA")
    val uiS by vm.ui.collectAsState()
    AccountCard(vm, d, uiS.confirm)
    OvCard {
        Title("Backup", 20)
        Sub("Your log, plan and notes live on this phone. Save a backup file every week or two." +
            (st.lastBackup?.let { " Last backup ${(System.currentTimeMillis() - it) / 86_400_000} days ago." } ?: ""))
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("💾 Save backup file", { save.launch("overload-backup-${LocalDate.now()}.json") }, bg = ov.ink, fg = ov.bg)
            OvButton("📂 Restore from file", { pick.launch(arrayOf("application/json", "text/plain", "*/*")) })
        }
        Sub("Restore also accepts a backup file from the web version.", Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(14.dp))
        OvButton(if (confirmWipe) "Tap again to erase everything" else "Erase all data", { if (confirmWipe) { vm.wipe(); confirmWipe = false } else confirmWipe = true }, fg = Red)
    }
    Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sub("Overload $VERSION · "); OvButton("What's new", { vm.showNews() }, small = true)
        }
        Sub("© 2026 Jake Scarpa. All rights reserved.", Modifier.padding(top = 6.dp))
    }
}

@Composable private fun SecLabel(t: String) = OvText(t, 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))

@Composable
private fun TogRow(label: String, sub: String?, on: Boolean, onChange: (Boolean) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { OvText(label, 16, FontWeight.SemiBold); if (sub != null) Sub(sub) }
        Toggle(on, onChange)
    }

// ============================ ONBOARDING ============================
@Composable
fun OnboardScreen(vm: OverloadViewModel) {
    var name by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFF3D4A), Color(0xFFFF7A3D), Color(0xFFFFBE0B)))).padding(horizontal = 20.dp, vertical = 24.dp)) {
            Column {
                OvText("Welcome to", 16, FontWeight.Bold, Color.White.copy(.9f))
                Title("Overload", 56, Color.White)
                Spacer(Modifier.height(8.dp))
                OvText("Log every set, beat last week, watch the numbers climb. Pick a plan to start. Every day and exercise can be changed later.", 16, FontWeight.Medium, Color.White)
                Spacer(Modifier.height(14.dp))
                TextBox(name, { name = it }, "Your name (optional)", Modifier.widthIn(max = 260.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        CardButton("☁️", "Already have an account?", "Sign in to bring back all your workouts") { vm.openSheet(Sheet.SignIn) }
        TEMPLATES.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(22.dp)).background(ov.card).clickable { vm.startPlan(t.key, name) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Plate(t.color, "done", Modifier.size(52.dp)); Spacer(Modifier.width(14.dp))
                Column { Title(t.label, 20); Sub(t.desc) }
            }
        }
    }
}
