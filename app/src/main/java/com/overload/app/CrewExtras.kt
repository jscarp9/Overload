package com.overload.app

import android.content.Context
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

/*
 * 0.14-0.16 crew features on top of CrewService: crew rank, trophies, records, boss raids, lifting
 * together, the Hall of Fame, respect, nicknames, roles, jerseys, crest, rename, crew backups and the
 * shame plate. All talk to the same server endpoints as the web app.
 */

private val CrewService.cScope get() = vmRef.viewModelScope
private fun CrewService.fireC(p: String, body: JsonObject, done: (JsonObject) -> Unit = {}) = cScope.launch { done(call(p, body)) }

/** A member, or someone who left (shows their old name). */
fun CrewService.member2(id: String?): Member? { val c = cs ?: return null; c.members.firstOrNull { it.id == id }?.let { return it }; val f = c.former[id] ?: return null; return Member(id!!, f) }
fun CrewService.isOwner() = cs?.crew?.owner != null && cs?.crew?.owner == vmRef.myId()
fun CrewService.myMember() = cs?.members?.firstOrNull { it.id == vmRef.myId() }
fun CrewService.shamed(id: String?) = cs?.crew?.shame?.let { it.target == id && it.until > System.currentTimeMillis() } == true

class CrewSum(val ms: List<Member>, val vol: Double, val wo: Int, val streak: Int)
fun CrewService.crewSum(): CrewSum {
    val ms = cs?.members?.filter { it.profile != null } ?: emptyList()
    return CrewSum(ms, ms.sumOf { mine(it.profile!!.lifeVol, it.profile.unit) }, ms.sumOf { it.profile!!.workouts }, if (ms.isEmpty()) 0 else ms.minOf { it.profile!!.streak })
}
val CREW_RANK_PTS = listOf(0, 400, 1200, 2500, 4500, 7500, 11500, 17000)
class CrewRank(val r: Int, val pts: Int, val nx: Int?, val frac: Double)
fun CrewService.crewRankInfo(): CrewRank {
    val c = cs?.crew ?: return CrewRank(0, 0, 400, 0.0)
    val pts = (c.xp + c.trophies.size * 400).toInt(); val r = maxOf(c.rank, CREW_RANK_PTS.count { pts >= it } - 1).coerceIn(0, 7); val nx = CREW_RANK_PTS.getOrNull(r + 1)
    return CrewRank(r, pts, nx, if (nx != null) minOf(1.0, (pts - CREW_RANK_PTS[r]).toDouble() / (nx - CREW_RANK_PTS[r])) else 1.0)
}
val CTROPHY = listOf(
    listOf("mpc", "Million Pound Club", "🏋️", "1M,5M,10M", "Lift 1,000,000 lb together"), listOf("unbroken", "Unbroken", "⛓", "", "Everyone trains in the same week"),
    listOf("prstorm", "PR Storm", "⚡", "", "10 PRs as a crew in one week"), listOf("fullhouse", "Full House", "🏠", "", "Everyone (3+) trains on the same day"),
    listOf("ironweek", "Iron Week", "🛡", "", "Everyone trains 3+ days in one week"), listOf("century", "Century Club", "💯", "100,500,1000", "100 crew workouts"),
    listOf("comeback", "Comeback Crew", "↩", "", "Someone comes back after 2+ weeks off")
)

// ---------- owner tools ----------
fun CrewService.rename(name: String) = fireC("rename", buildJsonObject { put("name", name) }) { if (it.err == null) vmRef.toast("Crew renamed to $name") }
fun CrewService.saveCrest(c: Crest) = fireC("crest", buildJsonObject { put("crest", vmRef.jsonCodec.encodeToJsonElement(c)) }) { if (it.err == null) { vmRef.toast("Crest saved"); vmRef.closeSheet() } }
fun CrewService.saveStyle(motto: String, c1: String) = fireC("crewstyle", buildJsonObject { put("motto", motto); put("c1", c1); put("c2", c1) }) { if (it.err == null) vmRef.toast("Crew style saved") }
fun CrewService.setScene(k: String) { if (!sceneOK(cs, k)) { vmRef.toast("🔒 ${SCENES[k]?.second ?: ""}"); return }; banner(k) }
val crewSnaps = MutableStateFlow<List<String>?>(null)
fun CrewService.loadSnaps() { crewSnaps.value = null; fireC("snaps", JsonObject(emptyMap())) { r -> crewSnaps.value = (r["days"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("day")?.jsonPrimitive?.contentOrNull ?: (it as? JsonPrimitive)?.contentOrNull } ?: emptyList() } }
fun CrewService.restoreSnap(day: String) = fireC("snaprestore", buildJsonObject { put("day", day) }) { r ->
    vmRef.toast(if (r.err != null) "Could not restore" else "Restored. ${r["added"]?.jsonPrimitive?.intOrNull ?: 0} item${if (r["added"]?.jsonPrimitive?.intOrNull == 1) "" else "s"} came back"); vmRef.closeSheet() }
val crewTrash = MutableStateFlow<List<JsonObject>?>(null)
fun CrewService.loadTrash() { crewTrash.value = null; if (!on) { crewTrash.value = emptyList(); return }; fireC("trash", JsonObject(emptyMap())) { r -> crewTrash.value = (r["items"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: emptyList() } }
fun CrewService.restoreTrash(id: String) = fireC("restore", buildJsonObject { put("item", id) }) { r -> if (r.err == null) { vmRef.toast("Restored to the crew"); crewTrash.value = crewTrash.value?.filter { it["id"]?.jsonPrimitive?.contentOrNull != id }; vmRef.chat.reset() } }
fun CrewService.shameVote(id: String) = fireC("shame", buildJsonObject { put("target", id) }) { r -> if (r.err == null) vmRef.toast(if (shamed(id)) "Shame plate awarded 🩷" else "Vote counted") }

// ---------- crew profiles ----------
fun CrewService.respect(id: String) = fireC("respect", buildJsonObject { put("target", id) }) { if (it.err == null) { vmRef.haptic("heavy"); vmRef.toast("💪 Respect given") } }
fun CrewService.nick(id: String, n: String) = fireC("nick", buildJsonObject { put("target", id); put("nick", n) }) { if (it.err == null) vmRef.toast(if (n.isNotEmpty()) "Nickname vote counted" else "Vote removed") }
fun CrewService.role(id: String, r: String) = fireC("role", buildJsonObject { put("target", id); put("role", r) })
fun CrewService.jersey(n: Int) = fireC("jersey", buildJsonObject { put("num", n) }) { r -> vmRef.toast(if (r.err != null) (if (r.err == "taken") "That number is taken" else "Could not claim it") else "You’re #$n") }
fun CrewService.hofMember(id: String) = fireC("hof", buildJsonObject { put("kind", "member"); put("ref", id); put("text", "") }) { if (it.err == null) vmRef.toast("${member2(id)?.name ?: "They"} is in the Hall of Fame") }
fun CrewService.hofPost(id: String, text: String) = fireC("hof", buildJsonObject { put("kind", "post"); put("ref", id); put("text", text) }) { if (it.err == null) { vmRef.toast("Inducted into the Hall of Fame 🏛"); vmRef.closeSheet() } }
fun CrewService.hofRemove(id: String) = fireC("hof", buildJsonObject { put("remove", id) })

// ---------- lift together ----------
private fun prefs(c: Context) = c.getSharedPreferences("overload", Context.MODE_PRIVATE)
val grpState = MutableStateFlow<String?>(null)
fun CrewService.grp(): String? { if (grpState.value == null) grpState.value = prefs(vmRef.appContext).getString("grp", "") ?: ""; return grpState.value?.ifEmpty { null } }
fun CrewService.setGrp(g: String?) { grpState.value = g ?: ""; prefs(vmRef.appContext).edit().putString("grp", g ?: "").apply() }
/** What you share while lifting together: sets, volume, current exercise, rest end. */
fun CrewService.grpLive(): JsonObject {
    val t = today(); val s = vmRef.data.value.sessions[sid(iso(t), DAYS[dayIdx(t)])] ?: return JsonObject(emptyMap())
    var sets = 0; var vol = 0.0; var ex = ""
    s.ex.forEach { (id, a) -> a.forEach { x -> if (full(x)) { sets++; vol += x.w!! * x.r!!; ex = vmRef.exName(id) } } }
    val r = vmRef.ui.value.rest
    return buildJsonObject { put("g", grp() ?: ""); put("sets", sets); put("vol", jsRound(vol)); put("ex", ex); put("rest", if (r != null && r.endAt > System.currentTimeMillis()) r.endAt else 0L); put("prs", s.prShown.size); put("unit", vmRef.unit) }
}
fun CrewService.groupStart() = fireC("group/start", buildJsonObject { put("sync", true) }) { r ->
    val g = r["gid"]?.jsonPrimitive?.contentOrNull ?: run { vmRef.toast("Could not start a group"); return@fireC }
    setGrp(g); vmRef.closeSheet(); vmRef.setTab("log"); sendLive(); vmRef.toast("Group started. Your crew got a ping") }
fun CrewService.groupJoin(g: String) = fireC("group/join", buildJsonObject { put("gid", g) }) { r -> if (r.err == null) { setGrp(g); vmRef.closeSheet(); vmRef.toast("Lifting together 👥"); sendLive() } else vmRef.toast("That group already finished") }
fun CrewService.groupLeave() { val g = grp() ?: return; setGrp(null); fireC("group/leave", buildJsonObject { put("gid", g) }) }
fun CrewService.groupEnd() {
    val g = grp() ?: return; val me = vmRef.myId() ?: return; val l = grpLive()
    val kg = vmRef.unit == "kg"
    fireC("group/end", buildJsonObject { put("gid", g); putJsonObject("rows") { putJsonObject(me) {
        put("sets", l["sets"]?.jsonPrimitive?.intOrNull ?: 0); put("vol", jsRound((l["vol"]?.jsonPrimitive?.doubleOrNull ?: 0.0) * if (kg) 2.20462 else 1.0)); put("prs", l["prs"]?.jsonPrimitive?.intOrNull ?: 0) } } }) { r ->
        setGrp(null)
        (r["post"] as? JsonObject)?.let { p -> runCatching { vmRef.jsonCodec.decodeFromJsonElement(Post.serializer(), p) }.getOrNull()?.let { vmRef.openSheet(Sheet.GroupCard(it)) } }
    }
}
