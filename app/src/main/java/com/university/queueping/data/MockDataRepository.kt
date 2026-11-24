package com.university.queueping.data

import com.university.queueping.model.EntryStatus
import com.university.queueping.model.QueueEntryModel
import com.university.queueping.model.QueueModel
import com.university.queueping.model.QueueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MOCK DATA REPOSITORY (Student 1 Workspace)
 * 
 * DESIGN PATTERN: Proxy / Secret Switch
 * This file allows Student 1 to keep using 'MockDataRepository' in her UI code.
 * Student 2 can toggle 'USE_REAL_BACKEND' to true to silently switch the entire 
 * app to Firebase without changing a single line of UI code.
 */
object MockDataRepository : QueueRepository {

    // --- THE SECRET SWITCH ---
    private const val USE_REAL_BACKEND = true 
    private val realBackend by lazy { FirebaseRepository() }

    // --- MOCK DATA (Fallback) ---
    private val _queues = MutableStateFlow<List<QueueModel>>(
        listOf(
            QueueModel(queueId = "q_health_01", name = "General Consultation", businessName = "City Central Medical", description = "General checkup", currentNumber = "MED-084", status = QueueStatus.ACTIVE),
            QueueModel(queueId = "q_admin_02", name = "Student Finance Desk", businessName = "UoM Administration", description = "Tuition fees", currentNumber = "ADM-012", status = QueueStatus.ACTIVE)
        )
    )
    
    private val _queueEntries = MutableStateFlow<List<QueueEntryModel>>(emptyList())
    private val _activeCustomerEntry = MutableStateFlow<QueueEntryModel?>(null)

    // --- PROXIED PROPERTIES ---
    override val queues: StateFlow<List<QueueModel>> 
        get() = if (USE_REAL_BACKEND) realBackend.queues else _queues.asStateFlow()

    override val myQueues: StateFlow<List<QueueModel>> 
        get() = if (USE_REAL_BACKEND) realBackend.myQueues else _queues.asStateFlow()

    override val queueEntries: StateFlow<List<QueueEntryModel>> 
        get() = if (USE_REAL_BACKEND) realBackend.queueEntries else _queueEntries.asStateFlow()

    override val activeCustomerEntry: StateFlow<QueueEntryModel?> 
        get() = if (USE_REAL_BACKEND) realBackend.activeCustomerEntry else _activeCustomerEntry.asStateFlow()

    // --- PROXIED FUNCTIONS ---
    override fun getQueueById(queueId: String): QueueModel? = 
        if (USE_REAL_BACKEND) realBackend.getQueueById(queueId) else _queues.value.find { it.queueId == queueId }

    override fun getEntriesForQueue(queueId: String): List<QueueEntryModel> = 
        if (USE_REAL_BACKEND) realBackend.getEntriesForQueue(queueId) else _queueEntries.value.filter { it.queueId == queueId }

    override fun getWaitingCount(queueId: String): Int = 
        if (USE_REAL_BACKEND) realBackend.getWaitingCount(queueId) else _queueEntries.value.count { it.queueId == queueId && it.status == EntryStatus.WAITING }

    override fun getPeopleAhead(queueId: String, customerQueueNumber: String): Int = 
        if (USE_REAL_BACKEND) realBackend.getPeopleAhead(queueId, customerQueueNumber) else {
            val queue = getQueueById(queueId)
            val servingNumber = queue?.currentNumber ?: ""
            if (servingNumber.isNotEmpty() && customerQueueNumber < servingNumber) -1 else 0
        }

    override fun joinQueue(queueId: String): QueueEntryModel = 
        if (USE_REAL_BACKEND) realBackend.joinQueue(queueId) else {
            val newEntry = QueueEntryModel("m_01", queueId, "user", "Guest", "M-101", EntryStatus.WAITING)
            _activeCustomerEntry.value = newEntry
            newEntry
        }

    override fun leaveQueue() = if (USE_REAL_BACKEND) realBackend.leaveQueue() else { _activeCustomerEntry.value = null }

    override fun createQueue(name: String, businessName: String, description: String): QueueModel = 
        if (USE_REAL_BACKEND) realBackend.createQueue(name, businessName, description) else {
            val q = QueueModel(queueId = "m_new", name = name, businessName = businessName, description = description, currentNumber = "M-001", status = QueueStatus.ACTIVE)
            _queues.value = _queues.value + q
            q
        }

    override fun callNextCustomer(queueId: String): QueueEntryModel? = 
        if (USE_REAL_BACKEND) realBackend.callNextCustomer(queueId) else null

    override fun markServed(entryId: String) = if (USE_REAL_BACKEND) realBackend.markServed(entryId) else Unit
    override fun skipCustomer(entryId: String) = if (USE_REAL_BACKEND) realBackend.skipCustomer(entryId) else Unit
    override fun toggleQueueStatus(queueId: String) = if (USE_REAL_BACKEND) realBackend.toggleQueueStatus(queueId) else Unit
    override fun deleteQueue(queueId: String) = if (USE_REAL_BACKEND) realBackend.deleteQueue(queueId) else Unit

    override fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (USE_REAL_BACKEND) {
            realBackend.signIn(email, password, onSuccess, onError)
        } else {
            onSuccess() // Mock Success
        }
    }

    override fun signUp(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (USE_REAL_BACKEND) {
            realBackend.signUp(email, password, onSuccess, onError)
        } else {
            onSuccess() // Mock Success
        }
    }

    override fun signOut() = if (USE_REAL_BACKEND) realBackend.signOut() else Unit

    override fun simulateCallMe() = if (USE_REAL_BACKEND) realBackend.simulateCallMe() else Unit
}
