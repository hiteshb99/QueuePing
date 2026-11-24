package com.university.queueping.ui.screens.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.university.queueping.data.MockDataRepository
import com.university.queueping.model.EntryStatus
import com.university.queueping.model.QueueStatus
import com.university.queueping.ui.components.EntryStatusBadge
import com.university.queueping.ui.components.QueuePingTopAppBar
import com.university.queueping.ui.components.QueueStatusBadge
import com.university.queueping.ui.theme.AccentAmber
import com.university.queueping.ui.theme.AccentAmberLight
import com.university.queueping.ui.theme.DividerColor
import com.university.queueping.ui.theme.PrimaryBlue
import com.university.queueping.ui.theme.PrimaryBlueLight
import com.university.queueping.ui.theme.SecondaryTeal
import com.university.queueping.ui.theme.SecondaryTealDark
import com.university.queueping.ui.theme.StatusGreen
import com.university.queueping.ui.theme.StatusGreenLight
import com.university.queueping.ui.theme.SurfaceLight
import com.university.queueping.ui.theme.TextPrimary
import com.university.queueping.ui.theme.TextSecondary

@Composable
fun QueueDashboardScreen(
    queueId: String,
    onNavigateToQR: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val queues by MockDataRepository.queues.collectAsState()
    val queue = queues.find { it.queueId == queueId } ?: MockDataRepository.getQueueById(queueId)
    val entries by MockDataRepository.queueEntries.collectAsState()
    
    // Debug Logging
    android.util.Log.d("QueueDashboard", "Dashboard filtering for ID: $queueId")
    android.util.Log.d("QueueDashboard", "Total Entries in App: ${entries.size}")
    if (entries.isNotEmpty()) {
        android.util.Log.d("QueueDashboard", "First Entry QueueID: '${entries.first().queueId}'")
    }

    val currentQueueEntries = entries.filter { it.queueId.trim() == queueId.trim() }
    val waitingEntries = currentQueueEntries.filter { it.status == EntryStatus.WAITING }
    val calledEntry = currentQueueEntries.find { it.status == EntryStatus.CALLED }

    var showDeleteConfirm by remember { androidx.compose.runtime.mutableStateOf(false) }

    if (showDeleteConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Queue?") },
            text = { Text("This will permanently remove this queue and all its current entries. This action cannot be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        MockDataRepository.deleteQueue(queueId)
                        onNavigateBack()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            QueuePingTopAppBar(
                title = queue?.name ?: "Queue Dashboard",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = { onNavigateToQR(queueId) }) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Show QR",
                            tint = SurfaceLight
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (queue == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Queue not found", color = TextSecondary)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = queue.businessName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Counter Dashboard (ID: $queueId)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QueueStatusBadge(status = queue.status)

                        IconButton(
                            onClick = { MockDataRepository.toggleQueueStatus(queueId) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (queue.status == QueueStatus.ACTIVE) AccentAmberLight else StatusGreenLight)
                        ) {
                            Icon(
                                imageVector = if (queue.status == QueueStatus.ACTIVE) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Toggle Status",
                                tint = if (queue.status == QueueStatus.ACTIVE) AccentAmber else StatusGreen
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NOW SERVING",
                            style = MaterialTheme.typography.labelMedium.copy(
                                letterSpacing = 2.sp, 
                                fontWeight = FontWeight.Bold,
                                color = SecondaryTeal
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = calledEntry?.queueNumber ?: "---",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 64.sp,
                                lineHeight = 70.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = if (calledEntry != null) SecondaryTealDark else TextSecondary.copy(alpha = 0.3f)
                        )

                        Text(
                            text = calledEntry?.customerName ?: "Waiting for next customer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (calledEntry != null) TextPrimary else TextSecondary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { MockDataRepository.callNextCustomer(queueId) },
                            enabled = waitingEntries.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryTeal,
                                disabledContainerColor = DividerColor
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (waitingEntries.isNotEmpty()) "CALL NEXT (${waitingEntries.first().queueNumber})" else "NO ONE WAITING",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (calledEntry != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { MockDataRepository.markServed(calledEntry.entryId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusGreen)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mark Served", color = StatusGreen, fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { MockDataRepository.skipCustomer(calledEntry.entryId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp), tint = AccentAmber)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Skip", color = AccentAmber, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Waiting List (${waitingEntries.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
            }

            if (waitingEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "All caught up! No customers in queue.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(waitingEntries) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryBlueLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = entry.queueNumber,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = PrimaryBlue
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = entry.customerName,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Joined recently",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                            }

                            EntryStatusBadge(status = entry.status)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete This Queue", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
