package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/*
 * 0.22 "Rank up": one place that explains ranking up. Opened from the rank emblem in the top bar,
 * the crew banner's "Next rank" bar and the crew rank chip. Three tabs: You, Crew and Plates.
 * Client only: crew points and trophies come from data the server already sends.
 */

/** One step from your next rank, or in a promotion trial. */
fun Ctx.meNear(): Boolean { val e = eng; if (e.trial != null) return true; val nx = RANKS.getOrNull(e.rank + 1) ?: return false
    val a = nx.weeks - e.active; val b = nx.badges - e.bcount; return (a <= 0 && b <= 1) || (b <= 0 && a <= 1) }
/** The crew is within 100 points of its next rank. */
fun CrewService.crewNear(): Boolean { if (cs == null) return false; val R = crewRankInfo(); return R.nx != null && R.nx - R.pts <= 100 }

@Composable
private fun Bar22(cur: Double, tgt: Double, label: String, sub: String = "", fmt: (Double) -> String = { loc(it) }) {
    val done = cur >= tgt
    Column(Modifier.padding(vertical = 5.dp)) {
        Row { OvText(label, 14, FontWeight.SemiBold, modifier = Modifier.weight(1f)); OvText(if (done) "✓" else "${fmt(cur)}/${fmt(tgt)}", 14, FontWeight.ExtraBold, if (done) Green else ov.ink) }
        Spacer(Modifier.height(4.dp)); Bar(if (tgt > 0) cur / tgt else 0.0, if (done) Green else hex("#e8b923"))
        if (sub.isNotEmpty()) Sub(sub, Modifier.padding(top = 2.dp))
    }
}
@Composable private fun RkCard(hot: Boolean = false, content: @Composable ColumnScope.() -> Unit) =
    OvCard(if (hot) Modifier.border(1.5.dp, hex("#ff7a1a", .6f), RoundedCornerShape(24.dp)) else Modifier, content)
@Composable private fun Glbl(t: String) = OvText(t, 12, FontWeight.ExtraBold, ov.mute, Modifier.padding(bottom = 4.dp))
@Composable private fun RkItem(ic: String, name: String, tag: String, sub: String, won: Boolean = false, body: (@Composable () -> Unit)? = null) =
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp).alpha(if (won) .75f else 1f), verticalAlignment = Alignment.Top) {
        OvText(ic, 22, modifier = Modifier.width(36.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) { OvText(name, 15, FontWeight.Bold, modifier = Modifier.weight(1f)); OvText(tag, 12, FontWeight.ExtraBold, if (won) Green else hex("#c99a2e")) }
            Sub(sub); if (body != null && !won) Box(Modifier.padding(top = 4.dp)) { body() }
        }
    }

@Composable
fun RankUpSheet(vm: OverloadViewModel, c: Ctx, tab: String) {
    val e = c.eng
    SheetHead("Rank up") { vm.closeSheet() }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(ov.card, RoundedCornerShape(16.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("you" to "You", "crew" to "Crew", "plates" to "Plates").forEach { (k, l) -> val dot = (k == "you" && c.meNear()) || (k == "crew" && vm.crew.crewNear())
            Row(Modifier.weight(1f).height(40.dp).clip(RoundedCornerShape(12.dp)).background(if (tab == k) ov.ink else Color.Transparent).clickable { vm.openSheet(Sheet.RankUp(k)) },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                OvText(l, 14, FontWeight.Bold, if (tab == k) ov.bg else ov.mute); if (dot) Box(Modifier.padding(start = 5.dp).size(7.dp).background(hex("#e8b923"), CircleShape)) } }
    }
    when (tab) {
        "crew" -> CrewTab22(vm, c)
        "plates" -> PlatesTab22(vm, c)
        else -> {
            val r = RANKS[e.rank]; val nx = RANKS.getOrNull(e.rank + 1)
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Emblem(e.rank, Modifier.size(84.dp), spin = true); Spacer(Modifier.width(12.dp))
                Column { Sub("YOUR RANK"); Title(e.rname.ifEmpty { r.name }, 26); Sub("${e.power} rank power · 🔥 ${e.streak} wk streak") } }
            val t = e.trial
            if (t != null) RkCard(hot = true) { Glbl("🔥 PROMOTION TRIAL · ${RANKS[t.tier].name.uppercase()}")
                Sub("You’ve earned a shot at ${RANKS[t.tier].name}. Pass it this week to rank up. Miss it and you try again next week.")
                Bar22(t.days.toDouble(), t.need.toDouble(), "Train on your planned days"); Bar22(t.beat.toDouble(), t.needBeat.toDouble(), "Beat last week on lifts") }
            else if (nx != null) RkCard { Glbl("NEXT: ${nx.name.uppercase()}"); Sub("Fill both bars to start a one-week promotion trial.")
                Bar22(e.active.toDouble(), nx.weeks.toDouble(), "Active weeks", "A week counts when you train 2+ times")
                Bar22(e.bcount.toDouble(), nx.badges.toDouble(), "Badges earned", "Every badge counts, weekly ones too") }
            else RkCard { Glbl("TOP RANK"); Sub("You’re ${r.name}. Nothing above this. Keep the streak alive.") }
            RkCard { Glbl("HOW IT WORKS")
                listOf("Train" to "2+ times a week to bank active weeks.", "Earn badges" to "by hitting milestones, PRs and weekly goals.", "Pass the trial:" to "one week on your plan, beating last week on 3 lifts.")
                    .forEachIndexed { i, (a, b) -> Row(Modifier.padding(vertical = 3.dp)) { OvText("${i + 1}. ", 14, FontWeight.Bold); OvText(a, 14, FontWeight.Bold); OvText(" $b", 14) } }
                val u = UNLOCKS[e.rank + 1]; if (nx != null && u != null) Sub("${nx.name} unlocks: $u and the ${nx.name} plate. See Plates.", Modifier.padding(top = 8.dp)) }
        }
    }
}

@Composable
private fun CrewTab22(vm: OverloadViewModel, c: Ctx) {
    val s = vm.crewState.value
    if (s == null) { RkCard { Sub("Join or start a crew to rank up together. Crew ranks unlock better crew banners and a fancier crest.")
        OvButton("Go to Crew", { vm.closeSheet(); vm.setTab("crew") }, Modifier.padding(top = 10.dp), bg = Red, fg = Color.White, wide = true) }; return }
    val R = vm.crew.crewRankInfo(); val cr = s.crew; val S = vm.crew.crewSum(); val me = vm.myId(); val ms = s.members.filter { it.profile != null }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CrestView(cr.crest, R.r, Modifier.size(72.dp), holEmoji(s)); Spacer(Modifier.width(12.dp))
        Column { Sub(cr.name.uppercase()); Title(RANKS[R.r].name, 26); Sub("${loc(R.pts)} crew points") } }
    RkCard { if (R.nx != null) Bar22(R.pts.toDouble(), R.nx.toDouble(), "Next: ${RANKS[R.r + 1].name}", "${loc(R.nx - R.pts)} points to go") else Sub("Top crew rank. Legends.") }
    fun nm(m: Member) = if (m.id == me) "You" else m.name.split(" ").first()
    @Composable fun per(f: (Member) -> Int, t: Int) = FlowRowC { ms.forEach { m -> val v = f(m); OvText("${nm(m)} ${minOf(v, t)}/$t", 12, FontWeight.Bold, if (v >= t) Green else ov.mute,
        Modifier.background(if (v >= t) Green.copy(.12f) else ov.soft, RoundedCornerShape(8.dp)).padding(horizontal = 7.dp, vertical = 3.dp)) } }
    fun wk(m: Member) = vm.crew.wkOf(m.profile)
    Glbl("CREW TROPHIES · +400 EACH")
    OvCard {
        CTROPHY.forEach { d -> val k = d[0]; val won = cr.trophies.containsKey(k)
            val body: (@Composable () -> Unit)? = when (k) {
                "unbroken" -> { { if (ms.size < 2) Sub("Needs 2+ members in the crew") else per({ wk(it).days }, 1) } }
                "ironweek" -> { { if (ms.size < 2) Sub("Needs 2+ members in the crew") else per({ wk(it).days }, 3) } }
                "prstorm" -> { { Bar22(ms.sumOf { wk(it).prs }.toDouble(), 10.0, "Crew PRs this week") } }
                "fullhouse" -> { { Sub(if (ms.size < 3) "Needs 3+ members · you have ${ms.size}" else "Pick a day you all train") } }
                "century" -> { { Bar22(S.wo.toDouble(), 100.0, "Crew workouts") } }
                "mpc" -> { { Bar22(Math.round(if (c.kg) S.vol * 2.20462 else S.vol).toDouble(), 1e6, "Lifted together (lb)") { v -> if (v >= 1e6) "${fmtN(v / 1e6)}M" else kfmt(v) } } }
                else -> null
            }
            RkItem(d[2], d[1], if (won) "Won ✓" else "+400", d[4], won, body)
        }
    }
    Glbl("EVERY WEEK")
    OvCard {
        val r = s.raid
        RkItem("👹", "Beat the boss raid", "+300", if (r == null) "A new boss shows up every Monday" else if (r.beaten) "Beaten this week ✓" else "${r.name} · ${kfmt(maxOf(0.0, r.hp - r.dmg))} HP left. Every pound you lift hits it.")
        RkItem("👯", "Lift together", "+25 each", "Start a group workout with 2+ of you")
        RkItem("🏋️", "Every workout", "+10", "Anyone in the crew, signed in and synced")
        RkItem("📣", "Post a workout", "+5", "Share it to the crew feed")
    }
    Sub("Higher crew ranks unlock crew banners and a fancier crest.", Modifier.fillMaxWidth())
}

@Composable
private fun PlatesTab22(vm: OverloadViewModel, c: Ctx) {
    val e = c.eng
    Sub("Plates you unlock by ranking up. Lit ones are yours.", Modifier.padding(bottom = 10.dp))
    for (i in 1 until RANKS.size) {
        val pl = PLATE_LOCK.filter { it.value == i }.keys + FIN.values.filter { it.k.startsWith("r_") && it.how == "${RANKS[i].name} rank" }.map { it.k }
        val got = e.rank >= i; val next = e.rank + 1 == i
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(18.dp)).background(if (next) hex("#e8b923", .12f) else ov.card)
            .then(if (next) Modifier.border(1.5.dp, hex("#e8b923", .6f), RoundedCornerShape(18.dp)) else Modifier).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Emblem(i, Modifier.size(44.dp).alpha(if (got || next) 1f else .55f)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { OvText(RANKS[i].name, 16, FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (got) OvText("Unlocked ✓", 12, FontWeight.ExtraBold, Green) else if (next) OvText("Next", 12, FontWeight.ExtraBold, hex("#c99a2e")) }
                UNLOCKS[i]?.let { Sub(it) }
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { pl.forEach { k -> val own = if (FIN[k] != null) c.finOwned(k) else c.plateLocked(k).isEmpty()
                    Box(Modifier.size(36.dp).alpha(if (own) 1f else .4f).clickable { vm.toast(if (FIN[k] != null) "${plateName(k)} · ${if (own) "Unlocked" else FIN[k]!!.how}" else c.plateLocked(k).let { l -> if (l.isEmpty()) "Unlocked" else "Unlocks at $l" }) }) {
                        Plate(k, "done", Modifier.fillMaxSize()) } } }
            }
        }
    }
}
