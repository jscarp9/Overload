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
    val buddyDates: List<String> = emptyList(),
    // 0.14+
    val style: CrewStyle? = null,
    val former: Map<String, String> = emptyMap(),
    val raid: Raid? = null,
    val groups: Map<String, Group> = emptyMap(),
    val pins: List<ChatMsg> = emptyList(),
    val hof: List<Hof> = emptyList(),
    val respectGiven: List<String> = emptyList(),
    val nickVotes: Map<String, String> = emptyMap()
)

@Serializable data class CrewStyle(val motto: String = "", val c1: String = "", val c2: String = "")
@Serializable data class Crest(val shape: String = "shield", val icon: String = "barbell", val c1: String = "#ef2b36", val c2: String = "#14161c", val txt: String = "")
@Serializable data class CTrophy(val tier: Int = 1, val ts: Long = 0)
@Serializable data class Shame(val target: String = "", val until: Long = 0)
@Serializable data class ShameVotes(val tally: Map<String, Int> = emptyMap(), val mine: String? = null)
@Serializable data class ChatRecent(val ts: Long = 0, val by: String = "")
@Serializable data class RaidWin(val wk: String = "", val name: String = "", val by: List<String> = emptyList())
@Serializable data class Raid(
    val wk: String = "", val boss: String = "titan", val name: String = "Boss", val hp: Double = 1.0, val dmg: Double = 0.0,
    val hits: Map<String, Double> = emptyMap(), val beaten: Boolean = false, val wins: Int = 0, val won: List<RaidWin> = emptyList()
)
@Serializable data class Group(val host: String = "", val members: List<String> = emptyList(), val ts: Long = 0)
@Serializable data class Hof(val id: String = "", val kind: String = "post", val name: String = "", val who: String = "", val text: String = "", val data: PostData? = null)
@Serializable data class ChatCard(val day: String = "", val color: String = "red", val min: Int = 0, val sets: Int = 0, val vol: Double = 0.0, val prs: Int = 0, val unit: String = "lb", val top: List<TopLift> = emptyList())
@Serializable data class ChatReply(val id: String = "", val by: String = "", val text: String = "")
@Serializable data class ChatMsg(
    val id: String = "", val by: String = "", val ts: Long = 0, val text: String = "", val card: ChatCard? = null,
    val reacts: Map<String, List<String>> = emptyMap(), val reply: ChatReply? = null, val del: Boolean = false, val pending: Boolean = false
)

@Serializable
data class CrewInfo(
    val code: String = "", val name: String = "", val owner: String = "",
    val banner: String = "steel", val level: Int = 1, val xp: Double = 0.0,
    val challenge: CrewChallenge? = null,
    val created: Long = 0, val founders: Boolean = false, val fcrew: Boolean = false, val rank: Int = 0,
    val crest: Crest? = null, val trophies: Map<String, CTrophy> = emptyMap(),
    val chatRecent: List<ChatRecent> = emptyList(), val chatLast: Long = 0, val muted: Map<String, Long> = emptyMap(),
    val shame: Shame? = null, val shameVotes: ShameVotes? = null
)

@Serializable data class CrewChallenge(val type: String = "sets", val target: Double = 0.0, val wk: String = "", val title: String = "")

@Serializable data class Member(
    val id: String, val name: String = "?", val profile: MemberProfile? = null, val ok: Boolean = false,
    val respect: Int = 0, val nick: String = "", val role: String = "", val jersey: Int? = null
)

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
    val frame: String = "", val aura: String = "", val tag: String = "", val trial: Int = 0,
    // 0.14+
    val nameFx: String = "", val card: String = "", val pbanner: String = "", val troph: List<String>? = null,
    val motto: String = "", val gym: String = "", val phase: String = "", val ltype: String = "", val song: String = "",
    val av: Av? = null, val pacc: String = "", val rim: String = "", val tpins: List<String> = emptyList(),
    val flex: List<FlexStat> = emptyList(), val prtl: List<PrPt> = emptyList(), val best: PrPt? = null,
    val plates: List<String> = emptyList(), val fav: String = "", val founder: Boolean = false, val og: Int = 0
)
@Serializable data class FlexStat(val k: String = "", val l: String = "", val v: String = "")
@Serializable data class PrPt(val n: String = "", val w: Double = 0.0, val r: Int = 0, val d: String = "", val v: Double = 0.0)

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
    val rank: Int = 0, val rname: String = "",     // rank-up posts
    val name: String = "", val boss: String = "",  // trophy and boss posts
    val dmg: Map<String, Double> = emptyMap(),
    val rows: List<GroupRow> = emptyList()         // lifted-together posts
)
@Serializable data class GroupRow(val id: String = "", val sets: Int = 0, val vol: Double = 0.0, val prs: Int = 0)

@Serializable data class TopLift(val n: String = "", val w: Double = 0.0, val r: Int = 0, val pr: Int = 0)
@Serializable data class Comment(val by: String = "", val text: String = "", val ts: Long = 0)
@Serializable data class CrewEvent(val type: String = "", val from: String = "", val ts: Long = 0, val emoji: String = "", val text: String = "")
@Serializable data class VoteCat(val tally: Map<String, Int> = emptyMap(), val mine: String? = null)

/** A crew member who is lifting right now (from the live socket). g/sets/ex/rest are filled while lifting together. */
data class LiveInfo(val day: String, val start: Long, val g: String = "", val sets: Int = 0, val vol: Double = 0.0, val ex: String = "", val rest: Long = 0, val prs: Int = 0)

fun parseChatMsg(e: kotlinx.serialization.json.JsonElement?): ChatMsg? = e?.let { runCatching { lenient.decodeFromJsonElement<ChatMsg>(it) }.getOrNull() }

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
        buddyDates = runCatching { o["buddyDates"]?.let { lenient.decodeFromJsonElement<List<String>>(it) } }.getOrNull() ?: emptyList(),
        style = runCatching { o["style"]?.let { lenient.decodeFromJsonElement<CrewStyle>(it) } }.getOrNull(),
        former = runCatching { o["former"]?.let { lenient.decodeFromJsonElement<Map<String, String>>(it) } }.getOrNull() ?: emptyMap(),
        raid = runCatching { o["raid"]?.let { lenient.decodeFromJsonElement<Raid>(it) } }.getOrNull(),
        groups = runCatching { o["groups"]?.let { lenient.decodeFromJsonElement<Map<String, Group>>(it) } }.getOrNull() ?: emptyMap(),
        pins = list("pins") { lenient.decodeFromJsonElement<ChatMsg>(it) },
        hof = list("hof") { lenient.decodeFromJsonElement<Hof>(it) },
        respectGiven = runCatching { o["respectGiven"]?.let { lenient.decodeFromJsonElement<List<String>>(it) } }.getOrNull() ?: emptyList(),
        nickVotes = runCatching { o["nickVotes"]?.let { lenient.decodeFromJsonElement<Map<String, String>>(it) } }.getOrNull() ?: emptyMap()
    )
}
