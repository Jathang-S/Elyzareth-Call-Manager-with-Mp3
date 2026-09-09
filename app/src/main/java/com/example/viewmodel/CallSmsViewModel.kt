package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ContactEntity
import com.example.data.LogEntity
import com.example.data.SpamKeywordEntity
import com.example.repository.CallSmsRepository
import com.example.repository.SmsAnalysisResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- Models for Music-Infused Calling ---
data class Mp3Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val accentColorHex: String,
    val coverIcon: String = "🎵"
)

data class MusicPlayerState(
    val currentTrack: Mp3Track,
    val isPlaying: Boolean = false,
    val progressMs: Long = 12000,
    val audioFocusState: String = "Normal" // "Normal", "PausedDueToCall", "Restored"
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<Float> = listOf(0.5f, 0.7f, 0.4f, 0.8f, 0.6f), // 5 slider bands (0.0 to 1.0)
    val preset: String = "Lofi Beats" // "Lofi Beats", "Rock Booster", "Acoustic", "Jazz Cafe"
)

data class CallRecording(
    val id: String,
    val callerName: String,
    val timestamp: Long,
    val durationSeconds: Int,
    val fileName: String
)

class CallSmsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CallSmsRepository

    val allContacts: StateFlow<List<ContactEntity>>
    val spammerContacts: StateFlow<List<ContactEntity>>
    val allSpamKeywords: StateFlow<List<SpamKeywordEntity>>
    val allLogs: StateFlow<List<LogEntity>>

    // --- State for Interactive Testing & Simulation ---
    private val _simulatedCall = MutableStateFlow<SimulatedCall?>(null)
    val simulatedCall: StateFlow<SimulatedCall?> = _simulatedCall.asStateFlow()

    private val _simulatedSms = MutableStateFlow<SimulatedSms?>(null)
    val simulatedSms: StateFlow<SimulatedSms?> = _simulatedSms.asStateFlow()

    // --- State for Quick Manual Database Lookups ---
    private val _lookupResult = MutableStateFlow<LookupResult?>(null)
    val lookupResult: StateFlow<LookupResult?> = _lookupResult.asStateFlow()

    private val _filterTestResult = MutableStateFlow<SmsAnalysisResult?>(null)
    val filterTestResult: StateFlow<SmsAnalysisResult?> = _filterTestResult.asStateFlow()

    // --- Music & Call Fusion Features ---
    val availableTracks = listOf(
        Mp3Track("1", "Neon Lights", "LoFi Beats", "Midnight Drive", 185000, "#00F2FE", "🎧"),
        Mp3Track("2", "Rock Star", "Alex Harrison", "Rebel Heart", 210000, "#FF007F", "🎸"),
        Mp3Track("3", "Cyberpunk Synth", "Vector Force", "Neo Grid 2026", 240000, "#39FF14", "⚡"),
        Mp3Track("4", "Aura Breeze", "Luna Chill", "Whispers", 152000, "#BD00FF", "🌸"),
        Mp3Track("5", "Raindrops", "Nature Sounds", "Atmosphere", 300000, "#00E5FF", "🌧️")
    )

    private val _musicPlayerState = MutableStateFlow(MusicPlayerState(currentTrack = availableTracks[0]))
    val musicPlayerState: StateFlow<MusicPlayerState> = _musicPlayerState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    private val _recordings = MutableStateFlow<List<CallRecording>>(emptyList())
    val recordings: StateFlow<List<CallRecording>> = _recordings.asStateFlow()

    // --- In-Call States & Ambient Settings ---
    private val _activeAmbientSound = MutableStateFlow("None") // "None", "Rain", "Café Noise", "Lo-Fi Beats"
    val activeAmbientSound: StateFlow<String> = _activeAmbientSound.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isHoldMusicOn = MutableStateFlow(false)
    val isHoldMusicOn: StateFlow<Boolean> = _isHoldMusicOn.asStateFlow()

    // Real-time animated audio wave amplitudes (FFT Visualizer bars)
    private val _audioWaveAmplitudes = MutableStateFlow(List(16) { 0.2f })
    val audioWaveAmplitudes: StateFlow<List<Float>> = _audioWaveAmplitudes.asStateFlow()

    // Simulated scanning status for Music Library
    private val _isScanningLibrary = MutableStateFlow(false)
    val isScanningLibrary: StateFlow<Boolean> = _isScanningLibrary.asStateFlow()

    // Custom ringtone mapper (phone number -> track id)
    private val _customRingtones = MutableStateFlow<Map<String, String>>(
        mapOf("+15550142398" to "2") // Default Alex Harrison -> Rock Star
    )
    val customRingtones: StateFlow<Map<String, String>> = _customRingtones.asStateFlow()

    private var playerProgressJob: Job? = null
    private var visualizerJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = CallSmsRepository(database.callSmsDao())

        allContacts = repository.allContacts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        spammerContacts = repository.spammerContacts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allSpamKeywords = repository.allSpamKeywords
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allLogs = repository.allLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Seed default database entities
        viewModelScope.launch {
            repository.checkAndSeedDatabase()
        }

        // Seed default recordings
        _recordings.value = listOf(
            CallRecording("r1", "Alex Harrison", System.currentTimeMillis() - 86400000, 42, "rec_alex_harrison_aug.mp3"),
            CallRecording("r2", "Unknown Spammer", System.currentTimeMillis() - 172800000, 15, "rec_spam_block_warning.mp3")
        )

        // Start background updater loops
        startMusicProgressLoop()
        startVisualizerAnimationLoop()
    }

    // --- Music Player Operations ---
    fun togglePlayPause() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isPlaying = !current.isPlaying)
    }

    fun playTrack(track: Mp3Track) {
        _musicPlayerState.value = _musicPlayerState.value.copy(
            currentTrack = track,
            isPlaying = true,
            progressMs = 0
        )
    }

    fun nextTrack() {
        val currentTrack = _musicPlayerState.value.currentTrack
        val index = availableTracks.indexOfFirst { it.id == currentTrack.id }
        val nextIndex = (index + 1) % availableTracks.size
        playTrack(availableTracks[nextIndex])
    }

    fun previousTrack() {
        val currentTrack = _musicPlayerState.value.currentTrack
        val index = availableTracks.indexOfFirst { it.id == currentTrack.id }
        val prevIndex = if (index - 1 < 0) availableTracks.size - 1 else index - 1
        playTrack(availableTracks[prevIndex])
    }

    fun seekProgress(ms: Long) {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(progressMs = ms.coerceIn(0, current.currentTrack.durationMs))
    }

    fun scanMusicLibrary() {
        viewModelScope.launch {
            _isScanningLibrary.value = true
            delay(2500) // Simulate scanning phone files
            _isScanningLibrary.value = false
        }
    }

    // --- Custom Ringtone Assignments ---
    fun assignRingtone(phoneNumber: String, trackId: String) {
        val updated = _customRingtones.value.toMutableMap()
        updated[phoneNumber] = trackId
        _customRingtones.value = updated
    }

    fun cutAndAssignRingtone(
        baseTrackId: String,
        cutName: String,
        startSec: Float,
        endSec: Float,
        targetContactPhone: String
    ) {
        viewModelScope.launch {
            repository.insertLog(
                LogEntity(
                    type = "SYSTEM",
                    phoneNumber = targetContactPhone.ifEmpty { "0" },
                    senderName = "MP3 Cutter",
                    messageBody = "Trimmed track '$baseTrackId' into loop '$cutName' (${String.format("%.1f", startSec)}s - ${String.format("%.1f", endSec)}s)",
                    wasBlocked = false,
                    isSpam = false,
                    actionTaken = "Loop Saved & Mapped"
                )
            )
            if (targetContactPhone.isNotEmpty()) {
                assignRingtone(targetContactPhone, baseTrackId)
            }
        }
    }

    // --- Equalizer Controls ---
    fun updateEqualizerBand(index: Int, value: Float) {
        val bands = _equalizerState.value.bands.toMutableList()
        if (index in bands.indices) {
            bands[index] = value.coerceIn(0.0f, 1.0f)
            _equalizerState.value = _equalizerState.value.copy(bands = bands, preset = "Custom")
        }
    }

    fun setEqualizerPreset(presetName: String) {
        val newBands = when (presetName) {
            "Lofi Beats" -> listOf(0.4f, 0.6f, 0.5f, 0.4f, 0.5f)
            "Rock Booster" -> listOf(0.8f, 0.5f, 0.4f, 0.6f, 0.8f)
            "Acoustic" -> listOf(0.5f, 0.4f, 0.6f, 0.7f, 0.5f)
            "Jazz Cafe" -> listOf(0.6f, 0.7f, 0.4f, 0.5f, 0.6f)
            else -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        }
        _equalizerState.value = EqualizerState(bands = newBands, preset = presetName)
    }

    // --- Active In-Call Utilities ---
    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleHoldMusic() {
        _isHoldMusicOn.value = !_isHoldMusicOn.value
    }

    fun selectAmbientSound(soundName: String) {
        _activeAmbientSound.value = soundName
    }

    // --- Database Manipulation ---
    fun addContact(phoneNumber: String, name: String, category: String, spamReason: String?, isBlocked: Boolean) {
        viewModelScope.launch {
            repository.insertContact(
                ContactEntity(
                    phoneNumber = phoneNumber,
                    name = name,
                    category = category,
                    spamReason = if (category == "SPAM") spamReason else null,
                    isBlocked = isBlocked
                )
            )
        }
    }

    fun removeContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
        }
    }

    fun removeContactById(id: Int) {
        viewModelScope.launch {
            repository.deleteContactById(id)
        }
    }

    fun addSpamKeyword(keyword: String) {
        viewModelScope.launch {
            repository.insertSpamKeyword(SpamKeywordEntity(keyword = keyword))
        }
    }

    fun removeSpamKeyword(keyword: SpamKeywordEntity) {
        viewModelScope.launch {
            repository.deleteSpamKeyword(keyword)
        }
    }

    fun deleteLog(id: Int) {
        viewModelScope.launch {
            repository.deleteLogById(id)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    // --- Interactive Search Lookups ---
    fun performLookup(number: String) {
        if (number.isBlank()) {
            _lookupResult.value = null
            return
        }
        viewModelScope.launch {
            val contact = repository.identifyCaller(number)
            _lookupResult.value = LookupResult(
                searchedNumber = number,
                foundContact = contact
            )
        }
    }

    fun clearLookup() {
        _lookupResult.value = null
    }

    fun performFilterTest(number: String, body: String) {
        if (number.isBlank() || body.isBlank()) {
            _filterTestResult.value = null
            return
        }
        viewModelScope.launch {
            val result = repository.analyzeSms(number, body)
            _filterTestResult.value = result
        }
    }

    fun clearFilterTest() {
        _filterTestResult.value = null
    }

    // --- Simulators for Calls and SMS (With Audio Focus Trigger) ---
    fun simulateIncomingCall(phoneNumber: String) {
        viewModelScope.launch {
            // [1] AUDIO FOCUS: Handle second-by-second pause of any active background music
            val wasMusicPlaying = _musicPlayerState.value.isPlaying
            if (wasMusicPlaying) {
                _musicPlayerState.value = _musicPlayerState.value.copy(
                    isPlaying = false,
                    audioFocusState = "PausedDueToCall"
                )
            }

            val contact = repository.identifyCaller(phoneNumber)
            val isSpam = contact?.category == "SPAM"
            val wasBlocked = contact?.isBlocked == true

            _simulatedCall.value = SimulatedCall(
                phoneNumber = phoneNumber,
                contactName = contact?.name ?: "Unknown Number",
                category = contact?.category ?: "UNKNOWN",
                spamReason = contact?.spamReason,
                isBlocked = wasBlocked
            )

            // Log this simulated screened call
            val action = when {
                wasBlocked -> "Blocked Spam Call"
                isSpam -> "Identified Spam Call"
                contact != null -> "Identified Call"
                else -> "Screened Unknown Call"
            }

            repository.insertLog(
                LogEntity(
                    type = "CALL",
                    phoneNumber = phoneNumber,
                    senderName = contact?.name ?: "Unknown Number",
                    messageBody = null,
                    wasBlocked = wasBlocked,
                    isSpam = isSpam,
                    actionTaken = action
                )
            )
        }
    }

    fun dismissSimulatedCall() {
        _simulatedCall.value = null
        
        // [2] AUDIO FOCUS: Fade back in when call is dismissed
        val focus = _musicPlayerState.value.audioFocusState
        if (focus == "PausedDueToCall") {
            _musicPlayerState.value = _musicPlayerState.value.copy(
                isPlaying = true,
                audioFocusState = "Restored"
            )
        }
    }

    fun simulateIncomingSms(phoneNumber: String, body: String) {
        viewModelScope.launch {
            val analysis = repository.analyzeSms(phoneNumber, body)
            val isSpam = analysis.isSpam
            val contact = repository.getContactByNumber(phoneNumber)
            val wasBlocked = isSpam && (contact?.isBlocked == true || analysis.reason.contains("keyword") || contact?.category == "SPAM")

            _simulatedSms.value = SimulatedSms(
                phoneNumber = phoneNumber,
                senderName = analysis.senderName ?: "Unknown Sender",
                body = body,
                isSpam = isSpam,
                reason = analysis.reason,
                wasBlocked = wasBlocked
            )

            // Log this SMS screening event
            val action = when {
                wasBlocked -> "Blocked Spam SMS"
                isSpam -> "Flagged Spam SMS"
                analysis.senderName != null -> "Identified SMS"
                else -> "Screened Unknown SMS"
            }

            repository.insertLog(
                LogEntity(
                    type = "SMS",
                    phoneNumber = phoneNumber,
                    senderName = analysis.senderName ?: "Unknown Sender",
                    messageBody = body,
                    wasBlocked = wasBlocked,
                    isSpam = isSpam,
                    actionTaken = action
                )
            )
        }
    }

    fun dismissSimulatedSms() {
        _simulatedSms.value = null
    }

    // --- Worker Tick Loops for Media and Visualizers ---
    private fun startMusicProgressLoop() {
        playerProgressJob?.cancel()
        playerProgressJob = viewModelScope.launch {
            while (true) {
                val state = _musicPlayerState.value
                if (state.isPlaying) {
                    val nextProgress = state.progressMs + 1000L
                    if (nextProgress >= state.currentTrack.durationMs) {
                        nextTrack()
                    } else {
                        _musicPlayerState.value = state.copy(progressMs = nextProgress)
                    }
                }
                delay(1000)
            }
        }
    }

    // --- Advanced Player & Hybrid Call Settings ---
    private val _crossfadeMs = MutableStateFlow(1500)
    val crossfadeMs: StateFlow<Int> = _crossfadeMs.asStateFlow()

    private val _skinMode = MutableStateFlow("Neon Spectrum") // "Neon Spectrum", "Vinyl Turntable", "Bouncing DVD"
    val skinMode: StateFlow<String> = _skinMode.asStateFlow()

    private val _isDialerLocked = MutableStateFlow(false)
    val isDialerLocked: StateFlow<Boolean> = _isDialerLocked.asStateFlow()

    private val _isSelfHoldActive = MutableStateFlow(false)
    val isSelfHoldActive: StateFlow<Boolean> = _isSelfHoldActive.asStateFlow()

    private val _isFlashAlertEnabled = MutableStateFlow(true)
    val isFlashAlertEnabled: StateFlow<Boolean> = _isFlashAlertEnabled.asStateFlow()

    private val _customMoodTags = MutableStateFlow<Map<String, String>>(
        mapOf("+15550142398" to "Energetic 🎸")
    )
    val customMoodTags: StateFlow<Map<String, String>> = _customMoodTags.asStateFlow()

    fun updateCrossfade(ms: Int) {
        _crossfadeMs.value = ms.coerceIn(0, 5000)
    }

    fun updateFlashAlert(enabled: Boolean) {
        _isFlashAlertEnabled.value = enabled
    }

    fun updateSkinMode(skin: String) {
        _skinMode.value = skin
    }

    fun updateDialerLocked(locked: Boolean) {
        _isDialerLocked.value = locked
    }

    fun updateSelfHold(active: Boolean) {
        _isSelfHoldActive.value = active
    }

    fun assignMoodTag(phoneNumber: String, mood: String) {
        val updated = _customMoodTags.value.toMutableMap()
        if (mood == "None") {
            updated.remove(phoneNumber)
        } else {
            updated[phoneNumber] = mood
        }
        _customMoodTags.value = updated
    }

    fun addCallRecording(recording: CallRecording) {
        val current = _recordings.value.toMutableList()
        current.add(0, recording) // Insert at top
        _recordings.value = current
    }

    private fun startVisualizerAnimationLoop() {
        visualizerJob?.cancel()
        visualizerJob = viewModelScope.launch {
            val rand = Random(System.currentTimeMillis())
            while (true) {
                // If call is active or music is playing, fluctuate visualizer peaks
                val isCallActive = _simulatedCall.value != null
                val isMusicPlaying = _musicPlayerState.value.isPlaying
                
                if (isCallActive || isMusicPlaying) {
                    val scale = if (isCallActive && _isHoldMusicOn.value) 0.9f else if (isCallActive) 0.7f else 0.4f
                    _audioWaveAmplitudes.value = List(16) {
                        (rand.nextFloat() * scale + 0.1f).coerceIn(0.05f, 1.0f)
                    }
                } else {
                    _audioWaveAmplitudes.value = List(16) { 0.05f }
                }
                delay(120) // Fast 60-FPS like updates for visual fluidity
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerProgressJob?.cancel()
        visualizerJob?.cancel()
    }
}

// Helper models for VM state
data class SimulatedCall(
    val phoneNumber: String,
    val contactName: String,
    val category: String,
    val spamReason: String?,
    val isBlocked: Boolean
)

data class SimulatedSms(
    val phoneNumber: String,
    val senderName: String,
    val body: String,
    val isSpam: Boolean,
    val reason: String,
    val wasBlocked: Boolean
)

data class LookupResult(
    val searchedNumber: String,
    val foundContact: ContactEntity?
)
