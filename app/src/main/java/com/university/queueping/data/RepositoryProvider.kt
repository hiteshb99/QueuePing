package com.university.queueping.data

/**
 * Central provider to switch between Mock and Firebase repositories.
 * Set useFirebase = true to use the real backend.
 */
object RepositoryProvider {
    var useFirebase: Boolean = true // Now using real Firebase backend
    
    val repository: QueueRepository by lazy {
        if (useFirebase) FirebaseRepository() else MockDataRepository
    }
}
