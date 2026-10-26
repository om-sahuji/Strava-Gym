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

package com.noahjutz.gymroutines.data.strava

import com.noahjutz.gymroutines.data.domain.Run
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class StravaTokenDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_at") val expiresAt: Long = 0L,
)

@Serializable
data class StravaActivityDto(
    val id: Long,
    val name: String = "",
    @SerialName("start_date") val startDate: String? = null,
    val distance: Double = 0.0,
    @SerialName("moving_time") val movingTime: Int = 0,
    @SerialName("elapsed_time") val elapsedTime: Int = 0,
    @SerialName("total_elevation_gain") val totalElevationGain: Double = 0.0,
    @SerialName("average_heartrate") val averageHeartRate: Double? = null,
    @SerialName("sport_type") val sportType: String? = null,
    val type: String? = null,
    val map: StravaMapDto? = null,
)

@Serializable
data class StravaMapDto(
    @SerialName("summary_polyline") val summaryPolyline: String? = null,
)

/**
 * Mapping between the Strava API and the local [Run] entity. Kept free of Android dependencies so
 * it can be unit tested on the JVM.
 */
object StravaMapper {
    val runSportTypes = setOf("Run", "TrailRun", "VirtualRun")

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val isoFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ssZ",
    )

    /**
     * Parses an ISO 8601 timestamp as returned by Strava (e.g. `2018-02-16T14:52:54Z`) into epoch
     * seconds. Returns null for unparseable input.
     */
    fun parseStartEpochSec(isoDate: String?): Long? {
        if (isoDate.isNullOrBlank()) return null
        for (format in isoFormats) {
            try {
                val parser = SimpleDateFormat(format, Locale.US)
                parser.timeZone = TimeZone.getTimeZone("UTC")
                val parsed = parser.parse(isoDate) ?: continue
                return parsed.time / 1000
            } catch (e: ParseException) {
                // try next format
            }
        }
        return null
    }

    /** Returns null if the body is not a JSON array of activities. */
    fun parseActivitiesOrNull(body: String): List<StravaActivityDto>? {
        return try {
            json.decodeFromString<List<StravaActivityDto>>(body)
        } catch (e: Exception) {
            null
        }
    }

    fun parseActivities(body: String): List<StravaActivityDto> {
        return parseActivitiesOrNull(body) ?: emptyList()
    }

    /**
     * Maps a Strava activity to a [Run]. Returns null if the activity is not a run or if the start
     * time cannot be parsed.
     */
    fun toRun(activity: StravaActivityDto): Run? {
        val sportType = activity.sportType ?: activity.type ?: return null
        if (sportType !in runSportTypes) return null
        val startEpochSec = parseStartEpochSec(activity.startDate) ?: return null
        return Run(
            stravaId = activity.id,
            name = activity.name,
            startTimeEpochSec = startEpochSec,
            distanceMeters = activity.distance,
            movingTimeSec = activity.movingTime,
            elapsedTimeSec = activity.elapsedTime,
            elevationGainM = activity.totalElevationGain,
            avgHeartRate = activity.averageHeartRate,
            summaryPolyline = activity.map?.summaryPolyline,
            sportType = sportType,
        )
    }

    /** Maps the JSON body of `GET /athlete/activities` to a list of runs. */
    fun toRuns(body: String): List<Run> {
        return parseActivities(body).mapNotNull { toRun(it) }
    }
}
