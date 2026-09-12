package com.example.telecom

import android.content.Intent
import android.os.Bundle
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android Telecom InCallService implementation for Elyzareth Caller.
 *
 * This service is bound by the Android Telecom framework when Elyzareth is
 * designated as the system Default Dialer (or during active calls).
 *
 * It satisfies all platform requirements for Default Dialer qualification:
 * 1. Binds to "android.telecom.InCallService"
 * 2. Declares android:permission="android.permission.BIND_INCALL_SERVICE"
 * 3. Includes <meta-data android:name="android.telecom.IN_CALL_SERVICE_UI" android:value="true" />
 */
class ElyzarethInCallService : InCallService() {

    companion object {
        private const val TAG = "ElyzarethInCallService"

        @Volatile
        private var instance: ElyzarethInCallService? = null

        private val _activeCall = MutableStateFlow<Call?>(null)
        val activeCall: StateFlow<Call?> = _activeCall.asStateFlow()

        private val _callState = MutableStateFlow<Int>(Call.STATE_DISCONNECTED)
        val callState: StateFlow<Int> = _callState.asStateFlow()

        private val _callAudioState = MutableStateFlow<CallAudioState?>(null)
        val callAudioState: StateFlow<CallAudioState?> = _callAudioState.asStateFlow()

        fun currentCall(): Call? = _activeCall.value
        fun currentAudioState(): CallAudioState? = _callAudioState.value
        fun getService(): ElyzarethInCallService? = instance
    }

    private val callCallbacks = mutableMapOf<Call, Call.Callback>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "ElyzarethInCallService initialized by Android Telecom")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _activeCall.value = null
        _callState.value = Call.STATE_DISCONNECTED
        _callAudioState.value = null
        callCallbacks.clear()
        Log.i(TAG, "ElyzarethInCallService destroyed")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.i(TAG, "onCallAdded: call=$call, initial state=${call.state}")

        val callback = object : Call.Callback() {
            override fun onStateChanged(targetCall: Call, newState: Int) {
                super.onStateChanged(targetCall, newState)
                Log.d(TAG, "Call onStateChanged: newState=$newState, call=$targetCall")
                if (_activeCall.value == targetCall) {
                    _callState.value = newState
                }
                if (newState == Call.STATE_DISCONNECTED) {
                    if (_activeCall.value == targetCall) {
                        _activeCall.value = null
                        _callState.value = Call.STATE_DISCONNECTED
                    }
                }
            }

            override fun onDetailsChanged(targetCall: Call, details: Call.Details) {
                super.onDetailsChanged(targetCall, details)
                Log.d(TAG, "Call onDetailsChanged: caller=${details.handle}")
            }

            override fun onConferenceableCallsChanged(targetCall: Call, conferenceableCalls: MutableList<Call>?) {
                super.onConferenceableCallsChanged(targetCall, conferenceableCalls)
                Log.d(TAG, "Call onConferenceableCallsChanged: count=${conferenceableCalls?.size ?: 0}")
            }
        }

        callCallbacks[call] = callback
        call.registerCallback(callback)

        _activeCall.value = call
        _callState.value = call.state
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.i(TAG, "onCallRemoved: call=$call")

        callCallbacks.remove(call)?.let { callback ->
            try {
                call.unregisterCallback(callback)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering callback for call", e)
            }
        }

        if (_activeCall.value == call) {
            _activeCall.value = null
            _callState.value = Call.STATE_DISCONNECTED
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState) {
        super.onCallAudioStateChanged(audioState)
        Log.d(TAG, "onCallAudioStateChanged: route=${audioState.route}, isMuted=${audioState.isMuted}")
        _callAudioState.value = audioState
    }

    override fun onBringToForeground(showDialpad: Boolean) {
        super.onBringToForeground(showDialpad)
        Log.d(TAG, "onBringToForeground requested by Telecom: showDialpad=$showDialpad")
    }

    override fun onCanAddCallChanged(canAddCall: Boolean) {
        super.onCanAddCallChanged(canAddCall)
        Log.d(TAG, "onCanAddCallChanged: canAddCall=$canAddCall")
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        Log.d(TAG, "onSilenceRinger requested by Telecom")
    }

    override fun onConnectionEvent(call: Call, event: String, extras: Bundle?) {
        super.onConnectionEvent(call, event, extras)
        Log.d(TAG, "onConnectionEvent: event=$event, call=$call")
    }
}
