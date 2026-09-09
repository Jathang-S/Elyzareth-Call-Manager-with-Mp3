package com.example.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CallSmsViewModel
import com.example.viewmodel.Mp3Track
import kotlinx.coroutines.delay

/**
 * WinampClassicPlayerView
 * Faithfully re-creates the legendary Winamp Classic aesthetic:
 * 1. Winamp Main Player Window (Beveled chassis, 7-seg digital LCD timer, stereo LED VU meter,
 *    green marquee display, horizontal orange volume bar, seekbar, classic transport buttons).
 * 2. Winamp Equalizer Window (ON/OFF rocker with green LED, PRESET button, 10 yellow band sliders).
 * 3. Winamp Playlist Window (Black CRT terminal list, track numbers, durations, active highlight,
 *    bottom digital time, [+] add audio file, [-] remove track, mini transport controls).
 */
@Composable
fun WinampClassicPlayerView(
    viewModel: CallSmsViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerState by viewModel.musicPlayerState.collectAsState()
    val eqState by viewModel.equalizerState.collectAsState()
    val availableTracks by viewModel.availableTracksFlow.collectAsState()
    val vuPeakLeft by viewModel.vuPeakLeft.collectAsState()
    val vuPeakRight by viewModel.vuPeakRight.collectAsState()

    val currentTrack = playerState.currentTrack
    val isPlaying = playerState.isPlaying
    val progressMs = playerState.progressMs
    val durationMs = if (playerState.durationMs > 0) playerState.durationMs else (if (currentTrack.durationMs > 0) currentTrack.durationMs else 210000L)
    val volume = playerState.volume

    // Audio file picker launcher for [+] button and [⏏] eject button
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var trackTitle = uri.lastPathSegment?.substringAfterLast("/")?.replace(".mp3", "") ?: "Imported Song"
            var trackArtist = "Local Device Audio"
            var trackDuration = 210000L

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, uri)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                if (!metaTitle.isNullOrBlank()) trackTitle = metaTitle
                if (!metaArtist.isNullOrBlank()) trackArtist = metaArtist
                metaDuration?.toLongOrNull()?.let { if (it > 0) trackDuration = it }
                retriever.release()
            } catch (e: Exception) {
                // Keep default metadata
            }

            viewModel.addLocalMp3Track(
                title = trackTitle,
                artist = trackArtist,
                album = "Imported MP3",
                durationMs = trackDuration,
                accentColorHex = "#00FF00",
                coverIcon = "🎵",
                uriString = uri.toString()
            )
            Toast.makeText(context, "Loaded: $trackTitle", Toast.LENGTH_SHORT).show()
        }
    }

    var showPresetMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF14161C))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // =========================================================================
        // 1. WINAMP MAIN PLAYER WINDOW
        // =========================================================================
        WinampWindowFrame(title = "ELYZARETH CALLER") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Screen: Left (Time & Peak Meter) + Right (Title Marquee & Volume)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // LEFT DISPLAY: 7-segment / LCD Time Counter & Stereo LED VU Meter
                    Box(
                        modifier = Modifier
                            .weight(0.48f)
                            .fillMaxHeight()
                            .winampSunkenBevel()
                            .background(Color(0xFF000000))
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Digital status indicator & time counter
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Green Play Symbol
                                Text(
                                    text = if (isPlaying) "▶" else "❚❚",
                                    color = if (isPlaying) Color(0xFF00FF00) else Color(0xFF006600),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                // Digital 7-Segment MM:SS Display
                                val elapsedSec = (progressMs / 1000).toInt()
                                val min = elapsedSec / 60
                                val sec = elapsedSec % 60
                                val timeText = String.format("%02d:%02d", min, sec)

                                Text(
                                    text = timeText,
                                    color = Color(0xFF00FF00),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.testTag("winamp_lcd_timer")
                                )

                                // KBPS readout
                                Text(
                                    text = "320",
                                    color = Color(0xFF00AA00),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Stereo LED VU Peak Meter (Dual rows: Left & Right channel)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                WinampVuMeterRow(peak = if (isPlaying) vuPeakLeft else 0f)
                                WinampVuMeterRow(peak = if (isPlaying) vuPeakRight else 0f)
                            }
                        }
                    }

                    // RIGHT DISPLAY: Glowing Green Title Screen & Horizontal Volume Slider
                    Box(
                        modifier = Modifier
                            .weight(0.52f)
                            .fillMaxHeight()
                            .winampSunkenBevel()
                            .background(Color(0xFF000000))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Track Title Marquee Box
                            val trackIndex = availableTracks.indexOfFirst { it.id == currentTrack.id }.let { if (it >= 0) it + 59 else 59 }
                            val marqueeText = "$trackIndex. ${currentTrack.title} - ${currentTrack.artist} (${formatTime(durationMs)})"

                            Text(
                                text = marqueeText,
                                color = Color(0xFF00FF00),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("winamp_marquee_title")
                            )

                            // Volume Bar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = Color(0xFF9E9E9E),
                                    modifier = Modifier.size(15.dp)
                                )

                                // Custom Orange Grooved Slider
                                WinampVolumeSlider(
                                    value = volume,
                                    onValueChange = { viewModel.setPlayerVolume(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .testTag("winamp_volume_slider")
                                )

                                Text(
                                    text = "${(volume * 100).toInt()}%",
                                    color = Color(0xFFD0D0D0),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Seek Progress Bar (Horizontal Bronze Track with Metallic Thumb)
                WinampSeekBar(
                    progressMs = progressMs,
                    durationMs = durationMs,
                    onSeek = { viewModel.seekProgress(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .testTag("winamp_seek_bar")
                )

                // Classic Winamp Transport Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // |<< Prev
                    WinampButton(
                        text = "|<<",
                        onClick = { viewModel.previousTrack() },
                        modifier = Modifier.testTag("winamp_btn_prev")
                    )

                    // ▶ Play
                    WinampButton(
                        text = "▶",
                        isActive = isPlaying,
                        onClick = {
                            if (!isPlaying) viewModel.togglePlayPause()
                        },
                        modifier = Modifier.testTag("winamp_btn_play")
                    )

                    // ❚❚ Pause
                    WinampButton(
                        text = "❚❚",
                        isActive = !isPlaying && progressMs > 0,
                        onClick = {
                            if (isPlaying) viewModel.togglePlayPause()
                        },
                        modifier = Modifier.testTag("winamp_btn_pause")
                    )

                    // ■ Stop
                    WinampButton(
                        text = "■",
                        onClick = { viewModel.stopTrack() },
                        modifier = Modifier.testTag("winamp_btn_stop")
                    )

                    // >>| Next
                    WinampButton(
                        text = ">>|",
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.testTag("winamp_btn_next")
                    )

                    // ⏏ Eject / Open Audio File
                    WinampButton(
                        text = "⏏",
                        onClick = { audioPickerLauncher.launch("audio/*") },
                        modifier = Modifier.testTag("winamp_btn_eject")
                    )

                    // 🔁 Repeat Toggle
                    WinampButton(
                        text = "🔁",
                        isActive = playerState.isRepeat,
                        onClick = { viewModel.toggleRepeat() },
                        modifier = Modifier.testTag("winamp_btn_repeat")
                    )

                    // 🔀 Shuffle Toggle
                    WinampButton(
                        text = "🔀",
                        isActive = playerState.isShuffle,
                        onClick = { viewModel.toggleShuffle() },
                        modifier = Modifier.testTag("winamp_btn_shuffle")
                    )

                    // EQ Toggle
                    WinampButton(
                        text = "EQ",
                        isActive = playerState.isEqVisible,
                        onClick = { viewModel.toggleEqualizerVisibility() },
                        modifier = Modifier.testTag("winamp_btn_eq_toggle")
                    )

                    // PL Toggle
                    WinampButton(
                        text = "PL",
                        isActive = playerState.isPlaylistVisible,
                        onClick = { viewModel.togglePlaylistVisibility() },
                        modifier = Modifier.testTag("winamp_btn_pl_toggle")
                    )
                }
            }
        }

        // =========================================================================
        // 2. WINAMP EQUALIZER WINDOW
        // =========================================================================
        AnimatedVisibility(visible = playerState.isEqVisible) {
            WinampWindowFrame(title = "ELYZARETH EQUALIZER") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // ON / OFF Toggle button with green LED
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            WinampButton(
                                text = if (eqState.isEnabled) "ON" else "OFF",
                                isActive = eqState.isEnabled,
                                onClick = { viewModel.toggleEqualizer(!eqState.isEnabled) },
                                modifier = Modifier.testTag("winamp_eq_power_btn")
                            )

                            // Green LED lamp
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (eqState.isEnabled) Color(0xFF00FF00) else Color(0xFF333333))
                                    .border(1.dp, Color(0xFF111111), RoundedCornerShape(4.dp))
                            )
                        }

                        // PRESET / PRO Button with dropdown selector
                        Box {
                            WinampButton(
                                text = "PRESET: ${eqState.preset.uppercase()}",
                                onClick = { showPresetMenu = true },
                                modifier = Modifier.testTag("winamp_eq_preset_btn")
                            )

                            DropdownMenu(
                                expanded = showPresetMenu,
                                onDismissRequest = { showPresetMenu = false },
                                modifier = Modifier.background(Color(0xFF20232B))
                            ) {
                                listOf("Rock", "Techno", "Pop", "Bass Boost", "Vocal", "Acoustic", "Lo-Fi", "Flat").forEach { preset ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = preset,
                                                color = if (eqState.preset == preset) Color(0xFF00FF00) else Color.White,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        },
                                        onClick = {
                                            viewModel.setEqualizerPreset(preset)
                                            showPresetMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 10-Band Graphic Equalizer: Vertical Yellow Sliders
                    val freqLabels = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")
                    val bands = eqState.bands

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .winampSunkenBevel()
                            .background(Color(0xFF0D0E12))
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    ) {
                        // 0dB Center horizontal guide line
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerY = size.height / 2f
                            drawLine(
                                color = Color(0xFF3A3F4C),
                                start = Offset(0f, centerY),
                                end = Offset(size.width, centerY),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // 10 Yellow Sliders Row
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            freqLabels.forEachIndexed { index, label ->
                                val bandVal = if (index in bands.indices) bands[index] else 0.5f

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Vertical Yellow Slider Track
                                    WinampVerticalSlider(
                                        value = bandVal,
                                        onValueChange = { newVal ->
                                            viewModel.updateEqualizerBand(index, newVal)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .width(16.dp)
                                            .testTag("winamp_eq_band_$index")
                                    )

                                    // Frequency Label
                                    Text(
                                        text = label,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 3. WINAMP PLAYLIST WINDOW
        // =========================================================================
        AnimatedVisibility(visible = playerState.isPlaylistVisible) {
            WinampWindowFrame(title = "ELYZARETH PLAYLIST") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Track list CRT terminal screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .winampSunkenBevel()
                            .background(Color(0xFF000000))
                    ) {
                        val listState = rememberLazyListState()

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        ) {
                            itemsIndexed(availableTracks) { index, track ->
                                val isTrackPlaying = track.id == currentTrack.id
                                val trackNum = index + 59 // Winamp style starting track index
                                val trackDurationStr = formatTime(track.durationMs)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (isTrackPlaying) Color(0xFF003814) else Color.Transparent
                                        )
                                        .clickable {
                                            viewModel.playTrack(track)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                        .testTag("winamp_track_item_$index"),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Track Index and Title
                                    Text(
                                        text = "$trackNum. ${track.title} - ${track.artist}",
                                        color = if (isTrackPlaying) Color(0xFF39FF14) else Color(0xFF00FF00),
                                        fontSize = 11.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isTrackPlaying) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Duration
                                    Text(
                                        text = trackDurationStr,
                                        color = if (isTrackPlaying) Color(0xFF39FF14) else Color(0xFF00DD00),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Playlist Bottom Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Sunken LCD Digital Time Counter
                        Box(
                            modifier = Modifier
                                .winampSunkenBevel()
                                .background(Color(0xFF000000))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            val elapsedStr = formatTime(progressMs)
                            val totalStr = formatTime(durationMs)
                            Text(
                                text = "$elapsedStr / $totalStr",
                                color = Color(0xFF00FF00),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Center: Action Buttons [+ Add MP3], [- Remove]
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            WinampButton(
                                text = "+ ADD",
                                onClick = { audioPickerLauncher.launch("audio/*") },
                                modifier = Modifier.testTag("winamp_pl_add_btn")
                            )

                            WinampButton(
                                text = "- REM",
                                onClick = { viewModel.removeLocalMp3Track(currentTrack.id) },
                                modifier = Modifier.testTag("winamp_pl_rem_btn")
                            )

                            WinampButton(
                                text = "LIST",
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("winamp_pl_settings_btn")
                            )
                        }

                        // Right: Mini Playback Controls
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            WinampMiniButton(text = "|<") { viewModel.previousTrack() }
                            WinampMiniButton(text = if (isPlaying) "||" else ">") { viewModel.togglePlayPause() }
                            WinampMiniButton(text = "■") { viewModel.stopTrack() }
                            WinampMiniButton(text = ">|") { viewModel.nextTrack() }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// RETRO WINAMP COMPOSABLE BUILDING BLOCKS
// =========================================================================

/**
 * Winamp Window Frame
 * Provides the authentic grooved metallic header, border bevels, and title text.
 */
@Composable
fun WinampWindowFrame(
    title: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .winampRaisedBevel()
            .background(Color(0xFF232630))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Grooved Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF3F4655), Color(0xFF252933))
                        )
                    )
                    .drawBehind {
                        // Subtle horizontal grooved stripes
                        val step = 3.dp.toPx()
                        var y = 2.dp.toPx()
                        while (y < size.height) {
                            drawLine(
                                color = Color(0x22000000),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f
                            )
                            y += step
                        }
                    }
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left close/minimize square
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .winampRaisedBevel()
                        .background(Color(0xFF333742)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("-", color = Color(0xFFD0D0D0), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }

                // Window Title
                Text(
                    text = title,
                    color = Color(0xFFE0E5F0),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                // Right window button
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .winampRaisedBevel()
                        .background(Color(0xFF333742)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("x", color = Color(0xFFD0D0D0), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Window Contents
            content()
        }
    }
}

/**
 * Winamp Stereo VU Meter Row
 * Renders 14 LED blocks: Green -> Yellow -> Red
 */
@Composable
fun WinampVuMeterRow(peak: Float) {
    val totalSegments = 14
    val litSegments = (peak.coerceIn(0f, 1f) * totalSegments).toInt()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        for (i in 0 until totalSegments) {
            val isLit = i < litSegments
            val segColor = when {
                i >= 12 -> if (isLit) Color(0xFFFF1744) else Color(0xFF440000) // Red Peak
                i >= 9 -> if (isLit) Color(0xFFFFEA00) else Color(0xFF443D00)  // Yellow High
                else -> if (isLit) Color(0xFF00E676) else Color(0xFF003814)    // Green Mid/Low
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(segColor)
            )
        }
    }
}

/**
 * Winamp Volume Slider (Horizontal Grooved Amber Bar with Metallic Knob)
 */
@Composable
fun WinampVolumeSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .winampSunkenBevel()
            .background(Color(0xFF0A0C10))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbXPx = with(density) { (value * (widthPx - 14.dp.toPx())).coerceAtLeast(0f) }
        val thumbXDp = with(density) { thumbXPx.toDp() }

        // Orange active fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = value.coerceIn(0f, 1f))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFFFA000), Color(0xFFE65100))
                    )
                )
        )

        // Metallic slider knob
        Box(
            modifier = Modifier
                .offset(x = thumbXDp)
                .width(14.dp)
                .fillMaxHeight()
                .winampRaisedBevel()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF888F9E), Color(0xFF555B68))
                    )
                )
        )
    }
}

/**
 * Winamp Seek Bar
 */
@Composable
fun WinampSeekBar(
    progressMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val fraction = if (durationMs > 0) (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    BoxWithConstraints(
        modifier = modifier
            .winampSunkenBevel()
            .background(Color(0xFF050608))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((frac * durationMs).toLong())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val frac = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek((frac * durationMs).toLong())
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val thumbXPx = with(density) { (fraction * (widthPx - 22.dp.toPx())).coerceAtLeast(0f) }
        val thumbXDp = with(density) { thumbXPx.toDp() }

        // Bronze / Amber Progress Fill
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = fraction)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFD4AF37), Color(0xFF8C7322))
                    )
                )
        )

        // Metallic slider thumb knob
        Box(
            modifier = Modifier
                .offset(x = thumbXDp)
                .width(22.dp)
                .fillMaxHeight()
                .winampRaisedBevel()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFB0B7C6), Color(0xFF6B7280))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // Thumb center indicator line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(8.dp)
                    .background(Color(0xFF22252C))
            )
        }
    }
}

/**
 * Winamp Vertical Equalizer Slider
 */
@Composable
fun WinampVerticalSlider(
    value: Float, // 0.0 to 1.0 (0.5 is 0dB)
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val fraction = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val fraction = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onValueChange(fraction)
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        val density = LocalDensity.current
        val heightPx = constraints.maxHeight.toFloat()
        val thumbYPx = with(density) { ((1f - value) * (heightPx - 16.dp.toPx())).coerceAtLeast(0f) }
        val thumbYDp = with(density) { thumbYPx.toDp() }

        // Center Yellow Track Groove
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Color(0xFFFFD600))
                .align(Alignment.Center)
        )

        // Silver Metallic Horizontal Slider Knob
        Box(
            modifier = Modifier
                .offset(y = thumbYDp)
                .fillMaxWidth()
                .height(14.dp)
                .winampRaisedBevel()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFCCCCCC), Color(0xFF777777))
                    )
                )
                .align(Alignment.TopCenter),
            contentAlignment = Alignment.Center
        ) {
            // Center notch line
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.5.dp)
                    .background(Color(0xFF1A1C22))
            )
        }
    }
}

/**
 * Winamp Square Beveled Push Button
 */
@Composable
fun WinampButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    Box(
        modifier = modifier
            .height(26.dp)
            .let { if (isActive) it.winampSunkenBevel() else it.winampRaisedBevel() }
            .background(
                if (isActive) {
                    Brush.verticalGradient(listOf(Color(0xFF1E222A), Color(0xFF2C323E)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF454B5A), Color(0xFF2D323C)))
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color(0xFF00FF00) else Color(0xFFE0E5F0),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Mini Playlist Transport Button
 */
@Composable
fun WinampMiniButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 22.dp, height = 20.dp)
            .winampRaisedBevel()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF454B5A), Color(0xFF2D323C)))
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFE0E5F0),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

// =========================================================================
// RETRO 3D BEVEL DRAWING EXTENSIONS
// =========================================================================

fun Modifier.winampRaisedBevel(): Modifier = this.drawBehind {
    val stroke = 1.5.dp.toPx()
    // Top highlight
    drawLine(Color(0xFF6B7386), Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
    // Left highlight
    drawLine(Color(0xFF6B7386), Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
    // Bottom shadow
    drawLine(Color(0xFF0A0C10), Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
    // Right shadow
    drawLine(Color(0xFF0A0C10), Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
}

fun Modifier.winampSunkenBevel(): Modifier = this.drawBehind {
    val stroke = 1.5.dp.toPx()
    // Top shadow
    drawLine(Color(0xFF0A0C10), Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
    // Left shadow
    drawLine(Color(0xFF0A0C10), Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), stroke)
    // Bottom highlight
    drawLine(Color(0xFF4D5363), Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
    // Right highlight
    drawLine(Color(0xFF4D5363), Offset(size.width - stroke / 2, 0f), Offset(size.width - stroke / 2, size.height), stroke)
}
