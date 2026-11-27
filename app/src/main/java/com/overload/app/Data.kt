@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
package com.overload.app

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * start = a starting weight the AI plan builder read from your notes (0.23), used as "Try" until there is history.
 * ai = "new" when the AI added the lift. Both are left out of the JSON when empty, same as the web app.
 */
@Serializable
data class Exercise(
    val id: String, val name: String, val m: String,
    val lo: Int = 0, val hi: Int = 0, val sets: Int = 2, val cue: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER) val start: StartW? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val ai: String? = null
)
@Serializable data class StartW(val w: Double = 0.0, val r: Int = 0)

@Serializable
data class DayPlan(
    val name: String, val type: String, val focus: String = "",
    val groups: List<List<Exercise>> = emptyList(), val rest: Int = 90, val color: String? = null
)

/** One logged set. w/r null = empty box. d = ticked as done. q = reps in reserve. */
@Serializable
data class SetEntry(val w: Double? = null, val r: Int? = null, val q: Int? = null, val d: Boolean = false)

@Serializable
data class Session(
    val date: String, val day: String,
    val ex: Map<String, List<SetEntry>> = emptyMap(),
    val names: Map<String, String> = emptyMap(),
    val notes: String = "",
    val exNotes: Map<String, String> = emptyMap(),
    val swap: Map<String, Exercise> = emptyMap(),
    val start: Long? = null, val end: Long? = null, val pauseAt: Long? = null, val paused: Long = 0,
    val hr: HeartRate? = null,
    val skips: Int = 0,                               // rest timer stopped early (Rest Timer Rebel)
    val prShown: Map<String, Int> = emptyMap(),       // PR celebrated once per exercise
    val posted: Boolean = false,                      // shared to the crew feed
    @SerialName("_t") val syncT: Long? = null,        // cloud sync: last change time
    @SerialName("_xt") val syncXt: Map<String, Long>? = null // cloud sync: per-exercise change times
)

/** One point on the heart rate graph: t = seconds from workout start, b = bpm. */
@Serializable
data class HrPt(val t: Int, val b: Int)

/** Heart rate for one finished workout. avg 0 = no readings found (yet). forEnd = the end time it was read for. */
@Serializable
data class HeartRate(
    val avg: Int = 0, val max: Int = 0, val min: Int = 0, val pts: List<HrPt> = emptyList(),
    val fetchedAt: Long = 0, val forEnd: Long = 0
)

@Serializable
data class Note(
    val id: String, val title: String = "", val text: String = "",
    val color: String = "yellow", val pin: Boolean = false, val ts: Long = 0
)

@Serializable
data class Settings(
    val unit: String = "lb", val sound: Boolean = true, val vibe: Boolean = true,
    val autoRest: Boolean = true, val awake: Boolean = false, val rir: Boolean = false,
    val theme: String = "auto", val name: String = "", val lastBackup: Long? = null,
    val restAlerts: Boolean = false, val seenNews: Int = 0,
    val heartRate: Boolean = false,
    // ---- ranks, badges, seasons (same keys as the web app so cloud sync lines up) ----
    val seen: List<String>? = null,       // badge keys already announced
    val held: Int? = null,                // rank you hold (you never lose it)
    val rankSeen: Int = 0, val divSeen: Int = 0, val presSeen: Int = 0,
    val v12: Int = 0, val v13: Int = 0,
    val unl: List<String> = emptyList(),  // features unlocked so far (progressive unlocks)
    val cos: Cosmetics = Cosmetics(),     // season pass rewards owned
    val frame: String = "", val aura: String = "", val tag: String = "",
    val showcase: List<String> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val chains: Map<String, Chain> = emptyMap(), val chainsDone: List<String> = emptyList(),
    val lastReport: String? = null,
    val curSeason: Int? = null, val seasons: Map<String, Int> = emptyMap(), val lastLvl: Int = 1,
    val yirSeen: Int = 0,
    val bw: Double = 0.0,                 // bodyweight, for lift ranks and relative lift boards
    val bellLocal: List<BellItem> = emptyList(), val bellSeen: Long = 0,
    // ---- crew ----
    val crew: CrewRef? = null, val crewMe: CrewMe? = null,
    val crewB: Map<String, String> = emptyMap(), val reactN: Int = 0,
    val rival: String? = null, val rivalSeen: String? = null,
    val titlesMine: List<String> = emptyList(),
    val arenaSnap: ArenaSnap? = null, val arenaLast: Map<String, Int> = emptyMap(),
    val recapSeen: String? = null,
    val chatSeen: Long = 0, val chatN: Int = 0,
    // ---- 0.14: unlocks and cosmetics (same keys as the web app) ----
    val look: Map<String, String> = emptyMap(),             // equipped look per category (accent, rest, sound...)
    val cos14: Map<String, List<String>> = emptyMap(),      // earned unlocks per category (mystery boxes, season finishes)
    val boxes: Map<String, String> = emptyMap(),            // mystery boxes already opened
    val roasts: Boolean = true, val restJokes: Boolean = true,
    val fb: Map<String, Int> = emptyMap(),                  // funny badge facts (ghost)
    val ghostD: GhostDay? = null,
    val seenVer: String = "",
    val excuses: Int = 0,
    // ---- 0.15: crew profile ----
    val prof: Prof = Prof(), val av: Av? = null, val flex: List<String> = listOf("best", "streak", "vol"),
    val rim: String = "", val founder: Boolean = false, val og: Int = 0, val fban16: Int = 0,
    // ---- 0.16 ----
    val tour16: Int = 0,
    // ---- 0.18-0.25 (same keys as the web app) ----
    val blocked: List<String> = emptyList(),              // crew members you blocked
    val cofounder: Boolean = false,
    val holSent: Map<String, Int> = emptyMap(), val holSeen: Map<String, Int> = emptyMap(), // holiday drops told to the crew / revealed
    val tap21: Map<String, Int> = emptyMap(),             // "tap to change" hints already used (ban, av)
    val ptip21: Int = 0, val hint24: Int = 0,             // Plan page tips dismissed
    val aiOk: Int = 0, val aiCoach: Boolean? = null, val aiSeen23: Int = 0, // Overload AI permission, coach notes switch
    // ---- cloud sync ----
    val acct: Acct? = null, val schema: Int = 0
)

/** What you show on your crew profile. */
@Serializable data class Prof(
    val motto: String = "", val gym: String = "", val phase: String = "", val ltype: String = "", val song: String = "",
    val acc: String = "", val tpins: List<String> = emptyList(), val fav: String = ""
)
/** Avatar: t = "icon" | "plate" | null (initials), v = icon or plate key, c = background colour. */
@Serializable data class Av(val t: String? = null, val v: String? = null, val c: String? = null)
@Serializable data class GhostDay(val d: String = "", val n: Int = 0)

@Serializable data class Cosmetics(val plate: List<String> = emptyList(), val aura: List<String> = emptyList(), val frame: List<String> = emptyList(), val tag: List<String> = emptyList())
@Serializable data class Goal(val ex: String, val w: Double, val date: String, val created: String, val doneAt: String? = null)
@Serializable data class Chain(@SerialName("M") val m: Int, val at: String = "")
@Serializable data class BellItem(val ts: Long, val icon: String, val text: String)
@Serializable data class CrewRef(val code: String)
@Serializable data class CrewMe(val id: String, val key: String)
@Serializable data class ArenaSnap(val wk: String, val pos: Map<String, Int> = emptyMap())
@Serializable data class Acct(val id: String, val token: String)

@Serializable
data class AppData(
    val plan: List<DayPlan>? = null,
    val sessions: Map<String, Session> = emptyMap(),
    val notes: List<Note> = emptyList(),
    val settings: Settings = Settings(),
    /** Per-day flags for rest-day challenges, e.g. {"2026-11-24": {"prog": 1}}. */
    val flags: Map<String, Map<String, Int>> = emptyMap()
)
