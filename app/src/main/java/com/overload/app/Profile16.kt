package com.overload.app

import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

// ============================ PROFILE TAB (0.16) ============================
@Composable
fun ProfileTab(vm: OverloadViewModel, c: Ctx) {
    val e = c.eng; val st = c.st; val nx = RANKS.getOrNull(e.rank + 1); val ban = c.myBanner(); val ctx = LocalContext.current
    val show = st.showcase.mapNotNull { e.badge(it) }
    val F = st.founder; val CF = !F && st.cofounder; val T = st.tap21
    OvCard {
        // 0.20: Edit and Share are icon buttons on the banner. 0.21: tap the banner or your plate to change them.
        Box(Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(18.dp)).background(ov.soft).clickable { vm.tap21("ban") }) {
            if (ban.isNotEmpty()) Scene(ban, Modifier.matchParentSize(), 8, "me$ban")
            if (T["ban"] == null) EditDot(Modifier.align(Alignment.BottomStart).padding(8.dp))
            Row(Modifier.align(Alignment.TopEnd).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconChip("✎") { vm.openSheet(Sheet.ProfEdit(null)) }; IconChip("⇪") { shareProfile(ctx, c) } }
        }
        Row(Modifier.fillMaxWidth().offset(y = (-30).dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.Bottom) {
            Box(Modifier.border(3.dp, ov.bg, CircleShape).clip(CircleShape).clickable { vm.tap21("av") }) {
                AvatarOf(c.myId ?: "me", st.name.ifBlank { "Lifter" }, st.av, 76); if (T["av"] == null) EditDot(Modifier.align(Alignment.BottomEnd)) }
            Spacer(Modifier.weight(1f)); Box(Modifier.clickable { vm.openSheet(Sheet.RankUp("you")) }) { FramedEmblem(e.rank, st.frame, st.aura, Modifier.size(64.dp)) }
        }
        Column(Modifier.fillMaxWidth().offset(y = (-22).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            // the crown sits outside the name, balanced by an invisible one so the name stays centred
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (F) OvText("👑", 20); NameText(st.name.ifBlank { "Lifter" }, c.look("nameFx"), 26, founder = F, cofounder = CF, crown = false, modifier = Modifier.padding(horizontal = 4.dp))
                if (F) OvText("👑", 20, modifier = Modifier.alpha(0f)) }
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (F) OvText("Founder #001 · ", 13, FontWeight.Black, hex("#c99a2e")) else if (CF) OvText("Co-Founder #002 · ", 13, FontWeight.Black, hex("#9aa0aa"))
                OvText("${e.rname} · 🔥 ${e.streak} wk", 13, FontWeight.SemiBold, ov.mute) }
            if (st.prof.motto.isNotEmpty()) OvText("“${st.prof.motto}”", 15, FontWeight.SemiBold, ov.mute, Modifier.padding(top = 6.dp), align = TextAlign.Center)
            Shelf(c.shelfItems(), { vm.openSheet(Sheet.BadgeInfo(it)) }, { vm.openSheet(Sheet.PinPick) })
        }
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("${e.workouts}" to "Workouts", lifeFmt(e.lifeVol) to "${c.unit} moved", "${e.lifePR}" to "PRs", "${e.power}" to "Rank power").forEach { (v, l) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Title(v, 20); Sub(l) } }
    }
    val t = e.trial
    if (t != null) OvCard { OvText("🔥 PROMOTION TRIAL · ${RANKS[t.tier].name.uppercase()}", 13, FontWeight.ExtraBold, ov.mute); LabeledBar("Train on your planned days", t.days, t.need); LabeledBar("Beat last week on lifts", t.beat, t.needBeat) }
    else if (nx != null) OvCard { OvText("NEXT RANK: ${nx.name.uppercase()}", 13, FontWeight.ExtraBold, ov.mute); LabeledBar("Active weeks", e.active, nx.weeks); LabeledBar("Badges", e.bcount, nx.badges) }
    val trs = c.trophies(); val bl = c.boxesLeft().size
    OvCard {
        if (c.cs != null) Row16("🪪", "Your crew profile", "What your crew sees") { vm.openSheet(Sheet.ProfilePage(c.myId ?: "")) }
        Row16(if (bl > 0) "🎁" else "🎨", "Customize", if (bl > 0) "$bl mystery box${if (bl > 1) "es" else ""}" else "Themes, effects, sounds") { vm.openSheet(Sheet.Locker(null)) }
        Row16("🏆", "Trophy room", "${trs.count { it.earned }}/${trs.size}") { vm.openSheet(Sheet.TrophyRoom(null)) }
        Row16("🏅", "Badges", "${e.B.count { it.earned }} earned") { vm.openSheet(Sheet.Badges16("mile")) }
        if (c.feat("season")) Row16("🎟", "Season pass", "Level ${e.lvl} of 30") { vm.openSheet(Sheet.Pass) }
    }
    OvCard {
        val lr = if (c.feat("liftranks")) c.liftRanks().size else 0
        if (lr > 0) Row16("🏋️", "Lift ranks", "$lr lifts ranked") { vm.openSheet(Sheet.Psec("lr")) }
        if (c.feat("mastery")) Row16("💪", "Muscle mastery", "Bronze to Legend") { vm.openSheet(Sheet.Psec("mast")) }
        Row16("🪜", "Rank ladder", "Rank ${e.rank + 1} of 8") { vm.openSheet(Sheet.Psec("ladder")) }
        Row16("📈", "Lifetime stats", "${loc(e.lifeSets)} sets") { vm.openSheet(Sheet.Psec("life")) }
        Row16("🎬", "${LocalDate.now().year} Year in review", if (LocalDate.now().monthValue == 12) "Ready" else "Preview") { vm.openYear(true) }
    }
}

@Composable private fun IconChip(t: String, onClick: () -> Unit) =
    Box(Modifier.size(34.dp).background(Color.Black.copy(.4f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { OvText(t, 15, FontWeight.Black, Color.White) }
/** The pulsing pencil hint shown until you've used tap-to-change once (0.21). */
@Composable fun EditDot(modifier: Modifier = Modifier) {
    val tr = androidx.compose.animation.core.rememberInfiniteTransition(label = "ed")
    val a by tr.animateFloat(.55f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(900), androidx.compose.animation.core.RepeatMode.Reverse), label = "ea")
    Box(modifier.size(24.dp).alpha(a).background(Red, CircleShape), contentAlignment = Alignment.Center) { OvText("✎", 12, FontWeight.Black, Color.White) }
}
fun OverloadViewModel.tap21(k: String) {
    if (data.value.settings.tap21[k] == null) setSettings { it.copy(tap21 = it.tap21 + (k to 1)) }
    profDraft.value = null; openSheet(Sheet.ProfEdit(if (k == "ban") "banner" else "plate21", direct = true))
}

/** A row in a clean list (Profile, Settings, Customize, Edit profile). */
@Composable
fun Row16(icon: String, label: String, value: String = "", onClick: () -> Unit) =
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(vertical = 11.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(icon, 20, modifier = Modifier.width(34.dp)); OvText(label, 16, FontWeight.SemiBold, modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Sub(value, Modifier.padding(horizontal = 6.dp)); OvText("›", 20, FontWeight.Bold, ov.mute)
    }

private fun shareProfile(ctx: android.content.Context, c: Ctx) {
    val e = c.eng
    val txt = "${c.st.name.ifBlank { "Me" }} on Overload 💪\n${e.rname} · 🔥 ${e.streak} week streak\n${e.workouts} workouts · ${lifeFmt(e.lifeVol)} ${c.unit} moved · ${e.lifePR} PRs"
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, txt), null))
}

/** Rank ladder, lifetime stats, lift ranks, mastery (opened from the profile lists). */
@Composable
fun PsecSheet(vm: OverloadViewModel, c: Ctx, k: String) {
    val e = c.eng
    when (k) {
        "ladder" -> { SheetHead("Rank ladder") { vm.closeSheet() }; Sub("Each rank has 3 divisions: III, II, then I.")
            RANKS.forEachIndexed { i, r -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).alpha(if (i <= e.rank) 1f else .65f), verticalAlignment = Alignment.CenterVertically) {
                Emblem(i, Modifier.size(40.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { OvText(r.name, 16, FontWeight.Bold); Sub(if (i == 0) "Start here" else "${r.weeks} active weeks · ${r.badges} badges · ${UNLOCKS[i]}") }
                OvText(if (i <= e.rank) "✓" else "🔒", 16, FontWeight.Black, if (i <= e.rank) Green else ov.mute) } } }
        "life" -> { SheetHead("Lifetime stats") { vm.closeSheet() }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatTile("${e.workouts}", "Workouts", Modifier.weight(1f)); StatTile(loc(e.lifeSets), "Sets", Modifier.weight(1f))
                StatTile(lifeFmt(e.lifeVol), "${c.unit} moved", Modifier.weight(1f)); StatTile("${e.lifePR}", "PRs", Modifier.weight(1f)) }
            Sub("Rank power ${e.power} · ${e.active} active weeks · ❄️ ${e.freeze} streak freeze${if (e.freeze == 1) "" else "s"}", Modifier.padding(top = 10.dp)) }
        "lr" -> { SheetHead("Lift ranks") { vm.closeSheet() }; LiftRanksCard(vm, c) }
        else -> { SheetHead("Muscle mastery") { vm.closeSheet() }; MasteryCard(c) }
    }
}

/** Badges behind one row, with category tabs (0.16). */
@Composable
fun Badges16Sheet(vm: OverloadViewModel, c: Ctx, cat: String) {
    val e = c.eng
    val cats = listOf("mile" to "Milestones", "week" to "Weekly") + listOf("Lift clubs", "Consistency", "Collections", "Crew", "Hidden", "Funny").filter { k -> e.B.any { it.cat == k } }.map { it to it }
    SheetHead("Badges") { vm.closeSheet() }
    Sub("${e.B.count { it.earned }} earned. Tap one to see it or pin it.", Modifier.padding(bottom = 8.dp))
    Chips(cats, cat) { vm.openSheet(Sheet.Badges16(it)) }
    when (cat) {
        "mile" -> { Sub("Each one levels up: Bronze, Silver, Gold, Platinum, Diamond, Legend.", Modifier.padding(bottom = 8.dp))
            LADDER.keys.chunked(3).forEach { row -> Row(Modifier.padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { k -> LadderTilePub(vm, e, k, Modifier.weight(1f)) }; repeat(3 - row.size) { Spacer(Modifier.weight(1f)) } } } }
        "week" -> { Sub("Earn these again every week.", Modifier.padding(bottom = 8.dp)); BadgeGrid(e.R, e.rank >= 2) { vm.openSheet(Sheet.BadgeInfo(it.k)) } }
        else -> BadgeGrid(e.B.filter { it.cat == cat }, e.rank >= 2) { vm.openSheet(Sheet.BadgeInfo(it.k)) }
    }
}

@Composable
fun PinPickSheet(vm: OverloadViewModel, c: Ctx) {
    val e = c.eng; val pins = c.st.showcase; val list = e.B.filter { it.earned } + e.R.filter { (it.count ?: 0) > 0 }
    SheetHead("Your shelf") { vm.closeSheet() }
    Sub("Pick up to 3 medals to show on your profile and to your crew. ${pins.size}/3 pinned.")
    if (list.isEmpty()) Sub("Earn your first badge and you can pin it here.", Modifier.padding(top = 8.dp))
    list.forEach { b -> Row(Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(14.dp)).background(if (b.k in pins) Red.copy(.1f) else ov.soft).clickable { vm.togglePin(b.k) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        BadgeMedal(b, 40.dp); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { OvText(b.name, 15, FontWeight.Bold); Sub("${tierName(b.k, b.name, b.count ?: 0)} · ${b.desc}") }
        OvText(if (b.k in pins) "Pinned" else "Pin", 13, FontWeight.Bold, if (b.k in pins) Red else ov.mute) } }
    // 0.21: pinned trophies live here too
    val earned = c.trophies().filter { it.earned }; val tp = c.st.prof.tpins
    H3("Trophies", Modifier.padding(top = 18.dp)); Sub("Up to 3 show on your crew card. ${tp.size}/3 pinned.")
    if (earned.isEmpty()) Sub("Win trophies with your crew and pin them here.")
    FlowRowC(Modifier.padding(top = 6.dp)) { earned.forEach { t -> val on = t.k in tp
        Column(Modifier.width(96.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Red.copy(.12f) else ov.soft).clickable {
            vm.setSettings { s0 -> val a = s0.prof.tpins.toMutableList(); if (t.k in a) a.remove(t.k) else { if (a.size >= 3) a.removeAt(0); a.add(t.k) }; s0.copy(prof = s0.prof.copy(tpins = a)) }; vm.crew.sync(true) }.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally) { TrophyView(t, Modifier.size(40.dp)); OvText(t.name.take(18), 11, FontWeight.Bold, align = TextAlign.Center) } } }
}

// ============================ CREW PROFILE PAGE (0.15) ============================
@Composable
fun ProfilePageSheet(vm: OverloadViewModel, c: Ctx, id: String) {
    val cs by vm.crewState.collectAsState(); val me = vm.myId(); val self = id == me || id.isEmpty()
    val m0 = cs?.members?.firstOrNull { it.id == (if (self) me else id) }
    val name = if (self) c.st.name.ifBlank { "You" } else m0?.name ?: run { Sub("They left the crew."); return }
    val p: MemberProfile = if (self) (m0?.profile ?: MemberProfile()).copy(motto = c.st.prof.motto, gym = c.st.prof.gym, phase = c.st.prof.phase, ltype = c.st.prof.ltype, song = c.st.prof.song,
        av = c.st.av, pacc = c.st.prof.acc, tpins = c.st.prof.tpins, flex = c.myFlex(), prtl = c.prTimeline().takeLast(20), best = c.bestLift(), fav = c.st.prof.fav, founder = c.st.founder, cofounder = c.st.cofounder,
        pbanner = c.look("pbanner"), nameFx = c.look("nameFx"), plates = FIN.keys.filter { FIN[it]!!.cat != "finish" && c.finOwned(it) }.take(40)) else m0?.profile ?: MemberProfile()
    val acc = if (p.pacc.isNotEmpty()) hex(p.pacc) else Red
    val pb = p.pbanner.takeIf { it.isNotEmpty() && it != "none" }?.let { OLDF20[it] ?: it } ?: if (p.founder) "feclipse" else if (p.cofounder) "cofounder" else "forge"
    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(18.dp))) { Scene(pb, Modifier.matchParentSize(), 8, "pp$id")
        OvButton("Done", { vm.closeSheet() }, Modifier.align(Alignment.TopEnd).padding(8.dp), small = true, bg = Color.White.copy(.85f), fg = Color(0xFF14161C)) }
    Row(Modifier.fillMaxWidth().offset(y = (-26).dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.Bottom) {
        Box(Modifier.border(3.dp, acc, CircleShape)) { AvatarOf(if (self) me ?: "me" else id, name, p.av, 84) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.padding(bottom = 4.dp)) {
            NameText(name, p.nameFx, 22, founder = p.founder, shame = vm.crew.shamed(id), cofounder = p.cofounder)
            if (m0?.nick?.isNotEmpty() == true) OvText("“${m0.nick}”", 13, FontWeight.SemiBold, ov.mute)
            FlowRowC { if (p.founder) Chip("Founder #001", hex("#e3c25a"), Color(0xFF14161C)) else if (p.cofounder) Chip("Co-Founder #002", hex("#4a4f57"), Color(0xFFEEF1F5))
                if (p.og > 0 && !p.founder) Chip("OG #${p.og.toString().padStart(3, '0')}")
                if (m0?.role?.isNotEmpty() == true) Chip(m0.role); m0?.jersey?.let { Chip("#$it") }; if (p.rname.isNotEmpty()) Chip(p.rname) }
        }
    }
    Column(Modifier.offset(y = (-18).dp)) {
        if (p.motto.isNotEmpty()) OvText("“${p.motto}”", 15, FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        FlowRowC { if (p.ltype.isNotEmpty()) Chip("🏋️ ${p.ltype}"); if (p.phase.isNotEmpty()) Chip("📈 ${p.phase}"); if (p.gym.isNotEmpty()) Chip("📍 ${p.gym}"); if (p.song.isNotEmpty()) Chip("🎵 ${p.song}") }
        val shelf = if (self) c.shelfItems() else memberShelf(p)
        if (shelf.isNotEmpty()) Shelf(shelf, if (self) ({ k: String -> vm.openSheet(Sheet.BadgeInfo(k)) }) else null, null, 72.dp)
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (self) { Sub("💪 ${m0?.respect ?: 0} respect from your crew", Modifier.weight(1f)); OvButton("Edit profile", { vm.openSheet(Sheet.ProfEdit(null)) }, small = true) }
            else { val given = id in (cs?.respectGiven ?: emptyList())
                OvButton(if (given) "Respect given today" else "💪 Give respect", { if (!given) vm.crew.respect(id) }, bg = if (given) ov.soft else acc, fg = if (given) ov.mute else Color.White); Spacer(Modifier.width(10.dp)); Sub("${m0?.respect ?: 0} respect") }
        }
        if (p.flex.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            p.flex.forEach { f -> Column(Modifier.weight(1f).background(ov.soft, RoundedCornerShape(14.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) { OvText(f.v, 16, FontWeight.Black, align = TextAlign.Center); Sub(f.l) } } }
        p.best?.let { b -> Column(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(acc.copy(.12f), RoundedCornerShape(16.dp)).padding(12.dp)) {
            Sub("Best lift"); OvText(b.n, 17, FontWeight.Bold); Title("${toMine(b.w, p.unit, c.unit).roundToInt()} ${c.unit} × ${b.r}", 22); Sub(b.d) } }
        val trs = c.trophies(); val tp = p.tpins.mapNotNull { k -> trs.firstOrNull { it.k == k } }.map { Trophy(it.k, it.type, it.name, it.how, true, it.color) }
        if (tp.isNotEmpty()) { Lbl("PINNED TROPHIES"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { tp.forEach { t -> Column(Modifier.weight(1f).clickable { vm.openSheet(Sheet.TrophyInfo(t.k, true)) }, horizontalAlignment = Alignment.CenterHorizontally) { TrophyView(t, Modifier.size(54.dp)); Sub(t.name) } } } }
        if (p.plates.isNotEmpty()) { Lbl("PLATE COLLECTION · ${p.plates.size}")
            val order = if (p.fav.isNotEmpty() && p.fav in p.plates) listOf(p.fav) + p.plates.filter { it != p.fav } else p.plates
            FlowRowC { order.filter { PC[it] != null }.forEachIndexed { i, k -> Box(Modifier.size(38.dp).then(if (i == 0 && p.fav.isNotEmpty()) Modifier.border(2.dp, hex("#e3c25a"), CircleShape) else Modifier)) { Plate(k, "done", Modifier.fillMaxSize(), og = p.og) } } } }
        if (p.prtl.isNotEmpty()) { var open by remember { mutableStateOf(false) }
            Lbl("PR TIMELINE · ${p.prtl.size}"); if (!open) OvButton("Show", { open = true }, small = true)
            else p.prtl.reversed().forEach { x -> RecRow(x.n, x.d, "${toMine(x.w, p.unit, c.unit).roundToInt()}×${x.r}") } }
        val posts = cs?.posts?.filter { it.by == id && it.type == "workout" }?.take(2) ?: emptyList()
        if (posts.isNotEmpty()) { Lbl("RECENT WORKOUTS"); posts.forEach { OvCard { PostBody(c, it, true) } } }
        if (!self && cs != null) {
            MemberExtras(vm, c, id)
            var nick by remember { mutableStateOf(cs!!.nickVotes[id] ?: "") }
            Lbl("SUGGEST A NICKNAME")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { TextBox(nick, { nick = it.take(20) }, "The Machine", Modifier.weight(1f)); OvButton("Vote", { vm.crew.nick(id, nick.trim()) }) }
            Sub("Majority vote wins. It shows under their name.")
            if (vm.crew.isOwner()) { Lbl("ROLE"); FlowRowC { ROLES.forEach { r -> Seg(r.ifEmpty { "None" }, (m0?.role ?: "") == r, { vm.crew.role(id, r) }) } }
                OvButton("🏛 Induct into Hall of Fame", { vm.crew.hofMember(id) }, Modifier.padding(top = 8.dp), small = true) }
            ShameButton(vm, id)
            if (p.troph != null) OvButton("🏆 View $name’s trophy room", { vm.openSheet(Sheet.TrophyRoom(id)) }, Modifier.padding(top = 10.dp), wide = true)
            BlockReportRow(vm, id)
        }
    }
}

// ============================ EDIT PROFILE (0.16: a clean list) ============================
class ProfDraft(val p: Prof, val av: Av?, val flex: List<String>, val rim: String)
val profDraft = mutableStateOf<ProfDraft?>(null)

@Composable
fun ProfEditSheet(vm: OverloadViewModel, c: Ctx, sec: String?, direct: Boolean = false) {
    if (profDraft.value == null) profDraft.value = ProfDraft(c.st.prof, c.st.av, c.st.flex, c.st.rim)
    val dr = profDraft.value!!; fun set(f: (ProfDraft) -> ProfDraft) { profDraft.value = f(profDraft.value!!) }
    val rows = listOf("banner" to ("🖼" to "Banner"), "avatar" to ("🙂" to "Avatar"), "motto" to ("💬" to "Motto"), "gym" to ("📍" to "Gym"), "song" to ("🎵" to "Walkout song"),
        "ltype" to ("🏋️" to "Lifter type"), "phase" to ("📈" to "Right now I’m"), "flex" to ("📊" to "Flex stats"), "tpins" to ("🏆" to "Pinned trophies"), "fav" to ("💿" to "Favorite plate"),
        "rim" to ("✍️" to "Name on plate"), "acc" to ("🎨" to "Profile color"), "jersey" to ("🔢" to "Jersey number"), "plate21" to ("💿" to "Plate and avatar")).filter { it.first != "jersey" || c.cs != null }
    // 0.21: banner, plate and avatar are changed by tapping them on the profile, so Edit profile is the personal stuff only
    val hide = setOf("banner", "avatar", "tpins", "fav", "rim", "plate21")
    /** Opened by tapping the banner or plate: Done saves and closes. */
    fun commit() { vm.setSettings { it.copy(prof = dr.p, av = dr.av?.takeIf { a -> a.t != null || a.c != null }, flex = dr.flex, rim = dr.rim.take(24)) }; profDraft.value = null; vm.closeSheet(); vm.crew.sync(true); vm.toast("Saved") }
    if (sec == null) {
        SheetHead("Edit profile", "Cancel") { profDraft.value = null; vm.closeSheet() }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { vm.openSheet(Sheet.ProfEdit("plate21")) }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AvatarOf(c.myId ?: "me", c.st.name, dr.av, 64); OvText("Change plate", 13, FontWeight.Bold, Red, Modifier.padding(top = 4.dp)) }
        OvCard(Modifier.padding(top = 10.dp)) {
            rows.filter { it.first !in hide }.forEach { (k, v) ->
                val value = when (k) { "banner" -> c.myBanner().let { b -> if (b.isEmpty()) "None" else LOCK.getValue("pbanner").items[b]?.label ?: b }
                    "avatar" -> when (dr.av?.t) { "icon" -> dr.av.v ?: ""; "plate" -> "Plate"; else -> "Initials" }
                    "motto" -> dr.p.motto.ifEmpty { "Not set" }; "gym" -> dr.p.gym.ifEmpty { "Not set" }; "song" -> dr.p.song.ifEmpty { "Not set" }
                    "ltype" -> dr.p.ltype.ifEmpty { "Not set" }; "phase" -> dr.p.phase.ifEmpty { "Not set" }
                    "flex" -> dr.flex.joinToString(", ") { FLEX[it]?.first ?: it }.ifEmpty { "None" }; "tpins" -> "${dr.p.tpins.size} of 3"
                    "fav" -> if (dr.p.fav.isNotEmpty()) plateName(dr.p.fav) else "None"; "rim" -> dr.rim.ifEmpty { "Default" }; "acc" -> if (dr.p.acc.isNotEmpty()) "●" else "Default"
                    else -> vm.crew.myMember()?.jersey?.let { "#$it" } ?: "None" }
                Row16(v.first, v.second, value) { vm.openSheet(Sheet.ProfEdit(k)) }
            }
        }
        OvButton("Save profile", { vm.saveProfile(dr.p, dr.av, dr.flex, dr.rim); profDraft.value = null }, bg = Red, fg = Color.White, wide = true)
        Sub("Tip: tap your banner or plate on your profile to change them.", Modifier.fillMaxWidth().padding(top = 10.dp))
        return
    }
    val lbl = rows.firstOrNull { it.first == sec }?.second?.second ?: ""
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (!direct) OvButton("‹ Back", { vm.openSheet(Sheet.ProfEdit(null)) }, small = true); Title(lbl, 20, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        OvButton("Done", { if (direct) commit() else vm.openSheet(Sheet.ProfEdit(null)) }, small = true, bg = Red, fg = Color.White)
    }
    val own = PC.keys.filter { c.plateOwn(it) }
    when (sec) {
        "banner" -> { LOCK.getValue("pbanner").items.values.filter { it.k !in FONLY18 || c.owns("pbanner", it.k) }.sortedBy { FONLY18.indexOf(it.k).let { i -> if (i < 0) 99 else i } }.chunked(2).forEach { row -> Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { it2 -> val ok = c.owns("pbanner", it2.k); val on = c.myBanner() == it2.k || (it2.k == "none" && c.myBanner().isEmpty())
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).then(if (on) Modifier.border(3.dp, ov.ink, RoundedCornerShape(14.dp)) else Modifier).clickable { vm.equipLook("pbanner", it2.k) }) {
                        if (it2.k == "none") Box(Modifier.fillMaxWidth().aspectRatio(380f / 130f).background(ov.soft), contentAlignment = Alignment.Center) { Sub("None") } else Box(Modifier.alpha(if (ok) 1f else .5f)) { Scene(it2.k, Modifier.fillMaxWidth(), 8, "pk${it2.k}") }
                        OvText(it2.label + if (ok) "" else " 🔒", 12, FontWeight.Bold, modifier = Modifier.padding(6.dp)) } }
                if (row.size == 1) Spacer(Modifier.weight(1f)) } }
            Sub("Locked ones show how to unlock them when you tap.", Modifier.padding(top = 8.dp)) }
        "avatar" -> { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { AvatarOf(c.myId ?: "me", c.st.name, dr.av, 84) }
            Lbl("ICON"); FlowRowC { Seg("Initials", dr.av?.t == null, { set { it2 -> ProfDraft(it2.p, Av(c = it2.av?.c), it2.flex, it2.rim) } })
                AV_ICONS.forEach { i -> Seg(i, dr.av?.t == "icon" && dr.av.v == i, { set { it2 -> ProfDraft(it2.p, Av("icon", i, it2.av?.c), it2.flex, it2.rim) } }) } }
            Lbl("COLOR"); FlowRowC { AV_COLS.forEach { col -> Box(Modifier.size(34.dp).clip(CircleShape).background(hex(col)).then(if (dr.av?.c == col) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier)
                .clickable { set { it2 -> ProfDraft(it2.p, (it2.av ?: Av()).copy(c = col), it2.flex, it2.rim) } }) } }
            if (own.isNotEmpty()) { Lbl("OR USE A PLATE"); FlowRowC { own.forEach { k -> Box(Modifier.size(40.dp).then(if (dr.av?.t == "plate" && dr.av.v == k) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier)
                .clickable { set { it2 -> ProfDraft(it2.p, Av("plate", k, it2.av?.c), it2.flex, it2.rim) } }) { Plate(k, "done", Modifier.fillMaxSize()) } } } } }
        "plate21" -> { Box(Modifier.fillMaxWidth().padding(bottom = 6.dp), contentAlignment = Alignment.Center) { AvatarOf(c.myId ?: "me", c.st.name, dr.av, 84) }
            if (own.isNotEmpty()) { Lbl("USE A PLATE"); FlowRowC { own.forEach { k -> Box(Modifier.size(42.dp).then(if (dr.av?.t == "plate" && dr.av.v == k) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier)
                .clickable { set { it2 -> ProfDraft(it2.p, Av("plate", k, it2.av?.c), it2.flex, it2.rim) } }) { Plate(k, "done", Modifier.fillMaxSize()) } } } }
            Lbl("OR AN ICON"); FlowRowC { Seg("Initials", dr.av?.t == null, { set { it2 -> ProfDraft(it2.p, Av(c = it2.av?.c), it2.flex, it2.rim) } })
                AV_ICONS.forEach { i -> Seg(i, dr.av?.t == "icon" && dr.av.v == i, { set { it2 -> ProfDraft(it2.p, Av("icon", i, it2.av?.c), it2.flex, it2.rim) } }) } }
            Lbl("ICON COLOR"); FlowRowC { AV_COLS.forEach { col -> Box(Modifier.size(34.dp).clip(CircleShape).background(hex(col)).then(if (dr.av?.c == col) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier)
                .clickable { set { it2 -> ProfDraft(it2.p, (it2.av ?: Av()).copy(c = col), it2.flex, it2.rim) } }) } }
            val fav = own.filter { FIN[it] != null }; Lbl("FAVORITE PLATE")
            if (fav.isEmpty()) Sub("Unlock special plates to show one off.")
            else FlowRowC { Seg("None", dr.p.fav.isEmpty(), { set { ProfDraft(it.p.copy(fav = ""), it.av, it.flex, it.rim) } })
                fav.forEach { k -> Box(Modifier.size(42.dp).then(if (dr.p.fav == k) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { set { ProfDraft(it.p.copy(fav = k), it.av, it.flex, it.rim) } }) { Plate(k, "done", Modifier.fillMaxSize()) } } }
            Lbl("NAME ON YOUR PLATE"); TextBox(dr.rim, { v -> set { ProfDraft(it.p, it.av, it.flex, v.take(24)) } }, c.st.name.ifBlank { "Your name" }.uppercase())
            Sub("Engraved around the big plate on Today.", Modifier.padding(top = 6.dp)) }
        "motto" -> TextBox(dr.p.motto, { v -> set { ProfDraft(it.p.copy(motto = v.take(60)), it.av, it.flex, it.rim) } }, "Leg day is a lifestyle")
        "gym" -> TextBox(dr.p.gym, { v -> set { ProfDraft(it.p.copy(gym = v.take(30)), it.av, it.flex, it.rim) } }, "Home gym")
        "song" -> TextBox(dr.p.song, { v -> set { ProfDraft(it.p.copy(song = v.take(30)), it.av, it.flex, it.rim) } }, "Lose Yourself")
        "ltype" -> FlowRowC { LTYPES.forEach { t -> Seg(t, dr.p.ltype == t, { set { ProfDraft(it.p.copy(ltype = if (it.p.ltype == t) "" else t), it.av, it.flex, it.rim) } }) } }
        "phase" -> FlowRowC { PHASES.forEach { t -> Seg(t, dr.p.phase == t, { set { ProfDraft(it.p.copy(phase = if (it.p.phase == t) "" else t), it.av, it.flex, it.rim) } }) } }
        "flex" -> { Sub("Pick 3 to show on your crew profile.", Modifier.padding(bottom = 6.dp)); FlowRowC { FLEX.forEach { (k, f) -> Seg(f.first, k in dr.flex, { set { d -> val a = d.flex.toMutableList(); if (k in a) a.remove(k) else { if (a.size >= 3) a.removeAt(0); a.add(k) }; ProfDraft(d.p, d.av, a, d.rim) } }) } } }
        "tpins" -> { val earned = c.trophies().filter { it.earned }; Sub("Up to 3.", Modifier.padding(bottom = 6.dp))
            if (earned.isEmpty()) Sub("Earn trophies to pin them.")
            FlowRowC { earned.forEach { t -> val on = t.k in dr.p.tpins
                Column(Modifier.width(96.dp).clip(RoundedCornerShape(14.dp)).background(if (on) Red.copy(.12f) else ov.soft).clickable { set { d -> val a = d.p.tpins.toMutableList(); if (t.k in a) a.remove(t.k) else { if (a.size >= 3) a.removeAt(0); a.add(t.k) }; ProfDraft(d.p.copy(tpins = a), d.av, d.flex, d.rim) } }.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    TrophyView(t, Modifier.size(40.dp)); OvText(t.name.take(18), 11, FontWeight.Bold, align = TextAlign.Center) } } } }
        "fav" -> { val l = own.filter { FIN[it] != null }; if (l.isEmpty()) Sub("Unlock special plates to show one off.")
            FlowRowC { l.forEach { k -> Box(Modifier.size(44.dp).then(if (dr.p.fav == k) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { set { ProfDraft(it.p.copy(fav = if (it.p.fav == k) "" else k), it.av, it.flex, it.rim) } }) { Plate(k, "done", Modifier.fillMaxSize()) } } } }
        "rim" -> { TextBox(dr.rim, { v -> set { ProfDraft(it.p, it.av, it.flex, v.take(24)) } }, c.st.name.ifBlank { "Your name" }.uppercase()); Sub("Engraved around the big plate on Today.", Modifier.padding(top = 6.dp)) }
        "acc" -> FlowRowC { Seg("Default", dr.p.acc.isEmpty(), { set { ProfDraft(it.p.copy(acc = ""), it.av, it.flex, it.rim) } })
            AV_COLS.forEach { col -> Box(Modifier.size(34.dp).clip(CircleShape).background(hex(col)).then(if (dr.p.acc == col) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { set { ProfDraft(it.p.copy(acc = col), it.av, it.flex, it.rim) } }) } }
        "jersey" -> { var n by remember { mutableStateOf(vm.crew.myMember()?.jersey?.toString() ?: "") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { NumBox(n, "23", { n = it.take(2) }, Modifier.weight(1f), decimal = false); OvButton("Claim", { n.toIntOrNull()?.let { vm.crew.jersey(it) } }) }
            Sub("Numbers are unique in your crew. #1 belongs to whoever started the crew.", Modifier.padding(top = 6.dp)) }
    }
}

// ============================ CUSTOMIZE (0.16: a clean list) ============================
@Composable
fun LockerSheet(vm: OverloadViewModel, c: Ctx, sec: String?) {
    val bl = c.boxesLeft().size
    if (sec == null) {
        SheetHead("Customize") { vm.closeSheet() }
        if (bl > 0) CardButton("🎁", "$bl mystery box${if (bl > 1) "es" else ""} to open", "Earned from your season pass and rank-ups") { vm.openBox() }
        OvCard {
            LOCK.filter { it.key != "pbanner" }.forEach { (k, v) -> val ownN = v.items.keys.count { c.owns(k, it) }; val n = v.items.size
                Row16(LK_ICON[k] ?: "•", v.name, if (k == "react") "$ownN of $n" else (v.items[c.look(k)]?.label ?: "") + (if (ownN < n) " · $ownN/$n" else "") + if (k in WEB_ONLY_LOOKS) " · web" else "") { vm.openSheet(Sheet.Locker(k)) } }
            Row16("🪞", "Frames and auras") { vm.openSheet(Sheet.Look) }
            Row16("😜", "Fun extras") { vm.openSheet(Sheet.Locker("fun")) }
        }
        Sub("Plate designs live in Plan → Plate. Profile banners are in Edit profile.", Modifier.fillMaxWidth(), ov.mute)
        return
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        OvButton("‹ Back", { vm.openSheet(Sheet.Locker(null)) }, small = true); Title(if (sec == "fun") "Fun extras" else LOCK[sec]?.name ?: "", 20, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
        OvButton("Done", { vm.closeSheet() }, small = true, bg = Red, fg = Color.White)
    }
    if (sec == "fun") {
        TogRowPub("Roast lines on crew posts", "A funny line under each workout post", c.st.roasts) { v -> vm.setSettings { it.copy(roasts = v) } }
        TogRowPub("Rest timer one-liners", null, c.st.restJokes) { v -> vm.setSettings { it.copy(restJokes = v) } }
        return
    }
    val v = LOCK[sec] ?: return
    FlowRowC { v.items.values.forEach { it2 -> val o = c.owns(sec, it2.k); val on = if (sec == "react") o else c.look(sec) == it2.k
        Row(Modifier.clip(RoundedCornerShape(12.dp)).background(if (on) ov.ink else ov.card).clickable { vm.equipLook(sec, it2.k) }.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            if (sec == "accent") { Box(Modifier.size(16.dp).background(hex(it2.extra), CircleShape)); Spacer(Modifier.width(6.dp)) }
            if (sec == "icon") { Plate(it2.extra, if (it2.extra == "black") "idle" else "done", Modifier.size(22.dp)); Spacer(Modifier.width(6.dp)) }
            OvText(it2.label + if (o) "" else " 🔒", 14, FontWeight.Bold, if (on) ov.bg else ov.ink)
        } } }
    Sub(when { sec == "react" -> "Reactions you own show up on crew posts."; sec in WEB_ONLY_LOOKS -> "This one only changes the web app for now. Tap a locked one to see how to unlock it."; else -> "Tap a locked one to see how to unlock it." }, Modifier.padding(top = 10.dp))
}

@Composable
fun BoxSheet(vm: OverloadViewModel, got: Pair<String, String>?, opening: Boolean) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(opening) { if (opening) { repeat(6) { shake.animateTo(if (it % 2 == 0) 12f else -12f, tween(70)) }; shake.animateTo(0f) } }
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OvText("🎁", 90, modifier = Modifier.graphicsLayer { rotationZ = shake.value; scaleX = if (opening) 1f else 1.15f; scaleY = if (opening) 1f else 1.15f })
        when {
            opening -> Title("Opening…", 26)
            got == null -> { Title("Nothing new", 26); Sub("You already own every mystery box item. Legend.") }
            else -> { Title(LOCK[got.first]?.items?.get(got.second)?.label ?: got.second, 26); Sub("New ${LOCK[got.first]?.name?.lowercase() ?: ""}. Equip it in Customize.") }
        }
        if (!opening) OvButton("Nice", { vm.openSheet(Sheet.Locker(null)) }, Modifier.padding(top = 16.dp), bg = Red, fg = Color.White, wide = true)
    }
}

// ============================ TROPHY ROOM (0.14) ============================
@Composable
fun TrophyRoomSheet(vm: OverloadViewModel, c: Ctx, of: String?) {
    val m = of?.let { vm.crew.member(it) }
    val list = c.trophies().let { l -> if (m != null) l.map { Trophy(it.k, it.type, it.name, it.how, it.k in (m.profile?.troph ?: emptyList()), it.color) } else l }
    SheetHead("Trophy room") { vm.closeSheet() }
    Row { H3(if (m != null) "${m.name}’s trophy room" else "Trophy room", Modifier.weight(1f)); Sub("${list.count { it.earned }}/${list.size}") }
    list.chunked(4).forEach { row ->
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { t -> Box(Modifier.weight(1f).clickable { vm.openSheet(Sheet.TrophyInfo(t.k, t.earned)) }, contentAlignment = Alignment.Center) { TrophyView(t, Modifier.size(58.dp)) } }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Box(Modifier.fillMaxWidth().height(6.dp).background(Brush.verticalGradient(listOf(Color.White.copy(.5f), Color.White.copy(.1f))), RoundedCornerShape(3.dp)))
        }
    }
    Sub("Tap a trophy to spin it.", Modifier.padding(top = 8.dp))
}

@Composable
fun TrophyInfoSheet(vm: OverloadViewModel, c: Ctx, k: String, earned: Boolean) {
    val t0 = c.trophies().firstOrNull { it.k == k } ?: return; val t = Trophy(t0.k, t0.type, t0.name, t0.how, earned, t0.color)
    val spin = remember { Animatable(0f) }
    LaunchedEffect(k) { while (true) { spin.snapTo(0f); spin.animateTo(360f, tween(3200, easing = LinearEasing)) } }
    SheetHead(t.name) { vm.closeSheet() }
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { TrophyView(t, Modifier.size(140.dp).graphicsLayer { rotationY = spin.value; cameraDistance = 12f * density }) }
    OvText(if (t.earned) "Earned" else "Not yet", 16, FontWeight.Bold, modifier = Modifier.fillMaxWidth(), align = TextAlign.Center)
    Sub(t.how, Modifier.fillMaxWidth().padding(top = 4.dp))
}

// ============================ PLATE PICKER (0.15) ============================
@Composable
fun PlatePickSheet(vm: OverloadViewModel, c: Ctx, di: Int) {
    val day = c.plan.getOrNull(di) ?: return; val col = colorOf(day)
    var cat by remember { mutableStateOf(plateCat(col)) }; var sel by remember { mutableStateOf(col) }; var locked by remember { mutableStateOf(false) }
    val all = PC.keys.filter { plateCat(it) == cat }; val vis = if (locked) all else all.filter { c.plateOwn(it) }; val nl = all.count { !c.plateOwn(it) }
    SheetHead("Plate") { vm.closeSheet() }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Plate(sel, "done", Modifier.size(150.dp), label = (c.st.rim.ifBlank { "OVERLOAD · ${day.name}" }).uppercase().take(28), og = myOG(c))
        OvText(plateName(sel), 17, FontWeight.Bold, modifier = Modifier.padding(top = 6.dp)); if (!c.plateOwn(sel)) Sub("🔒 ${c.plateHow(sel)}")
    }
    Chips(PCAT.map { (k, n) -> val cnt = PC.keys.count { plateCat(it) == k && c.plateOwn(it) }; k to (n + if (cnt > 0) " $cnt" else "") }, cat) { cat = it; sel = col }
    if (vis.isEmpty()) Sub("None unlocked here yet.")
    FlowRowC(gap = 8) { vis.forEach { k -> Box(Modifier.size(48.dp).clip(CircleShape).then(if (k == sel) Modifier.border(3.dp, ov.ink, CircleShape) else Modifier).clickable { sel = k }, contentAlignment = Alignment.Center) {
        Plate(k, "done", Modifier.fillMaxSize().alpha(if (c.plateOwn(k)) 1f else .45f)); if (!c.plateOwn(k)) OvText("🔒", 14) } } }
    if (nl > 0) OvButton(if (locked) "Hide locked" else "Show locked ($nl)", { locked = !locked }, Modifier.padding(top = 8.dp), small = true)
    val ok = c.plateOwn(sel)
    OvButton(if (ok) "Use this plate" else "Locked", { if (ok) { vm.editDay(di) { it.copy(color = sel) }; vm.closeSheet(); vm.toast("Plate set") } else vm.toast("🔒 ${c.plateHow(sel)}") }, Modifier.padding(top = 14.dp), bg = if (ok) Red else ov.soft, fg = if (ok) Color.White else ov.mute, wide = true)
}

// ============================ TRASH (0.14) ============================
@Composable
fun TrashSheet(vm: OverloadViewModel) {
    val items by vm.trash.items.collectAsState(); val crewT by crewTrash.collectAsState()
    LaunchedEffect(Unit) { vm.trash.load(); vm.crew.loadTrash() }
    SheetHead("Trash") { vm.closeSheet() }
    Sub("Anything you delete waits here for 30 days. Tap Restore to bring it back.")
    if (items.isEmpty()) Sub("Nothing on this phone is in the Trash.", Modifier.padding(top = 8.dp))
    items.forEach { x -> Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(TRASH_ICON[x.kind] ?: "•", 20, modifier = Modifier.width(32.dp))
        Column(Modifier.weight(1f)) { OvText(x.label, 14, FontWeight.Bold); Sub("${ago(x.ts)} · ${maxOf(0, 30 - ((System.currentTimeMillis() - x.ts) / 86_400_000L).toInt())} days left") }
        OvButton("Restore", { vm.toast(if (vm.trash.restore(x.id)) "Restored" else "Could not restore that one"); vm.checkUnlocks() }, small = true) } }
    if (vm.crew.on) {
        H3("Crew trash", Modifier.padding(top = 16.dp))
        when { crewT == null -> Sub("Loading…"); crewT!!.isEmpty() -> Sub("No deleted crew posts or messages.")
            else -> crewT!!.forEach { o -> val id = o["id"]?.toString()?.trim('"') ?: return@forEach; val kind = o["kind"]?.toString()?.trim('"')
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OvText(if (kind == "msg") "💬" else "📣", 20, modifier = Modifier.width(32.dp))
                    Column(Modifier.weight(1f)) { OvText((o["text"]?.toString()?.trim('"') ?: "Post").take(60), 14, FontWeight.Bold)
                        Sub("${if (kind == "msg") "Message" else "Post"} by ${vm.crew.member2(o["by"]?.toString()?.trim('"'))?.name ?: "?"} · ${o["left"]?.toString() ?: "?"} days left") }
                    OvButton("Restore", { vm.crew.restoreTrash(id) }, small = true) } } }
    }
}

// ============================ SMALL SHEETS ============================
@Composable
fun ExcuseSheet(vm: OverloadViewModel, i0: Int) {
    var i by remember { mutableStateOf(i0) }
    SheetHead("Excuse generator") { vm.closeSheet() }
    OvText("“${EX_A[i % EX_A.size]}”", 22, FontWeight.Black, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), align = TextAlign.Center)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OvButton("🎲 Another one", { i = (i + 1 + (Math.random() * (EX_A.size - 1)).toInt()) % EX_A.size })
        if (vm.crew.on) OvButton("Send to crew chat", { vm.closeSheet(); vm.setTab("crew"); vm.setSub("crew", "chat"); vm.chat.send("Can’t make it today. ${EX_A[i % EX_A.size]}"); vm.setSettings { it.copy(excuses = it.excuses + 1) } }, bg = Red, fg = Color.White)
    }
    Sub("Or, hear us out, just go lift.", Modifier.padding(top = 10.dp))
}

@Composable
fun TourSheet(vm: OverloadViewModel, i: Int) {
    val t = TOUR[i]; val last = i == TOUR.size - 1
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OvText(t.first, 64); Title(t.second, 28); Sub(t.third, Modifier.padding(top = 6.dp))
        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { TOUR.indices.forEach { j -> Box(Modifier.size(if (j == i) 18.dp else 8.dp, 8.dp).background(if (j == i) Red else ov.soft, RoundedCornerShape(4.dp))) } }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OvButton(if (i > 0) "Back" else "Skip", { if (i > 0) vm.openSheet(Sheet.Tour(i - 1)) else vm.closeSheet() }, Modifier.weight(1f))
            OvButton(if (last) "Let’s lift" else "Next", { if (last) vm.closeSheet() else vm.openSheet(Sheet.Tour(i + 1)) }, Modifier.weight(1f), bg = Red, fg = Color.White)
        }
    }
}

/** ⋯ on Today (0.17): Lift together, Need an excuse?, session notes. */
@Composable
fun TodayMoreSheet(vm: OverloadViewModel) {
    SheetHead("Today") { vm.closeSheet() }
    OvCard {
        Row16("↺", "Same as last time", "Fill every set from your last session") { vm.closeSheet(); vm.fillFromLast() }
        if (vm.ai.on) Row16("✦", "Ask Coach", "Swaps, short on time, questions") { vm.ai.openCoach() }
        if (vm.crew.on) Row16("👥", "Lift together", "Train with your crew live") { vm.openSheet(Sheet.GroupStart) }
        Row16("🎲", "Need an excuse?", "The generator, for emergencies") { vm.openSheet(Sheet.Excuse((Math.random() * EX_A.size).toInt())) }
        Row16("📝", "Session notes", "Jump to today’s notes") { vm.closeSheet(); vm.ui.value = vm.ui.value.copy(jumpNotes = vm.ui.value.jumpNotes + 1) }
    }
}

@Composable
fun FounderSheet(vm: OverloadViewModel) {
    var code by remember { mutableStateOf("") }
    SheetHead("Founder code", "Cancel") { vm.closeSheet() }
    Sub("For the people who built Overload. A Founder or Co-Founder code unlocks its own plate, title and banners on your account.")
    TextBox(code, { code = it.uppercase() }, "FNDR-XXXXXX-…", Modifier.padding(top = 10.dp))
    OvButton("Unlock", { vm.founderUnlock(code.trim()) }, Modifier.padding(top = 12.dp), bg = Red, fg = Color.White, wide = true)
}

@Composable
fun DashSheet(vm: OverloadViewModel) {
    val d by vm.dash.collectAsState()
    LaunchedEffect(Unit) { vm.loadDash() }
    SheetHead("Owner dashboard") { vm.closeSheet() }
    val j = d
    if (j == null) { Sub("Loading…"); return }
    j["error"]?.let { Sub(it.toString().trim('"')); return }
    fun n(k: String) = j[k]?.toString()?.trim('"') ?: "0"
    listOf("dau" to "today", "wau" to "this week", "mau" to "this month", "total" to "devices ever", "home" to "on home screen", "synced" to "signed in", "crews" to "crews", "wkWorkouts" to "workouts this week").chunked(4).forEach { row ->
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { (k, l) -> StatTile(n(k), l, Modifier.weight(1f)) } } }
    val days = (j["days"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonObject)?.get("n")?.toString()?.toIntOrNull() } ?: emptyList()
    if (days.isNotEmpty()) { Lbl("DAILY ACTIVE · LAST 30 DAYS"); val mx = maxOf(1, days.max())
        Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) { days.forEach { v -> Box(Modifier.weight(1f).fillMaxHeight(v.toFloat() / mx).background(Red, RoundedCornerShape(2.dp))) } } }
    Sub("Counts devices, not people. Nothing here is shared with anyone else.", Modifier.padding(top = 12.dp))
}
