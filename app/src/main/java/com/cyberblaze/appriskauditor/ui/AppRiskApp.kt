package com.cyberblaze.appriskauditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.FakeData
import com.cyberblaze.appriskauditor.scoring.Scorer
import com.cyberblaze.appriskauditor.ui.screens.AppListScreen
import com.cyberblaze.appriskauditor.ui.screens.DetailScreen

@Composable
fun AppRiskApp() {
    // Use FakeData until Person 1's real scanner is wired in.
    // When Person 1 is ready, replace FakeData.sampleApps with scanApps(ctx, includeSystem=false).
    val apps: List<AppInfo> = FakeData.sampleApps

    // Score every app with our real engine (Person 2).
    // FakeData.sampleResults is now ignored — scores come from Scorer.scoreApp().
    val results = remember(apps) {
        apps.associateWith { Scorer.scoreApp(it) }
    }

    // Sort scariest first
    val sorted = remember(results) {
        apps.sortedByDescending { results[it]?.score ?: 0 }
    }

    var selected by remember { mutableStateOf<AppInfo?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        if (selected == null) {
            AppListScreen(sorted, results) { selected = it }
        } else {
            val result = results[selected!!]
            if (result != null) {
                DetailScreen(selected!!, result) { selected = null }
            }
        }
    }
}
