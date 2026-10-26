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

/** One point of a decoded route. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/**
 * Decodes a Google encoded polyline (the format Strava uses for `map.summary_polyline`) into a list
 * of points. Pure Kotlin so it can be unit tested; no Google Maps SDK is involved.
 */
fun decodePolyline(encoded: String?): List<GeoPoint> {
    if (encoded.isNullOrEmpty()) return emptyList()

    val points = mutableListOf<GeoPoint>()
    var index = 0
    var latitude = 0
    var longitude = 0

    while (index < encoded.length) {
        var shift = 0
        var result = 0
        var byte: Int
        do {
            if (index >= encoded.length) return points
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        val deltaLatitude = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        latitude += deltaLatitude

        shift = 0
        result = 0
        do {
            if (index >= encoded.length) return points
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        val deltaLongitude = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        longitude += deltaLongitude

        points.add(
            GeoPoint(
                latitude = latitude / 1e5,
                longitude = longitude / 1e5,
            )
        )
    }

    return points
}
