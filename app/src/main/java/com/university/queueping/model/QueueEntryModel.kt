package com.university.queueping.model

enum class EntryStatus {
    WAITING,
    CALLED,
    SERVED,
    SKIPPED,
    CANCELLED
}

data class QueueEntryModel(
    val entryId: String = "",
    val queueId: String = "",
    val customerId: String = "",
    val customerName: String = "Customer",
    val queueNumber: String = "",
    val status: EntryStatus = EntryStatus.WAITING,
    val joinedAt: Long = System.currentTimeMillis()
)
