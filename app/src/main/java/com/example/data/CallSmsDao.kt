package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CallSmsDao {
    // --- Contacts Queries ---
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE category = 'SPAM' ORDER BY name ASC")
    fun getSpammerContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByNumber(phoneNumber: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Int)

    // --- Spam Keywords Queries ---
    @Query("SELECT * FROM spam_keywords ORDER BY keyword ASC")
    fun getAllSpamKeywords(): Flow<List<SpamKeywordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpamKeyword(keyword: SpamKeywordEntity)

    @Delete
    suspend fun deleteSpamKeyword(keyword: SpamKeywordEntity)

    // --- Logs Queries ---
    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)
}
