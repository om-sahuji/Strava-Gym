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
import com.noahjutz.gymroutines.data.strava.StravaMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StravaMapperTest {

    private val sampleActivities = """
        [
          {
            "id": 1234567890,
            "name": "Morning Run",
            "start_date": "2024-03-05T06:15:00Z",
            "distance": 10234.5,
            "moving_time": 3172,
            "elapsed_time": 3300,
            "total_elevation_gain": 87.3,
            "average_heartrate": 148.4,
            "sport_type": "Run",
            "type": "Run",
            "map": { "summary_polyline": "_p~iF~ps|U_ulLnnqC" }
          },
          {
            "id": 987654321,
            "name": "Evening Ride",
            "start_date": "2024-03-06T18:00:00Z",
            "distance": 30000.0,
            "moving_time": 3600,
            "elapsed_time": 4000,
            "total_elevation_gain": 200.0,
            "sport_type": "Ride",
            "type": "Ride"
          },
          {
            "id": 555555555,
            "name": "Trail loop",
            "start_date": "2024-03-07T09:30:00Z",
            "distance": 21000.0,
            "moving_time": 7200,
            "elapsed_time": 7600,
            "total_elevation_gain": 640.0,
            "sport_type": "TrailRun",
            "type": "Run",
            "map": { "summary_polyline": null }
          },
          {
            "id": 444444444,
            "name": "Treadmill",
            "start_date": "2024-03-08T07:00:00Z",
            "distance": 5000.0,
            "moving_time": 1500,
            "elapsed_time": 1500,
            "total_elevation_gain": 0.0,
            "type": "VirtualRun"
          }
        ]
    """.trimIndent()

    @Test
    fun mapsOnlyRunningActivities() {
        val runs = StravaMapper.toRuns(sampleActivities)

        assertEquals(3, runs.size)
        assertEquals(
            listOf(1234567890L, 555555555L, 444444444L),
            runs.map { it.stravaId }
        )
    }

    @Test
    fun mapsAllRunFields() {
        val run = StravaMapper.toRuns(sampleActivities).first()

        assertEquals(1234567890L, run.stravaId)
        assertEquals("Morning Run", run.name)
        assertEquals(1709619300L, run.startTimeEpochSec)
        assertEquals(10234.5, run.distanceMeters, 0.001)
        assertEquals(3172, run.movingTimeSec)
        assertEquals(3300, run.elapsedTimeSec)
        assertEquals(87.3, run.elevationGainM, 0.001)
        assertEquals(148.4, run.avgHeartRate ?: 0.0, 0.001)
        assertEquals("_p~iF~ps|U_ulLnnqC", run.summaryPolyline)
        assertEquals("Run", run.sportType)
    }

    @Test
    fun keepsNullHeartRateAndPolyline() {
        val trailRun = StravaMapper.toRuns(sampleActivities).first { it.stravaId == 555555555L }

        assertNull(trailRun.avgHeartRate)
        assertNull(trailRun.summaryPolyline)
        assertEquals("TrailRun", trailRun.sportType)
    }

    @Test
    fun fallsBackToTypeFieldWhenSportTypeIsMissing() {
        val treadmillRun = StravaMapper.toRuns(sampleActivities).first { it.stravaId == 444444444L }

        assertEquals("VirtualRun", treadmillRun.sportType)
    }

    @Test
    fun parsesIsoDateWithMilliseconds() {
        assertEquals(1709619300L, StravaMapper.parseStartEpochSec("2024-03-05T06:15:00Z"))
        assertEquals(1709619300L, StravaMapper.parseStartEpochSec("2024-03-05T06:15:00.000Z"))
    }

    @Test
    fun ignoresUnparseableStartDate() {
        assertNull(StravaMapper.parseStartEpochSec(null))
        assertNull(StravaMapper.parseStartEpochSec(""))
        assertNull(StravaMapper.parseStartEpochSec("not a date"))
    }

    @Test
    fun skipsRunWithoutStartDate() {
        val json = """[{"id": 1, "name": "Broken", "sport_type": "Run"}]"""

        assertEquals(emptyList<Run>(), StravaMapper.toRuns(json))
    }

    @Test
    fun handlesEmptyAndInvalidBodies() {
        assertEquals(emptyList<Run>(), StravaMapper.toRuns("[]"))
        assertEquals(emptyList<Run>(), StravaMapper.toRuns("not json"))
        assertNull(StravaMapper.parseActivitiesOrNull("not json"))
    }

    @Test
    fun ignoresUnknownFields() {
        val json = """
            [{
              "id": 42,
              "name": "Run with extra fields",
              "start_date": "2024-01-01T10:00:00Z",
              "distance": 1000.0,
              "moving_time": 300,
              "elapsed_time": 320,
              "total_elevation_gain": 3.0,
              "sport_type": "Run",
              "some_future_field": { "nested": true }
            }]
        """.trimIndent()

        val runs = StravaMapper.toRuns(json)

        assertEquals(1, runs.size)
        assertEquals(42L, runs.first().stravaId)
    }
}
