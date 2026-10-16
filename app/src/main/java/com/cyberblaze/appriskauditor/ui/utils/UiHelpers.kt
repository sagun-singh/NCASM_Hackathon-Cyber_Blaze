package com.cyberblaze.appriskauditor.ui.utils

import android.graphics.drawable.Drawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap

// ------------------------------------------------------------
// BAND COLOR (LOW / MEDIUM / HIGH / CRITICAL)
// ------------------------------------------------------------
fun bandColor(band: String): Color = when (band.uppercase()) {
    "CRITICAL" -> Color(0xFFD32F2F)
    "HIGH" -> Color(0xFFF57C00)
    "MEDIUM" -> Color(0xFFFBC02D)
    else -> Color(0xFF388E3C)
}

// ------------------------------------------------------------
// LOAD REAL APP ICON FROM PACKAGE NAME
// ------------------------------------------------------------
@Composable
fun loadAppIcon(pkg: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(pkg) {
        try {
            val drawable: Drawable = context.packageManager.getApplicationIcon(pkg)
            drawable.toBitmap(width = 96, height = 96).asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}