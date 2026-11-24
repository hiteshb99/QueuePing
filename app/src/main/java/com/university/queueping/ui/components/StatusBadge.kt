package com.university.queueping.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.queueping.model.EntryStatus
import com.university.queueping.model.QueueStatus
import com.university.queueping.ui.theme.AccentAmber
import com.university.queueping.ui.theme.AccentAmberLight
import com.university.queueping.ui.theme.PrimaryBlue
import com.university.queueping.ui.theme.PrimaryBlueLight
import com.university.queueping.ui.theme.StatusGreen
import com.university.queueping.ui.theme.StatusGreenLight
import com.university.queueping.ui.theme.StatusRed
import com.university.queueping.ui.theme.StatusRedLight

@Composable
fun QueueStatusBadge(status: QueueStatus) {
    val (bgColor, textColor, label) = when (status) {
        QueueStatus.ACTIVE -> Triple(StatusGreenLight, StatusGreen, "Active")
        QueueStatus.PAUSED -> Triple(AccentAmberLight, AccentAmber, "Paused")
        QueueStatus.CLOSED -> Triple(StatusRedLight, StatusRed, "Closed")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun EntryStatusBadge(status: EntryStatus) {
    val (bgColor, textColor, label) = when (status) {
        EntryStatus.WAITING -> Triple(PrimaryBlueLight, PrimaryBlue, "Waiting")
        EntryStatus.CALLED -> Triple(StatusGreenLight, StatusGreen, "Your Turn!")
        EntryStatus.SERVED -> Triple(StatusGreenLight, StatusGreen, "Served")
        EntryStatus.SKIPPED -> Triple(AccentAmberLight, AccentAmber, "Skipped")
        EntryStatus.CANCELLED -> Triple(StatusRedLight, StatusRed, "Cancelled")
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
