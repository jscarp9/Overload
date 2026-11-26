package com.overload.app

/** Every bottom sheet the app can show. Only one is open at a time (UiState.sheet). */
sealed class Sheet {
    data class Badges(val items: List<Bdg>) : Sheet()          // badges just unlocked
    data class ReportCard(val r: Report) : Sheet()              // Monday weekly report
    data class BadgeInfo(val k: String) : Sheet()
    data class Ladder(val cat: String) : Sheet()
    object Look : Sheet()                                        // frames, auras, nameplates
    object Pass : Sheet()                                        // season pass
    object NewGoal : Sheet()
    object Bell : Sheet()                                        // activity
    data class PostWorkout(val key: String) : Sheet()           // post to crew
    data class Comments(val post: String) : Sheet()
    object CrewSettings : Sheet()
    object SetChallenge : Sheet()
    object Awards : Sheet()
    data class CopyPlan(val member: String, val day: Int?) : Sheet() // day null = whole split
    object SignIn : Sheet()
    data class RecoveryCode(val code: String) : Sheet()
    data class SyncGuard(val n: Int) : Sheet()
    // ---- 0.14 ----
    object Trash : Sheet()
    data class Locker(val sec: String?) : Sheet()               // Customize (list, or one category)
    data class BoxOpen(val got: Pair<String, String>?, val opening: Boolean) : Sheet()
    data class TrophyRoom(val of: String?) : Sheet()             // of = crew member id, null = yours
    data class TrophyInfo(val k: String, val earned: Boolean) : Sheet()
    object PinPick : Sheet()
    data class Excuse(val i: Int) : Sheet()
    object ChatPlus : Sheet()
    data class Msg(val id: String) : Sheet()
    data class CrestEdit(val crest: Crest) : Sheet()
    object CrewRankInfo : Sheet()
    object CrewSnaps : Sheet()
    // ---- 0.15 ----
    data class PlatePick(val day: Int) : Sheet()
    data class ProfilePage(val id: String) : Sheet()            // crew profile ("" or your id = you)
    data class ProfEdit(val sec: String?) : Sheet()
    object RaidInfo : Sheet()
    object GroupStart : Sheet()
    data class GroupCard(val post: Post) : Sheet()
    data class HofPost(val post: String) : Sheet()
    object Founder : Sheet()
    object Dash : Sheet()
    // ---- 0.16 / 0.17 ----
    data class Badges16(val cat: String) : Sheet()
    data class Psec(val k: String) : Sheet()
    data class Tour(val i: Int) : Sheet()
    object TodayMore : Sheet()
}

/** Full-screen rank-up moment. */
data class Cine(val rank: Int, val label: String, val unlock: String, val sub: String)
