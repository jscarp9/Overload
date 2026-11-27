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

@Composable
fun Dropdown(current: String, options: List<String>, modifier: Modifier, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(ov.soft, RoundedCornerShape(14.dp)).clickable { open = true }.padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
            OvText(current, 14, FontWeight.SemiBold)
        }
        DropdownMenu(open, { open = false }) { options.forEach { o -> DropdownMenuItem({ Text(o) }, { onPick(o); open = false }) } }
    }
}

// ============================ SETTINGS (0.17: a short list, one page per section) ============================
@Composable
fun SettingsScreen(vm: OverloadViewModel, d: AppData) {
    val st = d.settings; val ctx = LocalContext.current; val u by vm.ui.collectAsState(); val c by vm.engCtx.collectAsState()
    var confirmWipe by remember { mutableStateOf(false) }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
    }
    val saveAll = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(exportAllJson(vm).toByteArray()) }; vm.setSettings { it.copy(lastBackup = System.currentTimeMillis()) }; vm.toast("Exported everything") }
    }
    val scope = rememberCoroutineScope()
    val hrPerm = rememberLauncherForActivityResult(Health.permissionContract()) { granted -> vm.enableHeartRate(granted.containsAll(Health.PERMS)) }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.enableRestAlerts(it) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) ctx.contentResolver.openInputStream(uri)?.use { vm.importJson(it.readBytes().decodeToString()) }
    }
    val pages = listOf("you" to ("🙂" to "Name, theme and units"), "workout" to ("🏋️" to "Workout"), "health" to ("❤️" to "Heart rate"), "sync" to ("☁️" to "Account and sync"),
        "ai" to ("✦" to "Overload AI"), "data" to ("🛟" to "Backup and restore"), "founder" to ("👑" to "Founder"), "help" to ("👋" to "Help"), "danger" to ("⚠️" to "Start over")).filter { it.first != "founder" || st.founder }
    val page = u.set17
    if (page == null) {
        Row(verticalAlignment = Alignment.CenterVertically) { Title("Settings", modifier = Modifier.weight(1f)); OvButton("Done", { vm.toggleSettings() }, small = true, bg = ov.ink, fg = ov.bg) }
        Spacer(Modifier.height(12.dp))
        if (backupDue(d) && st.acct == null) CardButton("💾", "Back up your log", (st.lastBackup?.let { "Last backup ${(System.currentTimeMillis() - it) / 86_400_000} days ago" } ?: "You haven’t backed up yet") + ". Takes 2 seconds") { vm.ui.value = u.copy(set17 = "data") }
        OvCard { pages.forEach { (k, v) -> Row16(v.first, v.second) { vm.ui.value = vm.ui.value.copy(set17 = k) } } }
        Column(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) { Sub("Overload $VERSION · "); OvButton("What's new", { vm.showNews() }, small = true) }
            Sub("© 2026 Jake Scarpa. All rights reserved.", Modifier.padding(top = 6.dp))
        }
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OvButton("‹ Settings", { vm.ui.value = vm.ui.value.copy(set17 = null) }, small = true)
        Title(pages.firstOrNull { it.first == page }?.second?.second ?: "", 22, modifier = Modifier.weight(1f).padding(start = 10.dp))
    }
    Spacer(Modifier.height(12.dp))
    when (page) {
        "you" -> OvCard {
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
            Sub("App theme colours, backgrounds and sounds are in Profile → Customize.", Modifier.padding(top = 6.dp))
        }
        "workout" -> OvCard {
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
        "health" -> OvCard {
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
        "sync" -> AccountCard(vm, d, u.confirm)
        "ai" -> AiSettingsPage(vm, c)
        "data" -> {
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
            }
            OvCard {
                Title("Safety net", 20)
                Sub("Deleted things wait in the Trash for 30 days. Before every update the app saves a full copy and puts back anything that goes missing.", Modifier.padding(vertical = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OvButton("🗑 Trash", { vm.openSheet(Sheet.Trash) }); OvButton("📦 Export everything", { saveAll.launch("overload-everything-${LocalDate.now()}.json") })
                }
            }
        }
        "founder" -> OvCard { Title("👑 Founder", 20); OvButton("📊 Owner dashboard", { vm.openSheet(Sheet.Dash) }, Modifier.padding(top = 8.dp), wide = true) }
        "help" -> OvCard {
            Row16("👋", "Take the app tour", "How Overload works") { vm.openSheet(Sheet.Tour(0)) }
            Row16("✨", "What's new", "Overload $APP_VER") { vm.showNews() }
            Row16("🔒", "Privacy", "What’s saved and shared") { vm.openSheet(Sheet.Privacy) }
            if (!st.founder && !st.cofounder) Row16("🔑", "Founder code", "For the people who built Overload") { vm.openSheet(Sheet.Founder) }
        }
        "danger" -> OvCard {
            Title("Start over", 20); Sub("Erases every workout, note and your plan from this phone.", Modifier.padding(vertical = 6.dp))
            OvButton(if (confirmWipe) "Tap again to erase everything" else "Erase all data", { if (confirmWipe) { vm.wipe(); confirmWipe = false } else confirmWipe = true }, fg = Red)
        }
    }
}

@Composable private fun SecLabel(t: String) = OvText(t, 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp))

@Composable
fun TogRowPub(label: String, sub: String?, on: Boolean, onChange: (Boolean) -> Unit) = TogRow(label, sub, on, onChange)

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
