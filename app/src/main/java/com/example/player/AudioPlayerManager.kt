package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import com.example.viewmodel.Mp3Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * AudioPlayerManager
 * Manages real Android MediaPlayer playback, real audio synthesis for demo tracks,
 * hardware volume control, and dynamic audio visualizer / VU-meter signals.
 */
class AudioPlayerManager(private val context: Context) {

    private val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    // Audio status flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(210000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // 16-band dynamic VU / spectrum levels (0f to 1f)
    private val _spectrumBands = MutableStateFlow(List(16) { 0f })
    val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

    // Stereo VU meter peak levels (Left, Right)
    private val _vuPeakLeft = MutableStateFlow(0f)
    val vuPeakLeft: StateFlow<Float> = _vuPeakLeft.asStateFlow()

    private val _vuPeakRight = MutableStateFlow(0f)
    val vuPeakRight: StateFlow<Float> = _vuPeakRight.asStateFlow()

    var onTrackCompleted: (() -> Unit)? = null

    private var synthWavFile: File? = null

    init {
        // Pre-generate a warm retro audio loop so offline demo tracks always produce rich sound
        coroutineScope.launch(Dispatchers.IO) {
            try {
                synthWavFile = generateWarmRetroWav(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate synth wav", e)
            }
        }
    }

    fun playTrack(track: Mp3Track) {
        releasePlayer()

        val player = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

        var sourceConfigured = false

        // 1. Try local content/file URI if provided
        if (!track.uriString.isNullOrBlank()) {
            try {
                val uri = Uri.parse(track.uriString)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    player.setDataSource(pfd.fileDescriptor)
                    sourceConfigured = true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not open URI: ${track.uriString}, falling back to synth audio", e)
            }
        }

        // 2. Fallback to generated high quality retro audio loop
        if (!sourceConfigured) {
            try {
                val wavFile = synthWavFile ?: generateWarmRetroWav(context).also { synthWavFile = it }
                player.setDataSource(wavFile.absolutePath)
                sourceConfigured = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load fallback audio", e)
            }
        }

        if (!sourceConfigured) {
            _isPlaying.value = false
            return
        }

        try {
            player.prepare()
            val trackDuration = if (player.duration > 0) player.duration.toLong() else track.durationMs
            _durationMs.value = trackDuration
            player.setVolume(_volume.value, _volume.value)
            
            // Attach Equalizer if available
            try {
                equalizer = Equalizer(0, player.audioSessionId).apply {
                    enabled = true
                }
            } catch (e: Exception) {
                Log.d(TAG, "Hardware equalizer not available on this device: ${e.message}")
            }

            player.setOnCompletionListener {
                _isPlaying.value = false
                _currentPositionMs.value = 0L
                onTrackCompleted?.invoke()
            }

            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                _isPlaying.value = false
                true
            }

            player.start()
            mediaPlayer = player
            _isPlaying.value = true
            startProgressLoop()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting playback", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            } else {
                player.start()
                _isPlaying.value = true
                startProgressLoop()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _isPlaying.value = true
                startProgressLoop()
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
        }
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _spectrumBands.value = List(16) { 0f }
        _vuPeakLeft.value = 0f
        _vuPeakRight.value = 0f
    }

    fun seekTo(ms: Long) {
        mediaPlayer?.let { player ->
            try {
                val clamped = ms.coerceIn(0L, _durationMs.value)
                player.seekTo(clamped.toInt())
                _currentPositionMs.value = clamped
            } catch (e: Exception) {
                Log.e(TAG, "Seek error", e)
            }
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        mediaPlayer?.setVolume(clamped, clamped)
    }

    fun applyEqualizerBands(bands: List<Float>) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands.toInt()
            val minEqLevel = eq.bandLevelRange[0]
            val maxEqLevel = eq.bandLevelRange[1]
            val range = maxEqLevel - minEqLevel

            for (i in 0 until minOf(numBands, bands.size)) {
                val normalized = bands[i].coerceIn(0f, 1f)
                val targetLevel = (minEqLevel + (normalized * range)).toInt().toShort()
                eq.setBandLevel(i.toShort(), targetLevel)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to apply EQ band levels: ${e.message}")
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            var phase = 0.0
            while (_isPlaying.value) {
                val player = mediaPlayer
                if (player != null && player.isPlaying) {
                    _currentPositionMs.value = player.currentPosition.toLong()
                    if (player.duration > 0) {
                        _durationMs.value = player.duration.toLong()
                    }

                    // Compute dynamic retro VU meter and spectrum bands
                    phase += 0.25
                    val bass = (sin(phase * 1.5) * 0.4 + 0.6).toFloat().coerceIn(0.1f, 1.0f)
                    val mid = (sin(phase * 2.7) * 0.35 + 0.55).toFloat().coerceIn(0.1f, 1.0f)
                    val treble = (sin(phase * 4.1) * 0.3 + 0.5).toFloat().coerceIn(0.1f, 1.0f)

                    _vuPeakLeft.value = (bass * 0.9f).coerceIn(0.05f, 1f)
                    _vuPeakRight.value = (mid * 0.85f).coerceIn(0.05f, 1f)

                    _spectrumBands.value = List(16) { index ->
                        val factor = when {
                            index < 4 -> bass * (1.0f - index * 0.08f)
                            index < 10 -> mid * (0.9f - (index - 4) * 0.05f)
                            else -> treble * (0.8f - (index - 10) * 0.07f)
                        }
                        (factor * _volume.value).coerceIn(0.02f, 1f)
                    }
                }
                delay(80)
            }
            // Clear meter on pause
            _vuPeakLeft.value = 0f
            _vuPeakRight.value = 0f
            _spectrumBands.value = List(16) { 0f }
        }
    }

    fun release() {
        progressJob?.cancel()
        releasePlayer()
        coroutineScope.cancel()
    }

    private fun releasePlayer() {
        try {
            equalizer?.release()
            equalizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
    }

    companion object {
        /**
         * Generates a pleasant 10-second 44.1kHz stereo 16-bit PCM WAV audio file with
         * rich retro synth chords (C - G - Am - F) and smooth bass, ensuring authentic,
         * audible music plays on device speakers.
         */
        fun generateWarmRetroWav(context: Context): File {
            val file = File(context.cacheDir, "retro_winamp_synth.wav")
            if (file.exists() && file.length() > 10000) {
                return file
            }

            val sampleRate = 44100
            val durationSeconds = 12
            val totalSamples = sampleRate * durationSeconds
            val bytesPerSample = 2
            val channels = 2
            val dataSize = totalSamples * channels * bytesPerSample

            val pcmData = ByteArray(dataSize)
            val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

            // Chord frequencies (Hz) for 4 progressions (3 seconds each):
            // C major (C4, E4, G4), G major (G3, B3, D4), A minor (A3, C4, E4), F major (F3, A3, C4)
            val chordProgressions = listOf(
                listOf(130.81, 261.63, 329.63, 392.00), // C3, C4, E4, G4
                listOf(98.00, 196.00, 246.94, 293.66),  // G2, G3, B3, D4
                listOf(110.00, 220.00, 261.63, 329.63), // A2, A3, C4, E4
                listOf(87.31, 174.61, 220.00, 261.63)   // F2, F3, A3, C4
            )

            for (i in 0 until totalSamples) {
                val t = i.toDouble() / sampleRate
                val chordIndex = ((t / 3.0).toInt()) % chordProgressions.size
                val chord = chordProgressions[chordIndex]
                val chordT = (t % 3.0)

                // Soft envelope with gentle attack and release
                val env = when {
                    chordT < 0.1 -> chordT / 0.1
                    chordT > 2.8 -> (3.0 - chordT) / 0.2
                    else -> 1.0
                }

                // Synth mix: Root bass + 3 chord voices + gentle sub-bass beat pulse
                var sampleLeft = 0.0
                var sampleRight = 0.0

                // Bassline (sawtooth/sine mix)
                val bassFreq = chord[0]
                val bassWave = sin(2.0 * PI * bassFreq * t) * 0.35 + (2.0 * ((t * bassFreq) % 1.0) - 1.0) * 0.15

                // Pad voices (smooth warm sine waves with slight detuning for analog chorus width)
                val voice1 = sin(2.0 * PI * chord[1] * t) * 0.22
                val voice2 = sin(2.0 * PI * chord[2] * t * 1.002) * 0.20
                val voice3 = sin(2.0 * PI * chord[3] * t * 0.998) * 0.18

                // Subtle rhythmic 4-on-the-floor kick pulse
                val beatPhase = (t * 2.0) % 1.0
                val kick = if (beatPhase < 0.08) sin(2.0 * PI * 65.0 * (1.0 - beatPhase / 0.08) * beatPhase) * 0.4 else 0.0

                val mixed = (bassWave + voice1 + voice2 + voice3) * env * 0.75 + kick
                val clamped = (mixed * 24000.0).coerceIn(-32000.0, 32000.0).toInt().toShort()

                buffer.putShort(clamped)
                buffer.putShort(clamped)
            }

            FileOutputStream(file).use { fos ->
                writeWavHeader(fos, channels, sampleRate, bytesPerSample * 8, dataSize)
                fos.write(pcmData)
            }

            return file
        }

        private fun writeWavHeader(
            out: FileOutputStream,
            channels: Int,
            sampleRate: Int,
            bitsPerSample: Int,
            dataSize: Int
        ) {
            val totalDataLen = dataSize + 36
            val byteRate = sampleRate * channels * bitsPerSample / 8
            val header = ByteArray(44)

            header[0] = 'R'.code.toByte()
            header[1] = 'I'.code.toByte()
            header[2] = 'F'.code.toByte()
            header[3] = 'F'.code.toByte()
            header[4] = (totalDataLen and 0xff).toByte()
            header[5] = ((totalDataLen shr 8) and 0xff).toByte()
            header[6] = ((totalDataLen shr 16) and 0xff).toByte()
            header[7] = ((totalDataLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte()
            header[9] = 'A'.code.toByte()
            header[10] = 'V'.code.toByte()
            header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte()
            header[13] = 'm'.code.toByte()
            header[14] = 't'.code.toByte()
            header[15] = ' '.code.toByte()
            header[16] = 16 // SubChunk1Size (16 for PCM)
            header[17] = 0
            header[18] = 0
            header[19] = 0
            header[20] = 1 // AudioFormat 1 = PCM
            header[21] = 0
            header[22] = channels.toByte()
            header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
            header[33] = 0
            header[34] = bitsPerSample.toByte()
            header[35] = 0
            header[36] = 'd'.code.toByte()
            header[37] = 'a'.code.toByte()
            header[38] = 't'.code.toByte()
            header[39] = 'a'.code.toByte()
            header[40] = (dataSize and 0xff).toByte()
            header[41] = ((dataSize shr 8) and 0xff).toByte()
            header[42] = ((dataSize shr 16) and 0xff).toByte()
            header[43] = ((dataSize shr 24) and 0xff).toByte()

            out.write(header, 0, 44)
        }
    }
}
