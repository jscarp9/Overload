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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer

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
    LaunchedEffect(u.tab, u.settings, u.prog, u.profile, u.sub) { scroll.scrollTo(0) }
    val c by vm.engCtx.collectAsState()

    val view = LocalView.current
    val s = vm.session(d, u)
    SideEffect { view.keepScreenOn = d.settings.awake && s.start != null && s.end == null && s.pauseAt == null }

    // 0.14 app theme accent, 0.17 background art per screen
    SideEffect { accentState.value = accentOf(c.look("accent")) }
    val art = when { d.plan == null -> "log"; u.settings -> "set"; u.profile || u.tab == "me" -> "stats"; u.tab == "crew" -> if (u.sub["crew"] == "arena") "stats" else "prog"
        u.tab == "stats" && u.sub["stats"] == "lifts" -> "prog"; u.tab == "plan" && u.sub["plan"] == "notes" -> "notes"; else -> u.tab }
    OverloadTheme(d.settings.theme, art, c.look("bg")) {
        if (u.news) WhatsNewDialog { vm.closeNews() }
        u.sheet?.let { sh -> BottomSheet({ vm.closeSheet() }) { SheetBody(vm, c, u, sh) } }
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
                    TopBar(vm, d, c, u)
                    when {
                        d.plan == null -> OnboardScreen(vm)
                        u.settings -> SettingsScreen(vm, d)
                        u.profile -> ProfileTab(vm, c)
                        else -> when (u.tab) {
                            "me" -> ProfileTab(vm, c)
                            "log" -> TodayScreen(vm, d, u, now)
                            "crew" -> CrewScreen(vm, c, u)
                            "stats" -> {
                                SegTabs(listOf("ov" to "Overview", "lifts" to "Lifts"), u.sub["stats"] ?: "ov") { vm.setSub("stats", it) }
                                if (u.sub["stats"] == "lifts") ProgressScreen(vm, d, u) else { GoalsCard(vm, c); ChainsCard(c); StatsScreen(vm, d) }
                            }
                            else -> {
                                SegTabs(listOf("plan" to "Plan", "notes" to "Notes"), u.sub["plan"] ?: "plan") { vm.setSub("plan", it) }
                                if (u.sub["plan"] == "notes") NotesScreen(vm, d, u) else PlanScreen(vm, d, u)
                            }
                        }
                    }
                    if (d.plan != null && !u.settings) Sub("Overload $VERSION", Modifier.fillMaxWidth().padding(top = 18.dp), ov.mute)
                }
            }
        }
        Confetti(u.confetti, u.confettiN, u.confettiKind)
        SetFxBurst(u.setFx, u.setFxKind)
        HypeBurst(u.hype.first, u.hype.second)
        u.cine?.let { Cinematic(vm, it) }
        if (u.yir) YearInReview(vm, c)
    }
}

@Composable
private fun TopBar(vm: OverloadViewModel, d: AppData, c: Ctx, u: UiState) {
    val sync by vm.sync.status.collectAsState()
    val hr = java.time.LocalTime.now().hour
    val greet = if (hr < 12) "Good morning" else if (hr < 18) "Good afternoon" else "Good evening"
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Plate("red", "done", Modifier.size(30.dp)); Spacer(Modifier.width(8.dp)); Title("Overload", 26)
                if (d.settings.acct != null) Box(Modifier.padding(start = 8.dp).size(9.dp).background(when (sync.state) { "pending" -> Color(0xFFFFBE0B); "err", "held" -> Red; else -> Green }, RoundedCornerShape(5.dp)))
            }
            if (d.settings.name.isNotEmpty()) Sub("$greet, ${d.settings.name}", Modifier.padding(top = 2.dp))
        }
        if (d.plan != null) {
            // 0.17: the backup reminder is a red dot on the gear
            val bk = backupDue(d) && d.settings.acct == null && !u.settings
            Box(Modifier.size(44.dp).background(ov.card, RoundedCornerShape(14.dp)).clickable { vm.toggleSettings() }, contentAlignment = Alignment.Center) {
                OvText("⚙", 22); if (bk) Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(9.dp).background(Red, RoundedCornerShape(5.dp)))
            }
        }
    }
}

@Composable
private fun NavBar(vm: OverloadViewModel, u: UiState) {
    val items = listOf("log" to ("🏋️" to "Today"), "crew" to ("🏟️" to "Crew"), "me" to ("🪪" to "Profile"), "stats" to ("📊" to "Stats"), "plan" to ("☰" to "Plan"))
    val dw = java.time.LocalDate.now().dayOfWeek.value
    val dot = vm.crew.on && (vm.crew.bellUnread() > 0 || vm.chat.unread() > 0 || (vm.crewState.value != null && dw in 1..3 && vm.data.value.settings.recapSeen != vm.crew.curWk()))
    Row(Modifier.fillMaxWidth().background(ov.card.copy(alpha = .92f), RoundedCornerShape(24.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (k, v) ->
            val sel = !u.settings && (u.tab == k || (k == "me" && u.profile))
            Column(Modifier.weight(1f).height(52.dp).background(if (sel) ov.ink else Color.Transparent, RoundedCornerShape(18.dp)).clickable { vm.setTab(k) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box { OvText(v.first, 18); if (k == "crew" && dot) Box(Modifier.align(Alignment.TopEnd).offset(x = 4.dp).size(8.dp).background(Red, RoundedCornerShape(4.dp))) }
                OvText(v.second, 11, FontWeight.Bold, if (sel) ov.bg else ov.mute)
            }
        }
    }
}

@Composable
private fun RestPill(vm: OverloadViewModel, u: UiState, now: Long) {
    val r = u.rest ?: return
    val skin = vm.engCtx.collectAsState().value.look("rest")
    val left = r.endAt - now
    val done = left <= 0
    val label = if (done) "Time's up" else "Rest"
    val sub = if (done) r.next.ifEmpty { "Next set" } else if (u.restLine.isNotEmpty() && (now / 6000) % 2 == 1L) u.restLine else if (r.next.isNotEmpty()) "Next: ${r.next}" else "Breathe"
    val secs = ((left + 999) / 1000).coerceAtLeast(0)
    val frac = if (done) 0f else (left / (r.dur * 1000f)).coerceIn(0f, 1f)
    val neon = skin == "neon"
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(if (done) Color(0xFF16B65B) else if (neon) Color(0xFF0D0F14) else Color(0xFF14161C), RoundedCornerShape(20.dp))
        .then(if (neon && !done) Modifier.border(1.5.dp, Color(0xFFFF2BD6), RoundedCornerShape(20.dp)) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (skin) {
                "bar" -> Spacer(Modifier.width(2.dp))
                "plate" -> { val rot by rememberInfiniteTransition(label = "rp").animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "r")
                    Box(Modifier.size(36.dp).graphicsLayer { rotationZ = if (done) 0f else rot }) { Plate("black", "idle", Modifier.fillMaxSize(), ring = 1 - frac) } }
                else -> Canvas(Modifier.size(36.dp)) {
                    val sw = 4.5.dp.toPx(); val inset = sw / 2
                    drawCircle((if (neon) Color(0xFF00E5FF) else Color.White).copy(.16f), size.minDimension / 2 - inset, style = Stroke(sw))
                    drawArc(if (neon) Color(0xFF00E5FF) else Color.White, -90f, 360f * (1 - frac), false, Offset(inset, inset), Size(size.width - sw, size.height - sw), style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            Spacer(Modifier.width(10.dp))
            Title(if (done) "Go" else "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}", 24, if (neon && !done) Color(0xFFFF7AE6) else Color.White, Modifier.widthIn(min = 58.dp))
            Text("$label · $sub", Modifier.weight(1f).padding(start = 4.dp), color = Color.White.copy(.8f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            RestBtn("−15") { vm.restMinus() }; RestBtn("+15") { vm.restPlus() }; RestBtn("↺") { vm.restReset() }; RestBtn("✕") { vm.restStop() }
        }
        if (skin == "bar" && !done) Box(Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp).fillMaxWidth().height(4.dp).background(Color.White.copy(.15f), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(1 - frac).fillMaxHeight().background(Color.White, RoundedCornerShape(2.dp))) }
    }
}

/** 3+ workouts and no backup file for a week (0.17: shows as a dot on the gear and a row in Settings). */
fun backupDue(d: AppData): Boolean {
    val n = d.sessions.values.count { s -> s.ex.values.any { a -> a.any(::full) } }
    val lb = d.settings.lastBackup
    return n >= 3 && (lb == null || System.currentTimeMillis() - lb > 7 * 86_400_000L)
}

@Composable
private fun RestBtn(t: String, onClick: () -> Unit) =
    Box(Modifier.padding(start = 3.dp).height(36.dp).widthIn(min = 34.dp).background(Color.White.copy(.12f), RoundedCornerShape(11.dp)).clickable(onClick = onClick).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
        OvText(t, 13, FontWeight.ExtraBold, Color.White)
    }

/** The body of whichever bottom sheet is open. */
@Composable
private fun ColumnScope.SheetBody(vm: OverloadViewModel, c: Ctx, u: UiState, sh: Sheet) {
    when (sh) {
        is Sheet.Badges -> BadgesSheet(vm, sh.items, c.eng.rank >= 2)
        is Sheet.ReportCard -> ReportSheet(vm, sh.r, c.unit)
        is Sheet.BadgeInfo -> BadgeInfoSheet(vm, c, sh.k)
        is Sheet.Ladder -> LadderSheet(vm, c, sh.cat)
        Sheet.Look -> LookSheet(vm, c)
        Sheet.Pass -> PassSheet(vm, c.eng)
        Sheet.NewGoal -> NewGoalSheet(vm, c)
        Sheet.Bell -> BellSheet(vm)
        is Sheet.PostWorkout -> PostSheet(vm, c, sh.key)
        is Sheet.Comments -> CommentsSheet(vm, sh.post)
        Sheet.CrewSettings -> CrewSettingsSheet(vm, c, u)
        Sheet.SetChallenge -> ChallengeSheet(vm, c)
        Sheet.Awards -> AwardsSheet(vm)
        is Sheet.CopyPlan -> CopyPlanSheet(vm, c, sh.member, sh.day)
        Sheet.SignIn -> SignInSheet(vm)
        is Sheet.RecoveryCode -> RecoveryCodeSheet(vm, sh.code)
        is Sheet.SyncGuard -> SyncGuardSheet(vm, sh.n)
        Sheet.Trash -> TrashSheet(vm)
        is Sheet.Locker -> LockerSheet(vm, c, sh.sec)
        is Sheet.BoxOpen -> BoxSheet(vm, sh.got, sh.opening)
        is Sheet.TrophyRoom -> TrophyRoomSheet(vm, c, sh.of)
        is Sheet.TrophyInfo -> TrophyInfoSheet(vm, c, sh.k, sh.earned)
        Sheet.PinPick -> PinPickSheet(vm, c)
        is Sheet.Excuse -> ExcuseSheet(vm, sh.i)
        Sheet.ChatPlus -> ChatPlusSheet(vm)
        is Sheet.Msg -> MsgSheet(vm, c, u, sh.id)
        is Sheet.CrestEdit -> CrestSheet(vm, sh.crest)
        Sheet.CrewRankInfo -> CrewRankSheet(vm)
        Sheet.CrewSnaps -> CrewSnapsSheet(vm, u)
        is Sheet.PlatePick -> PlatePickSheet(vm, c, sh.day)
        is Sheet.ProfilePage -> ProfilePageSheet(vm, c, sh.id)
        is Sheet.ProfEdit -> ProfEditSheet(vm, c, sh.sec)
        Sheet.RaidInfo -> RaidSheet(vm)
        Sheet.GroupStart -> GroupSheet(vm)
        is Sheet.GroupCard -> { SheetHead("Lifted together") { vm.closeSheet() }; OvCard { GroupPostBody(vm, sh.post) }; Sub("Posted to your crew feed.") }
        is Sheet.HofPost -> HofPostSheet(vm, c, sh.post)
        Sheet.Founder -> FounderSheet(vm)
        Sheet.Dash -> DashSheet(vm)
        is Sheet.Badges16 -> Badges16Sheet(vm, c, sh.cat)
        is Sheet.Psec -> PsecSheet(vm, c, sh.k)
        is Sheet.Tour -> TourSheet(vm, sh.i)
        Sheet.TodayMore -> TodayMoreSheet(vm)
    }
}
