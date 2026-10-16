package com.cyberblaze.appriskauditor.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.FakeData
import com.cyberblaze.appriskauditor.ui.screens.AppListScreen
import com.cyberblaze.appriskauditor.ui.screens.DetailScreen
import com.cyberblaze.appriskauditor.ui.screens.SummaryScreen

// Bottom tab definition
data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun AppRiskApp() {
    val apps = FakeData.sampleApps
    val results = FakeData.sampleResults
    val sorted = apps.sortedByDescending { results[it]?.score ?: 0 }

    // Navigation state
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        TabItem("Apps", Icons.Default.List),
        TabItem("Summary", Icons.Default.Home)
    )

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF5F5F5)) {
        // If an app is selected, show DetailScreen (covers everything)
        if (selectedApp != null) {
            DetailScreen(selectedApp!!, results[selectedApp!!]!!) { selectedApp = null }
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        tabs.forEachIndexed { index, tab ->
                            NavigationBarItem(
                                selected = tabIndex == index,
                                onClick = { tabIndex = index },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFF1976D2),
                                    selectedTextColor = Color(0xFF1976D2),
                                    indicatorColor = Color(0xFF1976D2).copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.padding(paddingValues)
                ) {
                    when (tabIndex) {
                        0 -> AppListScreen(sorted, results) { selectedApp = it }
                        1 -> SummaryScreen(sorted, results) { selectedApp = it }
                    }
                }
            }
        }
    }
}