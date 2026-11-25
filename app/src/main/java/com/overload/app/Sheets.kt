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
    data class MemberInfo(val id: String) : Sheet()
    object SetChallenge : Sheet()
    object Awards : Sheet()
    data class CopyPlan(val member: String, val day: Int?) : Sheet() // day null = whole split
    object SignIn : Sheet()
    data class RecoveryCode(val code: String) : Sheet()
    data class SyncGuard(val n: Int) : Sheet()
}

/** Full-screen rank-up moment. */
data class Cine(val rank: Int, val label: String, val unlock: String, val sub: String)
