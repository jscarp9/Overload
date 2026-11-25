package com.overload.app

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Checks the ported engine matches the web app (same seeds -> same challenges) and runs on real-sized data. */
class EngineTest {
    @Test fun seedsMatchWebApp() {
        val cases = mapOf("c2026-11-24" to (2076253034L to listOf(listOf(3, 0, 2), listOf(0, 5, 1))),
            "c2026-01-01" to (2329499928L to listOf(listOf(4, 0, 3), listOf(0, 1, 2))),
            "q2026-11-23" to (2894784833L to listOf(listOf(2, 0, 3), listOf(4, 0, 1))),
            "c2025-12-31" to (3716534490L to listOf(listOf(2, 0, 4), listOf(4, 2, 0))))
        cases.forEach { (k, v) ->
            val h = hashS(k); assertEquals(k, v.first, h)
            assertEquals(k, v.second[0], pick((0 until 5).toList(), 3, h))
            assertEquals(k, v.second[1], pick((0 until 6).toList(), 3, h))
        }
    }

    @Test fun engineRunsOnAYearOfData() {
        val plan = TEMPLATES.first { it.key == "ppl" }.days()
        val sessions = HashMap<String, Session>()
        var d = LocalDate.now().minusDays(400); var w = 100.0; var i = 0
        while (d <= LocalDate.now()) {
            val di = dayIdx(d); val p = plan[di]
            if (p.type != "off" && i % 5 != 0) {
                val ex = p.groups.flatten().associate { e -> e.id to List(e.sets) { SetEntry(w + (i % 7), 8 + (i % 4), null, true) } }
                val start = d.atTime(5 + i % 18, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                sessions[sid(d.toString(), DAYS[di])] = Session(d.toString(), DAYS[di], ex, exNotes = if (i % 9 == 0) mapOf(ex.keys.first() to "felt good") else emptyMap(), start = start, end = start + (40 + i % 60) * 60_000L)
                w += 0.3
            }
            d = d.plusDays(1); i++
        }
        val data = AppData(plan = plan, sessions = sessions, settings = Settings(bw = 180.0, unl = FEATS.map { it[0] }, goals = listOf(Goal(plan[0].groups[0][0].id, 400.0, LocalDate.now().plusMonths(3).toString(), LocalDate.now().minusMonths(2).toString()))))
        val c = Ctx(data, null, null); val e = c.eng
        org.junit.Assert.assertTrue(e.workouts > 200 && e.rank in 0..7 && e.bcount > 0)
        // everything the screens call must run without throwing
        c.dayChallenges(LocalDate.now()); c.weekQuests(LocalDate.now()); c.liftRanks(); c.mastery(); c.chains()
        c.goalInfo(data.settings.goals[0]); c.funStats(c.rs.last()); c.weekReport(); c.yearData(LocalDate.now().year); e.earnedOn(c.rs.last().date)
        // and on a brand new account
        val c2 = Ctx(AppData(plan = plan), null, null); assertEquals(0, c2.eng.rank); assertEquals(3, c2.dayChallenges(LocalDate.now()).size)
    }
}
