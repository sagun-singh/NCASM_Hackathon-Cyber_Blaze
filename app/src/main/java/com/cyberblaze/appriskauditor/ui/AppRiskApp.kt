package com.cyberblaze.appriskauditor.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.FakeData
import com.cyberblaze.appriskauditor.data.RiskResult

@Composable
fun AppRiskApp() {
    val apps = FakeData.sampleApps
    val results = FakeData.sampleResults
    val sorted = apps.sortedByDescending { results[it]?.score ?: 0 }

    var selected by remember { mutableStateOf<AppInfo?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        if (selected == null) {
            AppListScreen(sorted, results) { selected = it }
        } else {
            DetailScreen(selected!!, results[selected!!]!!) { selected = null }
        }
    }
}

// ------------------------------------------------------------
// BAND COLOR
// ------------------------------------------------------------
fun bandColor(band: String): Color = when (band.uppercase()) {
    "CRITICAL" -> Color(0xFFD32F2F)
    "HIGH" -> Color(0xFFF57C00)
    "MEDIUM" -> Color(0xFFFBC02D)
    else -> Color(0xFF388E3C)
}

// ------------------------------------------------------------
// APP LIST SCREEN
// ------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    apps: List<AppInfo>,
    results: Map<AppInfo, RiskResult>,
    onSelect: (AppInfo) -> Unit
) {
    val avg = if (results.isEmpty()) 0.0 else results.values.map { it.score }.take(5).average()
    val safety = (100 - avg).toInt().coerceIn(0, 100)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("App Risk Auditor", color = Color.White) },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1A1A))
        )

        // Safety grade card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Phone Safety Grade", fontSize = 14.sp, color = Color.Gray)
                Text(
                    "$safety% SAFE",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (safety >= 70) Color(0xFF388E3C) else Color(0xFFD32F2F)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "${results.values.count { it.band == "CRITICAL" }} Critical · " +
                    "${results.values.count { it.band == "HIGH" }} High · " +
                    "${apps.size} total apps",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
            }
        }

        Text(
            "Scariest first",
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
            fontSize = 13.sp,
            color = Color.Gray
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                val result = results[app] ?: return@items
                AppRow(app, result) { onSelect(app) }
            }
        }
    }
}

@Composable
fun AppRow(app: AppInfo, result: RiskResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bandColor(result.band).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.label.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = bandColor(result.band)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(app.pkg, fontSize = 11.sp, color = Color.Gray)
                Text(result.persona, fontSize = 11.sp, color = bandColor(result.band))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${result.score}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = bandColor(result.band)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(bandColor(result.band))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(result.band, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ------------------------------------------------------------
// DETAIL SCREEN
// ------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(app: AppInfo, result: RiskResult, onBack: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(app.label, color = Color.White) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = bandColor(result.band))
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Spy Card
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("SPY CARD", fontSize = 11.sp, color = Color.Gray)
                        Text(result.persona, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Score ${result.score} / 100 · ${result.band}",
                            color = bandColor(result.band),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Why flagged
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, null, tint = bandColor(result.band))
                            Spacer(Modifier.width(8.dp))
                            Text("Why flagged", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        result.reasons.forEach { r ->
                            Text("• $r", fontSize = 14.sp, modifier = Modifier.padding(vertical = 3.dp))
                        }
                    }
                }
            }

            // Rooms
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Rooms", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        result.rooms.forEach { (room, perms) ->
                            Text("• $room", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            perms.forEach { p ->
                                Text("    - $p", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Buttons
            item {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", app.pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open settings: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("Open Settings", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.fromParts("package", app.pkg, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot uninstall: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Uninstall", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}