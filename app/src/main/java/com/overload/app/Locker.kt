package com.overload.app

import androidx.compose.ui.graphics.Color
import java.time.DayOfWeek
import java.time.LocalDate

/*
 * Port of the web app's 0.14-0.16 unlocks: special plate finishes, themed plates, the Customize
 * "locker", mystery boxes, trophies, flex stats and the funny lines. Keys match the web app so
 * settings sync across both.
 */

// ============================ PLATES ============================
/** A special plate. cat: finish | rank | lift | crew | funny | holiday | special. */
class Fin(val k: String, val name: String, val base: Long, val light: Long, val dark: Long, val how: String,
          val test: ((Ctx) -> Boolean)?, val season: Int? = null, val cat: String = "finish")

private fun liftTierMax(c: Ctx) = runCatching { c.liftRanks().maxOfOrNull { it.t } ?: 0 }.getOrDefault(0)
fun crewLv(c: Ctx) = c.cs?.crew?.level ?: 0
fun crewRk(c: Ctx) = c.cs?.crew?.rank ?: 0
private fun sessOnHoliday(c: Ctx, test: (LocalDate) -> Boolean) = c.rs.any { test(toD(it.date)) }
private fun isThanks(t: LocalDate) = t.monthValue == 11 && t.dayOfWeek == DayOfWeek.THURSDAY && t.dayOfMonth in 22..28
fun liftLegend(c: Ctx, k: String) = runCatching { c.liftRanks().firstOrNull { it.k == k }?.let { it.t >= 6 } == true }.getOrDefault(false)
fun crewTro(c: Ctx, k: String) = c.cs?.crew?.trophies?.containsKey(k) == true
fun raidWon(c: Ctx) = c.cs?.raid?.won?.any { c.myId != null && c.myId in it.by } == true
fun myOG(c: Ctx): Int = c.cs?.members?.firstOrNull { it.id == c.myId }?.profile?.og?.takeIf { it > 0 } ?: c.st.og

val FIN: LinkedHashMap<String, Fin> = linkedMapOf<String, Fin>().apply {
    fun f(k: String, n: String, b: Long, l: Long, d: Long, how: String, season: Int? = null, cat: String = "finish", t: ((Ctx) -> Boolean)?) { put(k, Fin(k, n, b, l, d, how, t, season, cat)) }
    f("mirror", "Mirror Chrome", 0xFFC9CED6, 0xFFFFFFFF, 0xFF6B717C, "Platinum rank") { it.eng.rank >= 4 }
    f("goldleaf", "Gold Leaf", 0xFFC99A2E, 0xFFFFE58A, 0xFF7A5410, "Gold rank") { it.eng.rank >= 3 }
    f("carbon", "Carbon Fiber", 0xFF2A2C31, 0xFF4A4D55, 0xFF101114, "Intermediate lift rank on any lift") { liftTierMax(it) >= 2 }
    f("lava", "Lava", 0xFFB3260B, 0xFFFF8A3D, 0xFF3D0B03, "Season pass level 5", season = 5, t = null)
    f("holo", "Holographic", 0xFFB9C6FF, 0xFFFFFFFF, 0xFF7A88C9, "Season pass level 15", season = 15, t = null)
    f("nebula", "Galaxy", 0xFF2A1260, 0xFF8F6BFF, 0xFF0C0526, "Season pass level 25", season = 25, t = null)
    f("neonring", "Neon Ring", 0xFF0D0F14, 0xFF2A2E38, 0xFF000000, "Crew level 5") { crewLv(it) >= 5 }
    f("diamondp", "Diamond Plate", 0xFF9AA1AB, 0xFFE7EAEE, 0xFF555B65, "Diamond rank") { it.eng.rank >= 5 }
    f("frost", "Frost", 0xFF8FD3FF, 0xFFE9F8FF, 0xFF2B7FB0, "Weekly Grinder collection") { hasB(it.eng, "col_week") }
    f("radioactive", "Toxic", 0xFF3CBF2F, 0xFFC8FF5A, 0xFF0F4D0A, "Around the Clock collection") { hasB(it.eng, "col_clock") }
    f("camo", "Camo", 0xFF5D6B3A, 0xFF8A9A5B, 0xFF2F3A1C, "Earn 3 funny badges") { c -> c.eng.B.count { it.cat == "Funny" && it.earned } >= 3 }
    f("marble", "Marble", 0xFFECE9E4, 0xFFFFFFFF, 0xFFA9A49C, "Crew rank Gold") { crewRk(it) >= 3 }
    f("bloodmoon", "Blood Moon", 0xFF7A0A12, 0xFFD4252F, 0xFF2A0306, "Titan rank") { it.eng.rank >= 6 }
    f("laurel", "Olympian Laurel", 0xFFF4E3B0, 0xFFFFFAF0, 0xFFB8862B, "Olympian rank") { it.eng.rank >= 7 }
    // 0.15 themed plates
    f("r_bronze", "Bronze Forged", 0xFFA0612B, 0xFFE0A066, 0xFF4A2A0E, "Bronze rank", cat = "rank") { it.eng.rank >= 1 }
    f("r_silver", "Silver Engraved", 0xFFA9B1BC, 0xFFF2F5F8, 0xFF5B6370, "Silver rank", cat = "rank") { it.eng.rank >= 2 }
    f("r_gold", "Gold Crest", 0xFFC99A2E, 0xFFFFE58A, 0xFF6B4A0C, "Gold rank", cat = "rank") { it.eng.rank >= 3 }
    f("r_plat", "Platinum Cut", 0xFFCFD8E3, 0xFFFFFFFF, 0xFF7D8A9B, "Platinum rank", cat = "rank") { it.eng.rank >= 4 }
    f("r_diamond", "Diamond Crystal", 0xFF7FD3FF, 0xFFE8F8FF, 0xFF1F6FA8, "Diamond rank", cat = "rank") { it.eng.rank >= 5 }
    f("r_titan", "Titan Stone", 0xFF3A3A40, 0xFF6B6B74, 0xFF121216, "Titan rank", cat = "rank") { it.eng.rank >= 6 }
    f("r_olympian", "Olympian Marble", 0xFFF1ECE2, 0xFFFFFFFF, 0xFFB9AD92, "Olympian rank", cat = "rank") { it.eng.rank >= 7 }
    f("c_mpc", "Million Pound", 0xFF4A4D55, 0xFF8B9099, 0xFF1A1B1F, "Crew trophy: Million Pound Club", cat = "crew") { crewTro(it, "mpc") }
    f("c_storm", "PR Storm", 0xFF2A2F4A, 0xFF5B6390, 0xFF0D0F1C, "Crew trophy: PR Storm", cat = "crew") { crewTro(it, "prstorm") }
    f("c_phoenix", "Phoenix", 0xFFC2410C, 0xFFFFB347, 0xFF3D0B03, "Crew trophy: Comeback Crew", cat = "crew") { crewTro(it, "comeback") }
    f("c_raid", "Boss Slayer", 0xFF3B1D4A, 0xFF8B5CF6, 0xFF12061A, "Help your crew beat a boss raid", cat = "crew") { raidWon(it) }
    f("f_chicken", "Chicken Legs", 0xFFF5D77A, 0xFFFFF3C4, 0xFFB8862B, "Funny badge: Leg Day Dodger", cat = "funny") { hasB(it.eng, "f_legs") }
    f("f_ghost", "Ghost", 0xFFE8ECF5, 0xFFFFFFFF, 0xFF9AA3B8, "Funny badge: Ghost", cat = "funny") { hasB(it.eng, "f_ghost") }
    f("f_moon", "Night Shift", 0xFF141B3D, 0xFF2F3B7A, 0xFF05081A, "Funny badge: Night Shift", cat = "funny") { hasB(it.eng, "f_night") }
    f("f_couch", "Couch Potato", 0xFFB07A3F, 0xFFE0B072, 0xFF5A3A17, "Funny badge: Rest Day Champion", cat = "funny") { hasB(it.eng, "f_rest") }
    f("f_peacock", "Peacock", 0xFF0F6B6B, 0xFF3CC9B5, 0xFF06302F, "Funny badge: Ego Lifter", cat = "funny") { hasB(it.eng, "f_ego") }
    f("h_xmas", "Christmas", 0xFFB3121F, 0xFFE8424E, 0xFF4D060B, "Lift on Christmas (Dec 25)", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 12 && it.dayOfMonth == 25 } }
    f("h_newyear", "New Year", 0xFF16130B, 0xFF4A3F1C, 0xFF000000, "Lift on New Year’s Day", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 1 && it.dayOfMonth == 1 } }
    f("h_valentine", "Valentine", 0xFFE04A82, 0xFFFF9FC4, 0xFF7A1236, "Lift on Valentine’s Day (Feb 14)", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 2 && it.dayOfMonth == 14 } }
    f("h_stpat", "Lucky Clover", 0xFF178A3C, 0xFF5ED47F, 0xFF063A17, "Lift on St. Patrick’s Day (Mar 17)", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 3 && it.dayOfMonth == 17 } }
    f("h_july4", "Fourth of July", 0xFF1D3A8A, 0xFF4A6FD6, 0xFF0A1640, "Lift on the Fourth of July", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 7 && it.dayOfMonth == 4 } }
    f("h_halloween", "Halloween", 0xFFE8650C, 0xFFFFA44A, 0xFF5A2302, "Lift on Halloween (Oct 31)", cat = "holiday") { c -> sessOnHoliday(c) { it.monthValue == 10 && it.dayOfMonth == 31 } }
    f("h_thanks", "Turkey Day", 0xFF9A4A17, 0xFFE0903F, 0xFF3D1A05, "Lift on Thanksgiving", cat = "holiday") { c -> sessOnHoliday(c, ::isThanks) }
    f("founder", "Founder 001", 0xFF0B0B0D, 0xFF2A2A2E, 0xFF000000, "Only the founder of Overload", cat = "special") { it.st.founder }
    f("og", "OG", 0xFFEFE6CF, 0xFFFFFAF0, 0xFFA8946A, "Join Overload before the App Store launch", cat = "special") { myOG(it) > 0 }
    LIFTS.forEach { l -> f("l_${l.k}", "${l.name} Legend", 0xFF2B2E35, 0xFF5A606B, 0xFF0D0E12, "Legend rank on ${l.name}", cat = "lift") { liftLegend(it, l.k) } }
}

val PCAT = linkedMapOf("colors" to "Colors", "finish" to "Finishes", "rank" to "Ranks", "lift" to "Lift legends", "crew" to "Crew", "funny" to "Funny", "holiday" to "Holiday", "special" to "Special")
fun plateCat(k: String) = FIN[k]?.cat ?: "colors"
fun plateName(k: String) = FIN[k]?.name ?: k.replaceFirstChar { it.uppercase() }
fun Ctx.cos14(t: String) = st.cos14[t] ?: emptyList()
fun Ctx.finOwned(k: String): Boolean { val f = FIN[k] ?: return true; if (k in cos14("plate")) return true; return f.test?.invoke(this) ?: false }
/** Owned and not rank-locked. */
fun Ctx.plateOwn(k: String) = plateLocked(k).isEmpty() && finOwned(k)
fun Ctx.plateHow(k: String) = plateLocked(k).ifEmpty { FIN[k]?.how ?: "" }

// ============================ THE LOCKER ============================
class LockItem(val k: String, val label: String, val extra: String, val how: String, val test: ((Ctx) -> Boolean)?, val box: Boolean = false)
class LockCat(val name: String, val items: LinkedHashMap<String, LockItem>)
private fun cat(name: String, vararg items: LockItem) = LockCat(name, linkedMapOf(*items.map { it.k to it }.toTypedArray()))
private fun li(k: String, label: String, extra: String, how: String, t: ((Ctx) -> Boolean)?) = LockItem(k, label, extra, how, t)
private fun box(k: String, label: String, extra: String = "") = LockItem(k, label, extra, "Mystery box", null, true)
private val yes: (Ctx) -> Boolean = { true }

val LOCK: LinkedHashMap<String, LockCat> = linkedMapOf(
    "accent" to cat("App theme", li("red", "Overload Red", "#ef2b36", "", yes), li("blue", "Ocean", "#2563ff", "Bronze rank") { it.eng.rank >= 1 },
        li("green", "Forest", "#16b65b", "Silver rank") { it.eng.rank >= 2 }, li("purple", "Royal", "#7c4dff", "Gold rank") { it.eng.rank >= 3 },
        li("cyan", "Ice", "#06b6d4", "Crew level 3") { crewLv(it) >= 3 }, li("gold", "Gold", "#c99a2e", "Platinum rank") { it.eng.rank >= 4 },
        li("mono", "Mono", "#14161c", "Season pass level 10") { it.eng.lvl >= 10 }, box("pink", "Hot Pink", "#ff3d8b")),
    "bg" to cat("Background", li("classic", "Classic", "", "", yes), li("minimal", "Dim art", "", "", yes),
        li("grid", "Blueprint", "", "Silver rank") { it.eng.rank >= 2 }, li("stars", "Starfield", "", "Diamond rank") { it.eng.rank >= 5 }),
    "welcome" to cat("Welcome screen", li("classic", "Classic", "", "", yes), li("lightning", "Lightning", "", "Gold rank") { it.eng.rank >= 3 },
        li("forge", "Forge sparks", "", "Season pass level 12") { it.eng.lvl >= 12 }, box("galaxy", "Galaxy")),
    "icon" to LockCat("App icon", linkedMapOf<String, LockItem>().apply {
        put("red", li("red", "Red plate", "red", "", yes)); put("black", li("black", "Black plate", "black", "", yes))
        put("gold", li("gold", "Gold plate", "gold", "Gold rank") { it.eng.rank >= 3 })
        FIN.values.filter { it.cat == "finish" }.forEach { f -> put(f.k, li(f.k, f.name, f.k, f.how) { it.finOwned(f.k) }) }
    }),
    "setfx" to cat("Set complete effect", li("pulse", "Pulse", "", "", yes), li("sparks", "Sparks", "", "Bronze rank") { it.eng.rank >= 1 },
        li("shock", "Shockwave", "", "Silver rank") { it.eng.rank >= 2 }, li("pop", "Confetti pop", "", "Season pass level 7") { it.eng.lvl >= 7 }, box("lightning", "Lightning")),
    "prfx" to cat("PR celebration", li("confetti", "Confetti", "", "", yes), li("fireworks", "Fireworks", "", "Gold rank") { it.eng.rank >= 3 },
        li("goldrain", "Gold rain", "", "Platinum rank") { it.eng.rank >= 4 }, box("storm", "Lightning storm")),
    "rest" to cat("Rest timer", li("classic", "Classic ring", "", "", yes), li("bar", "Minimal bar", "", "", yes),
        li("plate", "Spinning plate", "", "Bronze rank") { it.eng.rank >= 1 }, box("neon", "Neon")),
    "sound" to cat("Sound pack", li("classic", "Classic beep", "", "", yes), li("soft", "Soft chime", "", "", yes),
        li("gong", "Gong", "", "Silver rank") { it.eng.rank >= 2 }, li("airhorn", "Air horn", "", "Gold rank") { it.eng.rank >= 3 }, box("arcade", "Arcade")),
    "nameFx" to cat("Name effect", li("none", "Plain", "", "", yes), li("gold", "Gold shine", "", "Gold rank") { it.eng.rank >= 3 },
        li("neon", "Neon", "", "Crew level 4") { crewLv(it) >= 4 }, li("fire", "Fire", "", "Season pass level 17") { it.eng.lvl >= 17 }, box("rainbow", "Rainbow")),
    "card" to cat("Post card style", li("plain", "Plain", "", "", yes), li("carbon", "Carbon", "", "Silver rank") { it.eng.rank >= 2 },
        li("gold", "Gold", "", "Gold rank") { it.eng.rank >= 3 }, li("ice", "Ice", "", "Season pass level 21") { it.eng.lvl >= 21 }, box("neon", "Neon")),
    "pbanner" to cat("Profile banner", li("none", "None", "", "", yes), li("forge", "Forge", "", "", yes),
        li("city", "Night City", "", "Bronze rank") { it.eng.rank >= 1 }, li("ocean2", "Ocean Storm", "", "Silver rank") { it.eng.rank >= 2 },
        li("gym", "Iron Gym", "", "Gold rank") { it.eng.rank >= 3 }, li("aurora", "Northern Lights", "", "Platinum rank") { it.eng.rank >= 4 },
        li("volcano", "Volcano", "", "Diamond rank") { it.eng.rank >= 5 }, li("space", "Deep Space", "", "Titan rank") { it.eng.rank >= 6 },
        li("olympus", "Olympus", "", "Olympian rank") { it.eng.rank >= 7 }, li("colosseum", "Colosseum", "", "Season pass level 20") { it.eng.lvl >= 20 },
        li("thunder", "Thunder", "", "25 PRs") { it.eng.lifePR >= 25 },
        // 0.16 banners
        li("sunrise", "Sunrise", "", "", yes), li("neon", "Neon Gym", "", "4 week streak") { it.eng.streak >= 4 },
        li("summit", "Summit", "", "50 workouts") { it.eng.workouts >= 50 }, li("chalk", "Chalk Cloud", "", "10 PRs") { it.eng.lifePR >= 10 },
        li("founder", "Founder", "", "Founder only") { it.st.founder }),
    "share" to cat("Share card theme", li("plate", "Plate color", "", "", yes), li("midnight", "Midnight", "", "", yes),
        li("gold", "Gold", "", "Gold rank") { it.eng.rank >= 3 }, box("neon", "Neon")),
    "react" to cat("Reactions", li("fire", "🔥", "", "", yes), li("muscle", "💪", "", "", yes), li("trophy", "🏆", "", "", yes),
        li("clap", "👏", "", "Bronze rank") { it.eng.rank >= 1 }, li("heart", "❤️", "", "Silver rank") { it.eng.rank >= 2 },
        li("crown", "👑", "", "Gold rank") { it.eng.rank >= 3 }, li("rocket", "🚀", "", "Season pass level 9") { it.eng.lvl >= 9 },
        li("skull", "💀", "", "Earn a funny badge") { c -> c.eng.B.any { it.cat == "Funny" && it.earned } }, box("goat", "🐐"), box("lol", "😂"),
        li("crest", "🛡", "", "Be in a crew") { it.cs != null })
)
val LOOK_DEF = mapOf("accent" to "red", "bg" to "classic", "welcome" to "classic", "icon" to "red", "setfx" to "pulse", "prfx" to "confetti",
    "rest" to "classic", "sound" to "classic", "nameFx" to "none", "card" to "plain", "pbanner" to "none", "share" to "plate")
val LK_ICON = mapOf("accent" to "🎨", "bg" to "🌌", "welcome" to "👋", "icon" to "📱", "setfx" to "✨", "prfx" to "🎉", "rest" to "⏱", "sound" to "🔊",
    "nameFx" to "🔤", "card" to "🃏", "share" to "📸", "react" to "😂")
/** Locker categories that only change something in the web app (no splash, share card or icon switching on Android yet). */
val WEB_ONLY_LOOKS = setOf("welcome", "icon", "share")

/** All reactions, in order. The crest one is drawn as the crew crest. */
val RE14 = linkedMapOf("fire" to "🔥", "muscle" to "💪", "trophy" to "🏆", "clap" to "👏", "heart" to "❤️", "crown" to "👑", "rocket" to "🚀",
    "skull" to "💀", "goat" to "🐐", "lol" to "😂", "crest" to "🛡")

fun Ctx.owns(t: String, k: String): Boolean {
    val it = LOCK[t]?.items?.get(k) ?: return false
    if (k in cos14(t)) return true
    return it.test?.invoke(this) ?: false
}
fun Ctx.look(t: String): String { val k = st.look[t] ?: LOOK_DEF[t] ?: ""; return if (LOCK[t]?.items?.get(k) != null && owns(t, k)) k else LOOK_DEF[t] ?: "" }
fun Ctx.reactKeys() = LOCK.getValue("react").items.keys.filter { owns("react", it) }
fun Ctx.myBanner(): String { val k = look("pbanner"); if (k != "none") return k; return if (st.founder) "founder" else "" }
fun accentOf(k: String): Color = hex(LOCK.getValue("accent").items[k]?.extra ?: "#ef2b36")

// ---------- mystery boxes (earned only, never bought) ----------
val BOX_LVLS = listOf(3, 7, 11, 19, 23, 27)
fun Ctx.boxSources(): List<String> {
    val e = eng; val out = LinkedHashSet<String>()
    BOX_LVLS.forEach { l -> if (e.lvl >= l) out += "S${e.season}-$l" }
    for (r in 1..e.rank) out += "R$r"
    st.seasons.forEach { (s, l) -> BOX_LVLS.forEach { x -> if (l >= x) out += "S$s-$x" } }
    cs?.raid?.won?.forEach { w -> if (myId != null && myId in w.by) out += "RAID${w.wk}" }
    return out.toList()
}
fun Ctx.boxesLeft() = boxSources().filter { st.boxes[it] == null }
/** What the next box gives (cat to key), or null for "nothing new". Same pick as the web app. */
fun Ctx.boxPrize(src: String): Pair<String, String>? {
    val pool = mutableListOf<Pair<String, String>>()
    LOCK.forEach { (t, v) -> v.items.forEach { (k, it) -> if (it.box && k !in cos14(t)) pool += t to k } }
    if (pool.isEmpty()) return null
    return pool[(Math.floorMod(hashS(src + (st.crewMe?.id ?: "x")), pool.size.toLong())).toInt()]
}

// ============================ TROPHIES ============================
class Trophy(val k: String, val type: String, val name: String, val how: String, val earned: Boolean, val color: Long)
fun Ctx.trophies(): List<Trophy> {
    val e = eng; val out = mutableListOf<Trophy>()
    fun T(k: String, type: String, name: String, how: String, earned: Boolean, color: Long) { out += Trophy(k, type, name, how, earned, color) }
    for (r in 1..7) T("rank$r", "statue", "${RANKS[r].name} statue", "Reach ${RANKS[r].name}", e.rank >= r, RANKS[r].light)
    e.B.filter { it.cat == "Collections" }.forEach { b -> T("cup_${b.k}", "cup", "${b.name} cup", b.desc, b.earned, 0xFFE3C25A) }
    val seasons = st.seasons + (e.season.toString() to e.lvl)
    seasons.entries.sortedBy { it.key }.forEach { (s, l) ->
        val tier = if (l >= 30) "Gold" else if (l >= 20) "Silver" else if (l >= 10) "Bronze" else ""
        if (tier.isNotEmpty() || s == e.season.toString()) T("season$s", "medal", "Season $s ${tier} medal".replace("  ", " ").trim(), "Reach season pass level 10, 20 or 30", tier.isNotEmpty(),
            if (tier == "Gold") 0xFFE3C25A else if (tier == "Silver") 0xFFCFD4DC else 0xFFC98A4A)
    }
    runCatching {
        val me = myId; val c = cs
        if (me != null && c != null) c.votes.keys.forEach { mo -> awardWinnersOf(c, mo).forEach { (cat, id) -> if (id == me) AWARDS.firstOrNull { it.first == cat }?.let { a ->
            T("award_$mo$cat", "plaque", "${a.second} · ${MON[mo.substring(5).toInt() - 1]}", "Win a monthly crew award", true, 0xFF9B7BFF) } } }
    }
    if (out.none { it.type == "plaque" }) T("award_any", "plaque", "Crew award plaque", "Win a monthly crew award", false, 0xFF9B7BFF)
    val legend = runCatching { liftRanks().count { it.t >= 6 } }.getOrDefault(0)
    T("barbell", "barbell", "Golden barbell", "Reach Legend on a lift rank", legend > 0, 0xFFE3C25A)
    return out
}
val AWARDS = listOf(Triple("grinder", "Biggest Grinder", "⚙️"), Triple("hype", "Best Hype Man", "📣"), Triple("comeback", "Comeback of the Month", "🔁"),
    Triple("improved", "Most Improved", "📈"), Triple("post", "Post of the Month", "⭐"))
fun awardWinnersOf(cs: CrewState, mo: String): Map<String, String> {
    val v = cs.votes[mo] ?: return emptyMap(); val out = HashMap<String, String>()
    AWARDS.forEach { (k) -> val top = (v[k]?.tally ?: emptyMap()).entries.sortedByDescending { it.value }
        if (top.isNotEmpty() && (top.size < 2 || top[0].value > top[1].value)) out[k] = top[0].key }
    return out
}

// ============================ CREW PROFILE PIECES ============================
val AV_ICONS = listOf("🦍", "🐺", "🦁", "🐻", "🦅", "🐉", "💀", "👑", "⚡", "🔥", "🗿", "🦈", "🐂", "🥷", "🤖", "👽")
val AV_COLS = listOf("#ef2b36", "#2563ff", "#16b65b", "#7c4dff", "#c99a2e", "#06b6d4", "#ff3d8b", "#14161c", "#e8650c", "#64748b")
val PHASES = listOf("Bulking", "Cutting", "Maintaining", "Recomp", "Peaking")
val LTYPES = listOf("Powerlifter", "Bodybuilder", "Strength", "Hybrid", "Aesthetic", "Athlete", "Beginner")
val ROLES = listOf("", "Co-captain", "Hype Man", "Spotter", "Coach", "Rookie", "Legend", "Enforcer", "Comedian")

/** Every time a lift's best estimated 1RM went up. */
fun Ctx.prTimeline(): List<PrPt> {
    val best = HashMap<String, Double>(); val out = mutableListOf<PrPt>()
    rs.forEach { s -> s.ex.forEach { (id, a) -> val top = a.filter(::full).maxByOrNull(::e1) ?: return@forEach; val v = e1(top)
        if (best[id] != null && v > best[id]!! + .01) out += PrPt(s.names[id] ?: exName(id), top.w!!, top.r!!, s.date)
        if (best[id] == null || v > best[id]!!) best[id] = v } }
    return out
}
fun Ctx.bestLift(): PrPt? {
    var b: PrPt? = null
    rs.forEach { s -> s.ex.forEach { (id, a) -> a.filter(::full).forEach { x -> if (b == null || e1(x) > b!!.v) b = PrPt(s.names[id] ?: exName(id), x.w!!, x.r!!, s.date, e1(x)) } } }
    return b
}
val FLEX = linkedMapOf<String, Pair<String, (Ctx) -> String>>(
    "best" to ("Best lift" to { c -> c.bestLift()?.let { "${it.n.split(" ")[0]} ${it.w.toInt()}×${it.r}" } ?: "–" }),
    "streak" to ("Week streak" to { c -> "${c.eng.streak} wk" }),
    "vol" to ("Total lifted" to { c -> "${kfmt(c.rs.sumOf(::sVol))} ${c.unit}" }),
    "wo" to ("Workouts" to { c -> "${c.rs.size}" }),
    "prs" to ("PRs" to { c -> "${c.prTimeline().size}" }),
    "badges" to ("Badges" to { c -> "${c.eng.B.count { it.earned }}" }),
    "season" to ("Season level" to { c -> "Lv ${c.eng.lvl}" }),
    "rank" to ("Rank" to { c -> RANKS[c.eng.rank].name })
)
fun Ctx.myFlex() = st.flex.take(3).mapNotNull { k -> FLEX[k]?.let { FlexStat(k, it.first, it.second(this)) } }

// ============================ FUNNY STUFF ============================
val PR_LINES = listOf("Neighbors felt that one", "New PR. Somebody call the news", "The bar just asked for your number", "That plate is never going to be the same",
    "PR unlocked. Ego slightly inflated", "Gym mirror wants an autograph", "Gravity has filed a complaint", "Your future self says thanks")
val REST_LINES = listOf("Your phone is not a set", "Scroll less, lift more", "Breathe. Then go be dangerous", "Hydrate or diedrate", "The weights miss you already",
    "Rest is part of the program. Allegedly", "Don’t you dare check Instagram", "Shake it out. Look tough", "Count your blessings. And your reps", "Chalk up. Mentally",
    "The bar isn’t going to lift itself", "Silence. Champion resting", "Fix your hair, next set is the money set", "Rest like a lion. Lift like a bigger lion")
val PLATE_LINES = listOf("Put me down, I’m tired", "Hey! That tickles", "I’m a 45, not a frisbee", "Lift me, don’t poke me", "I’ve seen things. Heavy things",
    "Rack me when you’re done", "Spin me right round", "I’m load bearing, you know", "Squat day? Please say squat day", "You’re doing great, sweetie")
val EX_A = listOf("My pre-workout was decaf", "My foam roller filed for divorce", "Mercury is in retrograde and so are my gains", "My protein shaker is in the wash",
    "I pulled a muscle reaching for the remote", "The gym playlist hurt my feelings", "My dog said I looked too strong already", "I’m saving my energy for leg day next year",
    "My spotter is on vacation. In my mind", "It’s chest day somewhere, just not here", "My lifting shoes are in a mood", "The squat rack and I need space",
    "I’m in a committed relationship with my couch", "My muscles requested a personal day", "I’m bulking. Emotionally")

private val LEGS = Regex("quad|ham|glute|calf|adduct|leg", RegexOption.IGNORE_CASE)
/** A funny line under a crew workout post (same pick as the web app). */
fun roastLine(p: Post, unit: String): String? {
    if (p.type != "workout") return null
    val d = p.data; val legs = d.levels.keys.any { LEGS.containsMatchIn(it) }; val v = jsRound(toMine(d.vol, d.unit, unit)); val pool = mutableListOf<String>()
    if (!legs && d.levels.isNotEmpty()) pool += listOf("Legs filed a missing persons report", "Upper body day again? The legs are drafting a strongly worded letter")
    if (legs) pool += listOf("Leg day survived. Stairs are now the enemy", "Walking funny tomorrow, guaranteed")
    if (d.prs >= 2) pool += "${d.prs} PRs? Somebody check this person for performance enhancing vibes" else if (d.prs == 1) pool += "PR alert. Neighbors felt that one"
    if (v > 20000) pool += "${kfmt(v.toDouble())} $unit moved. The floor wants a break"
    if (d.sets >= 25) pool += "${d.sets} sets. Somebody is not going home early"
    if (d.min in 1..29) pool += "In and out in ${d.min} min. Speedrun or skipped half? We’ll never know"
    if (d.min >= 100) pool += "${d.min} minutes. Did you lift or move in?"
    pool += listOf("Solid work. The mirror is impressed", "Certified gym rat behavior", "Another one for the highlight reel")
    return pool[Math.floorMod(hashS(p.id), pool.size.toLong()).toInt()]
}

// ============================ APP TOUR ============================
val TOUR = listOf(
    Triple("🏋️", "Today", "Your workout for today. Tap a set to log it and the big plate fills up as you go. Rest timer starts on its own."),
    Triple("👥", "Crew", "Lift with friends. Post workouts, chat, and take down a boss together every week."),
    Triple("🪪", "Profile", "Your rank, badges and trophies. Make it yours in Edit profile and Customize."),
    Triple("📊", "Stats", "Your progress, PRs and how every lift is trending."),
    Triple("📋", "Plan", "Set your weekly split once. Each day gets its own plate and it repeats every week.")
)
