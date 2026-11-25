package com.overload.app

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDate
import kotlin.math.max

data class RestState(val endAt: Long, val dur: Int, val base: Int, val next: String, val buzzed: Boolean = false)
data class Picker(val ctx: String, val gi: Int = -1, val key: String = "") // ctx: today | plan | swap

data class UiState(
    val date: LocalDate = LocalDate.now(),
    val auto: Boolean = true,
    val tab: String = "log",           // log | prog | stats | notes | plan
    val prog: String? = null,
    val pfilter: String = "All",
    val pday: Int = dayIdx(LocalDate.now()),
    val settings: Boolean = false,
    val rest: RestState? = null,
    val picker: Picker? = null,
    val warm: Set<String> = emptySet(),
    val editNote: Note? = null
)

class OverloadViewModel(app: Application) : AndroidViewModel(app) {
    private val file = File(app.filesDir, "overload.json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }

    val data = MutableStateFlow(load())
    val ui = MutableStateFlow(UiState())
    val now = MutableStateFlow(System.currentTimeMillis())
    val toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000)
                now.value = System.currentTimeMillis()
                tickRest()
                val u = ui.value
                if (u.auto && u.date != LocalDate.now()) setDate(LocalDate.now(), false)
            }
        }
    }

    // ---------- persistence ----------
    private fun load(): AppData = try {
        if (file.exists()) json.decodeFromString<AppData>(file.readText()) else AppData()
    } catch (e: Exception) { AppData() }

    private var saveJob: Job? = null
    private fun update(f: (AppData) -> AppData) {
        data.update(f)
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(250)
            try { file.writeText(json.encodeToString(data.value)) } catch (_: Exception) {}
        }
    }
    private fun say(msg: String) { toasts.tryEmit(msg) }

    // ---------- derived helpers ----------
    val unit get() = data.value.settings.unit
    private fun dayKey(d: LocalDate) = DAYS[dayIdx(d)]
    fun curKey(u: UiState = ui.value) = sid(u.date.toString(), dayKey(u.date))
    fun session(d: AppData = data.value, u: UiState = ui.value): Session =
        d.sessions[curKey(u)] ?: Session(u.date.toString(), dayKey(u.date))
    fun todayPlan(d: AppData = data.value, u: UiState = ui.value): DayPlan? = d.plan?.getOrNull(dayIdx(u.date))
    fun todayGroups(d: AppData = data.value, u: UiState = ui.value): List<List<Exercise>> {
        val sw = session(d, u).swap
        return todayPlan(d, u)?.groups?.map { gr -> gr.map { sw[it.id] ?: it } } ?: emptyList()
    }
    private fun exMap(d: AppData = data.value): Map<String, Exercise> {
        val m = LinkedHashMap<String, Exercise>()
        d.plan?.forEach { day -> day.groups.forEach { gr -> gr.forEach { if (it.id !in m) m[it.id] = it } } }
        return m
    }
    fun exName(id: String, d: AppData = data.value): String =
        exMap(d)[id]?.name ?: d.sessions.values.firstNotNullOfOrNull { it.names[id] } ?: id
    fun exMuscle(id: String, d: AppData = data.value): String =
        exMap(d)[id]?.m ?: LIB.firstOrNull { slug(it.name) == id }?.m ?: "Other"
    fun allIds(d: AppData = data.value): List<String> =
        (exMap(d).keys + d.sessions.values.flatMap { it.ex.keys }).distinct()

    private fun editSession(f: (Session) -> Session) {
        val u = ui.value; val k = curKey(u)
        update { d -> d.copy(sessions = d.sessions + (k to f(d.sessions[k] ?: Session(u.date.toString(), dayKey(u.date))))) }
    }

    // ---------- navigation / date ----------
    fun setDate(d: LocalDate, manual: Boolean) =
        ui.update { it.copy(date = d, pday = dayIdx(d), picker = null, auto = !manual || d == LocalDate.now()) }
    fun shiftWeek(n: Int) = setDate(ui.value.date.plusWeeks(n.toLong()), true)
    fun goToday() = setDate(LocalDate.now(), false)
    fun setTab(t: String) {
        val u = ui.value
        val again = u.tab == t && !u.settings
        ui.update {
            it.copy(tab = t, settings = false, picker = null, editNote = null,
                pday = if (t == "plan") dayIdx(it.date) else it.pday,
                prog = if (t == "prog" && again) null else it.prog)
        }
        if (t == "log" && again) goToday()
    }
    fun toggleSettings() = ui.update { it.copy(settings = !it.settings) }
    fun setProg(id: String?) = ui.update { it.copy(prog = id) }
    fun setPFilter(f: String) = ui.update { it.copy(pfilter = f) }
    fun setPDay(i: Int) = ui.update { it.copy(pday = i, picker = null) }

    // ---------- onboarding / plan ----------
    fun startPlan(key: String, name: String) {
        val t = TEMPLATES.first { it.key == key }
        update { it.copy(plan = t.days(), settings = it.settings.copy(name = name.trim())) }
        say("You're set. Let's lift")
    }
    fun replacePlan(key: String) {
        val t = TEMPLATES.first { it.key == key }
        update { it.copy(plan = t.days()) }; say("Plan replaced")
    }
    fun editDay(idx: Int, f: (DayPlan) -> DayPlan) =
        update { d -> d.copy(plan = d.plan?.mapIndexed { i, p -> if (i == idx) f(p) else p }) }
    fun renameToday(name: String) { if (name.isNotBlank()) editDay(dayIdx(ui.value.date)) { it.copy(name = name.trim()) } }

    private fun uniqueId(name: String): String {
        val m = exMap(); val base = slug(name); var id = base; var n = 2
        while (m[id] != null && !m[id]!!.name.equals(name, true)) id = "$base-${n++}"
        return id
    }
    fun openPicker(p: Picker?) = ui.update { it.copy(picker = p) }
    fun pickExercise(name: String, m: String, lo: Int, hi: Int) {
        val p = ui.value.picker ?: return
        val d = data.value
        if (p.ctx == "swap") {
            val (gi, ei) = p.key.split("-").map { it.toInt() }
            val orig = d.plan!![dayIdx(ui.value.date)].groups[gi][ei]
            val ex = E(name, m, lo, hi, orig.sets, slug(name))
            editSession { it.copy(swap = it.swap + (orig.id to ex), names = it.names + (ex.id to name)) }
            say("Swapped to $name for today")
        } else {
            val di = if (p.ctx == "today") dayIdx(ui.value.date) else ui.value.pday
            val ex = E(name, m, lo, hi, 2, uniqueId(name))
            editDay(di) { day ->
                val g = day.groups.toMutableList()
                if (p.gi >= 0 && p.gi < g.size) g[p.gi] = g[p.gi] + ex else g.add(listOf(ex))
                day.copy(groups = g, type = if (day.type == "off") "other" else day.type)
            }
            say("$name added")
        }
        ui.update { it.copy(picker = null) }
    }
    fun removeEx(dayI: Int, gi: Int, ei: Int) = editDay(dayI) { day ->
        val g = day.groups.map { it.toMutableList() }.toMutableList()
        g[gi].removeAt(ei); if (g[gi].isEmpty()) g.removeAt(gi)
        day.copy(groups = g)
    }
    fun removeGroup(dayI: Int, gi: Int) = editDay(dayI) { it.copy(groups = it.groups.filterIndexed { i, _ -> i != gi }) }
    fun moveGroupUp(dayI: Int, gi: Int) = editDay(dayI) { day ->
        if (gi <= 0) day else day.copy(groups = day.groups.toMutableList().also { java.util.Collections.swap(it, gi, gi - 1) })
    }
    fun editEx(dayI: Int, gi: Int, ei: Int, f: (Exercise) -> Exercise) = editDay(dayI) { day ->
        day.copy(groups = day.groups.mapIndexed { g, lst -> if (g == gi) lst.mapIndexed { e, x -> if (e == ei) f(x) else x } else lst })
    }

    // ---------- logging ----------
    fun setField(e: Exercise, i: Int, field: Char, text: String) = editSession { s ->
        val list = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (list.size <= i) list.add(SetEntry())
        val x = list[i]
        list[i] = when (field) {
            'w' -> x.copy(w = text.toDoubleOrNull())
            'r' -> x.copy(r = text.toIntOrNull())
            else -> x.copy(q = text.toIntOrNull())
        }
        s.copy(ex = s.ex + (e.id to list), names = s.names + (e.id to e.name))
    }

    fun toggleSet(e: Exercise, gi: Int, ei: Int, i: Int, phW: Double?, phR: Int?) {
        val d = data.value; val u = ui.value; val s = session(d, u)
        val list = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (list.size <= i) list.add(SetEntry())
        var x = list[i]
        if (x.d) { list[i] = x.copy(d = false); editSession { it.copy(ex = it.ex + (e.id to list)) }; return }
        if (x.w == null && phW != null) x = x.copy(w = phW)
        if (x.r == null && phR != null) x = x.copy(r = phR)
        if (!full(x)) { say("Enter weight and reps first"); return }
        x = x.copy(d = true); list[i] = x
        val t = System.currentTimeMillis()
        editSession { s2 ->
            var n = s2
            if (n.start == null) n = n.copy(start = t, paused = 0, pauseAt = null, end = null)
            val pa = n.pauseAt
            if (pa != null) n = n.copy(paused = n.paused + (t - pa), pauseAt = null)
            n.copy(ex = n.ex + (e.id to list), names = n.names + (e.id to e.name))
        }
        val pb = bestBefore(d.sessions, e.id, s.date)
        val isPr = pb > 0 && e1(x) > pb
        if (isPr) say("New PR on ${e.name}")
        val groups = todayGroups(d, u); val gr = groups.getOrNull(gi) ?: emptyList()
        if (ei < gr.size - 1) {
            if (!isPr) say("Next: ${gr[ei + 1].name}")
        } else if (d.settings.autoRest) {
            val first = gr.firstOrNull()
            val next = if (first != null && i + 1 < max(first.sets, s.ex[first.id]?.size ?: 0)) "${first.name} · set ${i + 2}"
            else groups.getOrNull(gi + 1)?.firstOrNull()?.name ?: "Last round. Finish strong"
            startRest(todayPlan(d, u)?.rest ?: 90, next)
        }
    }
    fun addSet(e: Exercise) = editSession { s ->
        val l = (s.ex[e.id] ?: emptyList()).toMutableList()
        while (l.size < e.sets) l.add(SetEntry())
        l.add(SetEntry()); s.copy(ex = s.ex + (e.id to l))
    }
    fun removeSet(e: Exercise) = editSession { s ->
        val l = (s.ex[e.id] ?: emptyList()).dropLast(1); s.copy(ex = s.ex + (e.id to l))
    }
    fun unswap(orig: Exercise) { editSession { it.copy(swap = it.swap - orig.id) }; say("Back to ${orig.name}") }
    fun toggleWarm(id: String) = ui.update { it.copy(warm = if (id in it.warm) it.warm - id else it.warm + id) }
    fun setSessionNote(t: String) = editSession { it.copy(notes = t) }
    fun setExNote(id: String, t: String) = editSession { it.copy(exNotes = it.exNotes + (id to t)) }

    // ---------- workout clock ----------
    fun startWorkout() { editSession { if (it.start == null) it.copy(start = System.currentTimeMillis(), paused = 0, pauseAt = null, end = null) else it }; say("Clock is running") }
    fun togglePause() = editSession { s ->
        val t = System.currentTimeMillis(); val pa = s.pauseAt
        if (pa != null) s.copy(paused = s.paused + (t - pa), pauseAt = null) else s.copy(pauseAt = t)
    }
    fun finish() {
        editSession { s ->
            val t = System.currentTimeMillis(); val pa = s.pauseAt
            (if (pa != null) s.copy(paused = s.paused + (t - pa), pauseAt = null) else s).copy(end = t)
        }
        restStop()
    }
    fun resetWorkout() { editSession { it.copy(start = null, end = null, pauseAt = null, paused = 0) }; say("Workout clock reset") }
    fun reopen() = editSession { s -> if (s.end != null) s.copy(pauseAt = s.end, end = null) else s }

    // ---------- rest timer ----------
    fun startRest(sec: Int, next: String = "") =
        ui.update { it.copy(rest = RestState(System.currentTimeMillis() + sec * 1000L, sec, sec, next)) }
    fun restPlus() = ui.update { u -> u.copy(rest = u.rest?.let { it.copy(endAt = it.endAt + 15000, dur = it.dur + 15, buzzed = false) }) }
    fun restMinus() = ui.update { u -> u.copy(rest = u.rest?.let { it.copy(endAt = it.endAt - 15000) }) }
    fun restReset() { ui.value.rest?.let { startRest(it.base, it.next) } }
    fun restStop() = ui.update { it.copy(rest = null) }
    private fun tickRest() {
        val r = ui.value.rest ?: return
        if (!r.buzzed && System.currentTimeMillis() >= r.endAt) {
            ui.update { it.copy(rest = it.rest?.copy(buzzed = true)) }
            val st = data.value.settings
            if (st.sound) try { ToneGenerator(AudioManager.STREAM_ALARM, 80).startTone(ToneGenerator.TONE_PROP_BEEP2, 500) } catch (_: Exception) {}
            if (st.vibe) try {
                val v = getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
            } catch (_: Exception) {}
        }
    }

    // ---------- lock-screen rest alerts ----------
    private val appCtx get() = getApplication<Application>()
    private var alertPending = false
    fun enableRestAlerts(granted: Boolean) {
        if (!granted) { say("Notifications are blocked. Allow them in your phone settings"); return }
        setSettings { it.copy(restAlerts = true) }
        RestAlerts.show(appCtx, "Rest alerts are on", "You'll get a ping here when rest is over 💪")
        say("Rest alerts on. Test ping sent")
    }
    fun disableRestAlerts() { setSettings { it.copy(restAlerts = false) }; RestAlerts.cancel(appCtx); say("Rest alerts off") }
    /** App left the screen: if rest is running, set an alarm for when it ends. */
    fun onBackground() {
        val r = ui.value.rest ?: return
        if (!data.value.settings.restAlerts || r.endAt - System.currentTimeMillis() <= 3000) return
        RestAlerts.schedule(appCtx, r.endAt, r.next); alertPending = true
    }
    fun onForeground() { if (alertPending) { alertPending = false; RestAlerts.cancel(appCtx) } }

    // ---------- notes / settings ----------
    fun editNote(n: Note?) = ui.update { it.copy(editNote = n) }
    fun newNote() = editNote(Note(id = ""))
    fun saveNote(n: Note) {
        if (n.title.isBlank() && n.text.isBlank()) { say("Write something first"); return }
        val saved = n.copy(id = n.id.ifEmpty { "n" + System.currentTimeMillis().toString(36) }, ts = System.currentTimeMillis())
        update { d -> d.copy(notes = if (d.notes.any { it.id == saved.id }) d.notes.map { if (it.id == saved.id) saved else it } else d.notes + saved) }
        editNote(null); say("Note saved")
    }
    fun deleteNote(id: String) { update { d -> d.copy(notes = d.notes.filter { it.id != id }) }; editNote(null) }
    fun setSettings(f: (Settings) -> Settings) = update { it.copy(settings = f(it.settings)) }
    fun wipe() { RestAlerts.cancel(appCtx); update { AppData(settings = Settings(theme = it.settings.theme)) }; ui.update { UiState() } }

    // ---------- backup ----------
    fun exportJson(): String {
        setSettings { it.copy(lastBackup = System.currentTimeMillis()) }
        return json.encodeToString(data.value)
    }

    /** Accepts this app's backup, and the web version's backup file (plan wrapped in {days}, "" for empty boxes, d as 0/1). */
    fun importJson(text: String): Boolean = try {
        val root = Json.parseToJsonElement(text).jsonObject
        val fixed = JsonObject(root.mapValues { (k, v) ->
            if (k == "plan" && v is JsonObject && v["days"] != null) v["days"]!! else v
        })
        val imported = json.decodeFromJsonElement<AppData>(clean(fixed))
        update { d ->
            d.copy(
                sessions = d.sessions + imported.sessions,
                plan = imported.plan ?: d.plan,
                notes = if (root["notes"] != null) imported.notes else d.notes,
                settings = if (root["settings"] != null) imported.settings else d.settings
            )
        }
        say("Backup restored"); true
    } catch (e: Exception) { say("That file could not be read"); false }

    private fun clean(el: JsonElement): JsonElement = when (el) {
        is JsonObject -> JsonObject(el.mapValues { (k, v) ->
            when {
                v is JsonPrimitive && v.isString && v.content.isEmpty() && k in setOf("w", "r", "q", "lo", "hi", "sets") -> JsonNull
                k == "d" && v is JsonPrimitive && !v.isString && v.content in setOf("0", "1") -> JsonPrimitive(v.content == "1")
                else -> clean(v)
            }
        })
        is JsonArray -> JsonArray(el.map { clean(it) })
        else -> el
    }
}
