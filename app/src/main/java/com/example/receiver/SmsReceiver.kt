package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import com.example.data.AppDatabase
import com.example.data.LogEntity
import com.example.repository.CallSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repository = CallSmsRepository(db.callSmsDao())

                // Aggregate messages from the same sender (usually there's only 1 for simple texts)
                val sender = messages.first().originatingAddress ?: "Unknown"
                val body = messages.joinToString(separator = "") { it.messageBody ?: "" }

                val analysis = repository.analyzeSms(sender, body)
                val isSpam = analysis.isSpam
                val contact = repository.getContactByNumber(sender)
                val wasBlocked = isSpam && (contact?.isBlocked == true || analysis.reason.contains("keyword") || contact?.category == "SPAM")

                // Send a notification showing identity or block outcome
                NotificationHelper.showSmsNotification(
                    context,
                    analysis.senderName ?: sender,
                    body,
                    isSpam,
                    if (isSpam) analysis.reason else null
                )

                // Log the entry in database
                val action = when {
                    wasBlocked -> "Blocked Spam SMS"
                    isSpam -> "Flagged Spam SMS"
                    analysis.senderName != null -> "Identified SMS"
                    else -> "Screened Unknown SMS"
                }

                repository.insertLog(
                    LogEntity(
                        type = "SMS",
                        phoneNumber = sender,
                        senderName = analysis.senderName ?: "Unknown Sender",
                        messageBody = body,
                        wasBlocked = wasBlocked,
                        isSpam = isSpam,
                        actionTaken = action
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
