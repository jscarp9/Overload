package com.overload.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt

// ============================ CREW TAB ============================
@Composable
fun CrewScreen(vm: OverloadViewModel, c: Ctx, u: UiState) {
    val cs by vm.crewState.collectAsState()
    if (!vm.crew.on) { Title("Crew"); Spacer(Modifier.height(10.dp)); CrewEmpty(vm, c); return }
    val s = cs ?: run { Title("Crew"); OvCard { Sub("Loading your crew…") }; LaunchedEffect(Unit) { vm.crew.sync(true) }; return }
    LaunchedEffect(Unit) { vm.crew.connect() }
    CrewHead17(vm, s)
    val cu = vm.chat.unread()
    val sub = u.sub["crew"] ?: "feed"
    SegTabs(listOf("feed" to "Feed", "chat" to "Chat" + if (cu > 0) " (${if (cu > 9) "9+" else cu})" else "", "arena" to "Arena"), sub) { vm.setSub("crew", it) }
    when (sub) {
        "chat" -> ChatTab(vm, c, s)
        "feed" -> {
            RaidCard(vm, s)
            LiveStrip(vm)
            Recap(vm, s, c)
            vm.crew.lastFinished()?.let { lf -> if (!lf.posted) CardButton("📣", "Post your ${c.plan.getOrNull(DAYS.indexOf(lf.day))?.name ?: "workout"}", "Share your breakdown with the crew") { vm.openSheet(Sheet.PostWorkout(sid(lf.date, lf.day))) } }
            if (s.posts.isEmpty()) OvCard { Sub("No posts yet. Finish a workout and tap Post to crew, or share the code ${s.crew.code} so friends can join.") }
            // 0.17: all "joined the crew" posts collapse into one line
            val posts = s.posts.filter { !vm.blocked(it.by) }
            val joins = posts.filter { it.type == "join" }; var joinShown = false
            posts.forEach { p ->
                if (p.type == "ai") AiRecapPost(p)
                else if (p.type != "join") PostCard(vm, c, p, u)
                else if (!joinShown) { joinShown = true
                    val names = joins.map { vm.crew.member2(it.by)?.name ?: "Someone" }.distinct()
                    val nm = if (names.size > 3) names.take(3).joinToString(", ") + " and ${names.size - 3} more" else if (names.size > 1) names.dropLast(1).joinToString(", ") + " and " + names.last() else names.first()
                    OvText("👋 $nm joined the crew · ${ago(joins.first().ts)}", 13, FontWeight.SemiBold, ov.mute, Modifier.padding(start = 6.dp, bottom = 12.dp))
                }
            }
        }
        else -> ArenaTabs(vm, c, s, u) { Arena(vm, c, s, u) }
    }
}

@Composable
private fun CrewEmpty(vm: OverloadViewModel, c: Ctx) {
    var name by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var crewName by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    OvCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { OvText("🏟️", 48); Title("Train with your crew", 24)
            Sub("Post your workouts, react to your friends' lifts and climb the Arena together. No account needed. Works with friends on the web app too.", Modifier.padding(vertical = 6.dp)) }
        if (c.st.name.isBlank()) { OvText("YOUR NAME", 13, FontWeight.ExtraBold, ov.mute); TextBox(name, { name = it.take(24) }, "Jake") }
        OvText("JOIN A CREW", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 14.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextBox(code, { code = it.uppercase().take(11) }, "IRON-4821", Modifier.weight(1f))
            OvButton("Join", { if (!busy) { busy = true; vm.crew.joinOrCreate(true, code, if (c.st.name.isBlank()) name else null) { busy = false } } }, bg = Red, fg = Color.White)
        }
        OvText("OR START ONE", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 14.dp, bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextBox(crewName, { crewName = it.take(30) }, "Crew name", Modifier.weight(1f))
            OvButton("Create", { if (!busy) { busy = true; vm.crew.joinOrCreate(false, crewName, if (c.st.name.isBlank()) name else null) { busy = false } } })
        }
    }
}

@Composable
private fun CrewHead(vm: OverloadViewModel, s: CrewState) {
    val c = s.crew; val b = BANNERS[c.banner] ?: BANNERS.getValue("steel"); val lv = c.level; val a = crewXpFor(lv); val z = crewXpFor(lv + 1); val n = vm.crew.bellUnread()
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(b.first), Color(b.second)))).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Title(c.name, 26, Color.White); OvText("${c.code} · ${s.members.size} member${if (s.members.size > 1) "s" else ""}", 13, FontWeight.SemiBold, Color.White.copy(.85f)) }
            Box(Modifier.size(42.dp).background(Color.White.copy(.22f), RoundedCornerShape(14.dp)).clickable { vm.openSheet(Sheet.Bell); vm.setSettings { it.copy(bellSeen = System.currentTimeMillis()) } }, contentAlignment = Alignment.Center) {
                OvText("🔔", 18); if (n > 0) OvText(if (n > 9) "9+" else "$n", 10, FontWeight.Black, Color.White, Modifier.align(Alignment.TopEnd).background(Red, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp))
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(42.dp).background(Color.White.copy(.22f), RoundedCornerShape(14.dp)).clickable { vm.openSheet(Sheet.CrewSettings) }, contentAlignment = Alignment.Center) { OvText("⚙", 18, color = Color.White) }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            OvText("Lv $lv", 14, FontWeight.Black, Color.White)
            Box(Modifier.weight(1f).padding(horizontal = 10.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(.25f))) {
                Box(Modifier.fillMaxWidth(((c.xp - a) / (z - a).coerceAtLeast(1)).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(Color.White))
            }
            OvText("${loc(c.xp)} XP", 12, FontWeight.Bold, Color.White)
        }
    }
}

@Composable
private fun LiveStrip(vm: OverloadViewModel) {
    val live by vm.live.collectAsState(); val me = vm.myId()
    val list = live.filter { (id) -> id != me && vm.crew.member(id) != null }; if (list.isEmpty()) return
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        list.forEach { (id, l) ->
            Row(Modifier.clip(RoundedCornerShape(18.dp)).background(ov.card).border(1.5.dp, Red.copy(.5f), RoundedCornerShape(18.dp)).clickable { vm.crew.hype(id) }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Red, RoundedCornerShape(4.dp))); Spacer(Modifier.width(6.dp))
                Column { OvText(vm.crew.member(id)!!.name, 14, FontWeight.Bold); Sub("${l.day} · ${maxOf(1, ((System.currentTimeMillis() - l.start) / 60000).toInt())}m") }
                Spacer(Modifier.width(6.dp)); OvText("🔥", 18)
            }
        }
    }
}

@Composable
private fun Recap(vm: OverloadViewModel, s: CrewState, c: Ctx) {
    val dw = LocalDate.now().dayOfWeek.value; if (dw !in 1..3 || c.st.recapSeen == vm.crew.curWk()) return
    val mem = s.members.filter { it.profile != null }; if (mem.size < 2) return
    fun pts(w: WeekStat?) = if (w == null) 0 else w.days * 40 + w.sets * 3 + w.prs * 20
    val pwk = iso(weekStart(today()).minusDays(14)); val lwk = weekStart(today()).minusDays(7)
    val l = mem.map { m -> val p = m.profile!!; val lw = vm.crew.lastOf(p); val pv = p.prev?.takeIf { it.wk == pwk }
        Triple(m, pts(lw), pts(lw) - pts(pv)) to (lw?.let { vm.crew.mine(it.vol, p.unit) } ?: 0.0) }
    val mvp = l.maxByOrNull { it.first.second }!!.first.first; val imp = l.maxByOrNull { it.first.third }!!.first; val tot = l.sumOf { it.second }
    val t0 = lwk.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(); val t1 = t0 + 7 * 86_400_000L
    val tp = s.posts.filter { it.ts in t0 until t1 && it.type == "workout" }.map { it to it.reacts.values.sumOf { a -> a.size } }.maxByOrNull { it.second }
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { OvText("📊 Last week in ${s.crew.name}", 15, FontWeight.Bold, modifier = Modifier.weight(1f)); OvText("✕", 16, color = ov.mute, modifier = Modifier.clickable { vm.setSettings { it.copy(recapSeen = vm.crew.curWk()) } }.padding(6.dp)) }
        listOf("🏆 MVP" to mvp.name, "📈 Most improved" to (if (imp.third > 0) imp.first.name else "—"), "🏗️ Crew volume" to "${kfmt(tot)} ${c.unit}",
            "⭐ Top post" to (tp?.takeIf { it.second > 0 }?.let { "${vm.crew.member(it.first.by)?.name ?: "?"} · ${it.second} reacts" } ?: "—")).chunked(2).forEach { row ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { (k, v) -> Column(Modifier.weight(1f).background(ov.soft, RoundedCornerShape(14.dp)).padding(10.dp)) { Sub(k); OvText(v, 15, FontWeight.Bold) } } }
        }
    }
}

// ---------- posts ----------
@Composable
fun PostBody(c: Ctx, p: Post, preview: Boolean) {
    val d = p.data
    when (p.type) {
        "join" -> OvText("Joined the crew 👋", 15, modifier = Modifier.padding(top = 8.dp))
        "trophy" -> { val t = CTROPHY.firstOrNull { it[1] == d.name }
            Row(Modifier.padding(top = 8.dp).fillMaxWidth().background(hex("#ffd76a", .18f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText(t?.get(2) ?: "🏆", 30); Spacer(Modifier.width(10.dp)); Column { Sub("Crew trophy unlocked"); OvText(d.name.ifEmpty { "Trophy" }, 17, FontWeight.Black) } } }
        "raid" -> Row(Modifier.padding(top = 8.dp).fillMaxWidth().background(hex("#8b5cf6", .15f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            BossView(d.boss, Modifier.size(56.dp)); Spacer(Modifier.width(10.dp)); Column { Sub("Boss defeated"); OvText(d.name.ifEmpty { "Boss" }, 17, FontWeight.Black); Sub("${d.dmg.size} lifters took it down") } }
        "group" -> GroupPostBodyFor(c, p)
        "rank" -> Row(Modifier.padding(top = 8.dp).fillMaxWidth().background(Color(RANKS[d.rank.coerceIn(0, 7)].light).copy(.25f), RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Emblem(d.rank.coerceIn(0, 7), Modifier.size(48.dp)); Spacer(Modifier.width(10.dp)); Column { Sub("Ranked up to"); OvText(d.rname.ifEmpty { RANKS[d.rank.coerceIn(0, 7)].name }, 18, FontWeight.Black) } }
        else -> {
            if (p.cap.isNotEmpty()) OvText(p.cap, 15, modifier = Modifier.padding(top = 8.dp))
            val col = pc(d.color).base
            Row(Modifier.padding(top = 8.dp).fillMaxWidth().background(col.copy(.12f), RoundedCornerShape(18.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Anatomy("front", d.levels, Modifier.height(120.dp)); Anatomy("back", d.levels, Modifier.height(120.dp)); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row { MiniStat(if (d.min > 0) "${d.min}" else "–", "min", Modifier.weight(1f)); MiniStat("${d.sets}", "sets", Modifier.weight(1f)) }
                    Row { MiniStat(kfmt(c.unit.let { toMine(d.vol, d.unit, it) }), c.unit, Modifier.weight(1f)); MiniStat("${d.prs}", "PRs", Modifier.weight(1f)) }
                }
            }
            d.top.forEach { t -> Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                OvText(t.n, 14, modifier = Modifier.weight(1f)); if (t.pr > 0) Chip("PR", Red, Color.White)
                OvText("${fmtN((toMine(t.w, d.unit, c.unit) * 10).roundToInt() / 10.0)} × ${t.r}", 14, FontWeight.Bold) } }
            if (d.cmp.isNotEmpty() && !preview) Sub(d.cmp, Modifier.padding(top = 6.dp))
            if (d.badges.isNotEmpty()) Row(Modifier.padding(top = 6.dp)) { d.badges.forEach { b -> Chip("${b.getOrElse(0) { "" }} ${b.getOrElse(1) { "" }}") } }
        }
    }
}

@Composable private fun MiniStat(v: String, l: String, modifier: Modifier) = Column(modifier) { Title(v, 20); Sub(l) }

@Composable
private fun PostHead(vm: OverloadViewModel, p: Post, preview: Boolean, menu: (() -> Unit)?) {
    val m = vm.crew.member(p.by); val pr = m?.profile
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable { if (m != null) vm.openSheet(Sheet.ProfilePage(p.by)) }) { MemberAvatar(vm, p.by, m?.name ?: "Former member") }; Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NameText(m?.name ?: vm.crew.member2(p.by)?.name ?: "Former member", pr?.nameFx ?: "", 15, founder = pr?.founder == true, shame = vm.crew.shamed(p.by)); Spacer(Modifier.width(6.dp))
                if (pr != null) { Emblem(pr.rank.coerceIn(0, 7), Modifier.size(18.dp)); OvText(" ${pr.rname}", 12, FontWeight.Bold, ov.mute) }
                if (pr?.tag?.isNotEmpty() == true) { Spacer(Modifier.width(6.dp)); Chip(pr.tag, Color(0xFF14161C), Color.White) }
            }
            Sub((if (p.type == "workout") "${p.data.day.ifEmpty { "Workout" }} · " else "") + if (preview) "Preview" else ago(p.ts))
        }
        if (menu != null) OvText("⋯", 22, FontWeight.Bold, ov.mute, Modifier.clickable(onClick = menu).padding(8.dp))
    }
}

@Composable
private fun PostCard(vm: OverloadViewModel, c: Ctx, p: Post, u: UiState) {
    val me = vm.myId(); val cs = vm.crewState.value; val own = cs?.crew?.owner == me
    val canDel = p.by == me || own
    val style = vm.crew.member(p.by)?.profile?.card ?: ""
    val cardBg = when (style) { "carbon" -> Brush.linearGradient(listOf(hex("#1a1b1f"), hex("#2e3036"))); "gold" -> Brush.linearGradient(listOf(hex("#7a5410", .35f), hex("#ffe07a", .25f)))
        "ice" -> Brush.linearGradient(listOf(hex("#e9f8ff", .6f), hex("#8fd3ff", .35f))); "neon" -> Brush.linearGradient(listOf(hex("#0d0f14"), hex("#2a0f2e"))); else -> null }
    OvCard(if (cardBg != null) Modifier.background(cardBg, RoundedCornerShape(24.dp)) else Modifier) {
        PostHead(vm, p, false, if (canDel) ({ if (u.confirm != "p${p.id}") { vm.setConfirm("p${p.id}"); vm.toast("Tap ⋯ again to delete this post") } else { vm.setConfirm(null); vm.crew.deletePost(p.id) } }) else null)
        PostBody(c, p, false)
        if (c.st.roasts) roastLine(p, c.unit)?.let { Sub("🤖 $it", Modifier.padding(top = 6.dp)) }
        // 0.17: only reactions with counts show, ＋ opens the full row
        val open = u.confirm == "rx${p.id}"
        Row(Modifier.padding(top = 10.dp).horizontalScrollC(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            val keys = if (open) c.reactKeys() else p.reacts.filter { it.value.isNotEmpty() }.keys.toList()
            keys.forEach { k -> val a = p.reacts[k] ?: emptyList(); val on = me != null && me in a
                Box(Modifier.background(if (on) Red.copy(.15f) else ov.soft, RoundedCornerShape(12.dp)).then(if (on) Modifier.border(1.dp, Red.copy(.5f), RoundedCornerShape(12.dp)) else Modifier)
                    .clickable { vm.crew.react(p.id, k); if (open) vm.setConfirm(null) }.padding(horizontal = 12.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically) { if (k == "crest") CrestView(cs?.crew?.crest, 0, Modifier.size(16.dp)) else OvText(RE14[k] ?: "🔥", 14); if (a.isNotEmpty()) OvText(" ${a.size}", 14, FontWeight.Bold) } } }
            OvText(if (open) "✕" else "＋", 14, FontWeight.Bold, modifier = Modifier.background(ov.soft, RoundedCornerShape(12.dp)).clickable { vm.setConfirm(if (open) null else "rx${p.id}") }.padding(horizontal = 12.dp, vertical = 7.dp))
            OvText("💬" + if (p.comments.isNotEmpty()) " ${p.comments.size}" else "", 14, FontWeight.Bold, modifier = Modifier.background(ov.soft, RoundedCornerShape(12.dp)).clickable { vm.openSheet(Sheet.Comments(p.id)) }.padding(horizontal = 12.dp, vertical = 7.dp))
            if (own && p.type == "workout") OvText("🏛", 14, modifier = Modifier.background(ov.soft, RoundedCornerShape(12.dp)).clickable { vm.openSheet(Sheet.HofPost(p.id)) }.padding(horizontal = 10.dp, vertical = 7.dp))
        }
        p.comments.takeLast(2).forEach { cm -> OvText("${vm.crew.member2(cm.by)?.name ?: "?"}  ${cm.text}", 14, modifier = Modifier.padding(top = 6.dp)) }
    }
}

@Composable
private fun GroupPostBodyFor(c: Ctx, p: Post) { val vm: OverloadViewModel = androidx.lifecycle.viewmodel.compose.viewModel(); GroupPostBody(vm, p) }

// ---------- arena ----------
@Composable
private fun Arena(vm: OverloadViewModel, c: Ctx, s: CrewState, u: UiState) {
    val mode = u.sub["arena"] ?: "all"; val me = vm.myId()
    val order = vm.crew.arenaOrder(mode).filter { it.profile != null }; val t = vm.crew.titleHolders(); val snap = c.st.arenaSnap?.pos ?: emptyMap()
    if (order.isEmpty()) { OvCard { Sub("Rankings show up once crew members open the app.") }; return }
    fun value(m: Member) = if (mode == "week") "${vm.crew.weekPts(m.profile)} pts" else "${m.profile!!.power.roundToInt()}"
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        listOf(1 to 2, 0 to 1, 2 to 3).forEach { (idx, place) -> val m = order.getOrNull(idx)
            if (m == null) Spacer(Modifier.weight(1f)) else {
                val p = m.profile!!; val tt = t.entries.firstOrNull { it.value == m.id }?.let { e -> vm.crew.titles.first { it.k == e.key } }
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(ov.card).clickable { vm.openSheet(Sheet.ProfilePage(m.id)) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (tt != null) OvText("${tt.icon} ${tt.name}", 10, FontWeight.Bold, ov.mute, align = TextAlign.Center)
                    FramedEmblem(p.rank.coerceIn(0, 7), p.frame, p.aura, Modifier.size(if (place == 1) 74.dp else 58.dp))
                    OvText(m.name, 14, FontWeight.Bold, align = TextAlign.Center); OvText("${p.rname} · ${value(m)}", 11, color = ov.mute, align = TextAlign.Center)
                    Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(when (place) { 1 -> 44.dp; 2 -> 30.dp; else -> 22.dp }).background(listOf(Color(0xFFE8B923), Color(0xFFB8C2CC), Color(0xFFC07A3E))[place - 1], RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { OvText("$place", 16, FontWeight.Black, Color.White) }
                }
            }
        }
    }
    OvCard {
        if (order.size <= 3) Sub("Everyone in your crew is on the podium.")
        order.forEachIndexed { i, m -> if (i < 3) return@forEachIndexed; val p = m.profile!!; val prev = snap[m.id]; val mv = if (mode == "all" && prev != null) prev - i else 0
            val tk = t.filter { it.value == m.id }.keys.mapNotNull { k -> vm.crew.titles.firstOrNull { it.k == k } }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (m.id == me) Red.copy(.08f) else Color.Transparent).clickable { vm.openSheet(Sheet.ProfilePage(m.id)) }.padding(vertical = 8.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText("${i + 1}", 15, FontWeight.Black, ov.mute, Modifier.width(24.dp)); MemberAvatar(vm, m.id, m.name, 34); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OvText(m.name, 15, FontWeight.Bold); if (m.ok) OvText(" ✓", 12, FontWeight.Black, Green)
                        if (mv > 0) OvText(" ▲$mv", 12, FontWeight.Bold, Green) else if (mv < 0) OvText(" ▼${-mv}", 12, FontWeight.Bold, Red)
                        tk.firstOrNull()?.let { Spacer(Modifier.width(4.dp)); Chip("${it.icon} ${it.name}") }; if (tk.size > 1) Chip("+${tk.size - 1}")
                    }
                    Bar(if (p.rank >= 7) 1.0 else p.frac, Color(RANKS[p.rank.coerceIn(0, 7)].light), Modifier.padding(vertical = 3.dp), 4)
                    Sub("${p.rname} · 🔥${p.streak}" + if (p.trial > 0) " · Promotion trial" else "")
                }
                OvText(value(m), 15, FontWeight.Black)
            }
        }
    }
    OvCard {
        H3("Titles"); Sub("Only one person holds each title. Take it from them this week.")
        vm.crew.titles.chunked(2).forEach { row ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { tt -> val holder = vm.crew.member(t[tt.k])
                    Column(Modifier.weight(1f).background(if (t[tt.k] == me) Color(0xFFFFD75E).copy(.2f) else ov.soft, RoundedCornerShape(14.dp)).padding(10.dp)) {
                        OvText(tt.icon, 22); OvText(tt.name, 14, FontWeight.Bold); Sub(holder?.name ?: "Up for grabs"); OvText(tt.desc, 11, color = ov.mute)
                    }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val riv = vm.crew.member(c.st.rival); val mine = vm.crew.member(me)
        OvCard(Modifier.weight(1f)) {
            if (riv?.profile != null && mine?.profile != null) {
                val (w, l) = vm.crew.h2h(vm.crew.wkOf(mine.profile), mine.profile.unit, vm.crew.wkOf(riv.profile), riv.profile.unit)
                Sub("⚔️ Your rival"); OvText("You $w – $l ${riv.name}", 15, FontWeight.Bold); Sub((if (w > l) "You lead this week" else if (w < l) "They lead this week" else "Dead even") + " · days, sets, volume, PRs")
            } else { Sub("⚔️ Rival"); OvText("Pick a rival", 15, FontWeight.Bold); Sub("Tap anyone above to start a weekly head-to-head") }
        }
        OvCard(Modifier.weight(1f)) {
            val cp = vm.crew.chalProgress()
            Sub("🏁 Crew challenge")
            if (cp != null) { OvText(cp.title, 15, FontWeight.Bold); Bar(cp.cur / cp.target, Green, Modifier.padding(vertical = 4.dp)); Sub("${loc(cp.cur)} / ${loc(cp.target)}" + if (cp.cur >= cp.target) " · Done! 🎉" else "") }
            else { OvText("None this week", 15, FontWeight.Bold); OvButton("Set one", { vm.openSheet(Sheet.SetChallenge) }, Modifier.padding(top = 6.dp), small = true) }
        }
    }
}

@Composable
fun AwardsCardPublic(vm: OverloadViewModel, s: CrewState) {
    if (s.members.size < 2) return
    val d = LocalDate.now().dayOfMonth; val mo = vm.crew.monthKey(); val pm = vm.crew.prevMonthKey()
    if (d <= 7) { val w = vm.crew.awardWinners(pm); if (w.isNotEmpty()) OvCard { H3("🎖️ ${vm.crew.monthName(pm)} crew awards")
        vm.crew.awards.filter { w[it.first] != null }.forEach { a -> Row(Modifier.padding(top = 6.dp)) { OvText("${a.third} ${a.second}", 14, modifier = Modifier.weight(1f)); OvText(vm.crew.member(w[a.first])?.name ?: "?", 14, FontWeight.Bold) } } }; return }
    if (d >= 20) { val v = s.votes[mo] ?: emptyMap(); val done = vm.crew.awards.count { v[it.first]?.mine != null }
        CardButton("🎖️", "Vote: ${vm.crew.monthName(mo)} crew awards", "$done/${vm.crew.awards.size} votes cast · results on the 1st") { vm.openSheet(Sheet.Awards) } }
}

// ============================ CREW SHEETS ============================
@Composable
fun PostSheet(vm: OverloadViewModel, c: Ctx, key: String) {
    val s = c.d.sessions[key] ?: return; var cap by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val data = remember(key) { vm.crew.postData(s) }
    val fake = remember(data) { Post("x", vm.myId() ?: "", System.currentTimeMillis(), "workout", "", vm.jsonCodec.decodeFromJsonElement(PostData.serializer(), data)) }
    SheetHead("Post to crew", "Cancel") { vm.closeSheet() }
    TextBox(cap, { cap = it.take(280) }, "Say something about it (optional)", singleLine = false, minH = 60.dp)
    OvCard(Modifier.padding(top = 12.dp)) { PostHead(vm, fake, true, null); PostBody(c, fake.copy(cap = cap), true) }
    OvButton(if (busy) "Posting…" else "Post", { if (!busy) { busy = true; vm.crew.postWorkout(key, cap) { busy = false } } }, bg = Red, fg = Color.White, wide = true)
}

@Composable
fun CommentsSheet(vm: OverloadViewModel, postId: String) {
    val cs by vm.crewState.collectAsState(); val p = cs?.posts?.firstOrNull { it.id == postId } ?: return
    var t by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    SheetHead("Comments") { vm.closeSheet() }
    if (p.comments.isEmpty()) Sub("No comments yet. Say something.")
    p.comments.forEach { cm -> Row(Modifier.padding(vertical = 4.dp)) { OvText(vm.crew.member(cm.by)?.name ?: "?", 14, FontWeight.Bold); Spacer(Modifier.width(6.dp)); OvText(cm.text, 14, modifier = Modifier.weight(1f)); Sub(ago(cm.ts)) } }
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextBox(t, { t = it.take(200) }, "Add a comment", Modifier.weight(1f))
        OvButton("Send", { if (t.isNotBlank() && !busy) { busy = true; vm.crew.comment(postId, t.trim()) { ok -> busy = false; if (ok) t = "" } } }, bg = Red, fg = Color.White)
    }
}

@Composable
fun CrewSettingsSheet(vm: OverloadViewModel, c: Ctx, u: UiState) {
    val cs by vm.crewState.collectAsState(); val s = cs ?: return; val me = vm.myId(); val own = s.crew.owner == me; val ctx = LocalContext.current
    SheetHead(s.crew.name) { vm.closeSheet() }
    Column(Modifier.fillMaxWidth().background(ov.soft, RoundedCornerShape(18.dp)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Sub("Crew code"); Title(s.crew.code, 28)
    }
    OvButton("📨 Invite friends", { shareInvite(ctx, s) }, Modifier.padding(top = 10.dp), bg = Red, fg = Color.White, wide = true)
    H3("Members", Modifier.padding(top = 14.dp))
    s.members.forEach { m -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        MemberAvatar(vm, m.id, m.name, 34); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { OvText(m.name + (if (m.id == s.crew.owner) " · owner" else "") + (if (m.id == me) " · you" else ""), 15, FontWeight.Bold); Sub(m.profile?.rname ?: "Hasn’t synced yet") }
        if (own && m.id != me) OvButton(if (u.confirm == "k${m.id}") "Tap to confirm" else "Remove", { if (u.confirm != "k${m.id}") vm.setConfirm("k${m.id}") else { vm.setConfirm(null); vm.crew.kick(m.id) } }, small = true)
    } }
    if (own) { ReportsCard(vm, s); CrewOwnerExtras(vm, s) }
    Sub("Crew pings (lock-screen alerts for posts) aren’t available on Android yet. You still see everything in 🔔 Activity.", Modifier.padding(top = 14.dp))
    OvButton(if (u.confirm == "leave") "Tap again to leave" else "Leave crew", { if (u.confirm != "leave") vm.setConfirm("leave") else { vm.setConfirm(null); vm.crew.leave() } }, Modifier.padding(top = 14.dp), fg = Red, wide = true)
}

/** Head-to-head, best lifts, their plan and rival button (inside a crewmate's profile page). */
@Composable
fun MemberExtras(vm: OverloadViewModel, c: Ctx, id: String) {
    val m = vm.crew.member(id) ?: return; val p = m.profile ?: MemberProfile(); val me = vm.myId(); val mine = vm.crew.member(me)?.profile; val isMe = id == me
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        FramedEmblem(p.rank.coerceIn(0, 7), p.frame, p.aura, Modifier.size(64.dp)); Spacer(Modifier.width(10.dp))
        Column { Title(p.rname.ifEmpty { "Unranked" }, 20); if (p.tag.isNotEmpty()) Chip(p.tag, Color(0xFF14161C), Color.White)
            Sub("Rank Power ${p.power.roundToInt()} · 🔥 ${p.streak} week${if (p.streak == 1) "" else "s"} · ${p.bcount} badges") }
    }
    val lifts = p.lifts.entries.take(6).toList(); val common = if (mine != null) lifts.filter { mine.lifts[it.key] != null } else emptyList()
    if (!isMe && common.isNotEmpty()) {
        H3("Head-to-head", Modifier.padding(top = 14.dp))
        common.forEach { (n, l) -> val a = vm.crew.mine(mine!!.lifts[n]!!.e, mine.unit); val b = vm.crew.mine(l.e, p.unit)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText("${a.roundToInt()}", 16, FontWeight.Black, if (a >= b) Green else ov.mute, Modifier.width(60.dp)); OvText(n, 14, modifier = Modifier.weight(1f), align = TextAlign.Center)
                OvText("${b.roundToInt()}", 16, FontWeight.Black, if (b > a) Green else ov.mute, Modifier.width(60.dp), align = TextAlign.End) } }
        OvText("You vs ${m.name} · estimated 1RM", 13, color = ov.mute, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
    } else if (lifts.isNotEmpty()) {
        H3("Best lifts", Modifier.padding(top = 14.dp))
        lifts.forEach { (n, l) -> Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) { OvText(n, 14, modifier = Modifier.weight(1f)); OvText("${vm.crew.mine(l.w, p.unit).roundToInt()} × ${l.r}", 14, FontWeight.Bold) } }
    }
    val buddies = vm.crewState.value?.buddies?.get(id) ?: 0
    if (!isMe && buddies > 0) OvText("👯 $buddies gym buddy session${if (buddies > 1) "s" else ""} together", 13, color = ov.mute, modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), align = TextAlign.Center)
    val days = p.plan?.days
    if (!isMe && days != null) {
        H3("Their plan", Modifier.padding(top = 14.dp))
        days.forEachIndexed { i, d -> if (d.groups.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OvText("${DN[i]} · ${d.name}", 14, FontWeight.Bold, modifier = Modifier.weight(1f)); Sub("${d.groups.sumOf { it.size }} exercises  "); OvButton("Copy", { vm.openSheet(Sheet.CopyPlan(id, i)) }, small = true) } }
        OvButton("Copy their whole split", { vm.openSheet(Sheet.CopyPlan(id, null)) }, Modifier.padding(top = 8.dp), wide = true)
    }
    if (!isMe) OvButton(if (c.st.rival == id) "Remove rival" else "⚔️ Make them your rival", {
        val now = if (c.st.rival == id) null else id; vm.setSettings { it.copy(rival = now) }; vm.closeSheet(); vm.toast(if (now != null) "Rival set ⚔️ Weekly head-to-head is on" else "Rival removed")
    }, Modifier.padding(top = 12.dp), bg = Red, fg = Color.White, wide = true)
}

@Composable
fun ChallengeSheet(vm: OverloadViewModel, c: Ctx) {
    val n = maxOf(2, vm.crewState.value?.members?.size ?: 2); fun sc(v: Int) = ((v * n / 4.0 / 10).roundToInt() * 10).takeIf { it > 0 } ?: v
    val days = maxOf(4, 4 * n); val prs = maxOf(3, (2.5 * n).roundToInt()); val vol = sc(if (c.kg) 110000 else 250000)
    val pre = listOf(Triple("sets", sc(300), "${sc(300)} sets as a crew"), Triple("days", days, "$days workouts as a crew"), Triple("vol", vol, "Move ${kfmt(vol.toDouble())} ${c.unit} as a crew"), Triple("prs", prs, "$prs PRs as a crew"))
    SheetHead("Crew challenge", "Cancel") { vm.closeSheet() }
    Sub("Everyone's workouts this week count toward it. Resets Monday.")
    pre.forEach { (t, v, l) -> OvButton(l, { vm.crew.setChallenge(t, v, l) }, Modifier.padding(top = 8.dp), wide = true) }
}

@Composable
fun AwardsSheet(vm: OverloadViewModel) {
    val cs by vm.crewState.collectAsState(); val s = cs ?: return; val mo = vm.crew.monthKey(); val v = s.votes[mo] ?: emptyMap(); val me = vm.myId()
    var mine by remember(mo) { mutableStateOf(vm.crew.awards.associate { it.first to v[it.first]?.mine }) }
    SheetHead("${vm.crew.monthName(mo)} awards") { vm.closeSheet() }
    Sub("One vote per category. You can change it until the month ends.")
    vm.crew.awards.forEach { (k, n, i) ->
        OvText("$i ${n.uppercase()}", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 14.dp, bottom = 6.dp))
        s.members.filter { it.id != me }.chunked(3).forEach { row -> Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { m -> Seg(m.name, mine[k] == m.id, { mine = mine + (k to m.id); vm.crew.vote(k, m.id) }) } } }
    }
}

@Composable
fun CopyPlanSheet(vm: OverloadViewModel, c: Ctx, memberId: String, day: Int?) {
    val m = vm.crew.member(memberId) ?: return; val p = m.profile?.plan ?: return
    if (day == null) {
        Title("Copy ${m.name}'s whole split?", 24)
        Sub("This replaces all 7 days of your plan. Your workout history stays.", Modifier.padding(vertical = 8.dp))
        OvButton("Replace my plan", { vm.crew.copySplit(memberId) }, bg = Red, fg = Color.White, wide = true)
        OvButton("Cancel", { vm.closeSheet() }, Modifier.padding(top = 8.dp), wide = true); return
    }
    val d = p.days.getOrNull(day) ?: return
    SheetHead("Copy ${d.name}", "Cancel") { vm.closeSheet() }
    Sub("Which of your days should it replace?")
    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        DN.forEachIndexed { i, n -> Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(ov.soft).clickable { vm.crew.copyDay(memberId, day, i) }.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OvText(n, 13, FontWeight.Bold); OvText(c.plan.getOrNull(i)?.name ?: "", 9, color = ov.mute, align = TextAlign.Center) } }
    }
}

@Composable
fun BellSheet(vm: OverloadViewModel) {
    val items = vm.crew.bellItems()
    SheetHead("Activity") { vm.closeSheet() }
    if (items.isEmpty()) Sub("Nothing yet. Reactions, comments, titles and rank-ups show up here.")
    items.forEach { x -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(x.icon, 22, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { OvText(x.text, 14); Sub(ago(x.ts)) } } }
}
