package com.overload.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/* 0.23-0.25 Overload AI screens: gold and space. */

val AiGold = Color(0xFFE8B923)
private val AiInk = Color(0xFF0B0C12)
private val GoldText = Brush.linearGradient(listOf(Color(0xFFFFF1B8), Color(0xFFE8B923), Color(0xFF8A6212)))

/** The gold ✦ orb. */
@Composable
fun AiOrb(size: Dp = 34.dp, dim: Boolean = false) {
    val tr = rememberInfiniteTransition(label = "orb")
    val g by tr.animateFloat(.55f, 1f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "og")
    Box(Modifier.size(size).alpha(if (dim) .45f else 1f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Brush.radialGradient(listOf(AiGold.copy(.55f * g), Color.Transparent), center, this.size.minDimension / 2), this.size.minDimension / 2)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF4C9), AiGold, Color(0xFF8A6209)), center - Offset(this.size.width * .12f, this.size.height * .12f), this.size.minDimension * .45f), this.size.minDimension * .34f)
        }
        OvText("✦", (size.value * .38f).toInt(), FontWeight.Black, Color(0xFF2E1F02))
    }
}

/** Starfield background for the AI sheets. */
@Composable
private fun Stars(seed: String, n: Int = 70, modifier: Modifier = Modifier) = Canvas(modifier) {
    repeat(n) { i -> drawCircle(if (rnd("$seed c$i") < .3f) Color(0xFFF7D97C) else Color.White, rnd("$seed r$i", .4f, 1.6f) * density * .6f,
        Offset(rnd("$seed x$i") * size.width, rnd("$seed y$i") * size.height), alpha = rnd("$seed a$i", .25f, .85f)) }
}

/** The dark gold "space" panel every AI sheet sits in. */
@Composable
fun AiPanel(seed: String, title: String, sub: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) =
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.verticalGradient(listOf(Color(0xFF0B0A14), Color(0xFF161021), AiInk)))) {
        Stars(seed, modifier = Modifier.matchParentSize())
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AiOrb(40.dp); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { GoldTitle(title, 24); OvText(sub, 13, color = Color.White.copy(.7f)) }
                Box(Modifier.size(34.dp).background(Color.White.copy(.1f), CircleShape).clickable(onClick = onClose), contentAlignment = Alignment.Center) { OvText("✕", 14, FontWeight.Bold, Color.White) }
            }
            Spacer(Modifier.height(12.dp)); content()
        }
    }
@Composable fun GoldTitle(t: String, size: Int) = androidx.compose.material3.Text(t, style = androidx.compose.ui.text.TextStyle(brush = GoldText, fontSize = androidx.compose.ui.unit.TextUnit(size.toFloat(), androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = FontWeight.Black))
@Composable private fun GLbl(t: String, extra: String = "") = Row(Modifier.padding(top = 12.dp, bottom = 6.dp)) { OvText(t, 12, FontWeight.ExtraBold, AiGold); if (extra.isNotEmpty()) OvText("  $extra", 12, color = Color.White.copy(.5f)) }
@Composable private fun Cta(t: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Box(modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp).clip(RoundedCornerShape(16.dp)).background(if (enabled) Brush.horizontalGradient(listOf(Color(0xFFB8860B), Color(0xFFF7D97C), Color(0xFFE8B923))) else Brush.horizontalGradient(listOf(Color(0xFF3A3420), Color(0xFF3A3420))))
        .clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) { OvText(t, 16, FontWeight.Black, if (enabled) Color(0xFF2E1F02) else Color.White.copy(.4f)) }
@Composable private fun Ghost(t: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) =
    Box(modifier.padding(top = 8.dp).height(44.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, AiGold.copy(.4f), RoundedCornerShape(14.dp)).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
        OvText(t, 14, FontWeight.Bold, if (enabled) AiGold else Color.White.copy(.3f)) }
@Composable private fun GSeg(opts: List<Pair<String, String>>, cur: String, onPick: (String) -> Unit) =
    Row(Modifier.fillMaxWidth().background(Color.White.copy(.07f), RoundedCornerShape(12.dp)).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        opts.forEach { (v, l) -> Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (cur == v) AiGold else Color.Transparent).clickable { onPick(v) }, contentAlignment = Alignment.Center) {
            OvText(l, 13, FontWeight.Bold, if (cur == v) Color(0xFF2E1F02) else Color.White.copy(.8f)) } } }
@Composable private fun Warn(t: String) = OvText(t, 13, FontWeight.SemiBold, Color(0xFFFFD27A), Modifier.padding(top = 8.dp).fillMaxWidth().background(Color(0xFFFFB020).copy(.12f), RoundedCornerShape(12.dp)).padding(10.dp))
@Composable private fun Err(t: String) = OvText(t, 13, FontWeight.SemiBold, Color(0xFFFF8A8A), Modifier.padding(top = 8.dp).fillMaxWidth().background(Color(0xFFEF2B36).copy(.15f), RoundedCornerShape(12.dp)).padding(10.dp))
@Composable private fun Cons(vararg p: String) = Column(Modifier.fillMaxWidth().background(Color.White.copy(.06f), RoundedCornerShape(16.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    p.forEach { OvText(it, 14, color = Color.White.copy(.9f)) } }

private fun signIn(vm: OverloadViewModel) { vm.closeSheet(); vm.ui.value = vm.ui.value.copy(settings = true, set17 = "sync", profile = false) }

// ============================ PLAN BUILDER (0.23) ============================
@Composable
fun AiBuildSheet(vm: OverloadViewModel, c: Ctx) {
    val b by vm.ai.build.collectAsState(); val st by vm.ai.store.collectAsState(); val left = st.left ?: 3
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { vm.ai.addPhotos(it) }
    val close = { vm.closeSheet() }
    when {
        b.stage == "busy" -> AiPanel("busy", "Forging your week", "About 10 to 20 seconds", close) {
            val spin by rememberInfiniteTransition(label = "fg").animateFloat(0f, 360f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "fs")
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(150.dp)) { drawCircle(Color.White.copy(.08f), size.minDimension / 2 - 4, style = Stroke(6f))
                    drawArc(Brush.sweepGradient(listOf(Color(0xFF8A6212), Color(0xFFFFF1B8))), -90f, 360f * minOf(.92f, .18f + b.step * .24f), false, Offset(4f, 4f), androidx.compose.ui.geometry.Size(size.width - 8, size.height - 8), style = Stroke(7f, cap = androidx.compose.ui.graphics.StrokeCap.Round)) }
                Box(Modifier.size(90.dp).graphicsLayer { rotationZ = spin }) { Plate("gold", "idle", Modifier.fillMaxSize()) }
            }
            val src = when (b.src) { "hist" -> "Reading your history"; "text" -> "Reading your list"; else -> "Reading your ${if (b.imgs.size > 1) "${b.imgs.size} photos" else "photo"}" }
            listOf(src, "Matching your lifts", "Balancing muscles across the week", "Setting sets, reps and rest").forEachIndexed { i, t ->
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).background(if (i < b.step) AiGold else Color.White.copy(if (i == b.step) .25f else .08f), CircleShape), contentAlignment = Alignment.Center) { if (i < b.step) OvText("✓", 11, FontWeight.Black, Color(0xFF2E1F02)) }
                    OvText("  $t", 14, if (i == b.step) FontWeight.Bold else FontWeight.Normal, Color.White.copy(if (i <= b.step) .95f else .45f)) } }
        }
        b.stage == "preview" && b.res != null -> { val p = b.res!!; val days = vm.ai.toPlan(p); val nEx = days.sumOf { d -> d.groups.sumOf { it.size } }
            AiPanel("prev", "Your week is ready", "${days.count { it.type != "off" }} training days · $nEx exercises", close) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { days.forEachIndexed { i, d -> Column(Modifier.weight(1f).alpha(if (d.type == "off") .45f else 1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Plate(colorOf(d), if (d.type == "off") "rest" else "done", Modifier.fillMaxWidth()); OvText(DN[i].uppercase(), 10, FontWeight.Bold, Color.White.copy(.7f)) } } }
                if (p.summary.isNotEmpty()) OvText("✦ ${p.summary}", 14, FontWeight.SemiBold, Color(0xFFFFE7A0), Modifier.padding(top = 10.dp).fillMaxWidth().background(AiGold.copy(.1f), RoundedCornerShape(12.dp)).padding(10.dp))
                p.warnings.forEach { Warn("⚠️ $it") }
                days.forEachIndexed { i, d -> Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(Color.White.copy(.06f), RoundedCornerShape(14.dp)).padding(10.dp)) {
                    if (d.type == "off") OvText("${DN[i]} · Rest", 14, FontWeight.Bold, Color.White.copy(.6f))
                    else { Row { OvText(d.name, 15, FontWeight.Bold, Color.White, Modifier.weight(1f)); OvText("${DN[i].uppercase()} · ${d.groups.sumOf { it.size }} lifts", 11, FontWeight.Bold, pc(colorOf(d)).light) }
                        if (d.focus.isNotEmpty()) OvText(d.focus, 12, color = Color.White.copy(.6f))
                        FlowRowC(Modifier.padding(top = 6.dp)) { d.groups.forEachIndexed { gi, g -> val e0 = g[0]; val rr = "${e0.sets}×${e0.lo}${if (e0.hi > e0.lo) "–${e0.hi}" else ""}"
                            val (txt, col) = if (g.size > 1) (g.mapIndexed { ei, e -> "${'A' + gi}${ei + 1} ${e.name}" }.joinToString(" / ") + " · $rr") to AiGold
                                else ((if (e0.ai == "new") "+ " else "") + "${e0.name} $rr" + (e0.start?.let { " · ${fmtN(it.w)}" } ?: "")) to (if (e0.ai == "new") Color(0xFF5FF0A0) else Color.White.copy(.85f))
                            OvText(txt, 12, FontWeight.SemiBold, col, Modifier.background(col.copy(.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) } } } } }
                Cta("Use this plan") { vm.ai.use() }
                Ghost("↻ Try again", left > 0, Modifier.fillMaxWidth()) { vm.ai.setBuild { it.copy(stage = "form") }; vm.ai.go() }
                OvText("Your current plan goes to Trash first · $left build${if (left == 1) "" else "s"} left today", 12, color = Color.White.copy(.6f), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), align = TextAlign.Center)
            } }
        b.stage == "consent" -> AiPanel("cons", "Before we start", "One time only", close) {
            Cons("Overload AI uses Anthropic’s Claude to build plans and write short coach notes.", "Plan builder: the photos or text you add, your lift names and recent best sets.",
                "Coach notes: the sets, weights and reps from a finished workout or week.", "Your name, crew, chats and account are never sent. You can turn AI off any time in Settings → Overload AI.")
            Cta("Allow and continue") { vm.ai.allow(); if (b.after == "go") { vm.ai.setBuild { it.copy(stage = "form") }; vm.ai.go() } else vm.closeSheet() }
            Ghost("Not now", modifier = Modifier.fillMaxWidth(), onClick = close)
        }
        c.st.acct == null -> AiPanel("in", "Overload AI", "Turn anything into your perfect week", close) {
            Cons("Sign in first so your builds are saved and limits stay fair. It also backs up your log."); Cta("Sign in") { signIn(vm) } }
        else -> AiPanel("form", "Overload AI", "Turn anything into your perfect week", close) {
            GLbl("START FROM")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("photo" to "📷 Photo", "text" to "✍️ Type it", "hist" to "🕘 My history").forEach { (k, l) ->
                    Box(Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(14.dp)).background(if (b.src == k) AiGold.copy(.18f) else Color.White.copy(.06f))
                        .border(1.5.dp, if (b.src == k) AiGold else Color.Transparent, RoundedCornerShape(14.dp)).clickable { vm.ai.setBuild { it.copy(src = k, err = "") } }, contentAlignment = Alignment.Center) {
                        OvText(l, 13, FontWeight.Bold, Color.White, align = TextAlign.Center) } } }
            Spacer(Modifier.height(10.dp))
            when (b.src) {
                "photo" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    b.imgs.forEachIndexed { i, m -> Box(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(12.dp))) {
                        Image(m.bmp, "Photo ${i + 1}", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp).size(24.dp).background(Color.Black.copy(.6f), CircleShape).clickable { vm.ai.setBuild { it.copy(imgs = it.imgs.filterIndexed { j, _ -> j != i }) } }, contentAlignment = Alignment.Center) { OvText("✕", 11, FontWeight.Bold, Color.White) } } }
                    if (b.imgs.size < 3) Column(Modifier.weight(1f).height(96.dp).clip(RoundedCornerShape(12.dp)).border(1.5.dp, AiGold.copy(.5f), RoundedCornerShape(12.dp))
                        .clickable { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        OvText("＋", 22, FontWeight.Bold, AiGold); OvText(if (b.imgs.isEmpty()) "Add a photo" else "Add another", 12, FontWeight.Bold, Color.White)
                        if (b.imgs.isEmpty()) OvText("Notes, a screenshot or a whiteboard", 10, color = Color.White.copy(.6f), align = TextAlign.Center) }
                    repeat(maxOf(0, 2 - b.imgs.size)) { Spacer(Modifier.weight(1f)) } }
                "text" -> TextBox(b.text, { v -> vm.ai.setBuild { it.copy(text = v.take(4000)) } }, "Push day\nIncline press 230x6\nShoulder press 105x5\n…", singleLine = false, minH = 120.dp)
                else -> { val n = remember { vm.ai.history().size }
                    OvText(if (n > 0) "$n lifts from your last 10 weeks, with your best sets." else "No workouts logged yet. Add a photo or type your lifts instead.", 14, color = Color.White.copy(.85f),
                        modifier = Modifier.fillMaxWidth().background(Color.White.copy(.06f), RoundedCornerShape(12.dp)).padding(12.dp)) }
            }
            GLbl("WHAT DO YOU WANT?", "optional"); TextBox(b.ask, { v -> vm.ai.setBuild { it.copy(ask = v.take(300)) } }, "e.g. make it a PPL, or upper lower with an arm day")
            GLbl("DAYS A WEEK"); GSeg(listOf("3" to "3", "4" to "4", "5" to "5", "6" to "6"), "${b.days}") { v -> vm.ai.setBuild { it.copy(days = v.toInt()) } }
            GLbl("GOAL"); GSeg(listOf("strength" to "Strength", "size" to "Size", "both" to "Both"), b.goal) { v -> vm.ai.setBuild { it.copy(goal = v) } }
            GLbl("TIME PER WORKOUT"); GSeg(listOf("45" to "45 min", "60" to "60 min", "75" to "75 min", "90" to "90+"), "${b.mins}") { v -> vm.ai.setBuild { it.copy(mins = v.toInt()) } }
            if (b.err.isNotEmpty()) Err(b.err)
            OvText("🔒 Only ${when (b.src) { "photo" -> "your photos"; "text" -> "your list"; else -> "your lift history" }} and lift names go to Anthropic’s Claude. Nothing else leaves your phone.", 12,
                color = Color.White.copy(.7f), modifier = Modifier.padding(top = 10.dp))
            Cta("✦ Forge my week", left > 0) { vm.ai.go() }
            OvText("$left build${if (left == 1) "" else "s"} left today", 12, color = Color.White.copy(.6f), modifier = Modifier.fillMaxWidth().padding(top = 8.dp), align = TextAlign.Center)
        }
    }
}

// ============================ COACH NOTES (0.23) ============================
/** The holographic gold card for AI notes. text "" = still writing. */
@Composable
fun HoloCard(title: String, small: String, text: String, modifier: Modifier = Modifier) =
    Row(modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1408), Color(0xFF0F0C16), Color(0xFF1C1505))))
        .border(1.dp, Brush.linearGradient(listOf(Color(0xFFFFF1B8), Color(0xFF8A6212), Color(0xFFE8B923))), RoundedCornerShape(22.dp)).padding(14.dp), verticalAlignment = Alignment.Top) {
        AiOrb(34.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) { GoldTitle(title, 13); OvText("  $small", 11, color = Color.White.copy(.55f)) }
            if (text.isEmpty()) { val a by rememberInfiniteTransition(label = "sh").animateFloat(.25f, .6f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "sa")
                Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(10.dp).background(Color.White.copy(a * .3f), RoundedCornerShape(5.dp)))
                Box(Modifier.padding(top = 6.dp).fillMaxWidth(.6f).height(10.dp).background(Color.White.copy(a * .3f), RoundedCornerShape(5.dp))) }
            else OvText(text, 14, color = Color.White.copy(.92f), modifier = Modifier.padding(top = 4.dp))
        }
    }

@Composable
fun AiRecapCard(vm: OverloadViewModel, key: String, s: Session) {
    vm.ai.store.collectAsState().value; vm.ai.tick.collectAsState().value
    val t = vm.ai.recap(key, s) ?: return
    HoloCard("COACH RECAP", if (t.isEmpty()) "writing…" else "AI · once per workout", t)
}
@Composable
fun AiWeeklyCard(vm: OverloadViewModel, r: Report) {
    vm.ai.store.collectAsState().value; vm.ai.tick.collectAsState().value
    val t = vm.ai.weekly(vm.ctx(), r) ?: return
    HoloCard("COACH NOTE", if (t.isEmpty()) "writing…" else "AI · once a week", t, Modifier.padding(top = 12.dp))
}
/** The weekly AI crew recap post in the feed (0.25). */
@Composable
fun AiRecapPost(p: Post) = HoloCard("CREW RECAP", "Overload AI · ${ago(p.ts)}", p.cap.ifEmpty { "…" })

// ============================ COACH CHAT (0.25) ============================
private val CHIPS25 = listOf(Triple("🔄", "Swap an exercise", "Swap an exercise on today’s workout for something else"), Triple("⏱", "I only have 30 min", "I only have 30 minutes today. Shorten today’s workout to fit."),
    Triple("😮‍💨", "Deload week", "I feel beat up. Make my plan a deload week."), Triple("📈", "Why is my bench stuck?", "Why is my bench press stuck and what should I change?"),
    Triple("🗓", "Change my split", "I want to change my split. Ask me what I want."))

@Composable
fun CoachSheet(vm: OverloadViewModel, c: Ctx) {
    val st by vm.ai.store.collectAsState(); val ui by vm.ai.coach.collectAsState(); val left = st.chatLeft ?: 25; val close = { vm.closeSheet() }
    AiPanel("coach", "Coach", "Ask anything, or tell me what to change", close) {
        if (c.st.acct == null) { Cons("Sign in first so Coach knows it’s you and limits stay fair."); Cta("Sign in") { signIn(vm) }; return@AiPanel }
        if (c.st.aiOk == 0) { Cons("Coach uses Anthropic’s Claude. When you send a message it gets your message, your plan and your last few workouts (exercises, weights and reps).",
            "Your name, crew, chats and account are never sent. Nothing in your plan changes unless you tap Apply.")
            Cta("Allow and start") { vm.ai.allow() }; Ghost("Not now", modifier = Modifier.fillMaxWidth(), onClick = close); return@AiPanel }
        if (st.rest) Warn("Overload AI used up this month’s budget. Coach is back on the 1st.")
        val msgs = st.chat.withIndex().toList().takeLast(40)
        if (msgs.isEmpty()) Column {
            OvText("Hey${c.st.name.split(" ").firstOrNull()?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}. I can see your plan and your recent workouts. Try one of these:", 14, color = Color.White.copy(.9f))
            FlowRowC(Modifier.padding(top = 10.dp)) { CHIPS25.forEach { (i, l, q) -> OvText("$i $l", 13, FontWeight.SemiBold, Color.White, Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.08f)).clickable { vm.ai.send(q) }.padding(horizontal = 12.dp, vertical = 8.dp)) }
                OvText("✦ Build a whole new week", 13, FontWeight.Bold, Color(0xFF2E1F02), Modifier.clip(RoundedCornerShape(14.dp)).background(AiGold).clickable { vm.ai.openBuilder() }.padding(horizontal = 12.dp, vertical = 8.dp)) }
        }
        msgs.forEach { (i, m) ->
            if (m.r == "u") Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                OvText(m.t, 14, color = Color(0xFF2E1F02), modifier = Modifier.widthIn(max = 280.dp).background(AiGold, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) }
            else Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                AiOrb(28.dp); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    OvText(m.t, 14, color = Color.White.copy(.92f), modifier = Modifier.background(Color.White.copy(.07f), RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp))
                    m.ch?.let { ch -> Column(Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).border(1.dp, AiGold.copy(.5f), RoundedCornerShape(14.dp)).background(AiGold.copy(.06f)).padding(10.dp)) {
                        OvText("✦ ${ch.summary.ifEmpty { "Plan change" }}", 14, FontWeight.Bold, AiGold)
                        ch.days.forEachIndexed { j, d -> Column(Modifier.padding(top = 6.dp)) { OvText("${DN.getOrElse(d.day) { "?" }} · ${d.name}", 13, FontWeight.Bold, Color.White)
                            FlowRowC(Modifier.padding(top = 3.dp)) { (ch.diff.getOrNull(j) ?: emptyList()).forEach { x -> val col = if (x.startsWith("+")) Color(0xFF5FF0A0) else if (x.startsWith("−")) Color(0xFFFF8A8A) else Color.White.copy(.7f)
                                OvText(x, 12, FontWeight.SemiBold, col, Modifier.background(col.copy(.12f), RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) } } } }
                        when (m.st) { "applied" -> OvText("✓ Applied. Your old plan is in Trash.", 13, FontWeight.Bold, Color(0xFF5FF0A0), Modifier.padding(top = 8.dp))
                            "no" -> OvText("Not applied", 13, FontWeight.Bold, Color.White.copy(.5f), Modifier.padding(top = 8.dp))
                            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Cta("Apply", modifier = Modifier.weight(1f)) { vm.ai.apply(i) }; Ghost("No thanks", modifier = Modifier.padding(top = 12.dp).height(52.dp)) { vm.ai.decline(i) } } }
                    } }
                }
            }
        }
        if (ui.busy) Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { AiOrb(28.dp)
            val a by rememberInfiniteTransition(label = "ty").animateFloat(.2f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "ta")
            OvText("  • • •", 16, FontWeight.Black, Color.White.copy(a)) }
        if (ui.err.isNotEmpty()) Err(ui.err)
        var draft by remember(ui.draft) { mutableStateOf(ui.draft) }
        val off = st.rest || left <= 0
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).alpha(if (off) .5f else 1f)) { TextBox(draft, { if (!off) draft = it.take(500) }, if (st.rest) "Coach is resting until the 1st" else "Message Coach", singleLine = false) }
            Box(Modifier.size(46.dp).clip(CircleShape).background(if (ui.busy || off || draft.isBlank()) Color.White.copy(.15f) else AiGold).clickable(enabled = !ui.busy && !off) { vm.ai.send(draft); draft = "" }, contentAlignment = Alignment.Center) {
                OvText("↑", 20, FontWeight.Black, Color(0xFF2E1F02)) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
            OvText("$left message${if (left == 1) "" else "s"} left today", 12, color = Color.White.copy(.6f))
            if (msgs.isNotEmpty()) OvText(" · Clear chat", 12, FontWeight.Bold, AiGold, Modifier.clickable { vm.ai.clear() }) }
    }
}

// ============================ FORM TIPS (0.25) ============================
@Composable
fun CueSheet(vm: OverloadViewModel, c: Ctx, name: String) {
    val st by vm.ai.store.collectAsState(); val errs by vm.ai.cueErr.collectAsState(); val k = vm.ai.cueKey(name); val t = st.cues[k]
    AiPanel("cue$k", name, "Form tips", { vm.closeSheet() }) {
        when {
            c.st.acct == null -> { Cons("Sign in to get AI form tips."); Cta("Sign in") { signIn(vm) } }
            c.st.aiOk == 0 -> { Cons("Form tips are written by Anthropic’s Claude. Only the exercise name is sent."); Cta("Allow") { vm.ai.allow(); vm.later(100) { vm.ai.fetchCue(name) } } }
            t != null -> { t.cues.forEachIndexed { i, x -> Row(Modifier.padding(vertical = 4.dp)) { OvText("${i + 1}.", 14, FontWeight.Black, AiGold, Modifier.width(24.dp)); OvText(x, 14, color = Color.White.copy(.92f)) } }
                if (t.feel.isNotEmpty()) Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(Color(0xFF16B65B).copy(.12f), RoundedCornerShape(12.dp)).padding(10.dp)) { OvText("Feel it in", 12, FontWeight.ExtraBold, Color(0xFF5FF0A0)); OvText(t.feel, 14, color = Color.White) }
                if (t.mistake.isNotEmpty()) Column(Modifier.padding(top = 8.dp).fillMaxWidth().background(Color(0xFFEF2B36).copy(.12f), RoundedCornerShape(12.dp)).padding(10.dp)) { OvText("Avoid", 12, FontWeight.ExtraBold, Color(0xFFFF8A8A)); OvText(t.mistake, 14, color = Color.White) }
                OvText("AI tips, not a substitute for a coach watching you. Stop if anything hurts.", 11, color = Color.White.copy(.55f), modifier = Modifier.padding(top = 12.dp)) }
            errs[k] != null -> Err(errs[k]!!)
            else -> HoloCard("WRITING TIPS", "", "")
        }
    }
}

// ============================ ENTRY POINTS ============================
/** 0.25.1: Coach lives in the top bar. Hidden when AI is off, dimmed when the monthly budget is used up. */
@Composable
fun CoachOrb(vm: OverloadViewModel) {
    val st by vm.ai.store.collectAsState(); if (!st.on) return
    Row(Modifier.padding(end = 8.dp).height(44.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF14110A)).border(1.dp, AiGold.copy(.5f), RoundedCornerShape(22.dp))
        .clickable { vm.haptic("tap"); vm.ai.openCoach() }.padding(start = 4.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        AiOrb(34.dp, dim = st.rest); OvText("COACH", 11, FontWeight.Black, AiGold.copy(if (st.rest) .5f else 1f)) }
}
/** The gold "Build my plan with AI" bar on the Plan page (first time, or an empty plan). */
@Composable
fun AiPlanBar(vm: OverloadViewModel) =
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFF1A1408), Color(0xFF2A2008))))
        .border(1.dp, AiGold.copy(.6f), RoundedCornerShape(20.dp)).clickable { vm.ai.openBuilder() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AiOrb(38.dp); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { GoldTitle("Build my plan with AI", 15); OvText("From a photo of your notes, a list, or your history", 12, color = Color.White.copy(.7f)) }
        OvText("›", 22, FontWeight.Bold, AiGold)
    }

/** Settings → Overload AI. */
@Composable
fun AiSettingsPage(vm: OverloadViewModel, c: Ctx) {
    val st by vm.ai.store.collectAsState(); val on = c.st.aiOk > 0 && c.st.aiCoach != false
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { AiOrb(40.dp); Spacer(Modifier.width(10.dp))
            Column { OvText(if (st.on) "Overload AI is on" else "Overload AI is off right now", 16, FontWeight.Bold)
                Sub("Coach chat, plan builder, form tips, workout recaps and a weekly crew post. Powered by Anthropic’s Claude.") } }
        TogRowPub("Coach notes", "A short recap after each workout and a note in your Monday report.", on) { v ->
            vm.setSettings { if (v) it.copy(aiOk = 1, aiCoach = true) else it.copy(aiCoach = false) }; if (!v) vm.toast("Coach notes off") }
        Sub(if (c.st.aiOk > 0) "You allowed AI on this phone. Turning coach notes off stops all automatic AI. The plan builder always asks you first." else "You haven’t allowed AI yet. The plan builder will ask the first time you use it.", Modifier.padding(top = 6.dp))
        Sub("Sent only when you use it: your photos or typed lifts, lift names and sets. Never your name, crew, chat or account. Daily limits per person: 25 Coach messages, 4 plan builds, 15 new form tips, 1 recap per workout, 1 note a week. There is also a hard monthly budget for the whole app.", Modifier.padding(top = 6.dp))
        if (st.rest) Sub("This month’s AI budget is used up. Everything AI is back on the 1st.", Modifier.padding(top = 8.dp), Color(0xFFB35A00))
        if (st.on) Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("✦ Ask Coach", { vm.ai.openCoach() }, Modifier.weight(1f), bg = AiGold, fg = Color(0xFF2E1F02)); OvButton("✦ Build my plan", { vm.ai.openBuilder() }, Modifier.weight(1f)) }
    }
    OvCard { Row16("🔒", "Privacy", "What’s saved and shared") { vm.openSheet(Sheet.Privacy) } }
}
