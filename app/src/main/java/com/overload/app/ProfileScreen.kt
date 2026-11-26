package com.overload.app

import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ============================ PROFILE PIECES ============================
// (0.16: the profile page itself is ProfileTab in Profile16.kt)
@Composable
fun LadderTilePub(vm: OverloadViewModel, e: Eng, cat: String, modifier: Modifier) = LadderTile(vm, e, cat, modifier)

@Composable
private fun LadderTile(vm: OverloadViewModel, e: Eng, cat: String, modifier: Modifier) {
    val list = e.B.filter { it.cat == cat }; val n = list.count { it.earned }; val (nm, ic) = LADDER[cat]!!
    val tier = if (n > 0) TIERS[minOf(n, 6) - 1] else null; val next = list.getOrNull(n)
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(ov.soft).clickable { vm.openSheet(Sheet.Ladder(cat)) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OvText(ic, 26, modifier = Modifier.alpha(if (n > 0) 1f else .4f))
        OvText("$nm ${ROMAN.getOrElse(n) { "" }}", 12, FontWeight.Bold, align = TextAlign.Center)
        OvText(tier?.first ?: "Locked", 11, FontWeight.Bold, tier?.let { Color(it.second) } ?: ov.mute)
        if (next != null) Bar(next.cur / next.tgt, tier?.let { Color(it.second) } ?: Color(0xFF8C939E), Modifier.padding(top = 4.dp), 4) else Sub("Maxed")
    }
}

@Composable
private fun SeasonCard(vm: OverloadViewModel, e: Eng) {
    val (_, end) = seasonRange(e.season); val left = maxOf(0, ChronoUnit.DAYS.between(today(), end).toInt() + 1)
    val nextR = PASS.firstOrNull { it.first.first > e.lvl }
    OvCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { OvText("SEASON ${e.season} · $left DAYS LEFT", 13, FontWeight.ExtraBold, ov.mute); Row(verticalAlignment = Alignment.Bottom) { Title("Level ${e.lvl}", 22); Sub(" of 30") } }
            OvButton("Season pass", { vm.openSheet(Sheet.Pass) }, small = true)
        }
        Bar(if (e.lvl >= 30) 1.0 else (e.xp % 200) / 200.0, Color(0xFF7C4DFF), Modifier.padding(vertical = 8.dp))
        Sub(if (e.lvl >= 30) "Season maxed. Legend. 🏛️" else "${200 - e.xp % 200} XP to level ${e.lvl + 1}" + (nextR?.let { " · Next reward at ${it.first.first}: ${it.second}" } ?: ""))
        OvText("XP comes from daily challenges, weekly quests, workouts, PRs and weekly badges. Not from extra training days.", 12, color = ov.mute, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun LiftRanksCard(vm: OverloadViewModel, c: Ctx) {
    val list = c.liftRanks(); if (list.isEmpty()) return
    val bw = c.st.bw; var bwIn by remember { mutableStateOf("") }
    OvCard {
        H3("Lift ranks"); Sub("Each lift is ranked by your estimated 1-rep max compared with your bodyweight. These are general standards, so treat them as a guide.")
        if (bw <= 0) Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumBox(bwIn, "Bodyweight (${c.unit})", { bwIn = it }, Modifier.weight(1f), size = 16, h = 44.dp); OvButton("Save", { vm.saveBw(bwIn.toDoubleOrNull()) }, bg = Red, fg = Color.White)
        }
        list.chunked(2).forEach { row ->
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { x -> val t = if (x.t > 0) TIERS[x.t - 1] else null; val col = t?.let { Color(it.second) } ?: Color(0xFF8C939E)
                    Row(Modifier.weight(1f).background(ov.soft, RoundedCornerShape(16.dp)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(col, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { OvText(if (x.t > 0) ROMAN[x.t] else "–", 13, FontWeight.Black, Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Column { OvText(x.name, 14, FontWeight.Bold); Sub((t?.first ?: if (bw > 0) "Unranked" else "Add bodyweight") + if (bw > 0) " · ${String.format("%.2f", x.ratio)}×" else "")
                            if (x.nextW > 0) Sub("Next: ${x.nextW}×1") else if (x.t >= 6) Sub("Legend") }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MasteryCard(c: Ctx) {
    val list = c.mastery(); val colors = HashMap<String, Color>()
    list.forEach { x -> if (x.t > 0) MKEY[x.k]?.forEach { colors[it] = Color(TIERS[x.t - 1].second) } }
    OvCard {
        H3("Muscle mastery"); Sub("Every set levels up the muscle it hits, from Bronze to Legend.", Modifier.padding(bottom = 6.dp))
        BodyMap(emptyMap(), colors = colors, legend = false)
        if (list.isEmpty()) Sub("Log a workout to start.")
        list.forEach { x -> val col = if (x.t > 0) Color(TIERS[x.t - 1].second) else Color(0xFF8C939E)
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OvText(x.k, 14, modifier = Modifier.width(96.dp)); Bar(if (x.next != null) x.n.toDouble() / x.next else 1.0, col, Modifier.weight(1f))
                OvText(if (x.t > 0) TIERS[x.t - 1].first else "—", 13, FontWeight.Bold, col, Modifier.width(76.dp), align = TextAlign.End)
            }
        }
    }
}

// ============================ PROFILE SHEETS ============================
@Composable
fun BadgeInfoSheet(vm: OverloadViewModel, c: Ctx, k: String) {
    val b = c.eng.badge(k) ?: return; val got = b.got; val hidden = b.hid && !got
    val (rr, rn) = if (b.count != null) "Weekly" to "" else c.rarity(b)
    val rc = mapOf("Common" to 0xFF8C939E, "Rare" to 0xFF2F7DE1, "Epic" to 0xFF8B3DFF, "Legendary" to 0xFFE8A10B)[rr] ?: 0xFF8C939E
    val pinned = k in c.st.showcase
    SheetHead(if (hidden) "Hidden badge" else b.name) { vm.closeSheet() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        OvText(if (hidden) "❔" else b.icon, 64, modifier = Modifier.alpha(if (got) 1f else .45f))
        Chip(rr, Color(rc), Color.White); if (rn.isNotEmpty()) Sub(rn)
        OvText(if (hidden) "Keep training to discover it." else b.desc, 16, align = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
        Sub(if (b.count != null) "Earned ${b.count} time${if (b.count == 1) "" else "s"}" else if (got) "Earned ✓" else if (b.tgt > 1) "${loc(b.cur)} / ${loc(b.tgt)}" else "")
    }
    if (got) OvButton(if (pinned) "Unpin from showcase" else "📌 Pin to showcase", { vm.togglePin(k) }, Modifier.padding(top = 12.dp), bg = if (pinned) ov.soft else Red, fg = if (pinned) ov.ink else Color.White, wide = true)
}

@Composable
fun LadderSheet(vm: OverloadViewModel, c: Ctx, cat: String) {
    val list = c.eng.B.filter { it.cat == cat }; val (nm, ic) = LADDER[cat] ?: return
    SheetHead("$ic $nm") { vm.closeSheet() }
    list.forEachIndexed { i, b ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if (b.earned) 1f else .6f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Color(TIERS[i].second), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { OvText(ROMAN[i + 1], 12, FontWeight.Black, Color.White) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { OvText("${TIERS[i].first} · ${b.name}", 15, FontWeight.Bold); Sub(b.desc + if (b.earned) "" else " · ${loc(b.cur)}/${loc(b.tgt)}") }
            if (b.earned) OvButton(if (b.k in c.st.showcase) "Pinned" else "Pin", { vm.togglePin(b.k) }, small = true) else OvText("🔒", 16)
        }
    }
}

@Composable
fun LookSheet(vm: OverloadViewModel, c: Ctx) {
    val e = c.eng; val st = c.st
    SheetHead("Frames and auras") { vm.closeSheet() }
    H3("Frame")
    LookGrid(listOf("" to "None") + FRAMES.map { (k, v) -> k to v.label }, st.frame, { k -> k.isEmpty() || c.frameOK(k) }, { k -> FRAMES[k]?.how ?: "" },
        { k -> FramedEmblem(e.rank, k, "", Modifier.size(56.dp), spin = false) }) { k -> vm.setLook(frame = k) }
    H3("Aura", Modifier.padding(top = 12.dp))
    LookGrid(listOf("" to "None") + AURAS.map { (k, v) -> k to v.label }, st.aura, { k -> k.isEmpty() || c.auraOK(k) }, { k -> AURAS[k]?.how ?: "" },
        { k -> FramedEmblem(e.rank, "", k, Modifier.size(56.dp), spin = false) }) { k -> vm.setLook(aura = k) }
    H3("Nameplate", Modifier.padding(top = 12.dp))
    if (st.cos.tag.isEmpty()) Sub("Unlock nameplates in the season pass (levels 10 and 26).")
    else Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { (listOf("") + st.cos.tag).forEach { t -> Seg(t.ifEmpty { "None" }, st.tag == t, { vm.setLook(tag = t) }) } }
}

@Composable
private fun LookGrid(items: List<Pair<String, String>>, cur: String, ok: (String) -> Boolean, how: (String) -> String, preview: @Composable (String) -> Unit, pick: (String) -> Unit) {
    val vm: OverloadViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    items.chunked(3).forEach { row ->
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (k, label) -> val good = ok(k)
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (cur == k) ov.ink.copy(.12f) else ov.soft)
                    .clickable { if (good) pick(k) else vm.toast("Locked. ${how(k)}") }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.alpha(if (good) 1f else .4f)) { preview(k) }
                    OvText(if (good) label else "🔒 ${how(k)}", 11, FontWeight.Bold, if (good) ov.ink else ov.mute, align = TextAlign.Center)
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun PassSheet(vm: OverloadViewModel, e: Eng) {
    SheetHead("Season ${e.season} pass") { vm.closeSheet() }
    Sub("Level ${e.lvl} of 30 · ${loc(e.xp)} XP. Earn XP from daily challenges (10 each, +20 for a sweep), weekly quests (30), workouts (15), PRs (5) and weekly badges (25). Rewards stay yours forever.")
    PASS.forEach { (r, label) -> val lv = r.first; val got = e.lvl >= lv
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if (got) 1f else .6f), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(if (got) Green else Color(0xFF8C939E), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) { OvText("$lv", 12, FontWeight.Black, Color.White) }
            Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { OvText(label, 15, FontWeight.Bold); Sub("Level $lv") }
            OvText(if (got) "✓" else "🔒", 16, FontWeight.Black, if (got) Green else ov.mute)
        }
    }
    Sub("Finish the season to keep a permanent Season ${e.season} badge showing your level.", Modifier.padding(top = 6.dp))
}

@Composable
fun BadgesSheet(vm: OverloadViewModel, items: List<Bdg>, shine: Boolean) {
    SheetHead(if (items.size > 1) "${items.size} badges unlocked" else "Badge unlocked") { vm.closeSheet() }
    BadgeGrid(items.map { if (it.count != null) it.copy(count = maxOf(1, it.count)) else it.copy(earned = true) }, shine)
    OvButton("Nice 💪", { vm.closeSheet() }, Modifier.padding(top = 14.dp), bg = Red, fg = Color.White, wide = true)
}

@Composable
fun ReportSheet(vm: OverloadViewModel, r: Report, unit: String) {
    val pw = r.pw
    SheetHead("Your week") { vm.closeSheet() }
    Sub("${MON[pw[0].monthValue - 1]} ${pw[0].dayOfMonth} to ${MON[pw[6].monthValue - 1]} ${pw[6].dayOfMonth}")
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Title(r.grade, 72, if (r.grade.startsWith("A")) Green else if (r.grade == "B") Color(0xFFFF9F1C) else Red)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile("${r.a.done}/${r.a.planned}", "Sessions", Modifier.weight(1f)); StatTile("${r.a.sets}", "Sets", Modifier.weight(1f))
        StatTile(kfmt(r.a.vol), "$unit moved", Modifier.weight(1f)); StatTile("${r.a.prs}", "PRs", Modifier.weight(1f))
    }
    val dv = r.a.vol - r.b.vol
    Sub(if (r.b.vol > 0) (if (dv >= 0) "+${kfmt(dv)} $unit vs the week before 📈" else "${kfmt(-dv)} $unit less than the week before") else "", Modifier.padding(top = 10.dp))
    if (r.earned.isNotEmpty()) { H3("Weekly badges", Modifier.padding(top = 10.dp)); r.earned.forEach { OvText(it, 15) } }
    OvButton("Let's go 💪", { vm.closeSheet(); vm.later(300) { vm.checkUnlocks() } }, Modifier.padding(top = 14.dp), bg = Red, fg = Color.White, wide = true)
}

// ============================ RANK-UP MOMENT ============================
@Composable
fun Cinematic(vm: OverloadViewModel, c: Cine) =
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(RANKS[c.rank].light).copy(.55f), Color(0xF00B0C12)))).pointerInput(Unit) { detectTapGestures { } }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Emblem(c.rank, Modifier.size(200.dp), spin = true)
            OvText(c.sub, 16, FontWeight.Bold, Color.White.copy(.85f), Modifier.padding(top = 18.dp))
            Title(c.label.uppercase(), 40, Color.White)
            if (c.unlock.isNotEmpty()) OvText("Unlocked: ${c.unlock}", 15, FontWeight.SemiBold, Color.White.copy(.9f), Modifier.padding(top = 8.dp), align = TextAlign.Center)
            OvButton("Let's go", { vm.closeCine() }, Modifier.padding(top = 22.dp), bg = Color.White, fg = Color(0xFF14161C))
        }
    }

// ============================ YEAR IN REVIEW ============================
@Composable
fun YearInReview(vm: OverloadViewModel, c: Ctx) {
    val y = LocalDate.now().year; val d = c.yearData(y) ?: run { vm.openYear(false); return }; val e = c.eng
    val pos = if (vm.crew.on && vm.crewState.value != null) vm.crew.arenaOrder("all").indexOfFirst { it.id == vm.myId() } + 1 else 0
    val vc = c.volCompare(d.vol)
    class Slide(val a: Long, val b: Long, val body: @Composable ColumnScope.() -> Unit)
    @Composable fun Big(t: String, size: Int = 96) = Title(t, size, Color.White)
    @Composable fun H(t: String) = OvText(t, 26, FontWeight.Black, Color.White, align = TextAlign.Center)
    @Composable fun P(t: String) = OvText(t, 17, FontWeight.SemiBold, Color.White.copy(.9f), align = TextAlign.Center)
    val slides = listOfNotNull(
        Slide(0xFFEF2B36, 0xFFFF8A3D) { Big("$y"); H("Your year in Overload"); P("${c.st.name.ifBlank { "Lifter" }}, let's look back.") },
        Slide(0xFF2563FF, 0xFF00B7FF) { P("You showed up"); Big("${d.n}"); H("times"); if (d.hours > 0) P("That's about ${d.hours} hours under the bar.") },
        Slide(0xFFFF7A3D, 0xFFFFBE0B) { P("Total weight moved"); Big(lifeFmt(d.vol)); H(c.unit); P("${vc.first} " + vc.second.replace(Regex("^You moved [\\d,.]+ \\w+, "), "That’s ")) },
        Slide(0xFF16B65B, 0xFF00C2A8) { P("Sets logged"); Big(loc(d.sets)); H("${d.prs} personal records") },
        d.best?.let { b -> Slide(0xFF7C4DFF, 0xFFD14DFF) { P("Your strongest lift"); H(b.first); Big(b.second, 64); P("About a ${b.third.toInt()} ${c.unit} one-rep max") } },
        Slide(0xFFEF2B36, 0xFF7C4DFF) { P("Your most-trained muscle"); H(d.mk.firstOrNull() ?: "All of them")
            Row { Anatomy("front", d.levels, Modifier.height(200.dp), dark = true); Anatomy("back", d.levels, Modifier.height(200.dp), dark = true) }
            P(d.mk.drop(1).take(2).joinToString(" and ") + if (d.mk.size > 1) " close behind" else "") },
        Slide(0xFF00B7FF, 0xFF2563FF) { P("Your favorite day"); Big(DN[d.fav], 72); P(if (d.am > d.pm) "Morning lifter ☀️" else if (d.pm > d.am) "After-work grinder 🌙" else "") },
        Slide(0xFFFF7A3D, 0xFFEF2B36) { P("Longest streak"); Big("${e.best}"); H("weeks in a row 🔥") },
        Slide(0xFFB88600, 0xFFFFD75E) { P("You finish the year as"); Emblem(e.rank, Modifier.size(150.dp)); H(e.rname); P("${e.bcount} badges earned" + if (pos > 0) " · #$pos in your crew" else "") },
        Slide(0xFF0B0C12, 0xFF3A1D6E) { Big("${y + 1}", 64); H("See you under the bar."); P("Same plan. Heavier plates.") }
    )
    var i by remember { mutableStateOf(0) }
    val s = slides[i]
    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(s.a), Color(s.b)))).pointerInput(slides.size) {
        detectTapGestures { o -> if (o.x < size.width * .3f) i = maxOf(0, i - 1) else if (i >= slides.size - 1) vm.yearSeen() else i++ }
    }) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                slides.indices.forEach { k -> Box(Modifier.weight(1f).height(4.dp).background(Color.White.copy(if (k <= i) .95f else .3f), RoundedCornerShape(2.dp))) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) { OvText("✕", 24, FontWeight.Bold, Color.White, Modifier.clickable { vm.openYear(false) }.padding(8.dp)) }
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, content = s.body)
            OvText(if (i < slides.size - 1) "Tap to continue" else "Tap to close", 13, color = Color.White.copy(.7f), modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
