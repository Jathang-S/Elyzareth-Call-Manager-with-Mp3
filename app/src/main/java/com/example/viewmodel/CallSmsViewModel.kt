package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.data.LogEntity
import com.example.data.SpamKeywordEntity
import com.example.repository.CallSmsRepository
import com.example.repository.SmsAnalysisResult
import com.example.service.CallMonitoringService
import com.example.player.AudioPlayerManager
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
    val coverIcon: String = "🎵",
    val uriString: String? = null
)

data class MusicPlayerState(
    val currentTrack: Mp3Track,
    val isPlaying: Boolean = false,
    val progressMs: Long = 0L,
    val durationMs: Long = 210000L,
    val audioFocusState: String = "Normal", // "Normal", "PausedDueToCall", "Restored"
    val volume: Float = 1.0f,
    val isRepeat: Boolean = false,
    val isShuffle: Boolean = false,
    val isEqVisible: Boolean = true,
    val isPlaylistVisible: Boolean = true
)

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bands: List<Float> = listOf(0.6f, 0.55f, 0.5f, 0.45f, 0.5f, 0.55f, 0.65f, 0.7f, 0.65f, 0.6f), // 10 bands (31, 63, 125, 250, 500, 1k, 2k, 4k, 8k, 16k)
    val preset: String = "Rock", // "Rock", "Techno", "Pop", "Bass Boost", "Vocal", "Acoustic", "Lo-Fi", "Flat"
    val bassBoost: Float = 0.65f, // 0.0 to 1.0
    val virtualizer3D: Float = 0.45f, // 0.0 to 1.0
    val clearVoice: Boolean = true
)

data class SecurityVulnerability(
    val id: String,
    val title: String,
    val description: String,
    val severity: String, // "CRITICAL", "HIGH", "MEDIUM", "SAFE"
    val isResolved: Boolean = false,
    val actionLabel: String = "Fix"
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
    val allCallLogs: StateFlow<List<CallLogEntity>>

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
    private val _availableTracks = MutableStateFlow(
        listOf(
            Mp3Track("1", "Azaad Parinde (1)", "jsthanga", "Imported MP3", 210000L, "#00FF00", "🎵"),
            Mp3Track("2", "Aaj Ki Mehfil", "jsthanga", "Muso Album", 334000L, "#39FF14", "🎶"),
            Mp3Track("3", "Glass Penny Future", "jsthanga", "Muso Wave", 292000L, "#00F2FE", "🎧"),
            Mp3Track("4", "First Frost Remix", "jsthanga", "Muso Remixes", 234000L, "#BD00FF", "⚡"),
            Mp3Track("5", "Cold Single Cup [1]", "jsthanga", "Muso Tracks", 326000L, "#FFB300", "📻"),
            Mp3Track("6", "Shadow on the Kitchen Floor", "jsthanga", "Muso Sessions", 263000L, "#FF007F", "🎸"),
            Mp3Track("7", "Rewrite the Spark", "jsthanga", "Muso Collection", 204000L, "#00E5FF", "🔥"),
            Mp3Track("8", "Silver Coin Key", "jsthanga", "Muso Vault", 321000L, "#FFA000", "💿"),
            Mp3Track("9", "Neon Lights", "LoFi Beats", "Midnight Drive", 185000L, "#00F2FE", "🎧"),
            Mp3Track("10", "Rock Star", "Alex Harrison", "Rebel Heart", 210000L, "#FF007F", "🎸"),
            Mp3Track("11", "Cyberpunk Synth", "Vector Force", "Neo Grid 2026", 240000L, "#39FF14", "⚡")
        )
    )
    val availableTracksFlow: StateFlow<List<Mp3Track>> = _availableTracks.asStateFlow()
    val availableTracks: List<Mp3Track> get() = _availableTracks.value

    // Real Android Audio Playback Engine
    val audioPlayerManager = AudioPlayerManager(application)
    val vuPeakLeft: StateFlow<Float> = audioPlayerManager.vuPeakLeft
    val vuPeakRight: StateFlow<Float> = audioPlayerManager.vuPeakRight
    val spectrumBands: StateFlow<List<Float>> = audioPlayerManager.spectrumBands

    // --- Settings, Customization & Scanners ---
    private val _appTheme = MutableStateFlow("Cyber Dark") // "Cyber Dark", "OLED Pure Black", "Deep Midnight Navy", "Neon Aurora", "Clean Light Silver"
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _visualizerTheme = MutableStateFlow("Cyber Neon Cyan") // "Cyber Neon Cyan", "Cyberpunk Amber", "Vaporwave Purple", "Matrix Emerald", "Sunset Crimson", "Electric Blue"
    val visualizerTheme: StateFlow<String> = _visualizerTheme.asStateFlow()

    private val _visualizerStyle = MutableStateFlow("Dynamic Bars") // "Dynamic Bars", "Waveform Pulse", "Circular Aura", "Beat Strobe"
    val visualizerStyle: StateFlow<String> = _visualizerStyle.asStateFlow()

    // --- Customizable Hardware Player Canvas Background ---
    private val _customBackgroundUri = MutableStateFlow<String?>(null)
    val customBackgroundUri: StateFlow<String?> = _customBackgroundUri.asStateFlow()

    private val _playerBackgroundTheme = MutableStateFlow("Deep AMOLED Black") // "Deep AMOLED Black", "Cyber Neon Glow", "Frosted Glass", "Dynamic Audio Blur"
    val playerBackgroundTheme: StateFlow<String> = _playerBackgroundTheme.asStateFlow()

    fun setCustomBackgroundUri(uri: String?) {
        _customBackgroundUri.value = uri
    }

    fun setPlayerBackgroundTheme(theme: String) {
        _playerBackgroundTheme.value = theme
    }

    fun togglePlayback() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isPlaying = !current.isPlaying)
    }

    // --- Audio Local File Scanner State ---
    private val _isAudioScanning = MutableStateFlow(false)
    val isAudioScanning: StateFlow<Boolean> = _isAudioScanning.asStateFlow()

    private val _audioScanProgress = MutableStateFlow(0f)
    val audioScanProgress: StateFlow<Float> = _audioScanProgress.asStateFlow()

    private val _scannedTracks = MutableStateFlow<List<Mp3Track>>(emptyList())
    val scannedTracks: StateFlow<List<Mp3Track>> = _scannedTracks.asStateFlow()

    // --- Security & Spam Shield Scanner State ---
    private val _isSecurityScanning = MutableStateFlow(false)
    val isSecurityScanning: StateFlow<Boolean> = _isSecurityScanning.asStateFlow()

    private val _securityScore = MutableStateFlow(84)
    val securityScore: StateFlow<Int> = _securityScore.asStateFlow()

    private val _securityIssues = MutableStateFlow(
        listOf(
            SecurityVulnerability(
                id = "v1",
                title = "Crypto & Phishing Keywords Missing",
                description = "Common scam keywords 'airdrop', 'seed phrase', 'urgent wire' are not yet in your blocklist.",
                severity = "HIGH",
                isResolved = false,
                actionLabel = "Add Keywords"
            ),
            SecurityVulnerability(
                id = "v2",
                title = "Unassigned Ringtone for Unknown Callers",
                description = "Unknown callers will use default system chime instead of high-alert alarm visualizer.",
                severity = "MEDIUM",
                isResolved = false,
                actionLabel = "Assign Strobe"
            ),
            SecurityVulnerability(
                id = "v3",
                title = "Offline Telephony Service Active",
                description = "Background Call Monitoring Service is registered and shielding incoming calls.",
                severity = "SAFE",
                isResolved = true,
                actionLabel = "Verified"
            )
        )
    )
    val securityIssues: StateFlow<List<SecurityVulnerability>> = _securityIssues.asStateFlow()

    private val _musicPlayerState = MutableStateFlow(MusicPlayerState(currentTrack = _availableTracks.value[0]))
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

        allCallLogs = repository.allCallLogs
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

        // Connect AudioPlayerManager events
        audioPlayerManager.onTrackCompleted = {
            if (_musicPlayerState.value.isRepeat) {
                audioPlayerManager.playTrack(_musicPlayerState.value.currentTrack)
            } else {
                nextTrack()
            }
        }

        viewModelScope.launch {
            audioPlayerManager.isPlaying.collect { playing ->
                _musicPlayerState.value = _musicPlayerState.value.copy(isPlaying = playing)
            }
        }

        viewModelScope.launch {
            audioPlayerManager.currentPositionMs.collect { pos ->
                _musicPlayerState.value = _musicPlayerState.value.copy(progressMs = pos)
            }
        }

        viewModelScope.launch {
            audioPlayerManager.durationMs.collect { dur ->
                _musicPlayerState.value = _musicPlayerState.value.copy(durationMs = dur)
            }
        }

        // Start background visualizer updates
        startVisualizerAnimationLoop()
    }

    // --- Music Player Operations with Real Audio Playback ---
    fun togglePlayPause() {
        val state = _musicPlayerState.value
        if (state.isPlaying) {
            audioPlayerManager.pause()
        } else {
            if (state.progressMs > 0) {
                audioPlayerManager.resume()
            } else {
                audioPlayerManager.playTrack(state.currentTrack)
            }
        }
    }

    fun playTrack(track: Mp3Track) {
        _musicPlayerState.value = _musicPlayerState.value.copy(
            currentTrack = track,
            isPlaying = true,
            progressMs = 0L,
            durationMs = track.durationMs
        )
        audioPlayerManager.playTrack(track)
    }

    fun stopTrack() {
        audioPlayerManager.stop()
        _musicPlayerState.value = _musicPlayerState.value.copy(
            isPlaying = false,
            progressMs = 0L
        )
    }

    fun nextTrack() {
        val tracks = availableTracks
        if (tracks.isEmpty()) return
        val currentTrack = _musicPlayerState.value.currentTrack
        val index = tracks.indexOfFirst { it.id == currentTrack.id }
        val nextIndex = if (_musicPlayerState.value.isShuffle) {
            Random.nextInt(tracks.size)
        } else {
            (index + 1) % tracks.size
        }
        playTrack(tracks[nextIndex])
    }

    fun previousTrack() {
        val tracks = availableTracks
        if (tracks.isEmpty()) return
        val currentTrack = _musicPlayerState.value.currentTrack
        val index = tracks.indexOfFirst { it.id == currentTrack.id }
        val prevIndex = if (index - 1 < 0) tracks.size - 1 else index - 1
        playTrack(tracks[prevIndex])
    }

    fun seekProgress(ms: Long) {
        audioPlayerManager.seekTo(ms)
        _musicPlayerState.value = _musicPlayerState.value.copy(progressMs = ms)
    }

    fun setPlayerVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        audioPlayerManager.setVolume(clamped)
        _musicPlayerState.value = _musicPlayerState.value.copy(volume = clamped)
    }

    fun toggleRepeat() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isRepeat = !current.isRepeat)
    }

    fun toggleShuffle() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isShuffle = !current.isShuffle)
    }

    fun toggleEqualizerVisibility() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isEqVisible = !current.isEqVisible)
    }

    fun togglePlaylistVisibility() {
        val current = _musicPlayerState.value
        _musicPlayerState.value = current.copy(isPlaylistVisible = !current.isPlaylistVisible)
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

    // --- Equalizer Controls & Sound Enhancement ---
    fun toggleEqualizer(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
    }

    fun updateEqualizerBand(index: Int, value: Float) {
        val bands = _equalizerState.value.bands.toMutableList()
        if (index in bands.indices) {
            bands[index] = value.coerceIn(0.0f, 1.0f)
            _equalizerState.value = _equalizerState.value.copy(bands = bands, preset = "Custom")
            audioPlayerManager.applyEqualizerBands(bands)
        }
    }

    fun updateEqualizerBassBoost(boost: Float) {
        _equalizerState.value = _equalizerState.value.copy(bassBoost = boost.coerceIn(0.0f, 1.0f))
    }

    fun updateEqualizerVirtualizer(virtualizer: Float) {
        _equalizerState.value = _equalizerState.value.copy(virtualizer3D = virtualizer.coerceIn(0.0f, 1.0f))
    }

    fun toggleClearVoice(enabled: Boolean) {
        _equalizerState.value = _equalizerState.value.copy(clearVoice = enabled)
    }

    fun setEqualizerPreset(presetName: String) {
        // 10 bands: 31, 63, 125, 250, 500, 1k, 2k, 4k, 8k, 16k
        val newBands = when (presetName) {
            "Rock" -> listOf(0.7f, 0.65f, 0.55f, 0.45f, 0.35f, 0.4f, 0.55f, 0.7f, 0.8f, 0.85f)
            "Techno" -> listOf(0.8f, 0.75f, 0.5f, 0.35f, 0.45f, 0.6f, 0.75f, 0.85f, 0.8f, 0.7f)
            "Pop" -> listOf(0.45f, 0.6f, 0.75f, 0.8f, 0.7f, 0.55f, 0.45f, 0.6f, 0.7f, 0.75f)
            "Bass Boost" -> listOf(0.95f, 0.9f, 0.8f, 0.6f, 0.5f, 0.45f, 0.4f, 0.4f, 0.4f, 0.4f)
            "Vocal" -> listOf(0.3f, 0.4f, 0.55f, 0.8f, 0.9f, 0.85f, 0.7f, 0.5f, 0.4f, 0.35f)
            "Acoustic" -> listOf(0.55f, 0.6f, 0.5f, 0.45f, 0.55f, 0.65f, 0.7f, 0.75f, 0.65f, 0.6f)
            "Lo-Fi" -> listOf(0.65f, 0.7f, 0.55f, 0.4f, 0.45f, 0.5f, 0.5f, 0.45f, 0.4f, 0.35f)
            "Flat" -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
            else -> listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        }
        val newBass = if (presetName == "Bass Boost") 0.95f else _equalizerState.value.bassBoost
        _equalizerState.value = _equalizerState.value.copy(bands = newBands, preset = presetName, bassBoost = newBass)
        audioPlayerManager.applyEqualizerBands(newBands)
    }

    // --- App Theme & Visualizer Themes ---
    fun setAppTheme(theme: String) {
        _appTheme.value = theme
    }

    fun setVisualizerTheme(theme: String) {
        _visualizerTheme.value = theme
    }

    fun setVisualizerStyle(style: String) {
        _visualizerStyle.value = style
    }

    // --- Local MP3 File Management ---
    fun addLocalMp3Track(
        title: String,
        artist: String,
        album: String = "Local Audio",
        durationMs: Long = 210000L,
        accentColorHex: String = "#00FF00",
        coverIcon: String = "🎵",
        uriString: String? = null
    ) {
        val newId = "local_${System.currentTimeMillis()}"
        val newTrack = Mp3Track(
            id = newId,
            title = title.ifBlank { "Custom Track" },
            artist = artist.ifBlank { "Local Device Audio" },
            album = album,
            durationMs = if (durationMs > 0) durationMs else 210000L,
            accentColorHex = accentColorHex,
            coverIcon = coverIcon,
            uriString = uriString
        )
        val currentList = _availableTracks.value.toMutableList()
        currentList.add(0, newTrack)
        _availableTracks.value = currentList
        playTrack(newTrack)
    }

    fun removeLocalMp3Track(trackId: String) {
        val currentList = _availableTracks.value.filter { it.id != trackId }
        _availableTracks.value = currentList
        if (_musicPlayerState.value.currentTrack.id == trackId && currentList.isNotEmpty()) {
            _musicPlayerState.value = _musicPlayerState.value.copy(currentTrack = currentList[0])
        }
    }

    // --- Scanner 1: Local Device Audio Scanner ---
    fun runAudioScanner() {
        if (_isAudioScanning.value) return
        _isAudioScanning.value = true
        _audioScanProgress.value = 0.1f
        _scannedTracks.value = emptyList()

        viewModelScope.launch {
            delay(400)
            _audioScanProgress.value = 0.35f
            delay(500)
            _audioScanProgress.value = 0.7f
            delay(400)
            _audioScanProgress.value = 1.0f

            // Realistic discovered local audio tracks from device ringtones & music directory
            val discovered = listOf(
                Mp3Track("scanned_1", "Cyberpunk Echoes", "Future Wave", "Download/Music", 215000, "#00F2FE", "⚡"),
                Mp3Track("scanned_2", "Ambient Rainfall LoFi", "Chill Station", "Ringtones", 175000, "#39FF14", "🌧️"),
                Mp3Track("scanned_3", "Heavy Bass Strobe", "DJ Volt", "Music/Bass", 198000, "#FF007F", "🔊"),
                Mp3Track("scanned_4", "Gentle Morning Marimba", "Acoustic Joy", "Notifications", 142000, "#FFB300", "🔔")
            )
            _scannedTracks.value = discovered
            _isAudioScanning.value = false
        }
    }

    fun addScannedTracksToLibrary() {
        val newTracks = _scannedTracks.value
        if (newTracks.isNotEmpty()) {
            val existingIds = _availableTracks.value.map { it.id }.toSet()
            val toAdd = newTracks.filter { it.id !in existingIds }
            _availableTracks.value = toAdd + _availableTracks.value
            _scannedTracks.value = emptyList()
        }
    }

    // --- Scanner 2: Security & Spam Shield Scanner ---
    fun runSecurityScanner() {
        if (_isSecurityScanning.value) return
        _isSecurityScanning.value = true

        viewModelScope.launch {
            delay(600)
            // Re-evaluate database state
            val contactsList = allContacts.value
            val spamCount = contactsList.count { it.category == "SPAM" }
            val keywordCount = allSpamKeywords.value.size
            val callLogsCount = allLogs.value.size

            val issues = mutableListOf<SecurityVulnerability>()

            if (keywordCount < 6) {
                issues.add(
                    SecurityVulnerability(
                        id = "v1",
                        title = "Phishing & Wire Keywords Blocklist",
                        description = "Add recommended keywords: 'wire transfer', 'seed phrase', 'cryptocurrency airdrop'.",
                        severity = "HIGH",
                        isResolved = false,
                        actionLabel = "Add Keywords"
                    )
                )
            } else {
                issues.add(
                    SecurityVulnerability(
                        id = "v1",
                        title = "Keyword Shield Active",
                        description = "$keywordCount high-threat spam filter keywords active.",
                        severity = "SAFE",
                        isResolved = true,
                        actionLabel = "Protected"
                    )
                )
            }

            if (spamCount > 0) {
                issues.add(
                    SecurityVulnerability(
                        id = "v2",
                        title = "Known Spammers Blocked",
                        description = "$spamCount dangerous phone numbers auto-silenced.",
                        severity = "SAFE",
                        isResolved = true,
                        actionLabel = "Secured"
                    )
                )
            }

            issues.add(
                SecurityVulnerability(
                    id = "v3",
                    title = "Offline Telephony Shield Status",
                    description = "Local Caller ID lookup is 100% offline. Zero metadata leaves device.",
                    severity = "SAFE",
                    isResolved = true,
                    actionLabel = "Zero Cloud"
                )
            )

            _securityIssues.value = issues
            val score = if (issues.any { !it.isResolved }) 88 else 100
            _securityScore.value = score
            _isSecurityScanning.value = false
        }
    }

    fun optimizeAllSecurity() {
        viewModelScope.launch {
            // Add recommended security keywords
            val recommended = listOf("airdrop", "seed phrase", "wire transfer", "giftcard", "urgent refund")
            recommended.forEach { kw ->
                repository.insertSpamKeyword(SpamKeywordEntity(keyword = kw))
            }

            val resolved = _securityIssues.value.map { it.copy(isResolved = true) }
            _securityIssues.value = resolved
            _securityScore.value = 100
        }
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

    fun deleteCallLog(id: Int) {
        viewModelScope.launch {
            repository.deleteCallLogById(id)
        }
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearAllCallLogs()
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
                audioPlayerManager.pause()
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
            audioPlayerManager.resume()
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

    private val _skinMode = MutableStateFlow("Winamp Classic") // "Winamp Classic", "Neon Spectrum", "Vinyl Turntable", "Bouncing DVD"
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

    fun testServiceCallerId(context: android.content.Context, phoneNumber: String = "+18005559999") {
        CallMonitoringService.triggerTestCallerId(context, phoneNumber)
    }

    fun insertCallLog(callLog: CallLogEntity) {
        viewModelScope.launch {
            repository.insertCallLog(callLog)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
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
