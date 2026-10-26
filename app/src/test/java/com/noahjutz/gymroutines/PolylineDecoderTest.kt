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

import com.noahjutz.gymroutines.util.decodePolyline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PolylineDecoderTest {

    @Test
    fun decodesCanonicalExample() {
        // Reference example from the Google encoded polyline algorithm documentation.
        val points = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.00001)
        assertEquals(-120.2, points[0].longitude, 0.00001)
        assertEquals(40.7, points[1].latitude, 0.00001)
        assertEquals(-120.95, points[1].longitude, 0.00001)
        assertEquals(43.252, points[2].latitude, 0.00001)
        assertEquals(-126.453, points[2].longitude, 0.00001)
    }

    @Test
    fun handlesEmptyAndNullInput() {
        assertTrue(decodePolyline(null).isEmpty())
        assertTrue(decodePolyline("").isEmpty())
    }

    @Test
    fun decodesSinglePoint() {
        val points = decodePolyline("_p~iF~ps|U")

        assertEquals(1, points.size)
        assertEquals(38.5, points[0].latitude, 0.00001)
    }

    @Test
    fun stopsOnTruncatedInputInsteadOfThrowing() {
        val points = decodePolyline("_p~iF~ps|U_ulLnnqC_mq")

        assertEquals(2, points.size)
    }
}
