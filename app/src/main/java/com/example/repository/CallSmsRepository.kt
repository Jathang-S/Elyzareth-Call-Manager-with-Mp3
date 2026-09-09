package com.example.repository

import com.example.data.CallLogEntity
import com.example.data.CallSmsDao
import com.example.data.ContactEntity
import com.example.data.LogEntity
import com.example.data.SpamKeywordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale

data class SmsAnalysisResult(
    val isSpam: Boolean,
    val reason: String,
    val senderName: String?
)

class CallSmsRepository(private val dao: CallSmsDao) {

    val allContacts: Flow<List<ContactEntity>> = dao.getAllContacts()
    val spammerContacts: Flow<List<ContactEntity>> = dao.getSpammerContacts()
    val allSpamKeywords: Flow<List<SpamKeywordEntity>> = dao.getAllSpamKeywords()
    val allLogs: Flow<List<LogEntity>> = dao.getAllLogs()
    val allCallLogs: Flow<List<CallLogEntity>> = dao.getAllCallLogs()
    val spamCallLogs: Flow<List<CallLogEntity>> = dao.getSpamCallLogs()

    suspend fun insertCallLog(callLog: CallLogEntity): Long = dao.insertCallLog(callLog)
    suspend fun deleteCallLogById(id: Int) = dao.deleteCallLogById(id)
    suspend fun clearAllCallLogs() = dao.clearAllCallLogs()
    fun getCallLogsForNumber(phoneNumber: String): Flow<List<CallLogEntity>> =
        dao.getCallLogsForNumber(normalizeNumber(phoneNumber))

    suspend fun insertContact(contact: ContactEntity) {
        val normalized = normalizeNumber(contact.phoneNumber)
        dao.insertContact(contact.copy(phoneNumber = normalized))
    }

    suspend fun deleteContact(contact: ContactEntity) = dao.deleteContact(contact)

    suspend fun deleteContactById(id: Int) = dao.deleteContactById(id)

    suspend fun insertSpamKeyword(keyword: SpamKeywordEntity) {
        val trimmed = keyword.keyword.trim().lowercase(Locale.ROOT)
        if (trimmed.isNotEmpty()) {
            dao.insertSpamKeyword(keyword.copy(keyword = trimmed))
        }
    }

    suspend fun deleteSpamKeyword(keyword: SpamKeywordEntity) = dao.deleteSpamKeyword(keyword)

    suspend fun insertLog(log: LogEntity) = dao.insertLog(log)

    suspend fun clearAllLogs() = dao.clearAllLogs()

    suspend fun deleteLogById(id: Int) = dao.deleteLogById(id)

    suspend fun getContactByNumber(phoneNumber: String): ContactEntity? {
        val normalized = normalizeNumber(phoneNumber)
        return dao.getContactByNumber(normalized)
    }

    // Helper to normalize phone number formatting for exact lookups
    fun normalizeNumber(number: String): String {
        return number.replace(Regex("[^0-9+]"), "")
    }

    // Check if there is data, if not seed it
    suspend fun checkAndSeedDatabase() {
        val currentContacts = allContacts.first()
        if (currentContacts.isEmpty()) {
            // Seed default contacts and spammers
            val seedContacts = listOf(
                ContactEntity(phoneNumber = "+14155550199", name = "Mom ❤️", category = "PERSONAL"),
                ContactEntity(phoneNumber = "+16502530000", name = "Google HQ", category = "BUSINESS"),
                ContactEntity(phoneNumber = "+18005550143", name = "Verified Bank Support", category = "BUSINESS"),
                ContactEntity(phoneNumber = "+18889991111", name = "Aggressive Loan Offers", category = "SPAM", spamReason = "Robocall Telemarketing", isBlocked = true),
                ContactEntity(phoneNumber = "+18005559999", name = "IRS Tax Impersonation Scam", category = "SPAM", spamReason = "Phishing / Fraud", isBlocked = true),
                ContactEntity(phoneNumber = "+14155554321", name = "Alex Johnson", category = "PERSONAL")
            )
            for (contact in seedContacts) {
                insertContact(contact)
            }
        }

        val currentKeywords = allSpamKeywords.first()
        if (currentKeywords.isEmpty()) {
            // Seed default spam keywords
            val seedKeywords = listOf(
                "lottery", "urgent loan", "crypto investment", "free cash", "earn fast",
                "congratulations you won", "claim prize", "bitcoin", "gift card", "risk free"
            )
            for (kw in seedKeywords) {
                insertSpamKeyword(SpamKeywordEntity(keyword = kw))
            }
        }
    }

    // Perform Caller ID local lookup
    suspend fun identifyCaller(phoneNumber: String): ContactEntity? {
        val normalized = normalizeNumber(phoneNumber)
        return getContactByNumber(normalized)
    }

    // Local SMS classification engine
    suspend fun analyzeSms(phoneNumber: String, body: String): SmsAnalysisResult {
        val normalized = normalizeNumber(phoneNumber)
        val contact = getContactByNumber(normalized)

        // 1. Is the sender on our blocked list/marked as spam?
        if (contact != null) {
            if (contact.isBlocked || contact.category == "SPAM") {
                return SmsAnalysisResult(
                    isSpam = true,
                    reason = "Sender is blacklisted: ${contact.spamReason ?: "Spam Contact"}",
                    senderName = contact.name
                )
            }
            return SmsAnalysisResult(
                isSpam = false,
                reason = "Verified Contact: ${contact.name}",
                senderName = contact.name
            )
        }

        // 2. Scan text body for local spam keywords
        val keywordsList = allSpamKeywords.first()
        val lowerBody = body.lowercase(Locale.ROOT)
        for (kw in keywordsList) {
            if (lowerBody.contains(kw.keyword)) {
                return SmsAnalysisResult(
                    isSpam = true,
                    reason = "Triggered keyword: \"${kw.keyword}\"",
                    senderName = "Unknown Sender"
                )
            }
        }

        return SmsAnalysisResult(
            isSpam = false,
            reason = "No spam indicators found.",
            senderName = null
        )
    }
}
