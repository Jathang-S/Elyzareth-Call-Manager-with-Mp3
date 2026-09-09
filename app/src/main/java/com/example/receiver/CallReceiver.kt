package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.data.AppDatabase
import com.example.data.LogEntity
import com.example.repository.CallSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (state == TelephonyManager.EXTRA_STATE_RINGING && incomingNumber != null) {
            val pendingResult = goAsync()
            
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val repository = CallSmsRepository(db.callSmsDao())
                    
                    val contact = repository.identifyCaller(incomingNumber)
                    val name = contact?.name ?: "Unknown Number"
                    val isSpam = contact?.category == "SPAM"
                    val wasBlocked = contact?.isBlocked == true
                    val reason = contact?.spamReason ?: "Spam Telemarketer"

                    // Trigger notification
                    NotificationHelper.showCallNotification(
                        context,
                        incomingNumber,
                        name,
                        isSpam,
                        if (isSpam) reason else null
                    )

                    // Save screening log
                    repository.insertLog(
                        LogEntity(
                            type = "CALL",
                            phoneNumber = incomingNumber,
                            senderName = name,
                            messageBody = null,
                            wasBlocked = wasBlocked,
                            isSpam = isSpam,
                            actionTaken = if (wasBlocked) "Blocked Spam Call" else if (isSpam) "Identified Spam Call" else "Identified Call"
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
}
