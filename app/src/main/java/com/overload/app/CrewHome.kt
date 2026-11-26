package com.overload.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

// ============================ AVATARS + NAMES ============================
/** Round avatar: initials, an icon or a plate (Edit profile → Avatar). */
@Composable
fun MemberAvatar(vm: OverloadViewModel, id: String, name: String, size: Int = 40, av0: Av? = null) {
    val av = av0 ?: vm.crew.member(id)?.profile?.av ?: if (id == vm.myId()) vm.data.value.settings.av else null
    AvatarOf(id, name, av, size)
}
@Composable
fun AvatarOf(id: String, name: String, av: Av?, size: Int) {
    if (av?.t == "plate" && av.v != null && PC[av.v] != null) { Plate(av.v, "done", Modifier.size(size.dp)); return }
    val h = hue(id).toFloat()
    val bg = if (av?.c != null) hex(av.c) else Color.hsl(h, .7f, .88f)
    Box(Modifier.size(size.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        if (av?.t == "icon" && av.v != null) OvText(av.v, (size * .5f).toInt())
        else OvText(initials(name), (size * .38f).toInt(), FontWeight.ExtraBold, if (av?.c != null) Color.White else Color.hsl(h, .6f, .26f))
    }
}
/** A name with its name effect (gold, neon, fire, rainbow, founder). */
@Composable
fun NameText(name: String, fx: String, size: Int = 15, founder: Boolean = false, shame: Boolean = false, modifier: Modifier = Modifier) {
    val brush = when {
        founder || fx == "gold" -> Brush.linearGradient(listOf(hex("#9c7424"), hex("#ffe58a"), hex("#c99a2e")))
        fx == "neon" -> Brush.linearGradient(listOf(hex("#00e5ff"), hex("#7af7ff")))
        fx == "fire" -> Brush.linearGradient(listOf(hex("#ff2d2d"), hex("#ff8a1f"), hex("#ffd23f")))
        fx == "rainbow" -> Brush.linearGradient(listOf(hex("#ff3d4a"), hex("#ffbe0b"), hex("#16b65b"), hex("#2563ff"), hex("#7c4dff")))
        else -> null
    }
    val txt = (if (founder) "👑 " else "") + name + if (shame) " 🩷" else ""
    if (brush == null) OvText(txt, size, FontWeight.Bold, modifier = modifier)
    else Text(txt, modifier, style = TextStyle(brush = brush, fontSize = size.sp, fontWeight = FontWeight.ExtraBold))
}

// ============================ CREW HEAD (0.14) ============================
@Composable
fun CrewHead17(vm: OverloadViewModel, s: CrewState) {
    val c = s.crew; val n = vm.crew.bellUnread(); val live by vm.live.collectAsState(); val R = vm.crew.crewRankInfo(); val S = vm.crew.crewSum(); val own = vm.crew.isOwner()
    val founded = Instant.ofEpochMilli(if (c.created > 0) c.created else System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDate()
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(24.dp))) {
        Scene(c.banner, Modifier.matchParentSize(), c.level, "crew${c.code}")
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.05f), (s.style?.c1?.takeIf { it.isNotEmpty() }?.let { hex(it, .55f) } ?: Color.Black.copy(.55f))))))
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).clickable { if (own) vm.openSheet(Sheet.CrestEdit(c.crest ?: Crest())) else vm.toast("The crew owner designs the crest") }) { CrestView(c.crest, R.r, Modifier.fillMaxSize()) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Title(c.name, 24, Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (c.founders) Chip("★ Founders", hex("#ffd76a"), Color(0xFF14161C))
                        OvText("Founded ${MON[founded.monthValue - 1]} ${founded.year}", 12, FontWeight.SemiBold, Color.White.copy(.85f))
                    }
                    s.style?.motto?.takeIf { it.isNotEmpty() }?.let { OvText("“$it”", 12, FontWeight.SemiBold, Color.White.copy(.9f)) }
                }
                HeadBtn("🔔", n) { vm.openSheet(Sheet.Bell); vm.setSettings { it.copy(bellSeen = System.currentTimeMillis()) } }
                Spacer(Modifier.width(6.dp)); HeadBtn("⚙", 0) { vm.openSheet(Sheet.CrewSettings) }
            }
            val ln = live.keys.count { vm.crew.member(it) != null }
            if (ln > 0) Row(Modifier.padding(top = 8.dp).background(Color.Black.copy(.35f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(Red, CircleShape)); Spacer(Modifier.width(6.dp)); OvText("$ln training now", 12, FontWeight.Bold, Color.White) }
        }
    }
    val lv = c.level; val a = crewXpFor(lv); val z = crewXpFor(lv + 1)
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp, start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        StripCell("${kfmt(S.vol)}", "${vm.unit} lifted", Modifier.weight(1f)); StripCell("${S.streak}", "wk crew streak", Modifier.weight(1f))
        Column(Modifier.weight(1f).clickable { vm.openSheet(Sheet.CrewRankInfo) }, horizontalAlignment = Alignment.CenterHorizontally) { Emblem(R.r, Modifier.size(30.dp), spin = R.r >= 6); Sub(RANKS[R.r].name) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Title("Lv $lv", 18); Bar(((c.xp - a) / (z - a).coerceAtLeast(1)), Green, Modifier.padding(horizontal = 8.dp, vertical = 4.dp), 5) }
    }
}
@Composable private fun HeadBtn(icon: String, n: Int, onClick: () -> Unit) =
    Box(Modifier.size(40.dp).background(Color.White.copy(.22f), RoundedCornerShape(13.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        OvText(icon, 17, color = Color.White); if (n > 0) OvText(if (n > 9) "9+" else "$n", 10, FontWeight.Black, Color.White, Modifier.align(Alignment.TopEnd).background(Red, RoundedCornerShape(8.dp)).padding(horizontal = 4.dp))
    }
@Composable private fun StripCell(v: String, l: String, m: Modifier) = Column(m, horizontalAlignment = Alignment.CenterHorizontally) { Title(v, 18); Sub(l) }

// ============================ ARENA (0.17: one row of tabs) ============================
@Composable
fun ArenaTabs(vm: OverloadViewModel, c: Ctx, s: CrewState, u: UiState, ranks: @Composable () -> Unit) {
    val k = u.sub["arena2"] ?: "rank"; val mode = u.sub["arena"] ?: "all"; val cur = if (k == "rank") "r_$mode" else k
    Chips(listOf("r_all" to "Rankings", "r_week" to "This week", "r_lifts" to "Lifts", "tro" to "Trophies", "rec" to "Records", "fun" to "Awards"), cur) { a ->
        if (a.startsWith("r_")) { vm.setSub("arena2", "rank"); vm.setSub("arena", a.drop(2)) } else vm.setSub("arena2", a) }
    when (k) {
        "rank" -> ranks()
        "tro" -> { TrophyCase(vm, s); RaidWins(s); HallOfFame(vm, s); CrewCardButton(vm, s) }
        "rec" -> { CrewRecords(vm, c); SeasonWall(vm, s, c) }
        else -> JokeAwards(vm, s)
    }
}

/** Horizontal chips (Arena tabs, plate categories, badge tabs). */
@Composable
fun Chips(opts: List<Pair<String, String>>, cur: String, onPick: (String) -> Unit) =
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).horizontalScrollC(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opts.forEach { (k, l) -> OvText(l, 13, FontWeight.Bold, if (cur == k) ov.bg else ov.ink, Modifier.clip(RoundedCornerShape(14.dp)).background(if (cur == k) ov.ink else ov.card).clickable { onPick(k) }.padding(horizontal = 12.dp, vertical = 8.dp)) }
    }
@Composable fun Modifier.horizontalScrollC() = this.then(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()))

@Composable
fun TrophyCase(vm: OverloadViewModel, s: CrewState) = OvCard {
    val T = s.crew.trophies
    Row { H3("Crew trophy case", Modifier.weight(1f)); Sub("${T.size}/${CTROPHY.size}") }
    CTROPHY.chunked(3).forEach { row -> Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { t -> val got = T[t[0]]; val tiers = t[3].split(",").filter { it.isNotEmpty() }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (got != null) hex("#ffd76a", .18f) else ov.soft).clickable {
                vm.toast("${t[2]} ${t[1]}: ${if (got != null) "earned " + Instant.ofEpochMilli(got.ts).atZone(ZoneId.systemDefault()).toLocalDate() else t[4]}") }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OvText(if (got != null) t[2] else "?", 24); OvText(t[1], 11, FontWeight.Bold, align = TextAlign.Center); Sub(if (got != null && tiers.isNotEmpty()) tiers.getOrElse(got.tier - 1) { tiers.last() } else if (got != null) "Earned" else "Locked")
            } }
        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
    } }
}

@Composable
fun CrewRecords(vm: OverloadViewModel, c: Ctx) = OvCard {
    val ms = vm.crew.crewSum().ms
    Row { H3("Crew records", Modifier.weight(1f)); Sub("Best ever") }
    if (ms.isEmpty()) { Sub("Records show up once crew members log lifts."); return@OvCard }
    data class B(val v: Double, val w: Double, val r: Int, val by: String)
    val best = HashMap<String, B>()
    ms.forEach { m -> m.profile!!.lifts.forEach { (n, l) -> val v = vm.crew.mine(l.e, m.profile.unit); if (best[n] == null || v > best[n]!!.v) best[n] = B(v, vm.crew.mine(l.w, m.profile.unit), l.r, m.id) } }
    best.entries.sortedByDescending { it.value.v }.take(6).forEach { (n, b) -> RecRow(n, vm.crew.member2(b.by)?.name ?: "", "${b.w.roundToInt()}×${b.r}") }
    val wk = ms.map { m -> m to maxOf(vm.crew.mine(m.profile!!.week?.vol ?: 0.0, m.profile.unit), vm.crew.mine(m.profile.last?.vol ?: 0.0, m.profile.unit)) }.maxByOrNull { it.second }
    if (wk != null && wk.second > 0) RecRow("Biggest week", wk.first.name, "${kfmt(wk.second)} ${c.unit}")
    ms.maxByOrNull { it.profile!!.workouts }?.let { RecRow("Most workouts", it.name, "${it.profile!!.workouts}") }
}
@Composable fun RecRow(a: String, b: String, v: String) = Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
    OvText(a, 14, modifier = Modifier.weight(1f)); Sub(b, Modifier.padding(horizontal = 8.dp)); OvText(v, 14, FontWeight.Bold) }

@Composable
fun SeasonWall(vm: OverloadViewModel, s: CrewState, c: Ctx) = OvCard {
    val start = seasonRange(seasonOf(today())).first.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    var big: Triple<Double, String, String>? = null; var bigR = 0; var hy: Pair<Int, String>? = null
    s.posts.filter { it.ts >= start && it.type == "workout" }.forEach { p ->
        p.data.top.forEach { t -> val w = toMine(t.w, p.data.unit, c.unit); if (t.pr > 0 && (big == null || w > big!!.first)) { big = Triple(w, t.n, p.by); bigR = t.r } }
        val n = p.reacts.values.sumOf { it.size }; if (n > 0 && (hy == null || n > hy!!.first)) hy = n to p.by }
    val stk = vm.crew.crewSum().ms.maxByOrNull { it.profile!!.streak }
    Row { H3("Season ${seasonOf(today())} wall", Modifier.weight(1f)); Sub("Best moments") }
    WallTile("🏆", "Biggest PR", big?.let { "${it.second} ${it.first.roundToInt()}×$bigR" } ?: "No PRs posted yet", big?.third?.let { vm.crew.member2(it)?.name })
    WallTile("🔥", "Longest streak", if ((stk?.profile?.streak ?: 0) > 0) "${stk!!.profile!!.streak} weeks" else "–", if ((stk?.profile?.streak ?: 0) > 0) stk!!.name else null)
    WallTile("📣", "Most hyped post", hy?.let { "${it.first} reactions" } ?: "–", hy?.second?.let { vm.crew.member2(it)?.name })
}
@Composable private fun WallTile(icon: String, t: String, v: String, who: String?) = Row(Modifier.fillMaxWidth().padding(top = 8.dp).background(ov.soft, RoundedCornerShape(14.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
    OvText(icon, 24); Spacer(Modifier.width(10.dp)); Column { Sub(t); OvText(v, 15, FontWeight.Bold); Sub(who ?: "Up for grabs") } }

@Composable
fun JokeAwards(vm: OverloadViewModel, s: CrewState) {
    val ms = vm.crew.crewSum().ms
    if (ms.size >= 2) OvCard {
        val wk0 = System.currentTimeMillis() - 7 * 86_400_000L
        val grunts = ms.maxByOrNull { it.profile!!.last?.sets ?: 0 }
        val talk = HashMap<String, Int>(); s.crew.chatRecent.forEach { if (it.ts > wk0) talk[it.by] = (talk[it.by] ?: 0) + 1 }; s.posts.forEach { if (it.ts > wk0 && it.by.isNotEmpty()) talk[it.by] = (talk[it.by] ?: 0) + 1 }
        val tk = talk.entries.maxByOrNull { it.value }
        val legs = ms.associate { it.id to 0 }.toMutableMap(); val lr = Regex("quad|ham|glute|calf|adduct|leg", RegexOption.IGNORE_CASE)
        s.posts.forEach { p -> if (p.type == "workout" && p.ts > System.currentTimeMillis() - 14 * 86_400_000L && legs[p.by] != null) legs[p.by] = legs[p.by]!! + p.data.levels.keys.count { lr.containsMatchIn(it) } }
        val skip = legs.entries.minByOrNull { it.value }
        Row { H3("This week's awards", Modifier.weight(1f)); Sub("Totally official") }
        JA("🗣", "Most Grunts", grunts?.name, "${grunts?.profile?.last?.sets ?: 0} sets last week")
        JA("📢", "Biggest Talker", tk?.let { vm.crew.member2(it.key)?.name }, tk?.let { "${it.value} posts and messages" } ?: "quiet week")
        JA("🦵", "Most Likely to Skip Legs", skip?.let { vm.crew.member2(it.key)?.name }, if (skip?.value == 0) "no leg day posted in 2 weeks" else "barely any legs lately")
    }
    AwardsCardPublic(vm, s)
}
@Composable private fun JA(i: String, t: String, who: String?, why: String) = Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    OvText(i, 24); Spacer(Modifier.width(10.dp)); Column { OvText(t, 15, FontWeight.Bold); Sub("${who ?: "Nobody yet"} · $why") } }

@Composable
fun RaidWins(s: CrewState) { val r = s.raid ?: return; if (r.won.isEmpty()) return
    OvCard { Row { H3("Bosses defeated", Modifier.weight(1f)); Sub("${r.won.size}") }
        FlowRowC(Modifier.padding(top = 6.dp)) { r.won.takeLast(8).reversed().forEach { w -> Column(Modifier.background(ov.soft, RoundedCornerShape(12.dp)).padding(8.dp)) { OvText(w.name, 13, FontWeight.Bold); Sub(w.wk) } } } }
}

@Composable
fun HallOfFame(vm: OverloadViewModel, s: CrewState) = OvCard {
    val own = vm.crew.isOwner()
    Row { H3("🏛 Hall of Fame", Modifier.weight(1f)); Sub("${s.hof.size}") }
    if (s.hof.isEmpty()) Sub("Legendary sessions and members live here forever. " + if (own) "Induct a post from its ⋯ button or a member from their profile." else "The crew owner inducts them.")
    s.hof.forEach { x -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(if (x.kind == "member") "⭐" else "🏋️", 20); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) { OvText(if (x.kind == "member") x.name else vm.crew.member2(x.who)?.name ?: "?", 15, FontWeight.Bold)
            Sub(x.text + (x.data?.let { " · ${it.day} ${if (it.vol > 0) kfmt(toMine(it.vol, it.unit, vm.unit)) + " " + vm.unit else ""}" } ?: "")) }
        if (own) OvText("✕", 16, color = ov.mute, modifier = Modifier.clickable { vm.crew.hofRemove(x.id) }.padding(6.dp))
    } }
}

@Composable
fun CrewCardButton(vm: OverloadViewModel, s: CrewState) {
    val ctx = LocalContext.current
    OvButton("📸 Share crew card", { shareCrewCard(ctx, vm, s) }, Modifier.padding(bottom = 14.dp), wide = true)
}

// ============================ BOSS RAID (0.15) ============================
@Composable
fun RaidCard(vm: OverloadViewModel, s: CrewState) {
    val r = s.raid ?: return; val me = vm.myId(); val mine = r.hits[me] ?: 0.0; val pct = minOf(1.0, r.dmg / maxOf(1.0, r.hp))
    val left = maxOf(0, java.time.temporal.ChronoUnit.DAYS.between(today(), toD(r.wk.ifEmpty { iso(today()) }).plusDays(7)).toInt())
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(hex("#1b0f2e"), hex(if (r.beaten) "#16b65b" else "#3b1d4a"))))
        .clickable { vm.openSheet(Sheet.RaidInfo) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        BossView(r.boss, Modifier.size(70.dp)); Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            OvText(if (r.beaten) "BOSS DEFEATED" else "WEEKLY BOSS RAID · $left day${if (left == 1) "" else "s"} left", 11, FontWeight.ExtraBold, Color.White.copy(.75f))
            Title(r.name, 20, Color.White)
            Box(Modifier.padding(vertical = 5.dp).fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(.2f))) {
                Box(Modifier.fillMaxWidth(if (r.beaten) 0f else (1 - pct).toFloat()).fillMaxHeight().background(hex("#ff3d4a"))) }
            OvText(if (r.beaten) "Loot unlocked for everyone who hit it" else "${kfmt(r.hp - r.dmg)} lb HP left · you hit ${kfmt(mine)}", 12, color = Color.White.copy(.85f))
        }
    }
}
@Composable
fun RaidSheet(vm: OverloadViewModel) {
    val cs by vm.crewState.collectAsState(); val r = cs?.raid ?: run { Sub("No boss this week."); return }
    SheetHead(r.name) { vm.closeSheet() }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { BossView(r.boss, Modifier.size(140.dp)) }
    Sub((if (r.beaten) "Defeated! " else "") + "Every pound your crew lifts this week does damage. HP grows with crew size.", Modifier.fillMaxWidth().padding(vertical = 6.dp))
    Bar(if (r.beaten) 0.0 else 1 - minOf(1.0, r.dmg / maxOf(1.0, r.hp)), hex("#ff3d4a"), h = 12)
    OvText("${kfmt(r.dmg)} / ${kfmt(r.hp)} lb", 14, FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), align = TextAlign.Center)
    OvText("DAMAGE", 13, FontWeight.ExtraBold, ov.mute)
    val rows = r.hits.entries.sortedByDescending { it.value }
    if (rows.isEmpty()) Sub("No hits yet. Go lift.")
    rows.forEachIndexed { i, (id, v) -> RecRow((if (i == 0) "🥇 " else "") + (vm.crew.member2(id)?.name ?: "?"), "", "${kfmt(v)} lb") }
    OvText("LOOT", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 12.dp))
    Sub("Beat it before Monday: +300 crew XP, a mystery box for everyone who hit it, and the Boss Slayer plate. Raids won: ${r.wins}")
}

// ============================ LIFT TOGETHER (0.15) ============================
@Composable
fun GroupBar(vm: OverloadViewModel, now: Long) {
    val cs by vm.crewState.collectAsState(); val s = cs ?: return; if (!vm.crew.on) return
    val g0 by grpState.collectAsState(); val gid = vm.crew.grp(); val live by vm.live.collectAsState(); val me = vm.myId()
    if (gid != null && s.groups[gid] == null && s.groups.isNotEmpty()) LaunchedEffect(gid) { vm.crew.setGrp(null) }
    if (gid == null || s.groups[gid] == null) {
        s.groups.entries.firstOrNull { me !in it.value.members }?.let { (g, x) ->
            CardButton("👥", "${vm.crew.member2(x.host)?.name ?: "Someone"} is lifting${if (x.members.size > 1) " with ${x.members.size - 1} more" else ""}", "Tap to lift together") { vm.crew.groupJoin(g) } }
        return
    }
    val g = s.groups.getValue(gid)
    OvCard {
        Row { OvText("👥 Lifting together · ${g.members.size}", 15, FontWeight.Bold, modifier = Modifier.weight(1f)); Sub("${maxOf(1, ((now - g.ts) / 60000).toInt())} min") }
        g.members.forEach { id -> val m = vm.crew.member2(id); val l = if (id == me) null else live[id]
            val myL = if (id == me) vm.crew.grpLive() else null
            val sets = l?.sets ?: myL?.get("sets")?.toString()?.toIntOrNull() ?: 0
            val rest = l?.rest ?: (myL?.get("rest")?.toString()?.toLongOrNull() ?: 0L); val ex = l?.ex ?: myL?.get("ex")?.toString()?.trim('"') ?: ""
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(vm, id, m?.name ?: "?", 34); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) { OvText(if (id == me) "You" else m?.name ?: "?", 14, FontWeight.Bold); Sub("$sets sets") }
                val resting = rest > now
                OvText(if (resting) "Resting ${fmtLeft(rest - now)}" else ex.ifEmpty { "Warming up" }, 13, FontWeight.Bold, if (resting) Blue else ov.mute)
            } }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton("Finish together", { vm.crew.groupEnd() }, bg = Red, fg = Color.White); OvButton("Leave", { vm.crew.groupLeave() })
        }
    }
}
fun fmtLeft(ms: Long): String { val s = maxOf(0L, (ms + 999) / 1000); return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

@Composable
fun GroupSheet(vm: OverloadViewModel) {
    val cs by vm.crewState.collectAsState(); val s = cs ?: return
    SheetHead("Lift together", "Cancel") { vm.closeSheet() }
    Sub("Start a group when you’re at the gym with crewmates. Everyone sees each other’s sets and rest timers live, and you finish with one shared workout card.")
    s.groups.forEach { (g, x) -> CardButton("👥", "${vm.crew.member2(x.host)?.name ?: "?"}’s group", x.members.joinToString(", ") { vm.crew.member2(it)?.name ?: "?" }) { vm.crew.groupJoin(g) } }
    OvButton("Start a group", { vm.crew.groupStart() }, Modifier.padding(top = 10.dp), bg = Red, fg = Color.White, wide = true)
    Sub("Your crew gets a ping so they can join.", Modifier.padding(top = 8.dp))
}

@Composable
fun GroupPostBody(vm: OverloadViewModel, p: Post) {
    val d = p.data; val rows = d.rows.sortedByDescending { it.vol }; val tot = rows.sumOf { it.vol }
    Sub("Lifted together · ${d.min} min · ${ago(p.ts)}", Modifier.padding(top = 6.dp))
    OvText(rows.joinToString(" + ") { vm.crew.member2(it.id)?.name ?: "?" }, 15, FontWeight.Bold)
    rows.forEachIndexed { i, r -> RecRow((if (i == 0) "🥇 " else "") + (vm.crew.member2(r.id)?.name ?: "?"), "${r.sets} sets${if (r.prs > 0) " · ${r.prs} PR" else ""}", "${kfmt(toMine(r.vol, "lb", vm.unit))} ${vm.unit}") }
    Sub("Together: ${kfmt(toMine(tot, "lb", vm.unit))} ${vm.unit}", Modifier.padding(top = 4.dp))
}

// ============================ CREW SHEETS (0.14-0.15) ============================
@Composable
fun CrestSheet(vm: OverloadViewModel, start: Crest) {
    var c by remember { mutableStateOf(start) }; val R = vm.crew.crewRankInfo()
    SheetHead("Crew crest", "Cancel") { vm.closeSheet() }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CrestView(c, R.r, Modifier.size(120.dp)) }
    Lbl("SHAPE"); FlowRowC { listOf("shield", "plate", "circle", "hex").forEach { k -> Seg(k.replaceFirstChar { it.uppercase() }, c.shape == k, { c = c.copy(shape = k) }) } }
    Lbl("ICON"); FlowRowC { CREST_ICON.keys.forEach { k -> Seg(k.replaceFirstChar { it.uppercase() }, c.icon == k, { c = c.copy(icon = k) }) } }
    Lbl("COLORS"); FlowRowC { CREST_COLORS.forEach { (a, b) -> Box(Modifier.size(38.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(hex(a), hex(b))))
        .then(if (c.c1 == a) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { c = c.copy(c1 = a, c2 = b) }) } }
    Lbl("INITIALS"); TextBox(c.txt, { c = c.copy(txt = it.uppercase().take(3)) }, "TF")
    Sub("The crest gets fancier as your crew ranks up: borders at Silver, gold trim at Platinum, a glow at Titan and laurels at Olympian.", Modifier.padding(top = 6.dp))
    OvButton("Save crest", { vm.crew.saveCrest(c) }, Modifier.padding(top = 10.dp), bg = Red, fg = Color.White, wide = true)
}
@Composable fun Lbl(t: String) = OvText(t, 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 12.dp, bottom = 6.dp))

@Composable
fun CrewRankSheet(vm: OverloadViewModel) {
    val R = vm.crew.crewRankInfo()
    SheetHead("Crew rank") { vm.closeSheet() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Emblem(R.r, Modifier.size(120.dp), spin = true); Title(RANKS[R.r].name, 36)
        Sub("${loc(R.pts)} crew points" + (R.nx?.let { " · ${loc(it - R.pts)} to ${RANKS[R.r + 1].name}" } ?: " · Top rank"))
    }
    Bar(R.frac, Green, Modifier.padding(vertical = 10.dp))
    Sub("Crew points come from crew XP (every workout and post) plus 400 for each crew trophy. Higher ranks unlock better banners and a fancier crest.")
}

/** Owner tools in the crew settings sheet: name, banner, motto and colours, backups. */
@Composable
fun CrewOwnerExtras(vm: OverloadViewModel, s: CrewState) {
    var nm by remember { mutableStateOf(s.crew.name) }; var motto by remember { mutableStateOf(s.style?.motto ?: "") }; var col by remember { mutableStateOf(s.style?.c1 ?: "") }
    H3("Crew name", Modifier.padding(top = 14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TextBox(nm, { nm = it.take(30) }, "Crew name", Modifier.weight(1f)); OvButton("Rename", { if (nm.isBlank()) vm.toast("Enter a name") else vm.crew.rename(nm.trim()) }) }
    H3("Crew banner", Modifier.padding(top = 14.dp))
    val cur = LEGACY_SCENE[s.crew.banner] ?: s.crew.banner
    SCENES.entries.filter { it.key != "founder" || sceneOK(s, "founder") }.chunked(2).forEach { row -> Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        row.forEach { (k, v) -> val ok = sceneOK(s, k)
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).then(if (cur == k) Modifier.border(3.dp, ov.ink, RoundedCornerShape(14.dp)) else Modifier).clickable { vm.crew.setScene(k) }) {
                Box { Scene(k, Modifier.fillMaxWidth().alpha(if (ok) 1f else .5f), s.crew.level, "pick$k") }
                OvText(v.first + if (ok) "" else " · 🔒 ${v.second}", 11, FontWeight.Bold, modifier = Modifier.padding(6.dp))
            } }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    } }
    H3("Crew motto and colors", Modifier.padding(top = 14.dp))
    TextBox(motto, { motto = it.take(60) }, "No days off")
    FlowRowC(Modifier.padding(vertical = 8.dp)) { listOf("#ef2b36", "#2563ff", "#16b65b", "#7c4dff", "#c99a2e", "#06b6d4", "#ff3d8b", "#e8e8ec").forEach { x ->
        Box(Modifier.size(34.dp).clip(CircleShape).background(hex(x)).then(if (col == x) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { col = x }) } }
    OvButton("Save motto and colors", { vm.crew.saveStyle(motto.trim(), col) }, small = true)
    H3("Safety", Modifier.padding(top = 14.dp))
    OvButton("Crew backups (30 days)", { vm.openSheet(Sheet.CrewSnaps); vm.crew.loadSnaps() }, small = true)
}

@Composable
fun CrewSnapsSheet(vm: OverloadViewModel, u: UiState) {
    val days by crewSnaps.collectAsState()
    SheetHead("Crew backups") { vm.closeSheet() }
    Sub("The server saves your whole crew every day and keeps 30 days. Restoring only adds back what’s missing. Nothing current gets removed.")
    if (days == null) Sub("Loading…", Modifier.padding(top = 8.dp))
    else FlowRowC(Modifier.padding(top = 8.dp)) { days!!.forEach { d -> OvButton(if (u.confirm == "cs$d") "Tap to restore $d" else d, { if (u.confirm != "cs$d") vm.setConfirm("cs$d") else { vm.setConfirm(null); vm.crew.restoreSnap(d) } }, small = true) } }
}

@Composable
fun HofPostSheet(vm: OverloadViewModel, c: Ctx, postId: String) {
    val p = vm.crewState.value?.posts?.firstOrNull { it.id == postId }; var t by remember { mutableStateOf(p?.cap ?: "") }
    SheetHead("Hall of Fame", "Cancel") { vm.closeSheet() }
    if (p != null) OvCard { PostBody(c, p, true) }
    Lbl("WHY IT’S LEGENDARY"); TextBox(t, { t = it.take(80) }, "First 405 deadlift in crew history")
    OvButton("Induct", { vm.crew.hofPost(postId, t.trim()) }, Modifier.padding(top = 12.dp), bg = Red, fg = Color.White, wide = true)
}

/** Shame plate vote (skipping leg day), inside a crewmate's profile. */
@Composable
fun ShameButton(vm: OverloadViewModel, id: String) {
    val s = vm.crewState.value ?: return; if (id == vm.myId() || s.members.size < 2) return
    if (vm.crew.shamed(id)) { Sub("🩷 Wearing the shame plate this week. The crew has spoken.", Modifier.padding(top = 12.dp)); return }
    val v = s.crew.shameVotes; val mine = v?.mine == id; val n = v?.tally?.get(id) ?: 0; val need = maxOf(1, Math.ceil((s.members.size - 1) / 2.0).toInt())
    OvButton("${if (mine) "You voted" else "🩷 Vote shame plate"} · $n/$need", { vm.crew.shameVote(id) }, Modifier.padding(top = 12.dp), small = true)
    Sub("For skipping leg day. Lasts a week. All in good fun.")
}

/** Draws and shares the crew card image (1080 x 1350). */
fun shareCrewCard(ctx: android.content.Context, vm: OverloadViewModel, s: CrewState) {
    runCatching {
        val c = s.crew; val R = vm.crew.crewRankInfo(); val S = vm.crew.crewSum(); val W = 1080; val H = 1350
        val bmp = android.graphics.Bitmap.createBitmap(W, H, android.graphics.Bitmap.Config.ARGB_8888); val cv = android.graphics.Canvas(bmp)
        cv.drawColor(android.graphics.Color.parseColor("#0b0c10"))
        val bg = android.graphics.Paint().apply { shader = android.graphics.LinearGradient(0f, 0f, W.toFloat(), 420f, android.graphics.Color.parseColor(c.crest?.c1 ?: "#ef2b36"), android.graphics.Color.parseColor("#0b0c10"), android.graphics.Shader.TileMode.CLAMP) }
        cv.drawRect(0f, 0f, W.toFloat(), 420f, bg)
        fun txt(t: String, y: Float, size: Float, col: String, bold: Boolean = true, x: Float = W / 2f) = cv.drawText(t, x, y, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor(col); textSize = size; textAlign = android.graphics.Paint.Align.CENTER; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL) })
        txt("🛡", 380f, 200f, "#ffffff")
        txt(c.name, 580f, 84f, "#ffffff"); txt("${RANKS[R.r].name} crew · Level ${c.level}${if (c.founders) " · Founders" else ""}", 632f, 34f, "#9aa0ae", false)
        listOf(kfmt(S.vol) to "${vm.unit} lifted", "${S.wo}" to "workouts", "${c.trophies.size}" to "trophies", "${s.members.size}" to "members").forEachIndexed { i, (v, l) ->
            val x = W / 2f + (i - 1.5f) * 250f; txt(v, 800f, 72f, "#ffffff", x = x); txt(l, 846f, 30f, "#9aa0ae", false, x) }
        txt("Join with code ${c.code}", 1040f, 40f, "#ef2b36"); txt("OVERLOAD", 1260f, 30f, "#6b7080")
        val dir = java.io.File(ctx.cacheDir, "share").apply { mkdirs() }; val f = java.io.File(dir, "crew-card.png")
        f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), s.crew.name))
    }.onFailure { vm.toast("Could not make the card") }
}
