package com.overload.app

data class Lib(val name: String, val m: String, val lo: Int, val hi: Int)

val LIB = listOf(
  Lib("Bench press", "Chest", 6, 10),
  Lib("Incline bench press", "Chest", 6, 10),
  Lib("Incline DB press", "Chest", 8, 12),
  Lib("DB bench press", "Chest", 8, 12),
  Lib("Machine chest press", "Chest", 8, 12),
  Lib("Pec deck", "Chest", 10, 15),
  Lib("Cable fly", "Chest", 10, 15),
  Lib("Dips", "Chest", 8, 12),
  Lib("Push ups", "Chest", 10, 20),
  Lib("Lat pulldown", "Back", 8, 12),
  Lib("Pull ups", "Back", 6, 10),
  Lib("Chin ups", "Back", 6, 10),
  Lib("Barbell row", "Back", 6, 10),
  Lib("Seated cable row", "Back", 8, 12),
  Lib("Chest supported row", "Back", 8, 12),
  Lib("T bar row", "Back", 8, 12),
  Lib("One arm DB row", "Back", 8, 12),
  Lib("MTS row", "Back", 8, 12),
  Lib("Deadlift", "Back", 3, 6),
  Lib("Overhead press", "Shoulders", 5, 8),
  Lib("DB shoulder press", "Shoulders", 8, 12),
  Lib("Machine shoulder press", "Shoulders", 8, 12),
  Lib("Arnold press", "Shoulders", 8, 12),
  Lib("Lateral raise", "Side delts", 12, 20),
  Lib("Cable lateral raise", "Side delts", 12, 20),
  Lib("Upright row", "Side delts", 10, 15),
  Lib("Reverse pec deck", "Rear delts", 12, 20),
  Lib("Face pull", "Rear delts", 12, 20),
  Lib("Rear delt fly", "Rear delts", 12, 20),
  Lib("Barbell curl", "Biceps", 8, 12),
  Lib("DB curl", "Biceps", 10, 15),
  Lib("Hammer curl", "Biceps", 10, 15),
  Lib("Preacher curl", "Biceps", 10, 15),
  Lib("Incline DB curl", "Biceps", 10, 15),
  Lib("Cable curl", "Biceps", 10, 15),
  Lib("Tricep pushdown", "Triceps", 10, 15),
  Lib("Overhead tricep extension", "Triceps", 10, 15),
  Lib("Skull crushers", "Triceps", 8, 12),
  Lib("JM press", "Triceps", 8, 12),
  Lib("Close grip bench", "Triceps", 6, 10),
  Lib("Wrist curl", "Forearms", 12, 15),
  Lib("Reverse curl", "Forearms", 10, 15),
  Lib("Shrugs", "Traps", 10, 15),
  Lib("Squat", "Quads", 5, 8),
  Lib("Front squat", "Quads", 6, 10),
  Lib("Hack squat", "Quads", 8, 12),
  Lib("Leg press", "Quads", 10, 15),
  Lib("Leg extension", "Quads", 12, 15),
  Lib("Bulgarian split squat", "Quads", 8, 12),
  Lib("Walking lunge", "Glutes", 10, 12),
  Lib("Romanian deadlift", "Hamstrings", 8, 12),
  Lib("Stiff leg deadlift", "Hamstrings", 8, 12),
  Lib("Lying leg curl", "Hamstrings", 10, 15),
  Lib("Seated leg curl", "Hamstrings", 10, 15),
  Lib("Hip thrust", "Glutes", 8, 12),
  Lib("Cable kickback", "Glutes", 12, 15),
  Lib("Standing calf raise", "Calves", 10, 15),
  Lib("Seated calf raise", "Calves", 12, 20),
  Lib("Cable crunch", "Abs", 12, 20),
  Lib("Hanging leg raise", "Abs", 10, 15),
  Lib("Ab wheel", "Abs", 8, 15),
  Lib("Back extensions", "Lower back", 10, 15)
)

val MUSCLES = listOf("Chest", "Back", "Shoulders", "Side delts", "Rear delts", "Biceps", "Triceps", "Forearms",
    "Traps", "Quads", "Hamstrings", "Glutes", "Calves", "Abs", "Lower back", "Other")

val TYPES = mapOf(
    "up" to ("Upper" to "red"), "leg" to ("Lower" to "blue"), "full" to ("Full body" to "green"),
    "arm" to ("Arms" to "yellow"), "other" to ("Other" to "purple"), "off" to ("Rest" to "black"))

fun slug(s: String): String = s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "ex" }

fun E(name: String, m: String, lo: Int, hi: Int, sets: Int = 2, id: String = slug(name)) =
    Exercise(id, name, m, lo, hi, sets)

private fun D(name: String, type: String, focus: String, groups: List<List<Exercise>> = emptyList(),
              rest: Int = 90, color: String? = null) = DayPlan(name, type, focus, groups, rest, color)

private val J = mapOf(
  "incline" to E("Incline press", "Chest", 6, 10, 2, "incline"),
  "mts" to E("MTS row", "Back", 8, 12, 2, "mts"),
  "pecdeck" to E("Pec deck", "Chest", 10, 15, 2, "pecdeck"),
  "pulldown" to E("Lat pulldown", "Back", 8, 12, 2, "pulldown"),
  "lateral" to E("Lateral raise", "Side delts", 12, 20, 2, "lateral"),
  "revpec" to E("Reverse pec deck", "Rear delts", 12, 20, 2, "revpec"),
  "preacher" to E("Preacher curl", "Biceps", 10, 15, 2, "preacher"),
  "pushdown" to E("Tricep pushdown", "Triceps", 10, 15, 2, "pushdown"),
  "shoulder" to E("Shoulder press", "Shoulders", 6, 10, 2, "shoulder"),
  "closerow" to E("Close grip row", "Back", 8, 12, 2, "closerow"),
  "widerow" to E("Wide grip row", "Back", 10, 12, 2, "widerow"),
  "backext" to E("Back extensions", "Lower back", 10, 15, 2, "backext"),
  "jm" to E("JM press", "Triceps", 8, 12, 2, "jm"),
  "dbcurl" to E("DB curl", "Biceps", 10, 15, 2, "dbcurl"),
  "revcurl" to E("Reverse curl", "Forearms", 10, 15, 2, "revcurl"),
  "fcurl" to E("Forearm curl", "Forearms", 12, 15, 2, "fcurl"),
  "shrugs" to E("Shrugs", "Traps", 10, 15, 2, "shrugs"),
  "fcurl2" to E("Forearm curl 2", "Forearms", 12, 15, 2, "fcurl2")
)
private fun g(vararg ids: String) = ids.map { J.getValue(it) }
private fun s1(vararg e: Exercise) = e.map { listOf(it) }

private fun push() = D("Push", "up", "Chest, shoulders, triceps", s1(E("Bench press", "Chest", 6, 10, 3),
    E("Incline DB press", "Chest", 8, 12), E("DB shoulder press", "Shoulders", 8, 12),
    E("Lateral raise", "Side delts", 12, 20, 3), E("Tricep pushdown", "Triceps", 10, 15),
    E("Overhead tricep extension", "Triceps", 10, 15)), 120)
private fun pull() = D("Pull", "up", "Back, rear delts, biceps", s1(E("Lat pulldown", "Back", 8, 12, 3),
    E("Seated cable row", "Back", 8, 12, 3), E("Face pull", "Rear delts", 12, 20),
    E("Barbell curl", "Biceps", 8, 12), E("Hammer curl", "Biceps", 10, 15)), 120, "yellow")
private fun legs() = D("Legs", "leg", "Quads, hamstrings, glutes, calves", s1(E("Squat", "Quads", 5, 8, 3),
    E("Romanian deadlift", "Hamstrings", 8, 12, 3), E("Leg press", "Quads", 10, 15),
    E("Lying leg curl", "Hamstrings", 10, 15), E("Standing calf raise", "Calves", 10, 15, 3)), 150)
private fun up() = D("Upper", "up", "Chest, back, shoulders, arms", listOf(
    listOf(E("Bench press", "Chest", 6, 10, 3), E("Barbell row", "Back", 6, 10, 3)),
    listOf(E("DB shoulder press", "Shoulders", 8, 12), E("Lat pulldown", "Back", 8, 12)),
    listOf(E("Barbell curl", "Biceps", 10, 15), E("Tricep pushdown", "Triceps", 10, 15))), 90)
private fun lo() = D("Lower", "leg", "Quads, hamstrings, glutes, abs", s1(E("Squat", "Quads", 5, 8, 3),
    E("Romanian deadlift", "Hamstrings", 8, 12, 3), E("Walking lunge", "Glutes", 10, 12),
    E("Lying leg curl", "Hamstrings", 10, 15), E("Cable crunch", "Abs", 12, 20)), 150)
private fun fb() = D("Full body", "full", "Everything, 3 times a week", listOf(
    listOf(E("Squat", "Quads", 5, 8, 3)),
    listOf(E("Bench press", "Chest", 6, 10, 3), E("Barbell row", "Back", 6, 10, 3)),
    listOf(E("Romanian deadlift", "Hamstrings", 8, 12)),
    listOf(E("Lateral raise", "Side delts", 12, 20), E("Barbell curl", "Biceps", 10, 15))), 120)
private fun rest() = D("Rest", "off", "Recovery day")

class Template(val key: String, val label: String, val desc: String, val color: String, val days: () -> List<DayPlan>)

val TEMPLATES = listOf(
    Template("jake", "Jake's split", "Upper body 3x a week with supersets, 2 leg days, an arm day. 6 days.", "red") { listOf(
        D("Upper A", "up", "Chest, back, delts, arms", listOf(g("incline", "mts"), g("pecdeck", "pulldown"), g("lateral", "revpec"), g("preacher", "pushdown"))),
        D("Legs 1", "leg", "Hack squat focus", emptyList(), 150),
        D("Upper B", "up", "Chest, back, delts, lower back", listOf(g("shoulder", "closerow"), g("pecdeck", "pulldown"), g("lateral", "widerow"), g("backext"))),
        D("Arms", "arm", "Biceps, triceps, forearms, traps", listOf(g("jm", "dbcurl"), g("revcurl", "fcurl"), g("shrugs", "fcurl2")), 75),
        D("Legs 2", "leg", "SLDL focus", emptyList(), 150),
        D("Upper C", "up", "Chest, back, delts, arms", listOf(g("incline", "widerow"), g("pecdeck", "mts"), g("lateral", "revpec"), g("preacher", "pushdown"))),
        rest()) },
    Template("ppl", "Push pull legs", "The classic 6 day split. Every muscle twice a week.", "yellow") { listOf(push(), pull(), legs(), push(), pull(), legs(), rest()) },
    Template("ul", "Upper lower", "4 days a week. Simple, strong, easy to recover from.", "blue") { listOf(up(), lo(), rest(), up(), lo(), rest(), rest()) },
    Template("fb", "Full body", "3 days a week. Big lifts, in and out.", "green") { listOf(fb(), rest(), fb(), rest(), fb(), rest(), rest()) },
    Template("blank", "Build my own", "Seven empty days. Pick from the library or type your own.", "purple") {
        listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday").map { D(it, "other", "") } }
)
