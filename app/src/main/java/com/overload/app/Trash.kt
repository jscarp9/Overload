package com.overload.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.io.File

/*
 * 0.14 "nothing gets lost": anything deleted on this phone waits in the Trash for 30 days, a full
 * copy is saved before a new version touches anything, and "Export everything" writes one file.
 */

@Serializable
data class TrashItem(val id: String, val kind: String, val label: String, val ts: Long, val data: JsonElement)

val TRASH_ICON = mapOf("exercise" to "🏋️", "group" to "🏋️", "note" to "📝", "goal" to "🎯", "plan" to "📋", "set" to "✅", "workout" to "🗓", "undo-restore" to "↩")
private const val DAY_MS = 86_400_000L

class TrashStore(private val vm: OverloadViewModel) {
    private val file by lazy { File(vm.appContext.filesDir, "overload-trash.json") }
    private val json get() = vm.jsonCodec
    val items = MutableStateFlow<List<TrashItem>>(emptyList())
    private var loaded = false

    fun load(): List<TrashItem> {
        if (!loaded) { loaded = true; items.value = runCatching { json.decodeFromString(ListSerializer(TrashItem.serializer()), file.readText()) }.getOrDefault(emptyList()) }
        return items.value
    }
    private fun save() {
        val now = System.currentTimeMillis()
        items.value = items.value.filter { now - it.ts < 30 * DAY_MS }.take(400)
        runCatching { file.writeText(json.encodeToString(ListSerializer(TrashItem.serializer()), items.value)) }
    }
    fun put(kind: String, label: String, data: JsonElement) {
        load()
        val id = java.lang.Long.toString(System.currentTimeMillis(), 36) + java.lang.Long.toString((Math.random() * 1.7e6).toLong(), 36)
        items.value = listOf(TrashItem(id, kind, label, System.currentTimeMillis(), data)) + items.value
        save()
    }

    /** Puts an item back. Returns false if it can't go back (the day is gone, or the workout has new sets). */
    fun restore(id: String): Boolean {
        load(); val x = items.value.firstOrNull { it.id == id } ?: return false; val d = x.data
        val ok = runCatching {
            when (x.kind) {
                "exercise", "group" -> {
                    val o = d.jsonObject; val di = o["day"]!!.jsonPrimitive.int; val gi = o["gi"]?.jsonPrimitive?.intOrNull ?: 0
                    val g = if (x.kind == "group") json.decodeFromJsonElement<List<Exercise>>(o["group"]!!) else listOf(json.decodeFromJsonElement<Exercise>(o["ex"]!!))
                    if (vm.data.value.plan?.getOrNull(di) == null) return false
                    vm.editDay(di) { day -> val have = day.groups.flatten().map { it.id }.toSet(); val add = g.filter { it.id !in have }
                        if (add.isEmpty()) day else day.copy(groups = day.groups.toMutableList().also { it.add(minOf(gi, it.size), add) }) }
                }
                "note" -> { val n = json.decodeFromJsonElement<Note>(d); vm.update { a -> if (a.notes.any { it.id == n.id }) a else a.copy(notes = a.notes + n) } }
                "goal" -> { val g = json.decodeFromJsonElement<Goal>(d); vm.setSettings { it.copy(goals = it.goals + g) } }
                "plan" -> { val p = json.decodeFromJsonElement<List<DayPlan>>(d); vm.data.value.plan?.let { put("plan", "Plan before restoring", json.encodeToJsonElement(it)) }; vm.update { it.copy(plan = p) } }
                "set" -> {
                    val o = d.jsonObject; val key = o["key"]!!.jsonPrimitive.content; val exId = o["id"]!!.jsonPrimitive.content; val set = json.decodeFromJsonElement<SetEntry>(o["set"]!!)
                    val s = vm.data.value.sessions[key] ?: return false
                    val a = (s.ex[exId] ?: emptyList()).toMutableList(); val empty = a.indexOfFirst { !full(it) }
                    if (empty >= 0) a[empty] = set else a.add(set)
                    vm.update { dd -> dd.copy(sessions = dd.sessions + (key to s.copy(ex = s.ex + (exId to a)))) }
                }
                "workout" -> {
                    val o = d.jsonObject; val key = o["key"]!!.jsonPrimitive.content; val s = json.decodeFromJsonElement<Session>(o["sess"]!!)
                    val cur = vm.data.value.sessions[key]; if (cur != null && cur.ex.values.any { a -> a.any(::full) }) return false
                    vm.update { dd -> dd.copy(sessions = dd.sessions + (key to s.copy(syncT = System.currentTimeMillis()))) }
                }
                "undo-restore" -> { vm.update { json.decodeFromJsonElement<AppData>(d).let { b -> b.copy(settings = b.settings.copy(acct = it.settings.acct)) } } }
            }
            true
        }.getOrDefault(false)
        if (ok) { items.value = items.value.filter { it.id != id }; save() }
        return ok
    }
}

// ============================ UPDATE SAFETY NET ============================
/** The web app's version (0.17), without the Android build number. */
val APP_VER: String get() = VERSION.split(".").take(2).joinToString(".")

/**
 * Before a new version touches anything, keep a copy of the save file. For the first 2 opens (within
 * 30 minutes) after an update, anything that went missing is put back from that copy.
 * Returns how many things it put back.
 */
fun safetyNet(dir: File, current: AppData, json: Json, put: (AppData) -> Unit): Int = runCatching {
    val save = File(dir, "overload.json"); if (!save.exists()) return 0
    val ver = APP_VER; val snapF = File(dir, "preupd-$ver.json"); val metaF = File(dir, "preupd-$ver.meta")
    if (current.settings.seenVer != ver && !snapF.exists()) {
        save.copyTo(snapF, overwrite = true); metaF.writeText("${System.currentTimeMillis()} 0")
        val idx = File(dir, "preupd-index.txt"); val list = (runCatching { idx.readLines() }.getOrDefault(emptyList()) + ver).filter { it.isNotBlank() }.distinct()
        list.dropLast(4).forEach { File(dir, "preupd-$it.json").delete(); File(dir, "preupd-$it.meta").delete() }
        idx.writeText(list.takeLast(4).joinToString("\n"))
    }
    if (!snapF.exists()) return 0
    val (at, boots0) = runCatching { metaF.readText().trim().split(" ").let { it[0].toLong() to it[1].toInt() } }.getOrDefault(0L to 9)
    val boots = boots0 + 1; metaF.writeText("$at $boots")
    if (boots > 2 || System.currentTimeMillis() - at > 30 * 60_000L) return 0
    val old = json.decodeFromString<AppData>(snapF.readText())
    var back = 0; var sessions = current.sessions
    old.sessions.forEach { (k, v) ->
        if (v.ex.values.none { a -> a.any(::full) }) return@forEach
        val now = sessions[k]; if (now == null || now.ex.values.none { a -> a.any(::full) }) { sessions = sessions + (k to v); back++ }
    }
    var d = current.copy(sessions = sessions)
    if (d.plan == null && old.plan != null) { d = d.copy(plan = old.plan); back++ }
    if (d.notes.isEmpty() && old.notes.isNotEmpty()) { d = d.copy(notes = old.notes); back++ }
    if (back > 0) put(d)
    back
}.getOrDefault(0)

/** One file with everything: workouts, plan, notes, settings, trash and crew data (0.14 "Export everything"). */
fun exportAllJson(vm: OverloadViewModel): String {
    val d = vm.data.value; val j = vm.jsonCodec; val cs = vm.crewState.value
    return buildJsonObject {
        put("app", "overload"); put("kind", "full-export"); put("v", VERSION); put("at", java.time.Instant.now().toString())
        put("sessions", j.encodeToJsonElement(d.sessions)); d.plan?.let { put("plan", j.encodeToJsonElement(it)) }
        put("notes", j.encodeToJsonElement(d.notes)); put("settings", j.encodeToJsonElement(d.settings)); put("flags", j.encodeToJsonElement(d.flags))
        put("trash", j.encodeToJsonElement(ListSerializer(TrashItem.serializer()), vm.trash.load()))
        if (cs != null) putJsonObject("crew") { put("crew", j.encodeToJsonElement(cs.crew)); put("members", j.encodeToJsonElement(cs.members)); put("posts", j.encodeToJsonElement(cs.posts)) }
        else put("crew", JsonNull)
        put("chat", j.encodeToJsonElement(vm.chat.msgs.value))
    }.toString()
}
