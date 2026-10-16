package com.cyberblaze.appriskauditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cyberblaze.appriskauditor.data.AppInfo
import com.cyberblaze.appriskauditor.data.RiskResult
import com.cyberblaze.appriskauditor.ui.utils.bandColor

@Composable
fun QuickWinCard(
    app: AppInfo,
    result: RiskResult,
    onFix: () -> Unit
) {
    val color = bandColor(result.band)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.label.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    "${result.score} · ${result.band}",
                    fontSize = 12.sp,
                    color = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = color),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Fix", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}