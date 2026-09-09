package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CALL" or "SMS"
    val phoneNumber: String,
    val senderName: String?, // Resolved caller/sender name
    val messageBody: String?, // For SMS type logs
    val timestamp: Long = System.currentTimeMillis(),
    val wasBlocked: Boolean = false,
    val isSpam: Boolean = false,
    val actionTaken: String // "Identified", "Spam Blocked", "Allowed"
)
