package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Database entity for storing local call logs,
 * including phone numbers, timestamps, and spam status flags.
 */
@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val phoneNumber: String,
    val callerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val callType: String = "INCOMING", // "INCOMING", "OUTGOING", "MISSED", "REJECTED", "BLOCKED"
    val isSpam: Boolean = false,
    val spamReason: String? = null,
    val wasBlocked: Boolean = false
)
