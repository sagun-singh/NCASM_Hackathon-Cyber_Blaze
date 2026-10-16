package com.cyberblaze.appriskauditor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.RiskResult
import com.cyberblaze.appriskauditor.ui.components.AppRow

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