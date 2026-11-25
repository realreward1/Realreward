package com.example.core.firebase

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

/**
 * Provides a pre-warmed, persistent-cache-enabled FirebaseFirestore instance
 * pointing to the provisioned named database for fast offline-tolerant + live realtime updates.
 */
object FirestoreProvider {
    @Volatile
    private var configuredDbId: String? = null

    fun getDatabase(context: Context): FirebaseFirestore {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        if (configuredDbId != dbId) {
            synchronized(this) {
                if (configuredDbId != dbId) {
                    runCatching {
                        val cacheSettings = PersistentCacheSettings.newBuilder()
                            .setSizeBytes(50L * 1024L * 1024L)
                            .build()
                        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                            .setLocalCacheSettings(cacheSettings)
                            .build()
                    }
                    runCatching { db.enableNetwork() }
                    configuredDbId = dbId
                }
            }
        }
        return db
    }
}
