package com.university.queueping.data

import com.university.queueping.model.QueueEntryModel
import com.university.queueping.model.QueueModel
import kotlinx.coroutines.flow.StateFlow

/**
 * Common interface for data operations.
 */
interface QueueRepository {
    val queues: StateFlow<List<QueueModel>>
    val myQueues: StateFlow<List<QueueModel>>
    val queueEntries: StateFlow<List<QueueEntryModel>>
    val activeCustomerEntry: StateFlow<QueueEntryModel?>

    fun getQueueById(queueId: String): QueueModel?
    fun getEntriesForQueue(queueId: String): List<QueueEntryModel>
    fun getWaitingCount(queueId: String): Int
    fun getPeopleAhead(queueId: String, customerQueueNumber: String): Int
    
    // Customer Actions
    fun joinQueue(queueId: String): QueueEntryModel
    fun leaveQueue()
    
    // Staff Actions
    fun createQueue(name: String, businessName: String, description: String): QueueModel
    fun callNextCustomer(queueId: String): QueueEntryModel?
    fun markServed(entryId: String)
    fun skipCustomer(entryId: String)
    fun toggleQueueStatus(queueId: String)
    fun deleteQueue(queueId: String)

    // Staff Authentication
    fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun signUp(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit)
    fun signOut()
    
    // Simulation / Testing
    fun simulateCallMe()
}
