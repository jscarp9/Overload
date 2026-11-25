package com.overload.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val vm: OverloadViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) }
    }
    override fun onStart() { super.onStart(); vm.onForeground() }
    override fun onStop() { super.onStop(); if (!isChangingConfigurations) vm.onBackground() }
}

@Composable
fun App(vm: OverloadViewModel) {
    val d by vm.data.collectAsState()
    val u by vm.ui.collectAsState()
    val now by vm.now.collectAsState()
    val snack = remember { SnackbarHostState() }
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { vm.toasts.collect { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(it, duration = SnackbarDuration.Short) } }
    LaunchedEffect(u.tab, u.settings, u.prog) { scroll.scrollTo(0) }

    val view = LocalView.current
    val s = vm.session(d, u)
    SideEffect { view.keepScreenOn = d.settings.awake && s.start != null && s.end == null && s.pauseAt == null }

    OverloadTheme(d.settings.theme) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snack, Modifier.statusBarsPadding()) },
            bottomBar = {
                if (d.plan != null) Column(Modifier.navigationBarsPadding().padding(horizontal = 10.dp, vertical = 8.dp)) {
                    RestPill(vm, u, now)
                    NavBar(vm, u)
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 720.dp).fillMaxHeight().verticalScroll(scroll).padding(horizontal = 16.dp).padding(top = 14.dp, bottom = 28.dp)) {
                    TopBar(vm, d)
                    when {
                        d.plan == null -> OnboardScreen(vm)
                        u.settings -> SettingsScreen(vm, d)
                        else -> when (u.tab) {
                            "log" -> TodayScreen(vm, d, u, now)
                            "prog" -> ProgressScreen(vm, d, u)
                            "stats" -> StatsScreen(vm, d)
                            "notes" -> NotesScreen(vm, d, u)
                            else -> PlanScreen(vm, d, u)
                        }
                    }
                    if (d.plan != null) Sub("Overload · Kotlin port", Modifier.fillMaxWidth().padding(top = 18.dp), ov.mute)
                }
            }
        }
    }
}

@Composable
private fun TopBar(vm: OverloadViewModel, d: AppData) {
    val hr = java.time.LocalTime.now().hour
    val greet = if (hr < 12) "Good morning" else if (hr < 18) "Good afternoon" else "Good evening"
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Plate("red", "done", Modifier.size(30.dp)); Spacer(Modifier.width(8.dp)); Title("Overload", 26)
            }
            if (d.settings.name.isNotEmpty()) Sub("$greet, ${d.settings.name}", Modifier.padding(top = 2.dp))
        }
        if (d.plan != null) Box(Modifier.size(44.dp).background(ov.card, RoundedCornerShape(14.dp)).clickable { vm.toggleSettings() }, contentAlignment = Alignment.Center) { OvText("⚙", 22) }
    }
}

@Composable
private fun NavBar(vm: OverloadViewModel, u: UiState) {
    val items = listOf("log" to ("🏋️" to "Today"), "prog" to ("📈" to "Progress"), "stats" to ("📊" to "Stats"), "notes" to ("📝" to "Notes"), "plan" to ("☰" to "Plan"))
    Row(Modifier.fillMaxWidth().background(ov.card.copy(alpha = .92f), RoundedCornerShape(24.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (k, v) ->
            val sel = !u.settings && u.tab == k
            Column(Modifier.weight(1f).height(52.dp).background(if (sel) ov.ink else Color.Transparent, RoundedCornerShape(18.dp)).clickable { vm.setTab(k) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                OvText(v.first, 18); OvText(v.second, 11, FontWeight.Bold, if (sel) ov.bg else ov.mute)
            }
        }
    }
}

@Composable
private fun RestPill(vm: OverloadViewModel, u: UiState, now: Long) {
    val r = u.rest ?: return
    val left = r.endAt - now
    val done = left <= 0
    val label = if (done) "Time's up" else "Rest"
    val sub = if (done) r.next.ifEmpty { "Next set" } else if (r.next.isNotEmpty()) "Next: ${r.next}" else "Breathe"
    val secs = ((left + 999) / 1000).coerceAtLeast(0)
    val frac = if (done) 0f else (left / (r.dur * 1000f)).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(if (done) Color(0xFF16B65B) else Color(0xFF14161C), RoundedCornerShape(22.dp)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(44.dp)) {
            val sw = 5.dp.toPx(); val inset = sw / 2
            drawCircle(Color.White.copy(.16f), size.minDimension / 2 - inset, style = Stroke(sw))
            drawArc(Color.White, -90f, 360f * (1 - frac), false, Offset(inset, inset), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
        }
        Spacer(Modifier.width(10.dp))
        Title(if (done) "Go" else "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}", 28, Color.White, Modifier.widthIn(min = 70.dp))
        Column(Modifier.weight(1f)) {
            OvText(label, 14, FontWeight.Bold, Color.White)
            OvText(sub, 12, color = Color.White.copy(.7f))
        }
        RestBtn("−15") { vm.restMinus() }; RestBtn("+15") { vm.restPlus() }; RestBtn("↺") { vm.restReset() }; RestBtn("✕") { vm.restStop() }
    }
}

@Composable
private fun RestBtn(t: String, onClick: () -> Unit) =
    Box(Modifier.padding(start = 4.dp).height(38.dp).widthIn(min = 38.dp).background(Color.White.copy(.12f), RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
        OvText(t, 14, FontWeight.ExtraBold, Color.White)
    }
