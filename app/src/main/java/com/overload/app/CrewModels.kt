package com.overload.app

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

/*
 * Crew data as the Overload server (the same one the web app uses) sends it.
 * Every field has a default, and members/posts are decoded one at a time, so one odd
 * entry from the server can't break the whole Crew tab.
 */

@Serializable
data class CrewState(
    val crew: CrewInfo = CrewInfo(),
    val members: List<Member> = emptyList(),
    val posts: List<Post> = emptyList(),
    val events: List<CrewEvent> = emptyList(),
    val votes: Map<String, Map<String, VoteCat>> = emptyMap(),
    val buddies: Map<String, Int> = emptyMap(),
    val buddyDates: List<String> = emptyList()
)

@Serializable
data class CrewInfo(
    val code: String = "", val name: String = "", val owner: String = "",
    val banner: String = "steel", val level: Int = 1, val xp: Double = 0.0,
    val challenge: CrewChallenge? = null
)

@Serializable data class CrewChallenge(val type: String = "sets", val target: Double = 0.0, val wk: String = "", val title: String = "")

@Serializable data class Member(val id: String, val name: String = "?", val profile: MemberProfile? = null, val ok: Boolean = false)

/** What each member publishes about themselves (see myProfile in Crew.kt). */
@Serializable
data class MemberProfile(
    val rank: Int = 0, val div: Int = 0, val pres: Int = 0, val rname: String = "", val power: Double = 0.0,
    val streak: Int = 0, val bcount: Int = 0, val active: Int = 0, val workouts: Int = 0, val lifeVol: Double = 0.0,
    val unit: String = "lb", val since: String? = null, val frac: Double = 0.0,
    val keys: List<String> = emptyList(), val show: List<List<String>> = emptyList(),
    val week: WeekStat? = null, val last: WeekStat? = null, val prev: WeekStat? = null,
    val plan: PlanWrap? = null, val days: List<String> = emptyList(),
    val lifts: Map<String, LiftBest> = emptyMap(), val bw: Double = 0.0, val lvl: Int = 1,
    val frame: String = "", val aura: String = "", val tag: String = "", val trial: Int = 0
)

@Serializable data class WeekStat(val wk: String = "", val days: Int = 0, val sets: Int = 0, val vol: Double = 0.0, val prs: Int = 0, val early: Int = 0)
@Serializable data class PlanWrap(val days: List<DayPlan> = emptyList())
@Serializable data class LiftBest(val e: Double = 0.0, val w: Double = 0.0, val r: Int = 0)

@Serializable
data class Post(
    val id: String, val by: String = "", val ts: Long = 0, val type: String = "workout", val cap: String = "",
    val data: PostData = PostData(), val reacts: Map<String, List<String>> = emptyMap(), val comments: List<Comment> = emptyList()
)

@Serializable
data class PostData(
    val day: String = "", val color: String = "red", val date: String = "", val min: Int = 0, val sets: Int = 0,
    val vol: Double = 0.0, val prs: Int = 0, val unit: String = "lb", val top: List<TopLift> = emptyList(),
    @SerialName("L") val levels: Map<String, Int> = emptyMap(), val cmp: String = "",
    val badges: List<List<String>> = emptyList(),
    val rank: Int = 0, val rname: String = ""      // rank-up posts
)

@Serializable data class TopLift(val n: String = "", val w: Double = 0.0, val r: Int = 0, val pr: Int = 0)
@Serializable data class Comment(val by: String = "", val text: String = "", val ts: Long = 0)
@Serializable data class CrewEvent(val type: String = "", val from: String = "", val ts: Long = 0, val emoji: String = "", val text: String = "")
@Serializable data class VoteCat(val tally: Map<String, Int> = emptyMap(), val mine: String? = null)

/** A crew member who is lifting right now (from the live socket). */
data class LiveInfo(val day: String, val start: Long)

private val lenient = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

/** Decodes the server's crew state, skipping any member or post that doesn't fit. */
fun parseCrewState(o: JsonObject): CrewState? {
    if (o["crew"] !is JsonObject) return null
    fun <T> list(key: String, f: (kotlinx.serialization.json.JsonElement) -> T): List<T> =
        (o[key] as? JsonArray)?.mapNotNull { runCatching { f(it) }.getOrNull() } ?: emptyList()
    return CrewState(
        crew = runCatching { lenient.decodeFromJsonElement<CrewInfo>(o["crew"]!!) }.getOrElse { CrewInfo() },
        members = list("members") { lenient.decodeFromJsonElement<Member>(it) },
        posts = list("posts") { lenient.decodeFromJsonElement<Post>(it) },
        events = list("events") { lenient.decodeFromJsonElement<CrewEvent>(it) },
        votes = runCatching { o["votes"]?.let { lenient.decodeFromJsonElement<Map<String, Map<String, VoteCat>>>(it) } }.getOrNull() ?: emptyMap(),
        buddies = runCatching { o["buddies"]?.let { lenient.decodeFromJsonElement<Map<String, Int>>(it) } }.getOrNull() ?: emptyMap(),
        buddyDates = runCatching { o["buddyDates"]?.let { lenient.decodeFromJsonElement<List<String>>(it) } }.getOrNull() ?: emptyList()
    )
}
