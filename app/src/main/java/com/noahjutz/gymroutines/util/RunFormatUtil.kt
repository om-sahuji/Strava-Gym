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

import java.util.Locale
import kotlin.math.roundToInt

/** Placeholder for values that cannot be derived (e.g. pace of a run without distance). */
const val RUN_VALUE_PLACEHOLDER = "–"

fun formatDistanceKm(distanceMeters: Double): String =
    String.format(Locale.US, "%.2f km", distanceMeters / 1000.0)

/** Distance in km with 2 decimals and no unit, e.g. `5.00`. */
fun formatKmValue(distanceMeters: Double): String =
    String.format(Locale.US, "%.2f", distanceMeters / 1000.0)

/**
 * Pace in seconds per kilometre. Returns null when it cannot be computed, so the UI can show a
 * placeholder instead of `Infinity`.
 */
fun paceSecondsPerKm(distanceMeters: Double, movingTimeSec: Int): Int? {
    if (distanceMeters <= 0.0 || movingTimeSec <= 0) return null
    return (movingTimeSec / (distanceMeters / 1000.0)).roundToInt()
}

/** Formats a pace given in seconds per kilometre, e.g. `5:12 /km`. */
fun formatPace(distanceMeters: Double, movingTimeSec: Int): String {
    val pace = paceSecondsPerKm(distanceMeters, movingTimeSec) ?: return "$RUN_VALUE_PLACEHOLDER /km"
    return "${formatDuration(pace)} /km"
}

/** Formats a duration in seconds as `MM:SS` or `H:MM:SS`. */
fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val hours = safeSeconds / 3600
    val minutes = (safeSeconds % 3600) / 60
    val seconds = safeSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Formats elevation gain, e.g. `123 m`. */
fun formatElevation(elevationMeters: Double): String =
    String.format(Locale.US, "%.0f m", elevationMeters)

/** Formats heart rate, e.g. `152 bpm`, or a placeholder if unknown. */
fun formatHeartRate(heartRate: Double?): String =
    heartRate?.let { String.format(Locale.US, "%.0f bpm", it) } ?: RUN_VALUE_PLACEHOLDER
