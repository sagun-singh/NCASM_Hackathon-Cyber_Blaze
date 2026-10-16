package com.cyberblaze.appriskauditor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.cyberblaze.appriskauditor.ui.AppRiskApp
import com.cyberblaze.appriskauditor.ui.theme.AppRiskAuditorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRiskAuditorTheme {
                AppRiskApp()
            }
        }
    }
}
