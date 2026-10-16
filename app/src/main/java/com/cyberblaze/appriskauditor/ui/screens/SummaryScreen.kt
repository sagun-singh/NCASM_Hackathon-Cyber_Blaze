package com.cyberblaze.appriskauditor.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.RiskResult
import com.cyberblaze.appriskauditor.ui.components.BandCountCard
import com.cyberblaze.appriskauditor.ui.components.QuickWinCard
import com.cyberblaze.appriskauditor.ui.components.RoomBar
import com.cyberblaze.appriskauditor.ui.utils.bandColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    apps: List<AppInfo>,
    results: Map<AppInfo, RiskResult>,
    onOpenApp: (AppInfo) -> Unit
) {
    val context = LocalContext.current

    // --- Compute aggregates ---
    val scores = results.values.map { it.score }
    val avg = if (scores.isEmpty()) 0.0 else scores.take(5).average()
    val safety = (100 - avg).toInt().coerceIn(0, 100)

    val bandOrder = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW")
    val bandCounts = bandOrder.associateWith { band ->
        results.values.count { it.band.uppercase() == band }
    }

    // Aggregate rooms across all apps
    val roomCounts = mutableMapOf<String, Int>()
    results.values.forEach { r ->
        r.rooms.forEach { (room, _) ->
            roomCounts[room] = (roomCounts[room] ?: 0) + 1
        }
    }
    val sortedRooms = roomCounts.entries.sortedByDescending { it.value }
    val maxRoomCount = sortedRooms.maxOfOrNull { it.value } ?: 1

    // Top 3 quick wins (highest score)
    val quickWins = apps
        .sortedByDescending { results[it]?.score ?: 0 }
        .take(3)
        .mapNotNull { app -> results[app]?.let { app to it } }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Device Summary", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Grade Card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Phone Safety Grade", fontSize = 13.sp, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$safety%",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (safety >= 70) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                        Text(
                            if (safety >= 70) "You're doing great" else "Time to clean up",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // --- Band counts ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Apps by risk level",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        bandOrder.forEach { band ->
                            BandCountCard(band = band, count = bandCounts[band] ?: 0)
                        }
                    }
                }
            }

            // --- Rooms breakdown ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Which rooms are being accessed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        if (sortedRooms.isEmpty()) {
                            Text("No sensitive access detected", color = Color.Gray, fontSize = 13.sp)
                        } else {
                            sortedRooms.forEach { (room, count) ->
                                RoomBar(
                                    roomName = room,
                                    count = count,
                                    maxCount = maxRoomCount,
                                    barColor = Color(0xFF1976D2)
                                )
                            }
                        }
                    }
                }
            }

            // --- Quick Wins ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "3 quick wins",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            "Tap Fix to open app settings",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        quickWins.forEach { (app, result) ->
                            QuickWinCard(
                                app = app,
                                result = result,
                                onFix = {
                                    onOpenApp(app)
                                }
                            )
                        }
                    }
                }
            }

            // --- Privacy footer ---
            item {
                Text(
                    "All scanning runs on-device. No data leaves your phone.",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}