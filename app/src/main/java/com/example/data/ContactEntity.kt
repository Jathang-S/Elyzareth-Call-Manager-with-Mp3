package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String, // Normalized phone number
    val name: String,
    val category: String, // "PERSONAL", "BUSINESS", "SPAM"
    val spamReason: String? = null,
    val isBlocked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
