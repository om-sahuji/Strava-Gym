/*
 * Splitfit
 * Copyright (C) 2020  Noah Jutz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.noahjutz.gymroutines

import com.noahjutz.gymroutines.data.domain.Run
import com.noahjutz.gymroutines.util.weekStartEpochSec
import com.noahjutz.gymroutines.util.weeklyDistancesKm
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class RunStatsTest {

    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    // Thursday, 2024-03-07 12:00:00 UTC. The Monday of that week is 2024-03-04.
    private val now = 1709812800L
    private val daySec = 24 * 60 * 60L

    private fun run(id: Long, startEpochSec: Long, meters: Double) = Run(
        stravaId = id,
        name = "Run $id",
        startTimeEpochSec = startEpochSec,
        distanceMeters = meters,
        movingTimeSec = 1800,
        elapsedTimeSec = 1900,
        elevationGainM = 10.0,
        avgHeartRate = null,
        summaryPolyline = null,
        sportType = "Run",
    )

    @Test
    fun weekStartsOnMonday() {
        val weekStart = weekStartEpochSec(now, utc)

        assertEquals(1709510400L, weekStart) // 2024-03-04T00:00:00Z
        // Sunday belongs to the week that started six days earlier.
        assertEquals(weekStart, weekStartEpochSec(weekStart + 6 * daySec + 3600, utc))
        // The next day (Monday) starts a new week.
        assertEquals(weekStart + 7 * daySec, weekStartEpochSec(weekStart + 7 * daySec, utc))
    }

    @Test
    fun aggregatesLastFiveWeeks() {
        val runs = listOf(
            run(1, now - 2 * daySec, 5000.0), // this week
            run(2, now - daySec, 10000.0), // this week
            run(3, now - 8 * daySec, 7000.0), // last week
            run(4, now - 30 * daySec, 3000.0), // four weeks ago
            run(5, now - 120 * daySec, 42000.0), // outside the window
        )

        val weeks = weeklyDistancesKm(runs, now, weeks = 5, timeZone = utc)

        assertEquals(5, weeks.size)
        assertEquals(3.0, weeks[0].kilometers, 0.001) // four weeks ago
        assertEquals(0.0, weeks[1].kilometers, 0.001)
        assertEquals(0.0, weeks[2].kilometers, 0.001)
        assertEquals(7.0, weeks[3].kilometers, 0.001)
        assertEquals(15.0, weeks[4].kilometers, 0.001)

        val total = weeks.sumOf { it.kilometers }
        assertEquals(25.0, total, 0.001)
    }

    @Test
    fun includesRunsStartingExactlyAtWeekStart() {
        val weekStart = weekStartEpochSec(now, utc)
        val runs = listOf(run(1, weekStart, 6000.0))

        val weeks = weeklyDistancesKm(runs, now, weeks = 2, timeZone = utc)

        assertEquals(6.0, weeks.last().kilometers, 0.001)
    }

    @Test
    fun returnsZerosWhenThereAreNoRuns() {
        val weeks = weeklyDistancesKm(emptyList(), now, weeks = 5, timeZone = utc)

        assertEquals(5, weeks.size)
        assertEquals(0.0, weeks.sumOf { it.kilometers }, 0.001)
    }
}
