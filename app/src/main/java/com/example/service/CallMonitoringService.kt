package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.CallLogEntity
import com.example.data.LogEntity
import com.example.receiver.NotificationHelper
import com.example.repository.CallSmsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android Service that monitors incoming call states and triggers notifications
 * to perform and test local Caller ID detection against the offline database.
 */
class CallMonitoringService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var telephonyManager: TelephonyManager

    private var telephonyCallback: Any? = null
    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    private var lastObservedNumber: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "CallMonitoringService created")
        NotificationHelper.initNotificationChannels(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerCallStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand received action: $action")

        when (action) {
            ACTION_START_MONITORING -> {
                // Ensure channels and notification if started as foreground
                try {
                    val notification = NotificationHelper.buildServiceNotification(this)
                    startForeground(NotificationHelper.NOTIFICATION_ID_SERVICE, notification)
                } catch (e: Exception) {
                    Log.w(TAG, "Foreground start failed, running in background: ${e.message}")
                }
            }
            ACTION_STOP_MONITORING -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TEST_CALLER_ID -> {
                val testNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "+18005559999"
                Log.d(TAG, "Triggering test local caller ID detection for: $testNumber")
                processIncomingCall(testNumber)
            }
        }

        return START_STICKY
    }

    private fun registerCallStateListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleStateChanged(state, null)
                    }
                }
                telephonyCallback = callback
                telephonyManager.registerTelephonyCallback(mainExecutor, callback)
                Log.d(TAG, "Registered TelephonyCallback for Android S+")
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleStateChanged(state, phoneNumber)
                    }
                }
                phoneStateListener = listener
                @Suppress("DEPRECATION")
                telephonyManager.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d(TAG, "Registered PhoneStateListener for pre-Android S")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing READ_PHONE_STATE permission: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering call state listener: ${e.message}")
        }
    }

    private fun handleStateChanged(state: Int, phoneNumber: String?) {
        Log.d(TAG, "Call state changed: $state, number: $phoneNumber")
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                val numberToProcess = phoneNumber ?: lastObservedNumber
                if (!numberToProcess.isNullOrBlank()) {
                    lastObservedNumber = numberToProcess
                    processIncomingCall(numberToProcess)
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                lastObservedNumber = null
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered or dialing
            }
        }
    }

    /**
     * Identifies the caller using the local Room Database and triggers
     * a rich Caller ID notification and inserts a CallLogEntity.
     */
    fun processIncomingCall(phoneNumber: String) {
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = CallSmsRepository(db.callSmsDao())

                val contact = repository.identifyCaller(phoneNumber)
                val callerName = contact?.name ?: "Unknown Caller"
                val isSpam = contact?.category == "SPAM"
                val wasBlocked = contact?.isBlocked == true
                val reason = contact?.spamReason ?: if (isSpam) "Flagged by Community" else null

                // 1. Post rich Caller ID notification
                NotificationHelper.showCallNotification(
                    context = applicationContext,
                    number = phoneNumber,
                    title = callerName,
                    isSpam = isSpam,
                    reason = reason
                )

                // 2. Insert into CallLogEntity Room table
                val callLog = CallLogEntity(
                    phoneNumber = phoneNumber,
                    callerName = callerName,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = 0,
                    callType = if (wasBlocked) "BLOCKED" else "INCOMING",
                    isSpam = isSpam,
                    spamReason = reason,
                    wasBlocked = wasBlocked
                )
                repository.insertCallLog(callLog)

                // 3. Keep general audit log table in sync
                repository.insertLog(
                    LogEntity(
                        type = "CALL",
                        phoneNumber = phoneNumber,
                        senderName = callerName,
                        messageBody = null,
                        wasBlocked = wasBlocked,
                        isSpam = isSpam,
                        actionTaken = if (wasBlocked) "Blocked Spam Call" else if (isSpam) "Identified Spam Call" else "Identified Call"
                    )
                )

                Log.d(TAG, "Successfully processed and logged incoming call: $phoneNumber ($callerName)")
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming call: ${e.message}", e)
            }
        }
    }

    private fun unregisterCallStateListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (telephonyCallback as? TelephonyCallback)?.let {
                    telephonyManager.unregisterTelephonyCallback(it)
                }
            } else {
                @Suppress("DEPRECATION")
                phoneStateListener?.let {
                    telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering listener: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "CallMonitoringService destroyed")
        unregisterCallStateListener()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CallMonitoringService"

        const val ACTION_START_MONITORING = "com.example.service.ACTION_START_MONITORING"
        const val ACTION_STOP_MONITORING = "com.example.service.ACTION_STOP_MONITORING"
        const val ACTION_TEST_CALLER_ID = "com.example.service.ACTION_TEST_CALLER_ID"
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"

        fun startService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (e: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, CallMonitoringService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }

        fun triggerTestCallerId(context: Context, phoneNumber: String = "+18005559999") {
            val intent = Intent(context, CallMonitoringService::class.java).apply {
                action = ACTION_TEST_CALLER_ID
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            context.startService(intent)
        }
    }
}
