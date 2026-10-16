package com.cyberblaze.appriskauditor.ui.components

import androidx.compose.foundation.background
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
import com.cyberblaze.appriskauditor.ui.utils.bandColor

@Composable
fun BandCountCard(band: String, count: Int) {
    val color = bandColor(band)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                band,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF333333),
                modifier = Modifier.weight(1f)
            )
            Text(
                "$count apps",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = color
            )
        }
    }
}