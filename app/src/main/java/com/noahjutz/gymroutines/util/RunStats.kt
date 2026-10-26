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

package com.noahjutz.gymroutines.util

import com.noahjutz.gymroutines.data.domain.Run
import java.util.Calendar
import java.util.TimeZone

/** Total distance of one calendar week, Monday-based. */
data class WeeklyDistance(
    val weekStartEpochSec: Long,
    val kilometers: Double,
)

/** Epoch seconds of Monday 00:00 of the week containing [epochSec]. */
fun weekStartEpochSec(epochSec: Long, timeZone: TimeZone = TimeZone.getDefault()): Long {
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = epochSec * 1000
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        firstDayOfWeek = Calendar.MONDAY
    }
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_MONTH, -1)
    }
    return calendar.timeInMillis / 1000
}

/**
 * Distances per calendar week, oldest first, ending with the week containing [nowEpochSec].
 * The returned list always has [weeks] entries (weeks without runs have 0 km).
 */
fun weeklyDistancesKm(
    runs: List<Run>,
    nowEpochSec: Long,
    weeks: Int = 5,
    timeZone: TimeZone = TimeZone.getDefault(),
): List<WeeklyDistance> {
    require(weeks > 0) { "weeks must be positive" }
    val currentWeekStart = weekStartEpochSec(nowEpochSec, timeZone)
    val daySec = 24 * 60 * 60L
    val weekSec = 7 * daySec

    val totals = HashMap<Long, Double>()
    for (run in runs) {
        val runWeekStart = weekStartEpochSec(run.startTimeEpochSec, timeZone)
        totals[runWeekStart] = (totals[runWeekStart] ?: 0.0) + run.distanceMeters / 1000.0
    }

    return (0 until weeks).map { index ->
        val weekStart = currentWeekStart - (weeks - 1 - index) * weekSec
        WeeklyDistance(weekStartEpochSec = weekStart, kilometers = totals[weekStart] ?: 0.0)
    }
}
