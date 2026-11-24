package com.university.queueping.model

enum class QueueStatus {
    ACTIVE,
    PAUSED,
    CLOSED
}

data class QueueModel(
    val queueId: String = "",
    val creatorId: String = "",
    val name: String = "",
    val businessName: String = "",
    val description: String = "",
    val currentNumber: String = "A00",
    val status: QueueStatus = QueueStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val prefix: String = "",
    val lastCounter: Long = 0,
    val lastResetDate: String = ""
)
