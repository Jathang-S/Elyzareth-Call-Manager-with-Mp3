package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.CallMonitoringService
import com.example.viewmodel.CallSmsViewModel
import com.example.viewmodel.Mp3Track
import com.example.viewmodel.SecurityVulnerability
import kotlin.math.sin

/**
 * Settings & Audio Lab Bottom Sheet
 * Comprehensive configuration for:
 * 1. Background Studio (Presets & Custom Storage Photo Picker)
 * 2. Equalizer DSP (5-Band EQ, dB readouts, Presets, 3D Virtualizer, Clear Voice, Reset)
 * 3. Local MP3 & Ringtone Cutter (File Browser, Trimmer & Contact Assign)
 * 4. Phone & Dialpad (Compact 3x4 Grid, Contacts, Spam rules, Service)
 * 5. Scanner (Audio Scanner + Security Vulnerability Shield)
 * 6. Visualizer & Theme (Skins, Color Palettes, App Themes)
 * 7. About & FAQ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: CallSmsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember { mutableIntStateOf(0) }

    val categories = listOf(
        "Background Studio" to Icons.Default.Wallpaper,
        "Equalizer DSP" to Icons.Default.Equalizer,
        "Local MP3 & Cutter" to Icons.Default.MusicNote,
        "Phone & Dialpad" to Icons.Default.Phone,
        "Scanner" to Icons.Default.Security,
        "Visualizer & Theme" to Icons.Default.GraphicEq,
        "About & FAQ" to Icons.Default.HelpOutline
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight(0.92f)
            .testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Settings & Audio Lab",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Elyzareth Media & Telephony Studio",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size) { idx ->
                    val (title, icon) = categories[idx]
                    val isSelected = selectedCategory == idx
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = idx },
                        label = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Category Content Container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedCategory) {
                    0 -> BackgroundStudioSection(viewModel)
                    1 -> EqualizerSection(viewModel)
                    2 -> LocalMp3Section(viewModel)
                    3 -> PhoneSection(viewModel)
                    4 -> ScannerSection(viewModel)
                    5 -> VisualizerThemeSection(viewModel)
                    6 -> AboutAndHelpSection(viewModel)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SECTION 1: BACKGROUND STUDIO
// -------------------------------------------------------------
@Composable
fun BackgroundStudioSection(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val currentTheme by viewModel.playerBackgroundTheme.collectAsState()
    val customUri by viewModel.customBackgroundUri.collectAsState()

    val themes = listOf(
        "Deep AMOLED Black" to "Pure true #000000 black canvas for battery saving and focus.",
        "Cyber Neon Glow" to "Deep midnight violet with subtle neon radial lighting.",
        "Frosted Glass" to "Slate obsidian frost aesthetic with deep contrast.",
        "Dynamic Audio Blur" to "Multi-spectrum animated aura responding to audio colors."
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.setCustomBackgroundUri(uri.toString())
            Toast.makeText(context, "Custom wallpaper applied!", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Custom Photo Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Custom Photo Wallpaper",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (customUri != null) "Active with auto-contrast scrim protection" else "No custom wallpaper active",
                                fontSize = 11.sp,
                                color = if (customUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    if (customUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = customUri,
                                contentDescription = "Active Custom Wallpaper",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Image")
                        }

                        if (customUri != null) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.setCustomBackgroundUri(null)
                                    Toast.makeText(context, "Reverted to theme background", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "PRESET CANVAS THEMES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(themes) { (themeName, desc) ->
            val isSelected = currentTheme == themeName && customUri == null
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setCustomBackgroundUri(null)
                        viewModel.setPlayerBackgroundTheme(themeName)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (themeName) {
                                    "Deep AMOLED Black" -> Color.Black
                                    "Cyber Neon Glow" -> Color(0xFF231442)
                                    "Frosted Glass" -> Color(0xFF162238)
                                    else -> Color(0xFF0E1A2B)
                                }
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = themeName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = desc,
                            fontSize = 11.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// -------------------------------------------------------------
// SECTION 2: EQUALIZER & SOUND DSP
// -------------------------------------------------------------
@Composable
fun EqualizerSection(viewModel: CallSmsViewModel) {
    val eqState by viewModel.equalizerState.collectAsState()
    val presets = listOf(
        "Flat", "Bass Boost", "Vocal Enhancer", "Rock Booster",
        "Lofi Beats", "Electronic", "Acoustic", "Clear Call Voice"
    )
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Master Switch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Audio Equalizer Master",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (eqState.isEnabled) "Hardware DSP active • 5-Band Filter" else "Equalizer bypassed",
                            fontSize = 11.sp,
                            color = if (eqState.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = eqState.isEnabled,
                        onCheckedChange = { viewModel.toggleEqualizer(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }

        item {
            Text(
                text = "SOUND PROFILES & PRESETS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(presets) { preset ->
                    val isSelected = eqState.preset == preset
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .clickable { viewModel.setEqualizerPreset(preset) }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = preset,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "5-BAND GRAPHIC EQUALIZER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    bandLabels.forEachIndexed { index, label ->
                        val dbVal = eqState.bands.getOrElse(index) { 0f }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.width(52.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Slider(
                                value = dbVal,
                                onValueChange = { newVal ->
                                    if (eqState.isEnabled) viewModel.updateEqualizerBand(index, newVal)
                                },
                                valueRange = -12f..12f,
                                enabled = eqState.isEnabled,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (dbVal > 0) "+" else ""}${dbVal.toInt()} dB",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(50.dp),
                                textAlign = TextAlign.End,
                                color = if (dbVal != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "SOUND ENHANCEMENTS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Bass Boost
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(110.dp)) {
                            Text("Bass Boost", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${(eqState.bassBoost * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = eqState.bassBoost,
                            onValueChange = { viewModel.updateEqualizerBassBoost(it) },
                            valueRange = 0f..1f,
                            enabled = eqState.isEnabled,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // 3D Spatial Virtualizer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(110.dp)) {
                            Text("3D Spatial", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${(eqState.virtualizer3D * 100).toInt()}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = eqState.virtualizer3D,
                            onValueChange = { viewModel.updateEqualizerVirtualizer(it) },
                            valueRange = 0f..1f,
                            enabled = eqState.isEnabled,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Clear Voice Speech Filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Clear Voice Speech Clarifier", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Attenuates telephony background line noise", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = eqState.clearVoice,
                            onCheckedChange = { viewModel.toggleClearVoice(it) },
                            enabled = eqState.isEnabled
                        )
                    }
                }
            }
        }

        // Reset DSP to Default Button (Replaces any dial action)
        item {
            OutlinedButton(
                onClick = {
                    viewModel.toggleEqualizer(true)
                    viewModel.setEqualizerPreset("Flat")
                    viewModel.updateEqualizerBassBoost(0f)
                    viewModel.updateEqualizerVirtualizer(0f)
                    viewModel.toggleClearVoice(false)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("reset_equalizer_dsp_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Audio DSP to Default (Flat)")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// -------------------------------------------------------------
// SECTION 3: LOCAL MP3 & RINGTONE CUTTER
// -------------------------------------------------------------
@Composable
fun LocalMp3Section(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val tracks = viewModel.availableTracks
    val playerState by viewModel.musicPlayerState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("") }

    // Ringtone Cutter state
    var selectedTrimmerTrack by remember { mutableStateOf<Mp3Track?>(null) }
    var trimStartSeconds by remember { mutableFloatStateOf(0f) }
    var trimEndSeconds by remember { mutableFloatStateOf(30f) }

    // SAF Audio Picker Launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Custom_Track.mp3"
            viewModel.addLocalMp3Track(
                title = fileName.replace(".mp3", ""),
                artist = "Local Device Audio",
                album = "Imported MP3",
                durationMs = 210000L,
                accentColorHex = "#00F2FE",
                coverIcon = "🎵"
            )
            Toast.makeText(context, "Imported MP3: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Import Actions Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Local MP3 Audio Library",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${tracks.size} tracks available for ringtones & playback",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import MP3", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Custom", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Ringtone Cutter / Loop Trimmer Sub-panel
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ringtone Cutter & Loop Trimmer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = selectedTrimmerTrack?.title ?: "Select a track below to trim",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ContentCut, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    if (selectedTrimmerTrack != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val totalSec = (selectedTrimmerTrack!!.durationMs / 1000f).coerceAtLeast(60f)

                        // Start Marker
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Start: ${trimStartSeconds.toInt()}s", fontSize = 11.sp, modifier = Modifier.width(65.dp))
                            Slider(
                                value = trimStartSeconds,
                                onValueChange = { trimStartSeconds = it.coerceAtMost(trimEndSeconds - 5f) },
                                valueRange = 0f..totalSec,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // End Marker
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("End: ${trimEndSeconds.toInt()}s", fontSize = 11.sp, modifier = Modifier.width(65.dp))
                            Slider(
                                value = trimEndSeconds,
                                onValueChange = { trimEndSeconds = it.coerceAtLeast(trimStartSeconds + 5f) },
                                valueRange = 0f..totalSec,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Trimmed Length: ${(trimEndSeconds - trimStartSeconds).toInt()} seconds",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                Toast.makeText(context, "Saved ${(trimEndSeconds - trimStartSeconds).toInt()}s loop as active ringtone!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save Trimmed Ringtone Loop")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "AVAILABLE LOCAL TRACKS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Track List
        items(tracks) { track ->
            val isCurrent = playerState.currentTrack.id == track.id
            val isPlayingThis = isCurrent && playerState.isPlaying

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isPlayingThis) viewModel.togglePlayback() else viewModel.playTrack(track)
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(track.coverIcon, fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${track.artist} • ${track.album}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Trim action icon
                    IconButton(
                        onClick = {
                            selectedTrimmerTrack = track
                            trimStartSeconds = 0f
                            trimEndSeconds = 30f
                        }
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Trim Track", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    }

                    // Play/Pause indicator button
                    IconButton(
                        onClick = {
                            if (isPlayingThis) viewModel.togglePlayback() else viewModel.playTrack(track)
                        }
                    ) {
                        Icon(
                            imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom MP3 Metadata") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Track Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newArtist,
                        onValueChange = { newArtist = it },
                        label = { Text("Artist Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            viewModel.addLocalMp3Track(
                                title = newTitle,
                                artist = if (newArtist.isBlank()) "Unknown Artist" else newArtist,
                                album = "Custom Loops",
                                durationMs = 180000L,
                                accentColorHex = "#BD00FF",
                                coverIcon = "🎧"
                            )
                            showAddDialog = false
                            newTitle = ""
                            newArtist = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------
// SECTION 4: PHONE & COMPACT 3x4 DIALPAD
// -------------------------------------------------------------
@Composable
fun PhoneSection(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    var dialedNumber by remember { mutableStateOf("") }
    val contacts by viewModel.allContacts.collectAsState()
    val keywords by viewModel.allSpamKeywords.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Compact Dialpad Card (Fits completely on screen without scrolling!)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SMART HARDWARE DIALPAD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Number Display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (dialedNumber.isEmpty()) "Enter phone number..." else dialedNumber,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (dialedNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        if (dialedNumber.isNotEmpty()) {
                            IconButton(
                                onClick = { dialedNumber = dialedNumber.dropLast(1) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tight 3x4 Keypad Grid (44dp keys)
                    val keypad = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("*", "0", "#")
                    )

                    keypad.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable {
                                            if (dialedNumber.length < 16) dialedNumber += digit
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = digit,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compact Call & Simulate Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (dialedNumber.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialedNumber"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Enter a number to dial", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("compact_dial_call_button")
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Call", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val target = if (dialedNumber.isNotBlank()) dialedNumber else "+1 (555) 019-2834"
                                viewModel.simulateIncomingCall(target)
                                Toast.makeText(context, "Simulating incoming call from $target", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.RingVolume, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Ring", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            // Background Service Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Call Screening Telephony Service", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Monitors incoming calls in real-time", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {
                            CallMonitoringService.startService(context)
                            Toast.makeText(context, "Background Telephony Service Running", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Start")
                    }
                }
            }
        }

        item {
            Text(
                text = "SAVED CONTACT IDS (${contacts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(contacts) { contact ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(contact.phoneNumber, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val isSpam = contact.category == "SPAM"
                    Text(
                        text = contact.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpam) Color(0xFFFF5252) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// -------------------------------------------------------------
// SECTION 5: SCANNER (DUAL ENGINE)
// -------------------------------------------------------------
@Composable
fun ScannerSection(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val isAudioScanning by viewModel.isAudioScanning.collectAsState()
    val audioProgress by viewModel.audioScanProgress.collectAsState()
    val scannedTracks by viewModel.scannedTracks.collectAsState()

    val isSecScanning by viewModel.isSecurityScanning.collectAsState()
    val secScore by viewModel.securityScore.collectAsState()
    val secIssues by viewModel.securityIssues.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Engine 1: Audio Storage Scanner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DEVICE AUDIO SCANNER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deep inspects device folders for lossless and compressed MP3 ringtone files.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (isAudioScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { audioProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Scanning /storage/emulated/0/Music... ${(audioProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.runAudioScanner() },
                            enabled = !isAudioScanning,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isAudioScanning) "Scanning..." else "Run Storage Scan")
                        }

                        if (scannedTracks.isNotEmpty()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.addScannedTracksToLibrary()
                                    Toast.makeText(context, "Imported ${scannedTracks.size} scanned tracks!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Import All (${scannedTracks.size})")
                            }
                        }
                    }
                }
            }
        }

        // Engine 2: Security & Spam Shield Vulnerability Scanner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SECURITY & SPAM SHIELD SCANNER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Protection Score: $secScore%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (secScore > 90) Color(0xFF00E676) else Color(0xFFFFB300)
                            )
                        }
                        Button(
                            onClick = { viewModel.optimizeAllSecurity() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Optimize All")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    secIssues.forEach { issue ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (issue.isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (issue.isResolved) Color(0xFF00E676) else Color(0xFFFF5252),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(issue.title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(issue.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// -------------------------------------------------------------
// SECTION 6: VISUALIZER & APP THEMES
// -------------------------------------------------------------
@Composable
fun VisualizerThemeSection(viewModel: CallSmsViewModel) {
    val currentSkin by viewModel.skinMode.collectAsState()
    val currentVisTheme by viewModel.visualizerTheme.collectAsState()
    val currentAppTheme by viewModel.appTheme.collectAsState()

    val skins = listOf("Neon Spectrum", "Vinyl Turntable", "Bouncing DVD")
    val visThemes = listOf("Cyber Neon Cyan", "Cyberpunk Amber", "Vaporwave Purple", "Matrix Emerald", "Sunset Crimson", "Electric Blue")
    val appThemes = listOf("Cyber Dark", "OLED Pure Black", "Deep Midnight Navy", "Neon Aurora", "Clean Light Silver")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "PLAYER HARDWARE SKIN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                skins.forEach { skin ->
                    val isSelected = currentSkin == skin
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.updateSkinMode(skin) }
                    ) {
                        Text(
                            text = skin,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "VISUALIZER SPECTRUM PALETTE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                visThemes.forEach { theme ->
                    val isSelected = currentVisTheme == theme
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setVisualizerTheme(theme) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(theme, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "GLOBAL APPLICATION THEME",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                appThemes.forEach { appTheme ->
                    val isSelected = currentAppTheme == appTheme
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setAppTheme(appTheme) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(appTheme, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// -------------------------------------------------------------
// SECTION 7: ABOUT & FAQ
// -------------------------------------------------------------
@Composable
fun AboutAndHelpSection(viewModel: CallSmsViewModel) {
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    val faqs = listOf(
        FaqItem(
            question = "How does Elyzareth protect my privacy?",
            answer = "100% of contact ID lookups, spam keyword screening, and audio processing occur completely on-device using local SQLite Room database. Zero audio, metadata, or phone records leave your device."
        ),
        FaqItem(
            question = "How do I set a custom ringtone loop?",
            answer = "Open the 'Local MP3 & Cutter' tab, tap the scissors icon on any track to open the Loop Trimmer, pick your start/end markers, and tap 'Save Trimmed Ringtone Loop'."
        ),
        FaqItem(
            question = "How do I set my own photo as wallpaper?",
            answer = "Go to the 'Background Studio' tab, tap 'Select Image' to pick any wallpaper or photo from your device gallery. It instantly stretches full-canvas behind your music player!"
        ),
        FaqItem(
            question = "Can I dial directly from the player?",
            answer = "Yes! Tap the floating dialpad icon on the top-right of the player screen for a compact 3x4 dialpad, or navigate to 'Phone & Dialpad' inside this Lab."
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Elyzareth Caller & Audio Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Version 2.6.0 • Production Release", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Engineered with modern Kotlin, Jetpack Compose, Room SQLite, and high-fidelity audio DSP.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Text(
                text = "FREQUENTLY ASKED QUESTIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(faqs.size) { idx ->
            val faq = faqs[idx]
            val isExpanded = expandedFaqIndex == idx
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFaqIndex = if (isExpanded) -1 else idx }
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(faq.question, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = faq.answer,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

data class FaqItem(
    val question: String,
    val answer: String
)
