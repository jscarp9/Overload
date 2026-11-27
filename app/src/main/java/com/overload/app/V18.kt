package com.overload.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * 0.18: faster logging (same as last time, copy a set, swipe to remove with Undo), invites,
 * block and report, the privacy page and the co-founder.
 */

// ============================ FASTER LOGGING ============================
/** Fills empty sets from the last time you did each exercise. ids = null means every exercise today. */
fun OverloadViewModel.fillFromLast(ids: List<String>? = null) {
    val d = data.value; val u = ui.value; val key = curKey(u)
    val list = ids ?: todayGroups(d, u).flatten().map { it.id }
    var n = 0; val fill = HashMap<String, List<SetEntry>>()
    list.forEach { id ->
        val l = lastSession(d.sessions, id, key, u.date.toString()) ?: return@forEach
        val la = (l.ex[id] ?: emptyList()).filter { (it.w ?: 0.0) > 0 || (it.r ?: 0) > 0 }; if (la.isEmpty()) return@forEach
        val cur = (session(d, u).ex[id] ?: emptyList()).toMutableList()
        la.forEachIndexed { i, x -> while (cur.size <= i) cur.add(SetEntry()); val c = cur[i]
            if (c.d || c.w != null || c.r != null) return@forEachIndexed
            cur[i] = c.copy(w = x.w ?: c.w, r = x.r ?: c.r); n++ }
        fill[id] = cur
    }
    if (n == 0) { toast("Nothing to fill. No earlier sets for these"); return }
    val names = todayGroups(d, u).flatten().associate { it.id to it.name }
    editSession { s -> s.copy(ex = s.ex + fill, names = s.names + fill.keys.associateWith { names[it] ?: s.names[it] ?: it }) }
    toast("Filled $n set${if (n > 1) "s" else ""} from last time. Check each one off as you go")
}

/** Hold a set's number: copies it to the next set. */
fun OverloadViewModel.copySet(e: Exercise, i: Int) {
    val a = (session().ex[e.id] ?: emptyList()).toMutableList(); val x = a.getOrNull(i)
    if (x == null || (x.w == null && x.r == null)) { toast("Fill this set first"); return }
    while (a.size <= i + 1) a.add(SetEntry())
    if (a[i + 1].d) { toast("Next set is already done"); return }
    a[i + 1] = a[i + 1].copy(w = x.w, r = x.r, q = x.q ?: a[i + 1].q)
    editSession { s -> s.copy(ex = s.ex + (e.id to a), names = s.names + (e.id to e.name)) }
    haptic("tap"); toast("Copied to set ${i + 2}")
}

/** Swipe a set left: it's removed and the Undo bar puts it back exactly for 6 seconds. */
fun OverloadViewModel.delSet(e: Exercise, i: Int) {
    val key = curKey(); val was = session().ex[e.id] ?: return; if (i >= was.size) return
    editSession { s -> s.copy(ex = s.ex + (e.id to was.filterIndexed { j, _ -> j != i })) }
    haptic("tap")
    showUndo("Set ${i + 1} removed") {
        update { d -> val s = d.sessions[key] ?: return@update d; d.copy(sessions = d.sessions + (key to s.copy(ex = s.ex + (e.id to was)))) }; toast("Set restored")
    }
}

/** The Undo bar (0.18), shown above the tab bar. */
@Composable
fun UndoBarView(vm: OverloadViewModel, u: UiState, modifier: Modifier = Modifier) {
    val b = u.undo ?: return
    Row(modifier.padding(bottom = 8.dp).background(Color(0xFF14161C), RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        OvText(b.msg, 14, FontWeight.SemiBold, Color.White, Modifier.weight(1f, fill = false)); Spacer(Modifier.width(14.dp))
        OvText("Undo", 14, FontWeight.ExtraBold, Color(0xFF7FB2FF), Modifier.clickable { vm.doUndo() }.padding(4.dp))
    }
}

// ============================ INVITE, BLOCK, REPORT ============================
fun inviteText(s: CrewState) = "Join my crew \"${s.crew.name}\" on Overload 💪\n1. Get Overload (Android app or the web app)\n2. Crew tab → enter code ${s.crew.code}"
fun shareInvite(ctx: android.content.Context, s: CrewState) =
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_SUBJECT, "Join my crew on Overload").putExtra(Intent.EXTRA_TEXT, inviteText(s)), null))

val REP_WHY = listOf("Spam", "Rude or bullying", "Inappropriate", "Fake or cheating stats", "Something else")
fun OverloadViewModel.toggleBlock(id: String) {
    val on = id in data.value.settings.blocked
    setSettings { it.copy(blocked = if (on) it.blocked - id else it.blocked + id) }
    toast(if (on) "Unblocked" else "Blocked. You won’t see their posts or chat")
}
fun OverloadViewModel.report(id: String, why: String) = viewModelScopeLaunch {
    val r = crew.call("report", buildJsonObject { put("target", id); put("why", why) })
    if (r.err == null) { toast("Reported. Thanks for keeping the crew good"); openSheet(Sheet.ProfilePage(id)) } else toast("Could not send the report")
}
fun OverloadViewModel.blocked(id: String?) = id != null && id in data.value.settings.blocked

@Composable
fun ReportSheet18(vm: OverloadViewModel, id: String) {
    val m = vm.crew.member2(id)
    SheetHead("Report ${m?.name ?: "this person"}", "Cancel") { vm.openSheet(Sheet.ProfilePage(id)) }
    Sub("Only your crew owner sees reports. They won’t know it was you unless the owner checks.", Modifier.padding(bottom = 8.dp))
    OvCard { REP_WHY.forEach { w -> Row16("🚩", w) { vm.report(id, w) } } }
}

/** Report and Block buttons at the bottom of someone's crew profile. */
@Composable
fun BlockReportRow(vm: OverloadViewModel, id: String) = Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), horizontalArrangement = Arrangement.Center) {
    OvButton("Report", { vm.openSheet(Sheet.ReportMember(id)) }, small = true); Spacer(Modifier.width(8.dp))
    OvButton(if (vm.blocked(id)) "Unblock" else "Block", { vm.toggleBlock(id) }, small = true)
}

/** Reports, for the crew owner in crew settings. */
@Composable
fun ReportsCard(vm: OverloadViewModel, s: CrewState) {
    val r = s.crew.reports.reversed(); if (r.isEmpty()) return
    OvCard(Modifier.padding(top = 12.dp)) {
        H3("🚩 Reports (${r.size})")
        r.take(10).forEach { x -> Column(Modifier.padding(vertical = 6.dp)) {
            OvText("${vm.crew.member2(x.target)?.name ?: "Someone"} · ${x.why}", 14, FontWeight.Bold)
            Sub("by ${vm.crew.member2(x.by)?.name ?: "Someone"} · ${ago(x.ts)}") } }
        Sub("Reports are only visible to you as the crew owner. Nothing gets deleted automatically.", Modifier.padding(top = 4.dp))
    }
}

// ============================ PRIVACY ============================
@Composable
fun PrivacySheet(vm: OverloadViewModel) {
    SheetHead("Privacy") { vm.closeSheet() }
    Sub("Last updated November 2026. Plain version, no legal speak.")
    @Composable fun sec(h: String, p: String) { OvText(h, 16, FontWeight.ExtraBold, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)); OvText(p, 14) }
    sec("What stays on your phone", "Your workouts, plan, notes and settings are saved on your device. If you never sign in or join a crew, nothing leaves your phone except the basic usage counts below.")
    sec("What goes to the Overload server", "If you sign in, your log is backed up so you can restore it on another phone. If you join a crew, your name, profile, rank, workout summaries, posts, reactions and chat messages are shared with that crew. The server runs on Cloudflare.")
    sec("Usage stats", "The app counts how it is used so it can get better: a random device ID, phone type, app version and counts like workouts and sets. If you are signed in or in a crew, your name can show next to your device in the owner’s private dashboard.")
    sec("Overload AI", "AI is off until you allow it. When you message Coach, your message, your plan and your last few workouts are sent. When you ask for form tips, only the exercise name is sent. Once a week your crew’s weekly totals (first names, days trained, sets, volume, PRs) are sent to write the crew recap. When you use the plan builder, the photos or text you add, your lift names and recent best sets are sent through the Overload server to Anthropic’s Claude to build your plan. If coach notes are on, the sets, weights and reps from a finished workout or week are sent the same way to write a short note. Your name, crew, chats and account details are never sent. Anthropic processes this only to answer the request. Turn it off any time in Settings → Overload AI.")
    sec("Heart rate", "If you turn on heart rate, Overload reads it from Health Connect on this phone to show it after a workout. It stays on your phone.")
    sec("What we never do", "No ads, no selling or renting your data, no tracking across other apps or sites.")
    sec("Your choices", "You can stay signed out, leave a crew, block people and export everything in Settings → Backup and restore. To have your account data removed from the server, contact Jake Scarpa, the app owner.")
    Sub("© 2026 Jake Scarpa. All rights reserved.", Modifier.padding(top = 14.dp))
}

fun OverloadViewModel.viewModelScopeLaunch(block: suspend () -> Unit) = viewModelScope.launch { block() }
