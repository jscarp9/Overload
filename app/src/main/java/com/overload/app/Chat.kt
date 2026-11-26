package com.overload.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Crew chat (0.14): same server endpoints as the web app (chat/get, chat/send, chat/react ...). */
class ChatService(private val vm: OverloadViewModel) {
    val msgs = MutableStateFlow<List<ChatMsg>>(emptyList())
    val loaded = MutableStateFlow(false)
    val more = MutableStateFlow(false)
    val reply = MutableStateFlow<ChatMsg?>(null)
    private var page = 0
    private var loading = false
    private val scope get() = vm.viewModelScope
    private val crew get() = vm.crew

    fun reset() { msgs.value = emptyList(); loaded.value = false; more.value = false; page = 0; reply.value = null }
    fun isOpen() = vm.ui.value.let { it.tab == "crew" && !it.settings && !it.profile && (it.sub["crew"] ?: "feed") == "chat" }

    fun unread(): Int {
        val cs = vm.crewState.value ?: return 0; val me = vm.myId(); val seen = vm.data.value.settings.chatSeen
        return cs.crew.chatRecent.count { it.ts > seen && it.by != me }
    }

    fun load(older: Boolean = false) {
        if (loading || !crew.on) return; loading = true
        scope.launch {
            val r = crew.call("chat/get", buildJsonObject { if (older) put("before", maxOf(0, page - 1)) })
            loading = false; if (r.err != null) return@launch
            val got = (r["msgs"] as? JsonArray)?.mapNotNull(::parseChatMsg) ?: emptyList()
            msgs.update { cur ->
                if (older) { val have = cur.map { it.id }.toSet(); got.filter { it.id !in have } + cur }
                else { val m = LinkedHashMap<String, ChatMsg>(); cur.forEach { m[it.id] = it }; got.forEach { m[it.id] = it }; m.values.sortedBy { it.ts } }
            }
            page = r["page"]?.jsonPrimitive?.intOrNull ?: 0; more.value = r["more"]?.jsonPrimitive?.booleanOrNull ?: false; loaded.value = true; seen()
        }
    }

    fun seen() {
        val cs = vm.crewState.value ?: return
        val t = maxOf(cs.crew.chatLast, msgs.value.maxOfOrNull { it.ts } ?: 0L)
        if (t > vm.data.value.settings.chatSeen) vm.setSettings { it.copy(chatSeen = t) }
    }

    /** A message arrived on the live socket. */
    fun onMsg(m: ChatMsg) {
        vm.crewState.update { cs -> cs?.copy(crew = cs.crew.copy(chatRecent = (cs.crew.chatRecent + ChatRecent(m.ts, m.by)).takeLast(40), chatLast = m.ts)) }
        if (loaded.value && msgs.value.none { it.id == m.id }) msgs.update { it + m }
        if (isOpen()) seen()
    }
    fun onUpd(m: ChatMsg) = msgs.update { l -> l.map { if (it.id == m.id) m else it } }

    fun send(text: String, card: ChatCard? = null, done: (Boolean) -> Unit = {}) {
        val t = text.trim(); if (t.isEmpty() && card == null) return
        val rep = reply.value; reply.value = null; vm.haptic("tap")
        val tmp = ChatMsg("tmp" + System.currentTimeMillis(), vm.myId() ?: "", System.currentTimeMillis(), t, card, pending = true)
        msgs.update { it + tmp }
        scope.launch {
            val r = crew.call("chat/send", buildJsonObject { put("text", t); rep?.let { put("reply", it.id) }; card?.let { put("card", vm.jsonCodec.encodeToJsonElement(it)) } })
            msgs.update { l -> l.filter { it.id != tmp.id } }
            val e = r.err
            if (e != null) { vm.toast(when (e) { "muted" -> "You’re muted for now"; "slow down" -> "Slow down a sec"; else -> "Message not sent. Check your connection" }); done(false); return@launch }
            parseChatMsg(r["msg"])?.let { m -> if (msgs.value.none { it.id == m.id }) msgs.update { it + m } }
            vm.setSettings { it.copy(chatN = it.chatN + 1) }; done(true)
        }
    }
    fun react(mid: String, k: String) = scope.launch { crew.call("chat/react", buildJsonObject { put("mid", mid); put("emoji", k) }) }
    fun delete(mid: String) = scope.launch { crew.call("chat/delete", buildJsonObject { put("mid", mid) }); vm.toast("Deleted. It’s in the crew Trash for 30 days") }
    fun mute(target: String, off: Boolean) = scope.launch {
        crew.call("chat/mute", buildJsonObject { put("target", target); put("hours", 24); put("off", if (off) 1 else 0) }); vm.toast(if (off) "Unmuted" else "Muted for 24 hours"); crew.sync(true)
    }
    fun pin(mid: String) = scope.launch { crew.call("chat/pin", buildJsonObject { put("mid", mid) }); crew.sync(true) }
    fun shareWorkout() {
        val s = crew.lastFinished() ?: vm.ctx().rs.lastOrNull() ?: run { vm.toast("Finish a workout first"); return }
        val d = vm.jsonCodec.decodeFromJsonElement(PostData.serializer(), crew.postData(s))
        send("", ChatCard(d.day, d.color, d.min, d.sets, d.vol, d.prs, d.unit, d.top))
    }
}

// ============================ UI ============================
private val tFmt = DateTimeFormatter.ofPattern("h:mm a")
private val dFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
private fun dayOf(ts: Long) = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()

@Composable
fun ChatTab(vm: OverloadViewModel, c: Ctx, s: CrewState) {
    val list by vm.chat.msgs.collectAsState(); val loaded by vm.chat.loaded.collectAsState(); val more by vm.chat.more.collectAsState(); val rep by vm.chat.reply.collectAsState()
    LaunchedEffect(Unit) { if (!loaded) vm.chat.load(); vm.chat.seen() }
    LaunchedEffect(list.size) { vm.chat.seen() }
    val me = vm.myId()
    if (s.pins.isNotEmpty()) {
        var open by remember { mutableStateOf(false) }
        OvCard { OvText("📌 Pinned · ${s.pins.size}", 14, FontWeight.Bold, modifier = Modifier.clickable { open = !open })
            if (open) s.pins.forEach { x -> OvText("${vm.crew.member(x.by)?.name ?: "?"}  ${x.text.ifEmpty { x.card?.let { "Workout: ${it.day}" } ?: "" }}", 13, modifier = Modifier.padding(top = 6.dp)) } }
    }
    OvCard {
        if (!loaded) Sub("Loading chat…", Modifier.fillMaxWidth().padding(vertical = 24.dp))
        else if (list.isEmpty()) Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OvText("Say what's up", 17, FontWeight.Bold); Sub("Chat with your crew. Tag someone with @, reply to a message, or share your last workout.")
        } else {
            if (more) OvButton("Load older messages", { vm.chat.load(true) }, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 10.dp), small = true)
            var lastDay: java.time.LocalDate? = null
            list.forEachIndexed { i, m ->
                val d = dayOf(m.ts)
                if (d != lastDay) { lastDay = d; Sub(if (d == java.time.LocalDate.now()) "Today" else dFmt.format(d), Modifier.fillMaxWidth().padding(vertical = 8.dp), ov.mute) }
                val prev = list.getOrNull(i - 1)?.takeIf { dayOf(it.ts) == d }
                MsgRow(vm, c, m, prev, me)
            }
        }
    }
    // composer
    val mutedUntil = s.crew.muted[me] ?: 0L
    if (mutedUntil > System.currentTimeMillis()) { Sub("The owner muted you until ${tFmt.format(Instant.ofEpochMilli(mutedUntil).atZone(ZoneId.systemDefault()))}", Modifier.fillMaxWidth().padding(8.dp)); return }
    var text by remember { mutableStateOf("") }
    OvCard {
        rep?.let { r -> Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Sub("Replying to ${vm.crew.member(r.by)?.name ?: "?"}: ${r.text.take(60)}", Modifier.weight(1f)); OvText("✕", 16, color = ov.mute, modifier = Modifier.clickable { vm.chat.reply.value = null }.padding(6.dp)) } }
        val ment = Regex("@([^@\\s]*)$").find(text)
        if (ment != null) Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.members.filter { it.id != me && it.name.startsWith(ment.groupValues[1], true) }.take(4).forEach { m ->
                OvButton("@${m.name}", { text = text.replace(Regex("@[^@\\s]*$"), "") + "@${m.name} " }, small = true) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OvButton("＋", { vm.openSheet(Sheet.ChatPlus) }, small = true)
            TextBox(text, { text = it.take(500) }, "Message ${s.crew.name}", Modifier.weight(1f))
            OvButton("↑", { val t = text; text = ""; vm.chat.send(t) { ok -> if (!ok) text = t } }, bg = Red, fg = Color.White)
        }
    }
}

@Composable
private fun MsgRow(vm: OverloadViewModel, c: Ctx, m: ChatMsg, prev: ChatMsg?, me: String?) {
    val mine = m.by == me; val group = prev != null && prev.by == m.by && m.ts - prev.ts < 5 * 60_000
    val who = vm.crew.member(m.by)
    Row(Modifier.fillMaxWidth().padding(top = if (group) 2.dp else 8.dp), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        if (!mine) { if (!group) MemberAvatar(vm, m.by, who?.name ?: "Former member", 28) else Spacer(Modifier.width(28.dp)); Spacer(Modifier.width(6.dp)) }
        Column(Modifier.widthIn(max = 280.dp), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            if (!mine && !group) NameText(who?.name ?: "Former member", who?.profile?.nameFx ?: "", 12, shame = vm.crew.shamed(m.by))
            if (m.del) { OvText("Message deleted", 13, color = ov.mute, modifier = Modifier.background(ov.soft, RoundedCornerShape(14.dp)).padding(10.dp)); return@Column }
            m.reply?.let { r -> Sub("↪ ${vm.crew.member(r.by)?.name ?: "?"} ${r.text.take(60)}", Modifier.padding(bottom = 2.dp)) }
            Column(Modifier.clip(RoundedCornerShape(16.dp)).background(if (mine) Red else ov.soft).clickable { if (!m.pending) vm.openSheet(Sheet.Msg(m.id)) }.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (m.text.isNotEmpty()) Text(mentions(vm, m.text), color = if (mine) Color.White else ov.ink, fontSize = 15.sp)
                m.card?.let { k -> Column(Modifier.padding(top = 4.dp).background(pc(k.color).base.copy(.18f), RoundedCornerShape(12.dp)).padding(8.dp)) {
                    OvText(k.day.ifEmpty { "Workout" }, 14, FontWeight.Bold, if (mine) Color.White else ov.ink)
                    OvText("${if (k.min > 0) k.min else "–"} min · ${k.sets} sets · ${kfmt(toMine(k.vol, k.unit, c.unit))} ${c.unit} · ${k.prs} PRs", 12, color = if (mine) Color.White.copy(.85f) else ov.mute)
                    k.top.forEach { t -> OvText("${t.n} ${(toMine(t.w, k.unit, c.unit) * 10).roundToInt() / 10.0}×${t.r}${if (t.pr > 0) " · PR" else ""}", 12, color = if (mine) Color.White.copy(.85f) else ov.mute) }
                } }
                OvText(tFmt.format(Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault())) + if (m.pending) " · sending" else "", 10, color = if (mine) Color.White.copy(.7f) else ov.mute)
            }
            val rx = m.reacts.filter { it.value.isNotEmpty() }
            if (rx.isNotEmpty()) Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rx.forEach { (k, a) -> val on = me != null && me in a
                    OvText((RE14[k] ?: "") + if (a.size > 1) " ${a.size}" else "", 12, FontWeight.Bold, modifier = Modifier.background(if (on) Red.copy(.15f) else ov.soft, RoundedCornerShape(10.dp))
                        .then(if (on) Modifier.border(1.dp, Red.copy(.5f), RoundedCornerShape(10.dp)) else Modifier).clickable { vm.chat.react(m.id, k) }.padding(horizontal = 8.dp, vertical = 3.dp)) }
            }
        }
    }
}

private fun mentions(vm: OverloadViewModel, t: String) = buildAnnotatedString {
    val names = vm.crewState.value?.members?.map { it.name }?.filter { it.isNotBlank() }?.sortedByDescending { it.length } ?: emptyList()
    var i = 0
    while (i < t.length) {
        val hit = if (t[i] == '@') names.firstOrNull { t.regionMatches(i + 1, it, 0, it.length, true) } else null
        if (hit != null) { withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append("@" + t.substring(i + 1, i + 1 + hit.length)) }; i += hit.length + 1 }
        else { append(t[i]); i++ }
    }
}

/** ⋯ on a message: react, reply, delete, pin, mute. */
@Composable
fun MsgSheet(vm: OverloadViewModel, c: Ctx, u: UiState, id: String) {
    val list by vm.chat.msgs.collectAsState(); val m = list.firstOrNull { it.id == id }
    if (m == null) { Sub("Message gone."); OvButton("Close", { vm.closeSheet() }, wide = true); return }
    val me = vm.myId(); val mine = m.by == me; val cs = vm.crewState.value; val own = cs?.crew?.owner == me
    SheetHead("Message") { vm.closeSheet() }
    Sub(m.text.ifEmpty { "Workout card" }.take(120))
    FlowRowC(Modifier.padding(top = 10.dp)) { c.reactKeys().forEach { k -> OvButton(RE14[k] ?: "", { vm.closeSheet(); vm.chat.react(m.id, k) }, small = true) } }
    FlowRowC(Modifier.padding(top = 12.dp)) {
        OvButton("↪ Reply", { vm.chat.reply.value = m; vm.closeSheet() }, small = true)
        if (mine || own) OvButton(if (u.confirm == "md") "Tap to delete" else "Delete", { if (u.confirm != "md") vm.setConfirm("md") else { vm.setConfirm(null); vm.chat.delete(m.id); vm.closeSheet() } }, small = true)
        val myRole = cs?.members?.firstOrNull { it.id == me }?.role
        if (own || myRole == "Co-captain") OvButton("📌 ${if (cs?.pins?.any { it.id == m.id } == true) "Unpin" else "Pin"}", { vm.chat.pin(m.id); vm.closeSheet() }, small = true)
        if (own && !mine) { val on = (cs?.crew?.muted?.get(m.by) ?: 0L) > System.currentTimeMillis()
            OvButton(if (on) "Unmute" else "Mute 24h", { vm.chat.mute(m.by, on); vm.closeSheet() }, small = true) }
    }
}

/** ＋ in the chat bar: share your workout or a gym excuse. */
@Composable
fun ChatPlusSheet(vm: OverloadViewModel) {
    SheetHead("Share to chat", "Cancel") { vm.closeSheet() }
    val s = vm.crew.lastFinished() ?: vm.ctx().rs.lastOrNull()
    if (s != null) CardButton("🏋️", "Share your ${vm.ctx().plan.getOrNull(DAYS.indexOf(s.day))?.name ?: "workout"} workout", s.date) { vm.closeSheet(); vm.chat.shareWorkout() }
    CardButton("🎲", "Send a gym excuse", "For when you’re skipping") { vm.openSheet(Sheet.Excuse((Math.random() * EX_A.size).toInt())) }
}

/** Simple wrapping row (FlowRow is still experimental in this Compose version). */
@Composable
fun FlowRowC(modifier: Modifier = Modifier, gap: Int = 6, content: @Composable () -> Unit) =
    androidx.compose.ui.layout.Layout(content, modifier) { ms, cons ->
        val g = gap.dp.roundToPx(); val maxW = cons.maxWidth
        val ps = ms.map { it.measure(cons.copy(minWidth = 0, minHeight = 0)) }
        var x = 0; var y = 0; var rowH = 0; val pos = ArrayList<Pair<Int, Int>>()
        ps.forEach { p -> if (x > 0 && x + p.width > maxW) { x = 0; y += rowH + g; rowH = 0 }; pos += x to y; x += p.width + g; rowH = maxOf(rowH, p.height) }
        layout(maxW, if (ps.isEmpty()) 0 else y + rowH) { ps.forEachIndexed { i, p -> p.place(pos[i].first, pos[i].second) } }
    }
