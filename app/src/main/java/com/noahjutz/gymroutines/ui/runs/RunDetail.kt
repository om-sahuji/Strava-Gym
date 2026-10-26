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

package com.noahjutz.gymroutines.ui.runs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Run
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.util.GeoPoint
import com.noahjutz.gymroutines.util.decodePolyline
import com.noahjutz.gymroutines.util.formatDistanceKm
import com.noahjutz.gymroutines.util.formatDuration
import com.noahjutz.gymroutines.util.formatElevation
import com.noahjutz.gymroutines.util.formatHeartRate
import com.noahjutz.gymroutines.util.formatPace
import com.noahjutz.gymroutines.util.formatRunDate
import org.koin.androidx.compose.getViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RunDetail(
    stravaId: Long,
    popBackStack: () -> Unit,
    viewModel: RunDetailViewModel = getViewModel { parametersOf(stravaId) },
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_run_detail),
                navigationIcon = {
                    IconButton(onClick = popBackStack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_pop_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        val run by viewModel.run.collectAsState()

        run?.let { detail ->
            val points = remember(detail.stravaId, detail.summaryPolyline) {
                decodePolyline(detail.summaryPolyline)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues,
            ) {
                item { RunHeader(detail) }
                item { RunStatsCard(detail) }
                if (points.size >= 2) {
                    item { RunRouteCard(points) }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun RunHeader(run: Run) {
    Column(Modifier.padding(24.dp)) {
        Text(
            text = run.name.takeIf { it.isNotBlank() } ?: run.sportType,
            style = typography.h4,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatRunDate(run.startTimeEpochSec),
            style = typography.body2,
            color = colors.onSurface.copy(alpha = 0.7f),
        )
        Text(
            text = run.sportType,
            style = typography.caption,
            color = colors.onSurface.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun RunStatsCard(run: Run) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 1.dp,
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            StatRow(stringResource(R.string.run_distance), formatDistanceKm(run.distanceMeters))
            StatRow(
                stringResource(R.string.run_moving_time),
                formatDuration(run.movingTimeSec)
            )
            StatRow(
                stringResource(R.string.run_elapsed_time),
                formatDuration(run.elapsedTimeSec)
            )
            StatRow(
                stringResource(R.string.run_pace),
                formatPace(run.distanceMeters, run.movingTimeSec)
            )
            StatRow(stringResource(R.string.run_elevation), formatElevation(run.elevationGainM))
            StatRow(
                stringResource(R.string.run_avg_heart_rate),
                formatHeartRate(run.avgHeartRate)
            )
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = typography.body2,
            color = colors.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = typography.subtitle1)
    }
}

/**
 * Draws the decoded route with Compose Canvas only — no Google Maps SDK and no API key.
 */
@Composable
private fun RunRouteCard(points: List<GeoPoint>) {
    val pathColor = colors.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.run_route), style = typography.subtitle1)
            Spacer(Modifier.height(8.dp))
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                val padding = 12.dp.toPx()
                val availableWidth = size.width - 2 * padding
                val availableHeight = size.height - 2 * padding
                if (availableWidth <= 0f || availableHeight <= 0f) return@Canvas

                val minLat = points.minOf { it.latitude }
                val maxLat = points.maxOf { it.latitude }
                val minLon = points.minOf { it.longitude }
                val maxLon = points.maxOf { it.longitude }
                val spanLat = (maxLat - minLat).coerceAtLeast(1e-6).toFloat()
                val spanLon = (maxLon - minLon).coerceAtLeast(1e-6).toFloat()
                val scale = minOf(availableWidth / spanLon, availableHeight / spanLat)
                val offsetX = padding + (availableWidth - spanLon * scale) / 2f
                val offsetY = padding + (availableHeight - spanLat * scale) / 2f

                fun pointOffset(point: GeoPoint) = Offset(
                    x = offsetX + ((point.longitude - minLon).toFloat() * scale),
                    // Latitude grows northwards, the canvas grows downwards.
                    y = offsetY + availableHeight -
                        ((point.latitude - minLat).toFloat() * scale),
                )

                val path = Path()
                val first = pointOffset(points.first())
                path.moveTo(first.x, first.y)
                for (point in points.drop(1)) {
                    val offset = pointOffset(point)
                    path.lineTo(offset.x, offset.y)
                }
                drawPath(path, color = pathColor, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}
