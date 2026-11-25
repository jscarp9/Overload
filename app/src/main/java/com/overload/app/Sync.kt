package com.overload.app

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File

/** What the last successful sync looked like, so only changes get sent (same idea as the web app's syncbase). */
@Serializable
data class SyncBase(
    val seq: Long = 0, val first: Boolean = true,
    val sessions: Map<String, String> = emptyMap(),
    val plan: String? = null, val notes: String? = null, val settings: String? = null, val flags: String? = null,
    val planT: Long = 0, val notesT: Long = 0, val settingsT: Long = 0, val flagsT: Long = 0,
    /** Settings keys from other devices that this app doesn't use, kept so they survive a round trip. */
    val extra: String? = null
)

data class SyncStatus(val state: String = "off", val at: Long = 0, val err: String = "")

/** Keys never synced: per-device things (same list as the web app, plus Android-only ones). */
private val SYNC_SKIP = setOf("push", "crewPings", "acct", "hideSafari", "seenVer", "schema", "lastBackup", "bellSeen", "restAlerts", "heartRate", "seenNews")

/**
 * Cloud backup and sync through the same server as the web app (recovery code sign-in).
 * Data goes up in the web app's shapes ({days}, {list}, {d}) so both apps can share one account.
 */
class SyncService(private val vm: OverloadViewModel) {
    private val scope get() = vm.viewModelScope
    private val baseFile by lazy { File(vm.appContext.filesDir, "overload-syncbase.json") }
    private val json get() = vm.jsonCodec
    val status = MutableStateFlow(SyncStatus())
    val cloudDays = MutableStateFlow<List<String>?>(null)
    private var syncing = false
    private var again = false
    private var queued: Job? = null
    private var poll: Job? = null
    /** Set when the person confirmed that missing workouts really should be deleted. */
    var allowDel = false

    private val acct get() = vm.data.value.settings.acct

    fun start() { if (acct != null) { status.value = SyncStatus("ok"); scope.launch { delay(1200); now(true) }; startPoll() } }
    fun onForeground() { if (acct != null) queue(500) }
    private fun startPoll() { poll?.cancel(); if (acct == null) return; poll = scope.launch { while (isActive) { delay(90_000); now(false) } } }
    /** Called after any local change. */
    fun queue(ms: Long = 3500) { if (acct == null) return; queued?.cancel(); queued = scope.launch { delay(ms); now(false) } }

    private suspend fun vault(p: String, body: JsonObject): JsonObject {
        val (code, j) = postJson("$SERVER/vault$p", body)
        if (code == 0) throw SyncError("offline", 0)
        if (code !in 200..299) throw SyncError(j.err ?: "HTTP $code", code)
        return j
    }
    class SyncError(msg: String, val status: Int) : Exception(msg)
    private fun acctBody(extra: JsonObjectBuilder.() -> Unit = {}) = buildJsonObject { acct?.let { put("acct", it.id); put("token", it.token) }; extra() }

    private suspend fun loadBase(): SyncBase = withContext(Dispatchers.IO) { runCatching { json.decodeFromString<SyncBase>(baseFile.readText()) }.getOrNull() ?: SyncBase() }
    private suspend fun saveBase(b: SyncBase?) = withContext(Dispatchers.IO) { runCatching { if (b == null) baseFile.delete() else baseFile.writeText(json.encodeToString(b)) } }

    // ---------- converting between this app's data and the web app's shapes ----------
    private fun sessionOut(s: Session): JsonElement = json.encodeToJsonElement(s)
    private fun sessionIn(el: JsonElement): Session? = runCatching { json.decodeFromJsonElement<Session>(vm.cleanWeb(el)) }.getOrNull()
    private fun same(a: Session?, b: Session?) = a != null && b != null && a.copy(syncT = null, syncXt = null) == b.copy(syncT = null, syncXt = null)
    private fun planOut(p: List<DayPlan>) = buildJsonObject { put("days", json.encodeToJsonElement(p)) }
    private fun notesOut(n: List<Note>) = buildJsonObject { put("list", json.encodeToJsonElement(n)) }
    private fun flagsOut(f: Map<String, Map<String, Int>>) = buildJsonObject { put("d", json.encodeToJsonElement(f)) }
    private fun settingsOut(extra: JsonObject?): JsonObject {
        val mine = json.encodeToJsonElement(vm.data.value.settings).jsonObject
        val out = LinkedHashMap<String, JsonElement>()
        extra?.forEach { (k, v) -> if (k !in SYNC_SKIP) out[k] = v }
        mine.forEach { (k, v) -> if (k !in SYNC_SKIP && v !is JsonNull) out[k] = v }
        return JsonObject(out)
    }
    private fun strip(o: JsonObject) = JsonObject(o.filterKeys { it != "_t" && it != "_xt" && it != "_v" })

    // ---------- the sync ----------
    fun now(force: Boolean, done: (() -> Unit)? = null) = scope.launch { run(force); done?.invoke() }

    private suspend fun run(force: Boolean) {
        val a = acct ?: return
        val d0 = vm.data.value
        if (d0.plan == null && d0.sessions.isEmpty() && !force) return
        if (syncing) { again = true; return }
        syncing = true; status.value = status.value.copy(state = "pending")
        try {
            var base = loadBase(); val now = System.currentTimeMillis()
            if (base.first) {
                val r = vault("/sync", acctBody { put("since", 0); put("today", iso(today())); putJsonObject("changes") {} })
                base = applyRemote(r["changes"] as? JsonObject, base, true, null, null).copy(seq = r["seq"]?.jsonPrimitive?.longOrNull ?: 0, first = false)
                saveBase(base)
            }
            val sessOut = LinkedHashMap<String, JsonElement>(); var dels = 0; var live = 0
            val stamped = HashMap<String, Session>()
            vm.data.value.sessions.forEach { (k, s) ->
                live++
                val bo = base.sessions[k]?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
                val bs = bo?.takeIf { it["_del"] == null }?.let(::sessionIn)
                if (bs != null && same(bs, s)) return@forEach
                val xt = (s.syncXt ?: emptyMap()).toMutableMap()
                s.ex.keys.forEach { id -> if (bs == null || bs.ex[id] != s.ex[id]) xt[id] = now }
                val ns = s.copy(syncT = now, syncXt = xt); stamped[k] = ns; sessOut[k] = sessionOut(ns)
            }
            base.sessions.forEach { (k, j) -> if (vm.data.value.sessions[k] == null) {
                val bo = runCatching { Json.parseToJsonElement(j).jsonObject }.getOrNull()
                if (bo != null && bo["_del"] == null) { sessOut[k] = buildJsonObject { put("_del", 1); put("_t", now) }; dels++ } } }
            if (dels > 5 && dels > (live + dels) * .2 && !allowDel) {
                status.value = status.value.copy(state = "held"); syncing = false
                if (vm.ui.value.sheet == null) vm.openSheet(Sheet.SyncGuard(dels)); return
            }
            allowDel = false
            if (stamped.isNotEmpty()) vm.update { d -> d.copy(sessions = d.sessions + stamped.filterKeys { d.sessions[it] != null }) }
            val extra = base.extra?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
            val changes = LinkedHashMap<String, JsonElement>(); changes["sessions"] = JsonObject(sessOut)
            fun cmp(kind: String, v: JsonObject, old: String?): String? { val j = v.toString(); if (j != old) { changes[kind] = JsonObject(v + ("_t" to JsonPrimitive(now))); return j }; return null }
            val d = vm.data.value
            val pj = d.plan?.let { cmp("plan", planOut(it), base.plan) }
            val nj = cmp("notes", notesOut(d.notes), base.notes)
            val sj = cmp("settings", settingsOut(extra), base.settings)
            val fj = cmp("flags", flagsOut(d.flags), base.flags)
            val sentS = sessOut.mapValues { it.value.toString() }
            val r = vault("/sync", acctBody { put("since", base.seq); put("today", iso(today())); put("changes", JsonObject(changes)) })
            base = base.copy(sessions = base.sessions + sentS, plan = pj ?: base.plan, notes = nj ?: base.notes, settings = sj ?: base.settings, flags = fj ?: base.flags)
            val sentNow = sentS.keys.associateWith { k -> vm.data.value.sessions[k] }
            base = applyRemote(r["changes"] as? JsonObject, base, false, sentS, sentNow).copy(seq = r["seq"]?.jsonPrimitive?.longOrNull ?: base.seq)
            saveBase(base); status.value = SyncStatus("ok", System.currentTimeMillis())
        } catch (e: SyncError) {
            status.value = status.value.copy(state = "err", err = e.message ?: "")
            if (e.status == 403) { vm.toast("Signed out of sync on this device"); vm.setSettings { it.copy(acct = null) }; poll?.cancel() }
        } catch (e: Exception) { status.value = status.value.copy(state = "err", err = e.message ?: "") }
        syncing = false
        if (again) { again = false; queue(800) }
    }

    private fun applyRemote(c: JsonObject?, base0: SyncBase, first: Boolean, sentS: Map<String, String>?, sentNow: Map<String, Session?>?): SyncBase {
        if (c == null) return base0
        var base = base0; val sess = vm.data.value.sessions.toMutableMap(); var changed = false
        val bs = base.sessions.toMutableMap()
        (c["sessions"] as? JsonObject)?.forEach { (k, v) ->
            val o = v as? JsonObject ?: return@forEach
            val local = sess[k]
            if (sentS?.get(k) != null && sentNow?.get(k) != null && local != sentNow[k]) return@forEach // edited again since sending
            if (first && local?.syncT != null && o["_del"] == null) return@forEach
            if (o["_del"] != null) { if (local != null) { sess.remove(k); changed = true }; bs[k] = o.toString(); return@forEach }
            val rs = sessionIn(o) ?: return@forEach
            if (local != rs) { sess[k] = rs; changed = true }
            bs[k] = o.toString()
        }
        base = base.copy(sessions = bs)
        var d = vm.data.value.copy(sessions = sess)
        fun take(kind: String, t: Long, apply: (JsonObject) -> Unit): Boolean {
            val v = c[kind] as? JsonObject ?: return false
            val vt = v["_t"]?.jsonPrimitive?.longOrNull ?: 0
            if (first || vt >= t) { apply(strip(v)); changed = true; return true }
            return false
        }
        take("plan", base.planT) { v -> runCatching { json.decodeFromJsonElement<List<DayPlan>>(v["days"]!!) }.getOrNull()?.let { d = d.copy(plan = it) }
            base = base.copy(plan = v.toString(), planT = (c["plan"] as JsonObject)["_t"]?.jsonPrimitive?.longOrNull ?: 0) }
        take("notes", base.notesT) { v -> runCatching { json.decodeFromJsonElement<List<Note>>(vm.cleanWeb(v["list"]!!)) }.getOrNull()?.let { d = d.copy(notes = it) }
            base = base.copy(notes = v.toString(), notesT = (c["notes"] as JsonObject)["_t"]?.jsonPrimitive?.longOrNull ?: 0) }
        take("settings", base.settingsT) { v ->
            val keep = d.settings
            val merged = vm.decodeSettingsLenient(JsonObject(json.encodeToJsonElement(keep).jsonObject + v.filterKeys { it !in SYNC_SKIP }), keep)
            d = d.copy(settings = merged.copy(acct = keep.acct, lastBackup = keep.lastBackup, bellSeen = keep.bellSeen, restAlerts = keep.restAlerts,
                heartRate = keep.heartRate, seenNews = keep.seenNews, schema = keep.schema))
            base = base.copy(settings = v.toString(), extra = v.toString(), settingsT = (c["settings"] as JsonObject)["_t"]?.jsonPrimitive?.longOrNull ?: 0)
        }
        take("flags", base.flagsT) { v -> runCatching { json.decodeFromJsonElement<Map<String, Map<String, Int>>>(v["d"]!!) }.getOrNull()?.let { d = d.copy(flags = it) }
            base = base.copy(flags = v.toString(), flagsT = (c["flags"] as JsonObject)["_t"]?.jsonPrimitive?.longOrNull ?: 0) }
        if (changed) vm.update { d }
        return base
    }

    // ---------- account ----------
    fun create() = scope.launch {
        try {
            val j = vault("/new", buildJsonObject { put("name", vm.data.value.settings.name) })
            val id = j["acct"]!!.jsonPrimitive.content; val token = j["token"]!!.jsonPrimitive.content
            vm.setSettings { it.copy(acct = Acct(id, token)) }; saveBase(SyncBase(first = false))
            vm.openSheet(Sheet.RecoveryCode(j["code"]?.jsonPrimitive?.content ?: "")); run(true); startPoll()
        } catch (e: SyncError) { vm.toast(if (e.status == 0) "You’re offline. Try again" else "Could not create account") }
    }
    fun recover(code: String, done: () -> Unit) = scope.launch {
        if (code.replace(Regex("[^A-Za-z0-9]"), "").length < 16) { vm.toast("Codes have 16 letters and numbers"); done(); return@launch }
        try {
            val j = vault("/recover", buildJsonObject { put("code", code.trim()) })
            vm.closeSheet(); vm.toast("Signed in. Syncing your data…")
            vm.setSettings { it.copy(acct = Acct(j["acct"]!!.jsonPrimitive.content, j["token"]!!.jsonPrimitive.content)) }
            saveBase(SyncBase(first = true)); status.value = SyncStatus("pending"); run(true); startPoll()
            if (vm.crew.on) vm.crew.sync(true)
            vm.toast("All synced ✓")
        } catch (e: SyncError) { vm.toast(if (e.status == 404) "That code doesn’t match an account" else "Could not sign in. Check your connection") }
        done()
    }
    fun signOut() = scope.launch {
        runCatching { vault("/signout", acctBody()) }
        vm.setSettings { it.copy(acct = null) }; saveBase(null); poll?.cancel(); status.value = SyncStatus(); cloudDays.value = null
        vm.toast("Signed out of sync")
    }
    fun deleteAccount() = scope.launch {
        try {
            vault("/delete", acctBody()); vm.setSettings { it.copy(acct = null) }; saveBase(null); poll?.cancel(); status.value = SyncStatus(); cloudDays.value = null
            vm.toast("Cloud account deleted. Your data is still on this phone")
        } catch (e: Exception) { vm.toast("Could not delete. Check your connection") }
    }
    fun loadCloudDays() = scope.launch {
        if (acct == null || cloudDays.value != null) return@launch
        cloudDays.value = runCatching { (vault("/snaps", acctBody())["days"] as? JsonArray)?.map { it.jsonPrimitive.content } }.getOrNull() ?: emptyList()
    }
    /** Replaces local data with the cloud copy from one day. */
    fun restoreDay(day: String) = scope.launch {
        try {
            val r = vault("/snap", acctBody { put("day", day) })
            val o = Json.parseToJsonElement(r["data"]!!.jsonPrimitive.content).jsonObject
            val sess = (o["sessions"] as? JsonObject)?.filter { (_, v) -> (v as? JsonObject)?.get("_del") == null }?.mapNotNull { (k, v) -> sessionIn(v)?.let { k to it } }?.toMap() ?: emptyMap()
            val plan = (o["plan"] as? JsonObject)?.get("days")?.let { runCatching { json.decodeFromJsonElement<List<DayPlan>>(it) }.getOrNull() }
            val notes = (o["notes"] as? JsonObject)?.get("list")?.let { runCatching { json.decodeFromJsonElement<List<Note>>(vm.cleanWeb(it)) }.getOrNull() }
            val set = (o["settings"] as? JsonObject)?.let { vm.decodeSettingsLenient(JsonObject(strip(it)), vm.data.value.settings) }
            vm.update { d -> d.copy(sessions = sess, plan = plan ?: d.plan, notes = notes ?: d.notes, settings = set?.copy(acct = d.settings.acct) ?: d.settings) }
            vm.toast("Restored your data from $day")
        } catch (e: Exception) { vm.toast("Could not load that cloud copy") }
    }
    /** "Bring them back" from the missing-workouts guard. */
    fun bringBack() = scope.launch {
        vm.closeSheet()
        try {
            var base = loadBase(); base = base.copy(sessions = base.sessions.filterKeys { vm.data.value.sessions[it] != null })
            val r = vault("/sync", acctBody { put("since", 0); put("today", iso(today())); putJsonObject("changes") {} })
            val back = HashMap<String, Session>(); val bs = base.sessions.toMutableMap()
            ((r["changes"] as? JsonObject)?.get("sessions") as? JsonObject)?.forEach { (k, v) ->
                val o = v as? JsonObject ?: return@forEach
                if (o["_del"] == null && vm.data.value.sessions[k] == null) sessionIn(o)?.let { back[k] = it; bs[k] = o.toString() } }
            vm.update { d -> d.copy(sessions = d.sessions + back) }
            saveBase(base.copy(sessions = bs, seq = r["seq"]?.jsonPrimitive?.longOrNull ?: 0, first = false))
            vm.toast("Your workouts are back ✓"); status.value = SyncStatus("ok", System.currentTimeMillis())
        } catch (e: Exception) { vm.toast("Could not reach the cloud. Try again") }
    }
    fun deleteMissing() { allowDel = true; vm.closeSheet(); now(true) }
}
