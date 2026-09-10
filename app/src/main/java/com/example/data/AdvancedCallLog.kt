package com.example.data

/**
 * Advanced Call Log model representing a rich call record with:
 * - Contact resolution (high-res photo, display name, fallback initial)
 * - SIM Card / Carrier label (e.g. "airtel", "Jio")
 * - HD Audio badge
 * - Direction & Location (Incoming, Outgoing, Missed, Home, Mobile, India)
 * - Dynamic relative timestamp ("6 min ago", "Yesterday")
 * - Spam / Verified Business detection ("Likely: Jaydeb Roy")
 * - Inline Context Action ("Was this a business?")
 */
data class AdvancedCallLog(
    val id: Long,
    val phoneNumber: String,
    val contactName: String? = null,
    val callType: Int, // CallLog.Calls.INCOMING_TYPE, OUTGOING_TYPE, MISSED_TYPE, REJECTED_TYPE, etc.
    val date: Long, // timestamp in ms
    val durationSeconds: Long = 0,
    val carrierLabel: String = "airtel", // e.g. "airtel", "Jio 4G", "SIM 1"
    val locationLabel: String = "Mobile", // e.g. "Mobile", "Home", "Work", "India"
    val isHd: Boolean = false,
    val isSpam: Boolean = false,
    val spamWarning: String? = null, // e.g. "Likely: Jaydeb Roy"
    val isVerifiedBusiness: Boolean = false,
    val photoUri: String? = null,
    val isBusinessQuestionAnswered: Boolean = false
)

/**
 * Clean device contact item queried from ContactsContract
 */
data class DeviceContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val photoUri: String? = null,
    val typeLabel: String = "Mobile",
    val isSpam: Boolean = false,
    val isVerified: Boolean = false
)
