package com.university.queueping.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.university.queueping.model.EntryStatus
import com.university.queueping.model.QueueEntryModel
import com.university.queueping.model.QueueModel
import com.university.queueping.model.QueueStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FirebaseRepository : QueueRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _queues = MutableStateFlow<List<QueueModel>>(emptyList())
    override val queues: StateFlow<List<QueueModel>> = _queues.asStateFlow()

    private val _myQueues = MutableStateFlow<List<QueueModel>>(emptyList())
    override val myQueues: StateFlow<List<QueueModel>> = _myQueues.asStateFlow()

    private val _queueEntries = MutableStateFlow<List<QueueEntryModel>>(emptyList())
    override val queueEntries: StateFlow<List<QueueEntryModel>> = _queueEntries.asStateFlow()

    private val _activeCustomerEntry = MutableStateFlow<QueueEntryModel?>(null)
    override val activeCustomerEntry: StateFlow<QueueEntryModel?> = _activeCustomerEntry.asStateFlow()

    private var queuesListener: ListenerRegistration? = null
    private var myQueuesListener: ListenerRegistration? = null
    private var entriesListener: ListenerRegistration? = null

    companion object {
        // Persist the anonymous session ID in memory so the user doesn't lose their ticket 
        // if they navigate away and come back during the same app session.
        private var anonymousSessionId: String? = null
    }

    private fun getAnonymousId(): String {
        if (anonymousSessionId == null) {
            anonymousSessionId = "anon_${System.currentTimeMillis().toString().takeLast(6)}"
        }
        return anonymousSessionId!!
    }

    init {
        startListeners()
    }

    private fun startListeners() {
        // Clean up old listeners if any
        queuesListener?.remove()
        myQueuesListener?.remove()
        entriesListener?.remove()

        val currentUserId = auth.currentUser?.uid

        // 1. Listen for ALL QUEUES (For Customer lookup)
        queuesListener = db.collection("queues").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("Firebase", "All Queues Listen Failed", e)
                return@addSnapshotListener
            }
            val qs = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val bName = doc.getString("businessName") ?: doc.get("businessName")?.toString() ?: ""
                    val qName = doc.getString("name") ?: doc.get("name")?.toString() ?: ""
                    val desc = doc.getString("description") ?: doc.get("description")?.toString() ?: ""
                    val curNum = doc.getString("currentNumber") ?: doc.get("currentNumber")?.toString() ?: "A00"
                    val cId = doc.getString("creatorId") ?: doc.get("creatorId")?.toString() ?: ""
                    val pref = doc.getString("prefix") ?: doc.get("prefix")?.toString() ?: ""
                    val resetDate = doc.getString("lastResetDate") ?: doc.get("lastResetDate")?.toString() ?: ""

                    val statusRaw = doc.get("status")
                    val statusStr = when (statusRaw) {
                        is String -> statusRaw
                        is Map<*, *> -> (statusRaw["name"] ?: statusRaw["status"])?.toString()
                        else -> statusRaw?.toString()
                    } ?: "ACTIVE"

                    QueueModel(
                        queueId = doc.id,
                        creatorId = cId.trim(),
                        name = qName.trim(),
                        businessName = bName.trim(),
                        description = desc.trim(),
                        currentNumber = curNum.trim(),
                        status = try { QueueStatus.valueOf(statusStr.trim().uppercase()) } catch(ex: Exception) { QueueStatus.ACTIVE },
                        createdAt = doc.getLong("createdAt") ?: doc.get("createdAt")?.toString()?.toLongOrNull() ?: 0L,
                        prefix = pref.trim(),
                        lastCounter = doc.getLong("lastCounter") ?: doc.get("lastCounter")?.toString()?.toLongOrNull() ?: 0L,
                        lastResetDate = resetDate.trim()
                    )
                } catch (ex: Exception) {
                    Log.e("Firebase", "Queue Mapping Failed: ${doc.id}", ex)
                    null
                }
            } ?: emptyList()
            _queues.value = qs
        }

        // 2. Listen for MY QUEUES (For Staff Isolation)
        if (currentUserId != null) {
            myQueuesListener = db.collection("queues")
                .whereEqualTo("creatorId", currentUserId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("Firebase", "My Queues Listen Failed", e)
                        return@addSnapshotListener
                    }
                    val qs = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            val bName = doc.getString("businessName") ?: doc.get("businessName")?.toString() ?: ""
                            val qName = doc.getString("name") ?: doc.get("name")?.toString() ?: ""
                            
                            val statusRaw = doc.get("status")
                            val statusStr = when (statusRaw) {
                                is String -> statusRaw
                                is Map<*, *> -> (statusRaw["name"] ?: statusRaw["status"])?.toString()
                                else -> statusRaw?.toString()
                            } ?: "ACTIVE"

                            QueueModel(
                                queueId = doc.id,
                                creatorId = doc.getString("creatorId") ?: doc.get("creatorId")?.toString() ?: "",
                                name = qName.trim(),
                                businessName = bName.trim(),
                                description = doc.getString("description") ?: doc.get("description")?.toString() ?: "",
                                currentNumber = doc.getString("currentNumber") ?: doc.get("currentNumber")?.toString() ?: "A00",
                                status = try { QueueStatus.valueOf(statusStr.trim().uppercase()) } catch(ex: Exception) { QueueStatus.ACTIVE },
                                createdAt = doc.getLong("createdAt") ?: doc.get("createdAt")?.toString()?.toLongOrNull() ?: 0L,
                                prefix = doc.getString("prefix") ?: doc.get("prefix")?.toString() ?: "",
                                lastCounter = doc.getLong("lastCounter") ?: doc.get("lastCounter")?.toString()?.toLongOrNull() ?: 0L,
                                lastResetDate = doc.getString("lastResetDate") ?: doc.get("lastResetDate")?.toString() ?: ""
                            )
                        } catch (ex: Exception) {
                            null
                        }
                    } ?: emptyList()
                    _myQueues.value = qs
                }
        } else {
            _myQueues.value = emptyList()
        }

        // 3. Listen for ENTRIES
        entriesListener = db.collection("entries")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firebase", "Entries Listen Failed", e)
                    return@addSnapshotListener
                }
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val qId = doc.getString("queueId") ?: doc.get("queueId")?.toString() ?: ""
                        val custId = doc.getString("customerId") ?: doc.get("customerId")?.toString() ?: ""
                        val custName = doc.getString("customerName") ?: doc.get("customerName")?.toString() ?: "Customer"
                        val qNum = doc.getString("queueNumber") ?: doc.get("queueNumber")?.toString() ?: ""

                        // Robust Status Parsing (Enums can be stored as Maps or Strings in Firestore)
                        val statusRaw = doc.get("status")
                        val statusStr = when (statusRaw) {
                            is String -> statusRaw
                            is Map<*, *> -> (statusRaw["name"] ?: statusRaw["status"])?.toString()
                            else -> statusRaw?.toString()
                        } ?: "WAITING"

                        QueueEntryModel(
                            entryId = doc.id,
                            queueId = qId.trim(),
                            customerId = custId.trim(),
                            customerName = custName.trim(),
                            queueNumber = qNum.trim(),
                            status = try { 
                                EntryStatus.valueOf(statusStr.trim().uppercase()) 
                            } catch(ex: Exception) { 
                                EntryStatus.WAITING 
                            },
                            joinedAt = doc.getLong("joinedAt") ?: doc.get("joinedAt")?.toString()?.toLongOrNull() ?: 0L
                        )
                    } catch (ex: Exception) {
                        Log.e("Firebase", "Entry Mapping Failed: ${doc.id}", ex)
                        null
                    }
                } ?: emptyList()
                
                Log.d("Firebase", "Entries Received: ${entries.size}")
                val sortedEntries = entries.sortedBy { it.joinedAt }
                _queueEntries.value = sortedEntries
                
                // Authenticated users can be matched directly by their Firebase UID.
                // Anonymous users are matched through the optimistic entry's session ID.
                if (currentUserId != null) {
                    val active = sortedEntries.find {
                        it.customerId == currentUserId &&
                            (it.status == EntryStatus.WAITING || it.status == EntryStatus.CALLED)
                    }
                    _activeCustomerEntry.value = active
                } else {
                    val currentActive = _activeCustomerEntry.value
                    if (currentActive != null) {
                        val updated = sortedEntries.find {
                            it.customerId == currentActive.customerId &&
                                (it.status == EntryStatus.WAITING || it.status == EntryStatus.CALLED)
                        }
                        if (updated != null) {
                            _activeCustomerEntry.value = updated
                        }
                    }
                }
            }
    }

    override fun getQueueById(queueId: String): QueueModel? {
        return _queues.value.find { it.queueId == queueId }
    }

    override fun getEntriesForQueue(queueId: String): List<QueueEntryModel> = _queueEntries.value.filter { it.queueId == queueId }

    override fun getWaitingCount(queueId: String): Int = _queueEntries.value.count { it.queueId == queueId && it.status == EntryStatus.WAITING }

    override fun getPeopleAhead(queueId: String, customerQueueNumber: String): Int {
        if (customerQueueNumber == "..." || customerQueueNumber.isBlank()) return 0
        
        val queue = getQueueById(queueId)
        val servingNumber = queue?.currentNumber ?: ""
        
        // Remove prefixes to compare just the numeric parts.
        // e.g., PC-004 -> 004, Q-001 -> 001
        val customerNumPart = customerQueueNumber.substringAfter("-").toIntOrNull()
        val servingNumPart = servingNumber.substringAfter("-").toIntOrNull()

        if (servingNumPart != null && customerNumPart != null) {
             if (customerNumPart < servingNumPart) {
                 return -1
             }
        } else if (servingNumber.isNotEmpty() && servingNumber != "...") {
            // Fallback to string comparison if not in standard prefix-number format
            if (customerQueueNumber < servingNumber) {
                return -1
            }
        }

        val relevantEntries = _queueEntries.value
            .filter { it.queueId == queueId && (it.status == EntryStatus.WAITING || it.status == EntryStatus.CALLED) }
        
        val index = relevantEntries.indexOfFirst { it.queueNumber == customerQueueNumber }
        return if (index > 0) index else 0
    }

    override fun joinQueue(queueId: String): QueueEntryModel {
        Log.d("Firebase", "Joining Queue with ID: $queueId")
        
        // Backend Safety: Prevent duplicate queue joins.
        // Check before creating the optimistic entry so an existing ticket is not overwritten.
        val existingEntry = _activeCustomerEntry.value
        if (existingEntry != null && (existingEntry.status == EntryStatus.WAITING || existingEntry.status == EntryStatus.CALLED)) {
            Log.w("Firebase", "Duplicate join blocked. User is already in a queue.")
            return existingEntry
        }

        val currentUserId = auth.currentUser?.uid ?: getAnonymousId()
        val entryRef = db.collection("entries").document()
        val queueRef = db.collection("queues").document(queueId.trim())
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.runTransaction { transaction ->
            val queueSnapshot = transaction.get(queueRef)
            val prefix = queueSnapshot.getString("prefix") ?: "QX"
            val lastResetDate = queueSnapshot.getString("lastResetDate") ?: ""
            
            val isNewDay = lastResetDate != today
            val lastCount = if (!isNewDay) {
                queueSnapshot.getLong("lastCounter") ?: 0
            } else {
                0
            }

            val newCount = lastCount + 1
            val assignedNumber = "$prefix-${newCount.toString().padStart(3, '0')}"

            val newEntry = hashMapOf(
                "entryId" to entryRef.id,
                "queueId" to queueId,
                "customerId" to currentUserId,
                "customerName" to "User ${currentUserId.takeLast(4)}",
                "queueNumber" to assignedNumber,
                "status" to EntryStatus.WAITING.name,
                "joinedAt" to System.currentTimeMillis()
            )

            transaction.set(entryRef, newEntry)
            
            if (isNewDay) {
                transaction.update(queueRef, 
                    "lastCounter", newCount, 
                    "lastResetDate", today,
                    "currentNumber", "$prefix-000"
                )
            } else {
                transaction.update(queueRef, "lastCounter", newCount, "lastResetDate", today)
            }
            
            QueueEntryModel(
                entryId = entryRef.id,
                queueId = queueId,
                customerId = currentUserId,
                customerName = "User ${currentUserId.takeLast(4)}",
                queueNumber = assignedNumber,
                status = EntryStatus.WAITING,
                joinedAt = System.currentTimeMillis()
            )
        }.addOnSuccessListener {
            Log.d("Firebase", "Join Successful")
        }.addOnFailureListener { 
            Log.e("Firebase", "Join Transaction Failed", it)
            // Backend Safety: Rollback optimistic UI state if the network transaction fails
            if (_activeCustomerEntry.value?.entryId?.startsWith("temp_") == true) {
                _activeCustomerEntry.value = null
            }
        }

        val tempEntry = QueueEntryModel(
            entryId = "temp_${System.currentTimeMillis()}",
            queueId = queueId,
            customerId = currentUserId,
            customerName = "Joining...",
            queueNumber = "...",
            status = EntryStatus.WAITING,
            joinedAt = System.currentTimeMillis()
        )
        _activeCustomerEntry.value = tempEntry
        
        return tempEntry
    }

    override fun leaveQueue() {
        val active = _activeCustomerEntry.value ?: return
        _activeCustomerEntry.value = null 
        db.collection("entries").document(active.entryId).update("status", EntryStatus.CANCELLED.name)
    }

    override fun createQueue(name: String, businessName: String, description: String): QueueModel {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val sanitized = businessName.lowercase().replace(Regex("[^a-z0-9]"), "_").take(15)
        val docId = "${sanitized}_${System.currentTimeMillis().toString().takeLast(4)}"
        val prefix = name.split(" ").filter { it.isNotBlank() }.map { it.first().uppercase() }.joinToString("").take(4)
        val currentUserId = auth.currentUser?.uid ?: ""

        val newQueueMap = hashMapOf(
            "queueId" to docId,
            "creatorId" to currentUserId,
            "name" to name,
            "businessName" to businessName,
            "description" to description,
            "prefix" to prefix,
            "currentNumber" to "$prefix-000",
            "lastCounter" to 0,
            "lastResetDate" to today,
            "status" to QueueStatus.ACTIVE.name,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("queues").document(docId).set(newQueueMap)
        
        return QueueModel(
            queueId = docId,
            creatorId = currentUserId,
            name = name,
            businessName = businessName,
            description = description,
            prefix = prefix,
            currentNumber = "$prefix-000",
            lastCounter = 0,
            lastResetDate = today,
            status = QueueStatus.ACTIVE,
            createdAt = System.currentTimeMillis()
        )
    }

    override fun callNextCustomer(queueId: String): QueueEntryModel? {
        Log.d("Firebase", "Calling Next Customer for Queue: $queueId")
        // Trim queueId just in case
        val trimmedQueueId = queueId.trim()
        val nextInCache = _queueEntries.value.find { 
            it.queueId.trim() == trimmedQueueId && it.status == EntryStatus.WAITING 
        } ?: run {
             Log.w("Firebase", "No WAITING customer found in local cache for queue: $trimmedQueueId")
             return null
        }

        val currentCalled = _queueEntries.value.find { 
            it.queueId.trim() == trimmedQueueId && it.status == EntryStatus.CALLED 
        }

        val entriesRef = db.collection("entries")
        val queueRef = db.collection("queues").document(trimmedQueueId)

        db.runTransaction { transaction ->
            // Re-read queue
            val queueSnapshot = transaction.get(queueRef)
            if (!queueSnapshot.exists()) {
                throw IllegalStateException("Concurrency conflict: Target queue document does not exist.")
            }
            
            // Re-read next candidate
            val nextRef = entriesRef.document(nextInCache.entryId)
            val nextSnapshot = transaction.get(nextRef)
            
            val statusRaw = nextSnapshot.get("status")
            val nextStatusStr = when (statusRaw) {
                is String -> statusRaw
                is Map<*, *> -> (statusRaw["name"] ?: statusRaw["status"])?.toString()
                else -> statusRaw?.toString()
            }?.trim()?.uppercase() ?: ""
            
            if (!nextSnapshot.exists() || nextSnapshot.getString("queueId")?.trim() != trimmedQueueId || nextStatusStr != EntryStatus.WAITING.name) {
                throw IllegalStateException("Concurrency conflict: Next candidate is no longer WAITING or invalid.")
            }
            
            // Re-read current called if any
            if (currentCalled != null) {
                val currentRef = entriesRef.document(currentCalled.entryId)
                val currentSnapshot = transaction.get(currentRef)
                
                val currentStatusRaw = currentSnapshot.get("status")
                val currentStatusStr = when (currentStatusRaw) {
                    is String -> currentStatusRaw
                    is Map<*, *> -> (currentStatusRaw["name"] ?: currentStatusRaw["status"])?.toString()
                    else -> currentStatusRaw?.toString()
                }?.trim()?.uppercase() ?: ""
                
                if (!currentSnapshot.exists() || currentSnapshot.getString("queueId")?.trim() != trimmedQueueId || currentStatusStr != EntryStatus.CALLED.name) {
                    throw IllegalStateException("Concurrency conflict: Currently called entry is no longer CALLED or invalid.")
                }
                
                transaction.update(currentRef, "status", EntryStatus.SERVED.name)
            }
            
            transaction.update(nextRef, "status", EntryStatus.CALLED.name)
            transaction.update(queueRef, "currentNumber", nextSnapshot.getString("queueNumber") ?: nextInCache.queueNumber)
            null
        }.addOnSuccessListener {
            Log.d("Firebase", "Call Next Successful. New Number: ${nextInCache.queueNumber}")
        }.addOnFailureListener { 
            if (it is IllegalStateException) {
                Log.w("Firebase", "Call Next Aborted: ${it.message}")
            } else {
                Log.e("Firebase", "Call Next Transaction Failed due to network or Firestore error", it) 
            }
        }
        
        return nextInCache
    }

    override fun markServed(entryId: String) {
        db.collection("entries").document(entryId).update("status", EntryStatus.SERVED.name)
    }

    override fun signIn(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                startListeners() // Refresh listeners with the new UID
                onSuccess() 
            }
            .addOnFailureListener { onError(it.message ?: "Authentication Failed") }
    }

    override fun signUp(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { 
                startListeners() // Refresh listeners with the new UID
                onSuccess() 
            }
            .addOnFailureListener { onError(it.message ?: "Registration Failed") }
    }

    override fun skipCustomer(entryId: String) {
        db.collection("entries").document(entryId).update("status", EntryStatus.SKIPPED.name)
    }

    override fun deleteQueue(queueId: String) {
        db.collection("entries").whereEqualTo("queueId", queueId).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.size > 499) {
                    Log.e("Firebase", "Deletion aborted: Cannot atomically delete queue with more than 499 entries.")
                    return@addOnSuccessListener
                }
                
                val batch = db.batch()
                val queueRef = db.collection("queues").document(queueId)
                batch.delete(queueRef)
                
                snapshot.documents.forEach { batch.delete(it.reference) }
                
                batch.commit()
                    .addOnSuccessListener {
                        Log.d("Firebase", "Queue and associated entries atomically deleted: $queueId")
                        if (_activeCustomerEntry.value?.queueId == queueId) {
                            _activeCustomerEntry.value = null
                        }
                    }
                    .addOnFailureListener {
                        Log.e("Firebase", "Failed to commit atomic deletion for queue: $queueId", it)
                    }
            }
            .addOnFailureListener {
                Log.e("Firebase", "Failed to query entries for queue deletion", it)
            }
    }

    override fun toggleQueueStatus(queueId: String) {
        val queue = getQueueById(queueId) ?: return
        val newStatus = if (queue.status == QueueStatus.ACTIVE) QueueStatus.PAUSED else QueueStatus.ACTIVE
        db.collection("queues").document(queueId).update("status", newStatus.name)
    }

    override fun simulateCallMe() {
        val active = _activeCustomerEntry.value ?: return
        db.collection("entries").document(active.entryId).update("status", EntryStatus.CALLED.name)
        db.collection("queues").document(active.queueId).update("currentNumber", active.queueNumber)
    }

    override fun signOut() {
        auth.signOut()
        _myQueues.value = emptyList()
        _activeCustomerEntry.value = null
        startListeners()
    }
}
