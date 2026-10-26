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

package com.noahjutz.gymroutines.data.domain

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A running activity imported from Strava. Stored separately from gym workouts; nothing here is
 * referenced by the routine/workout tables.
 */
@Entity(
    tableName = "run_table",
    indices = [Index("startTimeEpochSec")]
)
data class Run(
    @PrimaryKey
    val stravaId: Long,
    val name: String,
    val startTimeEpochSec: Long,
    val distanceMeters: Double,
    val movingTimeSec: Int,
    val elapsedTimeSec: Int,
    val elevationGainM: Double,
    val avgHeartRate: Double? = null,
    val summaryPolyline: String? = null,
    val sportType: String,
)
