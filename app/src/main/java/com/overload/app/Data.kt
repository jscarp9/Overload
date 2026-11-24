package com.overload.app

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String, val name: String, val m: String,
    val lo: Int = 0, val hi: Int = 0, val sets: Int = 2, val cue: String = ""
)

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
    val start: Long? = null, val end: Long? = null, val pauseAt: Long? = null, val paused: Long = 0
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
    val theme: String = "auto", val name: String = "", val lastBackup: Long? = null
)

@Serializable
data class AppData(
    val plan: List<DayPlan>? = null,
    val sessions: Map<String, Session> = emptyMap(),
    val notes: List<Note> = emptyList(),
    val settings: Settings = Settings()
)
