package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.viewmodel.CallSmsViewModel
import com.example.viewmodel.Mp3Track
import kotlin.math.cos
import kotlin.math.sin

/**
 * ElyzarethPlayerScreen
 * A clean, distraction-free hardware music gadget front window.
 * Features:
 * - Zero clutter: No heavy headers, stat cards, or dual-tab navigation.
 * - Full-canvas dynamic background (AMOLED, gradients, or custom gallery photo).
 * - Interactive visualizers: Neon Spectrum, Vinyl Turntable, and Bouncing DVD / Neon Pulse.
 * - Minimal track title marquee and slim scrub bar.
 * - Single floating translucent control icon to open the Settings & Audio Lab.
 */
@Composable
fun ElyzarethPlayerScreen(
    viewModel: CallSmsViewModel,
    onOpenSettings: () -> Unit,
    onOpenQuickDialer: () -> Unit = {}
) {
    val backgroundUri by viewModel.customBackgroundUri.collectAsState()
    val backgroundTheme by viewModel.playerBackgroundTheme.collectAsState()
    val playerState by viewModel.musicPlayerState.collectAsState()
    val skinMode by viewModel.skinMode.collectAsState()
    val visualizerTheme by viewModel.visualizerTheme.collectAsState()
    val amps by viewModel.audioWaveAmplitudes.collectAsState()
    val eqState by viewModel.equalizerState.collectAsState()

    val currentTrack = playerState.currentTrack
    val isPlaying = playerState.isPlaying
    val progressMs = playerState.progressMs
    val durationMs = if (currentTrack.durationMs > 0) currentTrack.durationMs else 210000L
    val progressFraction = (progressMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    // Hardware Volume & Mute Popover state
    var showVolumePopup by remember { mutableStateOf(false) }
    var hardwareVolume by remember { mutableFloatStateOf(0.85f) }
    var isVolumeMuted by remember { mutableStateOf(false) }

    // Visualizer Theme Colors
    val (neonColor1, neonColor2) = when (visualizerTheme) {
        "Cyberpunk Amber" -> Color(0xFFFFB300) to Color(0xFFFF6F00)
        "Vaporwave Purple" -> Color(0xFFBD00FF) to Color(0xFFFF007F)
        "Matrix Emerald" -> Color(0xFF39FF14) to Color(0xFF00E676)
        "Sunset Crimson" -> Color(0xFFFF5722) to Color(0xFFE91E63)
        "Electric Blue" -> Color(0xFF2979FF) to Color(0xFF3D5AFE)
        else -> Color(0xFF00F2FE) to Color(0xFFBD00FF) // Cyber Neon Cyan
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("elyzareth_player_root")
    ) {
        // -------------------------------------------------------------
        // 1. FULL-CANVAS DYNAMIC BACKGROUND
        // -------------------------------------------------------------
        if (backgroundUri != null) {
            AsyncImage(
                model = backgroundUri,
                contentDescription = "Custom Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Multi-stop contrast scrim to keep white text & cyber controls crystal clear on bright or busy photos
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.55f),
                                Color.Black.copy(alpha = 0.70f),
                                Color.Black.copy(alpha = 0.90f)
                            )
                        )
                    )
            )
            // Subtle radial vignette
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        } else {
            // Theme preset brush
            when (backgroundTheme) {
                "Cyber Neon Glow" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF231442),
                                        Color(0xFF0D0A1C),
                                        Color(0xFF05030A)
                                    )
                                )
                            )
                    )
                }
                "Frosted Glass" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF162238),
                                        Color(0xFF0E1624),
                                        Color(0xFF080D14)
                                    )
                                )
                            )
                    )
                }
                "Dynamic Audio Blur" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF0E1A2B),
                                        Color(0xFF1B2838),
                                        Color(0xFF2F1840),
                                        Color(0xFF0E1A2B)
                                    )
                                )
                            )
                    )
                }
                else -> {
                    // "Deep AMOLED Black"
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF000000))
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 2. TOP FLOATING CONTROLS (Zero text on top left, hardware icons on top right)
        // -------------------------------------------------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Subtle Quick Dialer icon
                IconButton(
                    onClick = onOpenQuickDialer,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("quick_dialer_pill_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dialpad,
                        contentDescription = "Quick Keypad",
                        tint = neonColor1,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Translucent Settings Icon (Single floating entry point)
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("floating_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Audio Lab & Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // 3. FOCUSED AUDIO VISUALIZER & MEDIA PLAYER (CENTERED)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visualizer HUD
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(listOf(neonColor1.copy(alpha = 0.6f), neonColor2.copy(alpha = 0.6f))),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .clickable {
                        // Tapping visualizer toggles playback
                        viewModel.togglePlayback()
                    },
                contentAlignment = Alignment.Center
            ) {
                ActiveVisualizerView(
                    skin = skinMode,
                    isPlaying = isPlaying,
                    amplitudes = amps,
                    track = currentTrack,
                    color1 = neonColor1,
                    color2 = neonColor2
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Track Title & Artist
            Text(
                text = currentTrack.title,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${currentTrack.artist} • ${currentTrack.album}",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Slim Scrub Bar & Time Readouts
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progressFraction,
                    onValueChange = { fraction ->
                        viewModel.seekProgress((fraction * durationMs).toLong())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = neonColor1,
                        activeTrackColor = neonColor1,
                        inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .testTag("media_scrub_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(progressMs),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Text(
                        text = formatTime(durationMs),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Volume Popover Slider overlay
            AnimatedVisibility(
                visible = showVolumePopup,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14141E).copy(alpha = 0.96f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { isVolumeMuted = !isVolumeMuted },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isVolumeMuted || hardwareVolume == 0f) {
                                    Icons.AutoMirrored.Filled.VolumeOff
                                } else if (hardwareVolume < 0.5f) {
                                    Icons.AutoMirrored.Filled.VolumeDown
                                } else {
                                    Icons.AutoMirrored.Filled.VolumeUp
                                },
                                contentDescription = "Toggle Mute",
                                tint = if (isVolumeMuted) Color(0xFFFF5252) else neonColor1,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Slider(
                            value = if (isVolumeMuted) 0f else hardwareVolume,
                            onValueChange = {
                                hardwareVolume = it
                                isVolumeMuted = false
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = neonColor1,
                                activeTrackColor = neonColor1,
                                inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .testTag("quick_volume_slider")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isVolumeMuted) "MUTE" else "${(hardwareVolume * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isVolumeMuted) Color(0xFFFF5252) else Color.White,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.End
                        )

                        IconButton(
                            onClick = { showVolumePopup = false },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Volume",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Hardware Media Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interactive Hardware Volume / Mute Button
                IconButton(
                    onClick = {
                        showVolumePopup = !showVolumePopup
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (showVolumePopup) neonColor1.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                        )
                        .border(
                            1.dp,
                            if (showVolumePopup) neonColor1 else Color.Transparent,
                            CircleShape
                        )
                        .testTag("deck_volume_button")
                ) {
                    Icon(
                        imageVector = if (isVolumeMuted || hardwareVolume == 0f) {
                            Icons.AutoMirrored.Filled.VolumeOff
                        } else if (hardwareVolume < 0.5f) {
                            Icons.AutoMirrored.Filled.VolumeDown
                        } else {
                            Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = "Volume Controls",
                        tint = if (isVolumeMuted) Color(0xFFFF5252) else if (showVolumePopup) neonColor1 else Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous Track
                IconButton(
                    onClick = { viewModel.previousTrack() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("player_prev_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Hero Play / Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .shadow(16.dp, CircleShape, spotColor = neonColor1)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(neonColor1, neonColor2)))
                        .clickable { viewModel.togglePlayback() }
                        .testTag("player_play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next Track
                IconButton(
                    onClick = { viewModel.nextTrack() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .testTag("player_next_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Repeat / Crossfade indicator
                IconButton(
                    onClick = {
                        val currentMs = viewModel.crossfadeMs.value
                        viewModel.updateCrossfade(if (currentMs > 0) 0 else 2000)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Crossfade",
                        tint = if (viewModel.crossfadeMs.value > 0) neonColor1 else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * ActiveVisualizerView
 * Renders the chosen skin:
 * 1. Neon Spectrum: 24 responsive frequency bars with floating peak caps
 * 2. Vinyl Turntable: Grooved vinyl disc spinning at 33 RPM with tone arm
 * 3. Bouncing DVD / Neon Pulse: Rhythmic concentric neon sound aura
 */
@Composable
fun ActiveVisualizerView(
    skin: String,
    isPlaying: Boolean,
    amplitudes: List<Float>,
    track: Mp3Track,
    color1: Color,
    color2: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VisualizerAnim")

    when (skin) {
        "Vinyl Turntable" -> {
            val rotationAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(if (isPlaying) 2800 else 18000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "VinylSpin"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Spinning Vinyl Platter
                Canvas(
                    modifier = Modifier
                        .size(210.dp)
                        .rotate(rotationAngle)
                ) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Outer Vinyl Body
                    drawCircle(
                        color = Color(0xFF111111),
                        radius = radius,
                        center = center
                    )

                    // Concentric Grooves
                    for (i in 1..7) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.04f + (i % 2) * 0.03f),
                            radius = radius * (0.35f + i * 0.08f),
                            center = center,
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // Vinyl Center Label
                    drawCircle(
                        brush = Brush.radialGradient(listOf(color1, color2)),
                        radius = radius * 0.32f,
                        center = center
                    )

                    // Center Spindle Hole
                    drawCircle(
                        color = Color(0xFF080808),
                        radius = radius * 0.07f,
                        center = center
                    )
                }

                // Tone Arm overlaid on top
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val armColor = Color(0xFFCCCCCC)
                    val headColor = color1

                    // Tone Arm Base (top right)
                    drawCircle(
                        color = Color(0xFF444444),
                        radius = 12f,
                        center = Offset(size.width * 0.88f, size.height * 0.16f)
                    )

                    // Arm bar
                    drawLine(
                        color = armColor,
                        start = Offset(size.width * 0.88f, size.height * 0.16f),
                        end = Offset(size.width * 0.58f, size.height * 0.50f),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )

                    // Cartridge / Needle head
                    drawRect(
                        color = headColor,
                        topLeft = Offset(size.width * 0.55f, size.height * 0.48f),
                        size = Size(14f, 10f)
                    )
                }
            }
        }

        "Bouncing DVD" -> {
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.82f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(if (isPlaying) 550 else 1600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "NeonPulse"
            )

            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "RingRotate"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val baseRadius = (size.minDimension / 2f) * 0.75f

                // Expanding concentric soundwaves
                drawCircle(
                    color = color1.copy(alpha = 0.25f),
                    radius = baseRadius * pulseScale,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                drawCircle(
                    color = color2.copy(alpha = 0.45f),
                    radius = baseRadius * (pulseScale * 0.78f),
                    center = center,
                    style = Stroke(width = 3.5f)
                )
                drawCircle(
                    color = color1.copy(alpha = 0.65f),
                    radius = baseRadius * (pulseScale * 0.55f),
                    center = center,
                    style = Stroke(width = 4.5f)
                )

                // Glowing Center Core
                drawCircle(
                    brush = Brush.radialGradient(listOf(color1, color2, Color.Transparent)),
                    radius = baseRadius * 0.35f * pulseScale,
                    center = center
                )
            }
        }

        else -> {
            // "Neon Spectrum" (Studio Balanced Proportions)
            val barCount = 18
            val animPhase by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 6.28f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "SpectrumPhase"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                val availableWidth = size.width
                val maxHeight = size.height

                // Clamp peak bar height to ~58% of container height to guarantee 40%+ breathing room at top
                val maxBarHeight = maxHeight * 0.58f
                val minBarHeight = 10.dp.toPx()

                // Wider spacing ratio: 52% bar width, 48% gap between bars for crisp separation
                val barWidth = (availableWidth / barCount) * 0.52f
                val spacing = (availableWidth / barCount) * 0.48f

                // Studio Baseline reference line
                drawLine(
                    color = color1.copy(alpha = 0.25f),
                    start = Offset(0f, maxHeight),
                    end = Offset(availableWidth, maxHeight),
                    strokeWidth = 2f
                )

                for (i in 0 until barCount) {
                    // Soft-dampened composite amplitude calculation
                    val amp = if (isPlaying) {
                        val wave1 = sin(animPhase + i * 0.32f) * 0.5f + 0.5f
                        val wave2 = cos(animPhase * 1.15f + i * 0.48f) * 0.25f + 0.25f
                        val ampFactor = amplitudes.getOrElse(i % amplitudes.size) { 0.5f }
                        val composite = (wave1 * 0.5f + wave2 * 0.2f + ampFactor * 0.3f)
                        composite.coerceIn(0f, 1f)
                    } else {
                        // Gentle resting wave in idle state (15% - 25% of height)
                        (sin(animPhase * 0.5f + i * 0.28f) * 0.15f + 0.20f).coerceIn(0.1f, 0.35f)
                    }

                    // Soft cap keeping bars between 10.dp and 58% of container height
                    val barHeight = (minBarHeight + (amp * (maxBarHeight - minBarHeight))).coerceIn(minBarHeight, maxBarHeight)
                    val x = i * (barWidth + spacing) + spacing / 2f
                    val y = maxHeight - barHeight

                    // Vertical Bar Studio Gradient
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(color1, color2),
                            startY = y,
                            endY = maxHeight
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                    )

                    // Peak Floating Indicator Dot (seated comfortably with headroom)
                    val peakY = (y - 7f).coerceAtLeast(maxHeight - maxBarHeight - 10f)
                    drawCircle(
                        color = Color.White,
                        radius = barWidth * 0.45f,
                        center = Offset(x + barWidth / 2f, peakY)
                    )

                    // Subtle neon peak glow
                    drawCircle(
                        color = color1.copy(alpha = 0.45f),
                        radius = barWidth * 0.75f,
                        center = Offset(x + barWidth / 2f, peakY)
                    )
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
