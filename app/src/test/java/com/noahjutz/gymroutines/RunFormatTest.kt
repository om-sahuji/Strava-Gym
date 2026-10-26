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

import com.noahjutz.gymroutines.util.formatDistanceKm
import com.noahjutz.gymroutines.util.formatDuration
import com.noahjutz.gymroutines.util.formatElevation
import com.noahjutz.gymroutines.util.formatHeartRate
import com.noahjutz.gymroutines.util.formatKmValue
import com.noahjutz.gymroutines.util.formatPace
import com.noahjutz.gymroutines.util.paceSecondsPerKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunFormatTest {

    @Test
    fun formatsDistanceInKilometresWithTwoDecimals() {
        assertEquals("5.00 km", formatDistanceKm(5000.0))
        assertEquals("12.35 km", formatDistanceKm(12345.678))
        assertEquals("0.10 km", formatDistanceKm(100.0))
        assertEquals("0.00 km", formatDistanceKm(0.0))
        assertEquals("5.00", formatKmValue(5000.0))
    }

    @Test
    fun formatsDurations() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:45", formatDuration(45))
        assertEquals("45:10", formatDuration(2710))
        assertEquals("59:59", formatDuration(3599))
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:03", formatDuration(3723))
        assertEquals("0:00", formatDuration(-10))
    }

    @Test
    fun computesPaceInSecondsPerKilometre() {
        assertEquals(300, paceSecondsPerKm(5000.0, 1500))
        assertEquals(240, paceSecondsPerKm(10000.0, 2400))
        assertEquals(280, paceSecondsPerKm(21097.5, 5900))
        assertNull(paceSecondsPerKm(0.0, 1200))
        assertNull(paceSecondsPerKm(5000.0, 0))
        assertNull(paceSecondsPerKm(-1.0, 100))
    }

    @Test
    fun formatsPaceInMinutesPerKilometre() {
        assertEquals("5:00 /km", formatPace(5000.0, 1500))
        assertEquals("4:00 /km", formatPace(10000.0, 2400))
        assertEquals("6:05 /km", formatPace(5000.0, 1825))
        assertEquals("– /km", formatPace(0.0, 1500))
    }

    @Test
    fun formatsElevationAndHeartRate() {
        assertEquals("87 m", formatElevation(87.3))
        assertEquals("0 m", formatElevation(0.0))
        assertEquals("148 bpm", formatHeartRate(148.4))
        assertEquals("–", formatHeartRate(null))
    }
}
