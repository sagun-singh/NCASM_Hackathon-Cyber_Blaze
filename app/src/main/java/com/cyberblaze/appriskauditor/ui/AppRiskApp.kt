package com.cyberblaze.appriskauditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.FakeData
import com.cyberblaze.appriskauditor.ui.screens.AppListScreen
import com.cyberblaze.appriskauditor.ui.screens.DetailScreen

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