package com.university.queueping.ui.screens.customer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.university.queueping.data.MockDataRepository
import com.university.queueping.model.EntryStatus
import com.university.queueping.ui.components.EntryStatusBadge
import com.university.queueping.ui.components.QueuePingTopAppBar
import com.university.queueping.ui.theme.AccentAmber
import com.university.queueping.ui.theme.AccentAmberLight
import com.university.queueping.ui.theme.PrimaryBlue
import com.university.queueping.ui.theme.PrimaryBlueDark
import com.university.queueping.ui.theme.PrimaryBlueLight
import com.university.queueping.ui.theme.SecondaryTeal
import com.university.queueping.ui.theme.StatusRed
import com.university.queueping.ui.theme.StatusRed
import com.university.queueping.ui.theme.StatusRedLight
import com.university.queueping.ui.theme.SurfaceLight
import com.university.queueping.ui.theme.TextPrimary
import com.university.queueping.ui.theme.TextSecondary

@Composable
fun QueueStatusScreen(
    queueId: String,
    onNavigateToCalled: (String) -> Unit,
    onNavigateHome: () -> Unit
) {
    val queues by MockDataRepository.queues.collectAsState()
    val queue = queues.find { it.queueId == queueId } ?: MockDataRepository.getQueueById(queueId)
    val activeEntry by MockDataRepository.activeCustomerEntry.collectAsState()
    val entries by MockDataRepository.queueEntries.collectAsState()

    val currentQueueEntries = entries.filter { it.queueId == queueId }
    val calledEntry = currentQueueEntries.find { it.status == EntryStatus.CALLED }
    val displayServing = calledEntry?.queueNumber ?: "---"

    var showLeaveDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(activeEntry?.status) {
        if (activeEntry?.status == EntryStatus.CALLED) {
            onNavigateToCalled(queueId)
        }
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Queue?") },
            text = { Text("Are you sure you want to cancel your digital queue ticket? You will lose your current spot in line.") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveDialog = false
                        MockDataRepository.leaveQueue()
                        onNavigateHome()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Yes, Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Stay in Queue")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            QueuePingTopAppBar(
                title = "Live Queue Status",
                canNavigateBack = true,
                onNavigateBack = onNavigateHome,
                actions = {
                    IconButton(onClick = { /* Simple UI Feedback */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = SurfaceLight
                        )
                    }
                    IconButton(onClick = { showLeaveDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Leave Queue",
                            tint = SurfaceLight
                        )
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (queue == null || activeEntry == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No active queue ticket found",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onNavigateHome) {
                    Text("Return to Home")
                }
            }
            return@Scaffold
        }

        val entry = activeEntry!!
        val peopleAhead = MockDataRepository.getPeopleAhead(queueId, entry.queueNumber)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = queue.businessName,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary
                )
                Text(
                    text = queue.name,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                EntryStatusBadge(status = entry.status)
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "YOUR TICKET NUMBER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PrimaryBlue
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = entry.queueNumber,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 56.sp,
                            lineHeight = 60.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = PrimaryBlueDark
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val isTurnPassed = peopleAhead == -1
                    val statusText = when {
                        isTurnPassed -> "Your turn has passed! Please speak to the service staff."
                        peopleAhead == 0 -> "You are next in line! Stay close."
                        peopleAhead <= 2 -> "Almost your turn! Please be nearby."
                        else -> "You're in line. You can move freely around."
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isTurnPassed) StatusRedLight 
                                else if (peopleAhead <= 2) AccentAmberLight 
                                else PrimaryBlueLight
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = if (isTurnPassed) StatusRed else if (peopleAhead <= 2) AccentAmber else PrimaryBlueDark,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Now Serving",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = displayServing,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = SecondaryTeal
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceLight),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "People Ahead",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$peopleAhead",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (peopleAhead <= 2) AccentAmber else PrimaryBlue
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceLight)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Notifications are active. You will be alerted when your number is called.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlueLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Prototype Test Control",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PrimaryBlueDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            MockDataRepository.simulateCallMe()
                            onNavigateToCalled(queueId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Simulate Staff Calling My Ticket (${entry.queueNumber})")
                    }
                }
            }

            OutlinedButton(
                onClick = { showLeaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
            ) {
                Text("Leave Queue")
            }
        }
    }
}
