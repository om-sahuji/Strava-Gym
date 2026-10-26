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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.noahjutz.gymroutines.R
import com.noahjutz.gymroutines.data.domain.Run
import com.noahjutz.gymroutines.ui.components.TopBar
import com.noahjutz.gymroutines.util.WeeklyDistance
import com.noahjutz.gymroutines.util.formatDistanceKm
import com.noahjutz.gymroutines.util.formatDuration
import com.noahjutz.gymroutines.util.formatHeartRate
import com.noahjutz.gymroutines.util.formatPace
import com.noahjutz.gymroutines.util.formatRunDate
import com.noahjutz.gymroutines.util.formatWeekLabel
import com.noahjutz.gymroutines.util.weeklyDistancesKm
import org.koin.androidx.compose.getViewModel

@Composable
fun Runs(
    navToRunDetail: (Long) -> Unit,
    navToSettings: () -> Unit,
    viewModel: RunsViewModel = getViewModel(),
) {
    Scaffold(
        topBar = {
            TopBar(
                title = stringResource(R.string.screen_runs),
                actions = {
                    val isSyncing by viewModel.isSyncing.collectAsState()
                    IconButton(
                        onClick = { viewModel.sync() },
                        enabled = !isSyncing,
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, stringResource(R.string.btn_refresh))
                        }
                    }
                    Box {
                        var expanded by remember { mutableStateOf(false) }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.MoreVert, stringResource(R.string.btn_more))
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    expanded = false
                                    navToSettings()
                                }
                            ) {
                                Text(stringResource(R.string.screen_settings))
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        val runs by viewModel.runs.collectAsState(initial = emptyList())
        val isConnected by viewModel.isConnected.collectAsState()
        val lastSync by viewModel.lastSyncEpochSec.collectAsState(initial = 0L)
        val message by viewModel.message.collectAsState()

        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
        ) {
            val allRuns = runs
            if (allRuns.isNotEmpty()) {
                item {
                    WeeklyDistanceChart(
                        weeks = weeklyDistancesKm(
                            runs = allRuns,
                            nowEpochSec = System.currentTimeMillis() / 1000
                        )
                    )
                }
            }

            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = if (lastSync == 0L) {
                            stringResource(R.string.runs_never_synced)
                        } else {
                            stringResource(R.string.runs_last_sync, formatRunDate(lastSync))
                        },
                        style = typography.caption,
                        color = colors.onSurface.copy(alpha = 0.6f),
                    )
                    message?.let { syncMessage ->
                        Text(
                            text = syncMessage.count?.let {
                                stringResource(syncMessage.resId, it)
                            } ?: stringResource(syncMessage.resId),
                            style = typography.body2,
                            color = colors.onSurface.copy(alpha = 0.8f),
                            modifier = Modifier.clickable { viewModel.dismissMessage() },
                        )
                    }
                }
            }

            if (allRuns.isEmpty()) {
                item {
                    EmptyRunsCard(
                        isConnected = isConnected,
                        navToSettings = navToSettings,
                    )
                }
            } else {
                item {
                    Text(
                        stringResource(R.string.screen_run_history),
                        Modifier.padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 8.dp
                        ),
                        style = typography.h6,
                    )
                }
                items(allRuns, key = { it.stravaId }) { run ->
                    RunListItem(run = run, onClick = { navToRunDetail(run.stravaId) })
                }
            }

            item {
                // Keep the last card clear of the bottom navigation bar.
                Box(Modifier.height(16.dp)) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RunListItem(run: Run, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = 1.dp,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = run.name.takeIf { it.isNotBlank() } ?: run.sportType,
                    style = typography.subtitle1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatRunDate(run.startTimeEpochSec),
                    style = typography.caption,
                    color = colors.onSurface.copy(alpha = 0.6f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatDistanceKm(run.distanceMeters),
                    style = typography.subtitle1,
                )
                Text(
                    text = "${formatDuration(run.movingTimeSec)} · " +
                        formatPace(run.distanceMeters, run.movingTimeSec) +
                        if (run.avgHeartRate != null) {
                            " · ${formatHeartRate(run.avgHeartRate)}"
                        } else {
                            ""
                        },
                    style = typography.caption,
                    color = colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun EmptyRunsCard(isConnected: Boolean, navToSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = 1.dp,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.runs_empty_title),
                style = typography.h6,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    if (isConnected) R.string.runs_no_runs else R.string.runs_empty_connect
                ),
                style = typography.body2,
                color = colors.onSurface.copy(alpha = 0.7f),
            )
            if (!isConnected) {
                Spacer(Modifier.height(16.dp))
                Button(onClick = navToSettings) {
                    Icon(Icons.Default.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.strava_connect))
                }
            }
        }
    }
}

@Composable
private fun WeeklyDistanceChart(weeks: List<WeeklyDistance>) {
    val barColor = colors.primary
    val emptyBarColor = colors.onSurface.copy(alpha = 0.12f)
    val thisWeek = weeks.lastOrNull()?.kilometers ?: 0.0
    val lastFourWeeks = weeks.dropLast(1).sumOf { it.kilometers }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(30.dp),
        elevation = 2.dp,
    ) {
        Column {
            Surface(Modifier.fillMaxWidth(), color = colors.primary) {
                Text(
                    stringResource(R.string.runs_week_summary),
                    Modifier.padding(20.dp),
                    style = typography.h6,
                )
            }
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "${stringResource(R.string.runs_this_week)}: " +
                        formatDistanceKm(thisWeek * 1000),
                    style = typography.subtitle2,
                )
                Text(
                    text = "${stringResource(R.string.runs_last_weeks)}: " +
                        formatDistanceKm(lastFourWeeks * 1000),
                    style = typography.caption,
                    color = colors.onSurface.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(12.dp))
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                ) {
                    val maxKm = weeks.maxOfOrNull { it.kilometers }?.coerceAtLeast(1.0) ?: 1.0
                    val slotWidth = size.width / weeks.size
                    val barWidth = slotWidth * 0.5f
                    weeks.forEachIndexed { index, week ->
                        val barHeight =
                            (week.kilometers / maxKm).toFloat() * size.height
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = if (week.kilometers > 0.0) barColor else emptyBarColor,
                            topLeft = Offset(left, size.height - barHeight),
                            size = Size(barWidth, barHeight.coerceAtLeast(2f)),
                            cornerRadius = CornerRadius(barWidth / 4f, barWidth / 4f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    weeks.forEach { week ->
                        Text(
                            text = formatWeekLabel(week.weekStartEpochSec),
                            modifier = Modifier.weight(1f),
                            style = typography.caption,
                            textAlign = TextAlign.Center,
                            color = colors.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
