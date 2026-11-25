package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ============================ TODAY ============================
/** One card on Today that shows the most useful next thing (crew live, post, trial, goals…). */
@Composable
fun SmartCard(vm: OverloadViewModel, c: Ctx, u: UiState) {
    if (u.date != LocalDate.now()) return
    val s = c.sessOn(LocalDate.now()); if (s?.start != null && s.end == null) return
    val e = c.eng; val me = vm.myId(); val live by vm.live.collectAsState(); val crewOn = vm.crew.on
    val list = mutableListOf<Triple<String, String, Pair<String, String>>>()
    live.entries.firstOrNull { it.key != me && vm.crew.member(it.key) != null }?.let { (id, l) -> list += Triple("crew", "🔥", "${vm.crew.member(id)!!.name} is lifting right now" to "${l.day} · tap to send some hype") }
    if (crewOn) vm.crew.lastFinished()?.let { if (!it.posted) list += Triple("post", "📣", "Post your workout to the crew" to "Share your muscle map and stats") }
    e.trial?.let { t -> list += Triple("rank", "⚔️", "Promotion trial: ${RANKS[t.tier].name}" to "${maxOf(0, t.need - t.days)} more training day${if (t.need - t.days == 1) "" else "s"} and ${maxOf(0, t.needBeat - t.beat)} more lift${if (t.needBeat - t.beat == 1) "" else "s"} to beat this week") }
    if (crewOn && vm.crewState.value != null && LocalDate.now().dayOfWeek.value in 1..3 && c.st.recapSeen != vm.crew.curWk()) list += Triple("crew", "📊", "Your crew’s weekly recap is in" to "See last week’s MVP and Most Improved")
    c.st.goals.filter { it.doneAt == null }.forEach { g -> if (c.goalInfo(g).status == "behind") list += Triple("stats", "🎯", "${c.exName(g.ex)} ${fmtN(g.w)} is behind pace" to "Open Goals to see your projection") }
    if (c.feat("quests") && c.st.chains.isNotEmpty()) c.chains().first.firstOrNull()?.let { q -> q.steps.firstOrNull { !it.done }?.let { n -> list += Triple("stats", "🗺️", "Road to ${q.m}: next step" to "${q.name} ${n.w}×${n.r}") } }
    if (crewOn) vm.crew.chalProgress()?.let { cp -> if (cp.cur < cp.target) list += Triple("crew", "🏁", cp.title to "${loc(cp.cur)} of ${loc(cp.target)} as a crew") }
    if (c.feat("season") && e.lvl < 30) list += Triple("rank", "🎖️", "Season level ${e.lvl}" to "${200 - e.xp % 200} XP to level ${e.lvl + 1}")
    val x = list.firstOrNull() ?: return
    CardButton(x.second, x.third.first, x.third.second) {
        when (x.first) {
            "post" -> vm.crew.lastFinished()?.let { vm.openSheet(Sheet.PostWorkout(sid(it.date, it.day))) }
            "rank" -> vm.openProfile()
            "crew" -> { vm.setTab("crew"); vm.setSub("crew", "feed") }
            else -> { vm.setTab("stats"); vm.setSub("stats", "ov") }
        }
    }
}

/** Muscle map, fun stats and badges after finishing a workout. */
@Composable
fun Analysis(vm: OverloadViewModel, c: Ctx, s: Session, u: UiState) {
    if (vm.crew.on && !s.posted && u.date == LocalDate.now())
        OvButton("📣 Post to crew", { vm.openSheet(Sheet.PostWorkout(sid(s.date, s.day))) }, Modifier.padding(bottom = 12.dp), bg = Red, fg = Color.White, wide = true)
    val m = c.muscleSets(s); val keys = m.keys.sortedByDescending { m[it] }; val mx = maxOf(1, m.values.maxOrNull() ?: 1)
    OvCard {
        OvText("MUSCLES HIT", 13, FontWeight.ExtraBold, ov.mute)
        BodyMap(c.mLevels(m), Modifier.padding(top = 6.dp))
        if (keys.isEmpty()) Sub("Log some sets to light up the map.")
        keys.forEach { k -> Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            OvText(k, 14, modifier = Modifier.width(96.dp))
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(ov.soft)) {
                Box(Modifier.fillMaxWidth(m[k]!!.toFloat() / mx).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFFFF8A3D), Color(0xFFEF2B36)))))
            }
            OvText("${m[k]}", 14, FontWeight.Black, modifier = Modifier.width(30.dp).padding(start = 8.dp))
        } }
    }
    val fs = c.funStats(s)
    if (fs.isNotEmpty()) OvCard {
        OvText("FUN STATS", 13, FontWeight.ExtraBold, ov.mute)
        fs.forEach { (e, t) -> Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) { OvText(e, 24, modifier = Modifier.width(40.dp)); OvText(t, 15) } }
    }
    val today = c.eng.earnedOn(s.date)
    if (today.isNotEmpty()) OvCard { OvText("BADGES EARNED TODAY", 13, FontWeight.ExtraBold, ov.mute); Spacer(Modifier.height(8.dp)); BadgeGrid(today, c.eng.rank >= 2) }
}

/** Daily challenges + rank chip. */
@Composable
fun ChallengesCard(vm: OverloadViewModel, c: Ctx, u: UiState) {
    val e = c.eng; val list = c.dayChallenges(u.date); val n = list.count { it.done }
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OvText("${if (u.date == LocalDate.now()) "TODAY'S" else DN[dayIdx(u.date)].uppercase() + "'S"} CHALLENGES · $n/3", 13, FontWeight.ExtraBold, ov.mute, Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(14.dp)).background(ov.soft).clickable { vm.openProfile() }.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Emblem(e.rank, Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)); OvText(e.rname + if (e.streak > 1) " · 🔥${e.streak}" else "", 13, FontWeight.Bold)
            }
        }
        list.forEach { x ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(12.dp)).then(if (x.man && !x.done) Modifier.clickable { vm.toggleWalk() } else Modifier).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(if (x.done) Green else ov.soft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { if (x.done) OvText("✓", 13, FontWeight.Black, Color.White) }
                Spacer(Modifier.width(10.dp)); OvText(x.t, 15, if (x.done) FontWeight.Normal else FontWeight.SemiBold, if (x.done) ov.mute else ov.ink)
            }
        }
        if (n == 3) OvText("🧹 Daily Sweep earned", 14, FontWeight.Bold, Green, Modifier.padding(top = 8.dp))
        e.B.filter { !it.earned && !it.hid && it.tgt > 1 && it.cur / it.tgt >= .7 }.maxByOrNull { it.cur / it.tgt }?.let { b ->
            OvText("${b.icon} Almost there: ${b.name} · ${loc(b.tgt - b.cur)} to go", 13, FontWeight.SemiBold, ov.mute, Modifier.padding(top = 8.dp))
        }
    }
}

// ============================ STATS ============================
@Composable
fun QuestsCard(c: Ctx) {
    val q = c.weekQuests(today())
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { H3("Weekly quests", Modifier.weight(1f)); Sub("${q.count { it.done }}/3 · resets Monday") }
        q.forEach { x -> Column(Modifier.padding(top = 8.dp)) {
            Row { OvText((if (x.done) "✅ " else "🎯 ") + x.t, 14, FontWeight.SemiBold, modifier = Modifier.weight(1f)); OvText("${loc(x.c)}/${loc(x.n)}", 14, FontWeight.ExtraBold) }
            Bar(x.c / x.n, if (x.done) Green else Blue, Modifier.padding(top = 4.dp))
        } }
    }
}

@Composable
fun GoalsCard(vm: OverloadViewModel, c: Ctx) {
    if (!c.feat("goals")) return
    fun fmt(d: String) = toD(d).let { "${MON[it.monthValue - 1]} ${it.dayOfMonth}" }
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { H3("Goals", Modifier.weight(1f)); OvButton("+ Goal", { vm.openSheet(Sheet.NewGoal) }, small = true) }
        if (c.st.goals.isEmpty()) Sub("Pick a lift, a target and a date. Overload projects when you’ll hit it.")
        c.st.goals.forEachIndexed { i, g -> val inf = c.goalInfo(g)
            val col = when (inf.status) { "done" -> Green; "on" -> Blue; "behind" -> Color(0xFFFF9F1C); else -> Color(0xFF8C939E) }
            Column(Modifier.padding(top = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OvText("${c.exName(g.ex)} ${fmtN(g.w)} ${c.unit}", 15, FontWeight.Bold); Sub("  by ${fmt(g.date)}", Modifier.weight(1f))
                    OvText("✕", 15, color = ov.mute, modifier = Modifier.clickable { vm.removeGoal(i) }.padding(6.dp))
                }
                Bar(if (inf.done != null) 1.0 else inf.pct / 100.0, col, Modifier.padding(vertical = 4.dp))
                Sub(when { inf.done != null -> "🎉 Crushed on ${fmt(inf.done)}"; inf.status == "on" -> "On pace · projected ${fmt(inf.proj!!)}"
                    inf.status == "behind" -> "Behind pace · projected ${fmt(inf.proj!!)}"; else -> "Not enough recent data to project yet. Keep logging" })
            }
        }
    }
}

@Composable
fun ChainsCard(c: Ctx) {
    if (!c.feat("quests")) return
    val list = c.chains().first; if (list.isEmpty()) return
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) { H3("Quest chains", Modifier.weight(1f)); Sub("${c.st.chainsDone.size} done") }
        list.forEach { ch -> Column(Modifier.padding(top = 8.dp)) {
            OvText("🗺️ Road to ${ch.m} · ${ch.name}", 15, FontWeight.Bold)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                ch.steps.forEachIndexed { i, st ->
                    if (i > 0) OvText(" › ", 14, color = ov.mute)
                    Row(Modifier.background(if (st.done) Green.copy(.15f) else ov.soft, RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        OvText(if (st.done) "✓" else "${i + 1}", 12, FontWeight.Black, if (st.done) Green else ov.mute); Spacer(Modifier.width(4.dp)); OvText("${st.w}×${st.r}", 13, FontWeight.Bold)
                    }
                }
            }
        } }
    }
}

@Composable
fun NewGoalSheet(vm: OverloadViewModel, c: Ctx) {
    val cnt = HashMap<String, Int>(); c.rs.forEach { s -> s.ex.forEach { (id, a) -> if (a.any(::full)) cnt[id] = (cnt[id] ?: 0) + 1 } }
    val ids = cnt.keys.sortedByDescending { cnt[it] }
    var ex by remember { mutableStateOf(ids.firstOrNull() ?: "") }; var w by remember { mutableStateOf("") }; var date by remember { mutableStateOf(LocalDate.now().plusMonths(3)) }; var open by remember { mutableStateOf(false) }
    SheetHead("New goal", "Cancel") { vm.closeSheet() }
    if (ids.isEmpty()) { Sub("Log a workout first, then set a goal for one of your lifts."); return }
    OvText("LIFT", 13, FontWeight.ExtraBold, ov.mute)
    Box { OvButton("${c.exName(ex)} ▾", { open = true }, Modifier.padding(vertical = 6.dp), wide = true)
        androidx.compose.material3.DropdownMenu(open, { open = false }) { ids.forEach { id -> androidx.compose.material3.DropdownMenuItem({ androidx.compose.material3.Text(c.exName(id)) }, { ex = id; open = false }) } } }
    OvText("TARGET (${c.unit} for 1 rep)", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 10.dp))
    NumBox(w, "225", { w = it }, Modifier.padding(vertical = 6.dp))
    OvText("BY", 13, FontWeight.ExtraBold, ov.mute, Modifier.padding(top = 10.dp))
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OvButton("−1 mo", { date = date.minusMonths(1).let { if (it <= LocalDate.now()) LocalDate.now().plusWeeks(1) else it } }, small = true)
        OvText("${MON[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}", 16, FontWeight.Bold, modifier = Modifier.weight(1f))
        OvButton("+1 mo", { date = date.plusMonths(1) }, small = true)
    }
    OvButton("Set goal", { vm.addGoal(ex, w.toDoubleOrNull(), iso(date)) }, Modifier.padding(top = 10.dp), bg = Red, fg = Color.White, wide = true)
}
