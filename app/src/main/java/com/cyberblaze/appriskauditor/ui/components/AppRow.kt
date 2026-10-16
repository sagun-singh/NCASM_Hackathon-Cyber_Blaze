package com.cyberblaze.appriskauditor.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
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
import com.cyberblaze.appriskauditor.ui.utils.loadAppIcon

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
            // Icon container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bandColor(result.band).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = loadAppIcon(app.pkg)
                if (icon != null) {
                    Image(
                        bitmap = icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(
                        app.label.take(2).uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = bandColor(result.band)
                    )
                }
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
                    Text(
                        result.band,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}