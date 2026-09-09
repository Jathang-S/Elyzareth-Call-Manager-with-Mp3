package com.example.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ContactEntity
import com.example.data.LogEntity
import com.example.data.SpamKeywordEntity
import com.example.viewmodel.CallSmsViewModel
import com.example.viewmodel.SimulatedCall
import com.example.viewmodel.SimulatedSms
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(2) } // 0=Phone, 1=Messages, 2=Music, 3=Settings, 4=Premium
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQuickDialerDialog by remember { mutableStateOf(false) }

    // Collect Simulation & Lookup States
    val activeSimulatedCall by viewModel.simulatedCall.collectAsState()
    val activeSimulatedSms by viewModel.simulatedSms.collectAsState()

    // System Permissions Launcher
    val requiredPermissions = remember {
        mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val smsOk = results[Manifest.permission.RECEIVE_SMS] ?: false
        val phoneOk = results[Manifest.permission.READ_PHONE_STATE] ?: false
        val logOk = results[Manifest.permission.READ_CALL_LOG] ?: false
        
        if (smsOk && phoneOk && logOk) {
            Toast.makeText(context, "Telephony shield active.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    val navTabs = listOf(
        Triple("Phone", Icons.Default.Phone, "tab_phone"),
        Triple("Messages", Icons.Default.Chat, "tab_messages"),
        Triple("Music", Icons.Default.MusicNote, "tab_music"),
        Triple("Settings", Icons.Default.Tune, "tab_settings"),
        Triple("Premium", Icons.Default.WorkspacePremium, "tab_premium")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D0B14).copy(alpha = 0.96f),
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .testTag("app_bottom_nav_bar")
            ) {
                navTabs.forEachIndexed { index, (label, icon, testTag) ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00F2FE),
                            selectedTextColor = Color(0xFF00F2FE),
                            indicatorColor = Color(0xFF00F2FE).copy(alpha = 0.18f),
                            unselectedIconColor = Color.White.copy(alpha = 0.55f),
                            unselectedTextColor = Color.White.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.testTag(testTag)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 5 Primary Screens
            when (selectedTab) {
                0 -> PhoneTabScreen(
                    viewModel = viewModel,
                    onOpenDialpad = { showQuickDialerDialog = true }
                )
                1 -> MessagesTabScreen(viewModel = viewModel)
                2 -> ElyzarethPlayerScreen(
                    viewModel = viewModel,
                    onOpenSettings = { selectedTab = 3 },
                    onOpenQuickDialer = { showQuickDialerDialog = true }
                )
                3 -> SettingsTabScreen(viewModel = viewModel)
                4 -> PremiumTabScreen(viewModel = viewModel)
            }

            // Settings Modal Bottom Sheet (if explicitly opened)
            if (showSettingsSheet) {
                SettingsSheet(
                    viewModel = viewModel,
                    onDismiss = { showSettingsSheet = false }
                )
            }

            // Compact Smart Keypad Modal
            if (showQuickDialerDialog) {
                QuickCompactDialerDialog(
                    viewModel = viewModel,
                    onDismiss = { showQuickDialerDialog = false }
                )
            }

            // Realistic Call & SMS Incoming Overlays
            activeSimulatedCall?.let { simCall ->
                SimulatedCallHUD(
                    simCall = simCall,
                    onDismiss = { viewModel.dismissSimulatedCall() }
                )
            }

            activeSimulatedSms?.let { simSms ->
                SimulatedSmsHUD(
                    simSms = simSms,
                    onDismiss = { viewModel.dismissSimulatedSms() }
                )
            }
        }
    }
}

@Composable
fun QuickCompactDialerDialog(
    viewModel: CallSmsViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var dialedNumber by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMART HARDWARE KEYPAD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Display number
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (dialedNumber.isEmpty()) "Dial number..." else dialedNumber,
                        fontSize = 16.sp,
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

                // 3x4 Grid (compact)
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
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
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

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (dialedNumber.isNotBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dialedNumber"))
                                context.startActivity(intent)
                                onDismiss()
                            } else {
                                Toast.makeText(context, "Enter phone number", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676), contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val target = if (dialedNumber.isNotBlank()) dialedNumber else "+1 (555) 019-2834"
                            viewModel.simulateIncomingCall(target)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(Icons.Default.RingVolume, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}


// --- Stats Summary Header Component ---
@Composable
fun StatsSummaryHeader(
    contactsCount: Int,
    spammersCount: Int,
    keywordsCount: Int,
    logsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "All IDs",
            count = contactsCount.toString(),
            color = MaterialTheme.colorScheme.primaryContainer,
            textColor = MaterialTheme.colorScheme.onPrimaryContainer,
            icon = Icons.Default.ContactPhone,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Spammers",
            count = spammersCount.toString(),
            color = Color(0xFFFDE8E8),
            textColor = Color(0xFF9B1C1C),
            icon = Icons.Default.ReportProblem,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Keywords",
            count = keywordsCount.toString(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            textColor = MaterialTheme.colorScheme.onSecondaryContainer,
            icon = Icons.Default.Label,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Logs",
            count = logsCount.toString(),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            textColor = MaterialTheme.colorScheme.onTertiaryContainer,
            icon = Icons.Default.History,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    color: Color,
    textColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor.copy(alpha = 0.8f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = count,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Permission Requester Banner ---
@Composable
fun PermissionBanner(onRequest: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Permission Info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Background Interceptors Disabled",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "App needs SMS & Phone State permissions to screen physical incoming calls/texts.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(36.dp).testTag("grant_permissions_button")
            ) {
                Text("Activate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Tab 1: Tester Console (Interactive Dialer & MP3 Player Hub - Gemini Pro Style) ---
@Composable
fun TesterConsoleTab(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Player and database states from VM
    val playerState by viewModel.musicPlayerState.collectAsState()
    val eqState by viewModel.equalizerState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val availableTracks = viewModel.availableTracks
    val isScanning by viewModel.isScanningLibrary.collectAsState()
    val customRingtones by viewModel.customRingtones.collectAsState()
    val contacts by viewModel.allContacts.collectAsState()

    // Sub-modules state inside this tab (0 = Dial Pad, 1 = Music Lib, 2 = Equalizer, 3 = Rec Studio)
    var hubSubTab by remember { mutableStateOf(0) }

    // Smart Dial Pad Input
    var dialedNumber by remember { mutableStateOf("") }
    
    // Dial pad sound profiles: "System Beeps", "Piano Chords", "Synth Tones", "Marimba"
    var soundTheme by remember { mutableStateOf("Piano Chords") }
    val soundThemesList = listOf("Piano Chords", "System Beeps", "Synth Tones", "Marimba")

    // Contact Assignment Dialog
    var assignRingtoneTrackId by remember { mutableStateOf<String?>(null) }
    var assignDialogOpen by remember { mutableStateOf(false) }

    // Direct MP3 Ringtone Cutter states
    var cutterTrackId by remember { mutableStateOf<String?>(null) }
    var cutterDialogOpen by remember { mutableStateOf(false) }
    var cutterStartSec by remember { mutableStateOf(0f) }
    var cutterEndSec by remember { mutableStateOf(30f) }
    var cutterSelectedContactPhone by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dialer_mp3_hub_container"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ==========================================
        // TOP HALF: MP3 MINI PLAYER UNIT (WITH MULTI-SKINS)
        // ==========================================
        val skinMode by viewModel.skinMode.collectAsState()
        val isDialerLocked by viewModel.isDialerLocked.collectAsState()

        val infiniteTransition = rememberInfiniteTransition(label = "Player Animations")
        
        // Vinyl rotation angle
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (playerState.isPlaying) 5000 else 999999, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Vinyl Rotation"
        )

        // Bouncing DVD offsets
        val dvdX by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (playerState.isPlaying) 3200 else 999999, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DVD X"
        )
        val dvdY by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(if (playerState.isPlaying) 2300 else 999999, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "DVD Y"
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF131324) // Dynamic dark aesthetic
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF00F2FE), Color(0xFFBD00FF))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // [SKIN ROW & POCKET LOCK]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SKIN:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.5f))
                    listOf("Neon Spectrum", "Vinyl Turntable", "Bouncing DVD").forEach { skin ->
                        val isSelected = skinMode == skin
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) Color(0xFFBD00FF) else Color.White.copy(alpha = 0.08f))
                                .clickable { viewModel.updateSkinMode(skin) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(skin, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Pocket Lock button
                    IconButton(
                        onClick = {
                            viewModel.updateDialerLocked(!isDialerLocked)
                            Toast.makeText(context, if (!isDialerLocked) "Dialer keys locked to prevent pocket clicks!" else "Dialer keys unlocked!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = if (isDialerLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Pocket Lock",
                            tint = if (isDialerLocked) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))

                // [DYNAMIC SKIN PRESENTATION]
                when (skinMode) {
                    "Vinyl Turntable" -> {
                        // VINYL SPINNING SKIN
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Spinning Vinyl Disc
                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Black concentric circle
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black)
                                        .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                        .border(15.dp, Color(0xFF111111), CircleShape)
                                        .border(25.dp, Color(0xFF222222), CircleShape)
                                        .rotate(rotationAngle)
                                        .clickable { viewModel.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Album middle spindle
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF00F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(playerState.currentTrack.coverIcon, fontSize = 12.sp)
                                    }
                                }
                                
                                // Tonearm pointer (stationary arm pointing on vinyl)
                                Box(
                                    modifier = Modifier
                                        .size(90.dp),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .offset(x = (-4).dp, y = 14.dp)
                                            .width(26.dp)
                                            .height(3.dp)
                                            .rotate(-25f)
                                            .background(Color.LightGray)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Details text
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = playerState.currentTrack.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = playerState.currentTrack.artist,
                                    fontSize = 11.sp,
                                    color = Color(0xFF00F2FE),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "LP Vinyl Record 💽",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    "Bouncing DVD" -> {
                        // BOUNCING DVD LOGO SCREENSAVER
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(96.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF07070F))
                                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        ) {
                            val boundedX = (maxWidth - 100.dp).coerceAtLeast(0.dp) * dvdX
                            val boundedY = (maxHeight - 32.dp).coerceAtLeast(0.dp) * dvdY

                            Box(
                                modifier = Modifier
                                    .offset(x = boundedX, y = boundedY)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFBD00FF), Color(0xFF00F2FE))
                                        )
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ELYzARETH",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("💽", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    else -> {
                        // NEON SPECTRUM STANDARD STYLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00F2FE).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = playerState.currentTrack.coverIcon,
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "NOW PLAYING",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00F2FE),
                                        letterSpacing = 1.5.sp
                                    )
                                    Text(
                                        text = "${playerState.currentTrack.title} — ${playerState.currentTrack.artist}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Sound Theme Indicator Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFBD00FF).copy(alpha = 0.2f))
                                    .clickable {
                                        val nextIdx = (soundThemesList.indexOf(soundTheme) + 1) % soundThemesList.size
                                        soundTheme = soundThemesList[nextIdx]
                                        Toast.makeText(context, "Dial tones changed to: $soundTheme", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = soundTheme,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE0B0FF)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val progressSec = playerState.progressMs / 1000
                    val durationSec = playerState.currentTrack.durationMs / 1000
                    
                    Text(
                        text = String.format("%02d:%02d", progressSec / 60, progressSec % 60),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.width(36.dp)
                    )

                    Slider(
                        value = playerState.progressMs.toFloat(),
                        onValueChange = { viewModel.seekProgress(it.toLong()) },
                        valueRange = 0f..playerState.currentTrack.durationMs.toFloat(),
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color(0xFF00F2FE),
                            inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                            thumbColor = Color(0xFFBD00FF)
                        )
                    )

                    Text(
                        text = String.format("%02d:%02d", durationSec / 60, durationSec % 60),
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }

                // Control Player Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousTrack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFBD00FF))
                            .clickable { viewModel.togglePlayPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play or Pause",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    IconButton(
                        onClick = { viewModel.nextTrack() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // ==========================================
        // TAB ROW SWITCHER INSIDE CENTRAL HUB
        // ==========================================
        TabRow(
            selectedTabIndex = hubSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            Tab(
                selected = hubSubTab == 0,
                onClick = { hubSubTab = 0 },
                text = { Text("Smart Dial Pad", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = hubSubTab == 1,
                onClick = { hubSubTab = 1 },
                text = { Text("Music Lib", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = hubSubTab == 2,
                onClick = { hubSubTab = 2 },
                text = { Text("Equalizer", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = hubSubTab == 3,
                onClick = { hubSubTab = 3 },
                text = { Text("Studio Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        // ==========================================
        // LOWER HALF: SELECTED ACTION SCREEN
        // ==========================================
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (hubSubTab) {
                0 -> {
                    // --- SMART DIAL PAD ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dial Pad Entry display
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .height(52.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dialpad,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dialedNumber.ifEmpty { "Enter custom phone number..." },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dialedNumber.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            if (dialedNumber.isNotEmpty()) {
                                IconButton(
                                    onClick = { dialedNumber = dialedNumber.dropLast(1) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backspace,
                                        contentDescription = "Backspace",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Smart T9 contact lookup row
                        val filteredContacts = if (dialedNumber.isEmpty()) emptyList() else contacts.filter {
                            it.phoneNumber.replace("+", "").contains(dialedNumber) ||
                            it.name.lowercase().contains(dialedNumber.lowercase())
                        }
                        
                        if (filteredContacts.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(filteredContacts) { contact ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.clickable {
                                            dialedNumber = contact.phoneNumber
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("👤", fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Column {
                                                Text(contact.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(contact.phoneNumber, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (dialedNumber.isNotEmpty()) {
                            Text(
                                "No local contacts match dial entry.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        // Pad Container with potential Pocket protection lock
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Numerical Dial Grid
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                val keys = listOf(
                                    listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                                    listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                                    listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                                    listOf("*" to "", "0" to "+", "#" to "")
                                )

                                keys.forEach { row ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        row.forEach { (key, label) ->
                                            // Special Sound Theme Key at 9
                                            val isThemeKey = key == "9"
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(44.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isThemeKey) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                    .clickable(enabled = !isDialerLocked) {
                                                        dialedNumber += key
                                                        // Dynamic tone generation simulated feed
                                                        val chord = when (soundTheme) {
                                                            "Piano Chords" -> when (key) {
                                                                "1" -> "C Major 🎹"
                                                                "2" -> "D Minor 🎹"
                                                                "3" -> "E Minor 🎹"
                                                                "4" -> "F Major 🎹"
                                                                "5" -> "G Major 🎹"
                                                                "6" -> "A Minor 🎹"
                                                                "7" -> "B Diminished 🎹"
                                                                "8" -> "C7 Chord 🎹"
                                                                "9" -> "C Major high 🎹"
                                                                else -> "F# Chord 🎹"
                                                            }
                                                            "Synth Tones" -> "Square Wave ($key) ⚡"
                                                            "Marimba" -> "Wood Strike ($key) 🪵"
                                                            else -> "Standard DTMF ($key) 📞"
                                                        }
                                                        Toast
                                                            .makeText(context, chord, Toast.LENGTH_SHORT)
                                                            .show()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = key,
                                                            fontSize = 18.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (isThemeKey) {
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Icon(
                                                                imageVector = Icons.Default.Lightbulb,
                                                                contentDescription = "Sound Theme Toggler",
                                                                tint = Color(0xFFFFC107),
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                    }
                                                    if (label.isNotEmpty()) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 8.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Pocket Protection Overlay
                            if (isDialerLocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.88f))
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { /* Block clicks entirely */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = Color(0xFF00F2FE),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "DIALER SECURED",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Preventing accidental pocket dial commands.",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.updateDialerLocked(false) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD00FF)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Tap to Unlock 🔓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Green DIAL Button
                        Button(
                            onClick = {
                                if (isDialerLocked) {
                                    Toast.makeText(context, "Please unlock dialer keys first!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (dialedNumber.isBlank()) {
                                    Toast.makeText(context, "Please enter a number to call", Toast.LENGTH_SHORT).show()
                                } else {
                                    keyboardController?.hide()
                                    viewModel.simulateIncomingCall(dialedNumber)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDialerLocked) Color.Gray else Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("dialer_call_btn")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DIAL PHONE NUMBER", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                1 -> {
                    // --- MUSIC LIBRARY SCANNER ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Scanned Library Tracks",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { viewModel.scanMusicLibrary() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Folder", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isScanning) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Scanning internal & SD storage for MP3 files...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(availableTracks) { track ->
                                    val isCurrent = playerState.currentTrack.id == track.id
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(track.coverIcon, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(track.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(track.artist + " • " + track.album, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }

                                            // Quick Play track
                                            IconButton(onClick = { viewModel.playTrack(track) }) {
                                                Icon(
                                                    imageVector = if (isCurrent && playerState.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                                    contentDescription = "Play track",
                                                    tint = if (isCurrent) Color(0xFFBD00FF) else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }

                                            // Assign custom ringtone
                                            IconButton(onClick = {
                                                assignRingtoneTrackId = track.id
                                                assignDialogOpen = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.RingVolume,
                                                     contentDescription = "Assign ringtone",
                                                     tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                     modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Direct MP3 Ringtone Cutter
                                            IconButton(onClick = {
                                                cutterTrackId = track.id
                                                cutterDialogOpen = true
                                                cutterStartSec = 0f
                                                cutterEndSec = (track.durationMs / 1000f).coerceAtMost(30f)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCut,
                                                    contentDescription = "Cut Ringtone",
                                                    tint = Color(0xFF00F2FE),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Clean Banner Ad Placement inside Folder Scan
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFFF5722), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("SPONSORED AD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Aura Shield Premium: Speed up files search by 10x! Tap to Upgrade.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // --- PROFESSIONAL EQUALIZER ---
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("10-Band Graphic Equalizer", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            
                            // Presets Row
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Lofi Beats", "Rock Booster", "Acoustic", "Jazz Cafe").forEach { preset ->
                                    val isSel = eqState.preset == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { viewModel.setEqualizerPreset(preset) }
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(preset, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        // Volume Booster warning block
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aura sound booster configured. Equalizer changes apply in real-time.", fontSize = 10.sp, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // 5 Adjustment Sliders (Frequencies: 60Hz, 230Hz, 910Hz, 4kHz, 14kHz)
                        val freqLabels = listOf("60Hz", "230Hz", "910Hz", "4kHz", "14kHz")
                        freqLabels.forEachIndexed { idx, label ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(label, fontSize = 10.sp, modifier = Modifier.width(44.dp), fontWeight = FontWeight.Bold)
                                Slider(
                                    value = eqState.bands[idx],
                                    onValueChange = { viewModel.updateEqualizerBand(idx, it) },
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                                        thumbColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Text(
                                    text = String.format("%d%%", (eqState.bands[idx] * 100).toInt()),
                                    fontSize = 10.sp,
                                    modifier = Modifier.width(32.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                        
                        // Custom Crossfade slider
                        val crossfadeMs by viewModel.crossfadeMs.collectAsState()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00F2FE), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Studio Track Crossfade Blend", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text("${crossfadeMs}ms", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBD00FF))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Slider(
                                    value = crossfadeMs.toFloat(),
                                    onValueChange = { viewModel.updateCrossfade(it.toInt()) },
                                    valueRange = 0f..5000f,
                                    colors = SliderDefaults.colors(
                                        activeTrackColor = Color(0xFF00F2FE),
                                        thumbColor = Color(0xFFBD00FF)
                                    )
                                )
                                Text("Determines track overlap duration when switching songs locally.", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                3 -> {
                    // --- STUDIO RECORDING LOGS ---
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Rec Studio Folder", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${recordings.size} files recorded", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (recordings.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No recording files found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(recordings) { rec ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rec.callerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(rec.fileName + " • " + rec.durationSeconds + "s", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            IconButton(onClick = {
                                                Toast.makeText(context, "Playing call recording file: ${rec.fileName}", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = "Play recording", tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                // Native Ad for Call recording
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFF3F51B5), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text("PRO", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("Direct cloud backup active in Studio Pro. Tap to sync files.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ASSIGN RINGTONE COMPANION DIALOG ---
    if (assignDialogOpen && assignRingtoneTrackId != null) {
        val track = availableTracks.firstOrNull { it.id == assignRingtoneTrackId }
        AlertDialog(
            onDismissRequest = { assignDialogOpen = false },
            title = { Text("Assign Custom Ringtone", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a local Caller ID entity to assign \"${track?.title}\" as their ringtone:")
                    
                    if (contacts.isEmpty()) {
                        Text("No contacts registered. Create some inside Contacts ID tab first!", fontWeight = FontWeight.Bold, color = Color.Red)
                    } else {
                        LazyColumn(modifier = Modifier.height(180.dp)) {
                            items(contacts) { contact ->
                                val ringtoneMapped = customRingtones[contact.phoneNumber]
                                val mappedTrackName = availableTracks.firstOrNull { it.id == ringtoneMapped }?.title ?: "Default system"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            viewModel.assignRingtone(contact.phoneNumber, assignRingtoneTrackId!!)
                                            Toast.makeText(context, "Assigned ${track?.title} as ringtone for ${contact.name}!", Toast.LENGTH_SHORT).show()
                                            assignDialogOpen = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Current Ringtone: $mappedTrackName", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { assignDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- DIRECT MP3 RINGTONE CUTTER DIALOG ---
    if (cutterDialogOpen && cutterTrackId != null) {
        val track = availableTracks.firstOrNull { it.id == cutterTrackId }
        val durationSec = (track?.durationMs ?: 0L) / 1000f

        AlertDialog(
            onDismissRequest = { cutterDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = Color(0xFF00F2FE),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Direct MP3 Ringtone Cutter", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Trim and assign custom segments of \"${track?.title}\" as offline Caller ID ringtones.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Start Time Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Start Position", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("%.1fs", cutterStartSec), fontSize = 11.sp, color = Color(0xFFBD00FF), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = cutterStartSec,
                            onValueChange = {
                                cutterStartSec = it.coerceAtMost(cutterEndSec - 2f)
                            },
                            valueRange = 0f..durationSec,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF00F2FE),
                                thumbColor = Color(0xFFBD00FF)
                            )
                        )
                    }

                    // End Time Slider
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("End Position", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(String.format("%.1fs", cutterEndSec), fontSize = 11.sp, color = Color(0xFFBD00FF), fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = cutterEndSec,
                            onValueChange = {
                                cutterEndSec = it.coerceAtLeast(cutterStartSec + 2f)
                            },
                            valueRange = 0f..durationSec,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Color(0xFF00F2FE),
                                thumbColor = Color(0xFFBD00FF)
                            )
                        )
                    }

                    // Interval Duration indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF00F2FE).copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("Selected Segment Length: %.1f seconds", cutterEndSec - cutterStartSec),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00F2FE)
                        )
                    }

                    // Target Contact dropdown or selector
                    Text("Select Contact Entity to Apply:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (contacts.isEmpty()) {
                        Text("No contacts found. Save file generally.", fontSize = 10.sp, color = Color.Red)
                    } else {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedContact = contacts.firstOrNull { it.phoneNumber == cutterSelectedContactPhone }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { expanded = true }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedContact?.let { "${it.name} (${it.phoneNumber})" } ?: "General Ringtone (All contacts)",
                                    fontSize = 11.sp
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("General Ringtone (All contacts)") },
                                    onClick = {
                                        cutterSelectedContactPhone = ""
                                        expanded = false
                                    }
                                )
                                contacts.forEach { contact ->
                                    DropdownMenuItem(
                                        text = { Text("${contact.name} (${contact.phoneNumber})") },
                                        onClick = {
                                            cutterSelectedContactPhone = contact.phoneNumber
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (track != null) {
                            val cutName = "${track.title}_cut_${cutterStartSec.toInt()}s"
                            viewModel.cutAndAssignRingtone(
                                baseTrackId = track.id,
                                cutName = cutName,
                                startSec = cutterStartSec,
                                endSec = cutterEndSec,
                                targetContactPhone = cutterSelectedContactPhone
                            )
                            Toast.makeText(context, "Successfully cut \"$cutName\"! Assigned and saved offline.", Toast.LENGTH_LONG).show()
                            cutterDialogOpen = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBD00FF))
                ) {
                    Text("Cut & Save Ringtone ✂️")
                }
            },
            dismissButton = {
                TextButton(onClick = { cutterDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Tab 2: Contacts ID Manager ---
@Composable
fun ContactsTab(contacts: List<ContactEntity>, viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var addDialogOpen by remember { mutableStateOf(false) }

    // New contact fields
    var newPhone by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newCategory by remember { mutableStateOf("PERSONAL") } // "PERSONAL", "BUSINESS", "SPAM"
    var newSpamReason by remember { mutableStateOf("") }
    var newIsBlocked by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📋 Local Identity Database",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${contacts.size} records registered offline",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FloatingActionButton(
                onClick = {
                    // Reset fields
                    newPhone = ""
                    newName = ""
                    newCategory = "PERSONAL"
                    newSpamReason = ""
                    newIsBlocked = false
                    addDialogOpen = true
                },
                modifier = Modifier.height(44.dp).testTag("add_contact_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add ID", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (contacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ContactPhone, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Database Empty", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Seed default contacts using console or tap Add above.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(contacts, key = { it.id }) { c ->
                    ContactListItem(contact = c, onDelete = { viewModel.removeContactById(c.id) })
                }
            }
        }
    }

    // --- Add Contact Dialog ---
    if (addDialogOpen) {
        AlertDialog(
            onDismissRequest = { addDialogOpen = false },
            title = { Text("Register Local Caller ID", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. +14155551234") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_number")
                    )

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name / Organization") },
                        placeholder = { Text("e.g. John Doe or spam bank") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_contact_name")
                    )

                    Text("Classification Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("PERSONAL", "BUSINESS", "SPAM").forEach { cat ->
                            val selected = newCategory == cat
                            Button(
                                onClick = {
                                    newCategory = cat
                                    if (cat != "SPAM") {
                                        newIsBlocked = false
                                    } else {
                                        newIsBlocked = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        when (cat) {
                                            "SPAM" -> Color.Red
                                            "BUSINESS" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.secondary
                                        }
                                    } else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.weight(1f).height(38.dp).testTag("cat_btn_$cat")
                            ) {
                                Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (newCategory == "SPAM") {
                        OutlinedTextField(
                            value = newSpamReason,
                            onValueChange = { newSpamReason = it },
                            label = { Text("Reason for Spam label") },
                            placeholder = { Text("e.g. Robocall Loan Spam") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("add_contact_spam_reason")
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = newIsBlocked,
                                onCheckedChange = { newIsBlocked = it },
                                modifier = Modifier.testTag("add_contact_blocked_checkbox")
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Auto-block (Silence) this number", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPhone.isBlank() || newName.isBlank()) {
                            Toast.makeText(context, "Please complete number and name fields", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addContact(
                            phoneNumber = newPhone,
                            name = newName,
                            category = newCategory,
                            spamReason = newSpamReason,
                            isBlocked = newIsBlocked
                        )
                        addDialogOpen = false
                    },
                    modifier = Modifier.testTag("save_contact_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { addDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactListItem(contact: ContactEntity, onDelete: () -> Unit) {
    val viewModel: CallSmsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val customRingtones by viewModel.customRingtones.collectAsState()
    val moodTags by viewModel.customMoodTags.collectAsState()
    
    val ringtoneTrackId = customRingtones[contact.phoneNumber]
    val ringtoneName = viewModel.availableTracks.firstOrNull { it.id == ringtoneTrackId }?.title
    val assignedMood = moodTags[contact.phoneNumber]

    var vibeDialogOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when (contact.category) {
                    "SPAM" -> Color.Red.copy(alpha = 0.2f)
                    "BUSINESS" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge depending on category
            val (badgeBg, badgeTint, badgeIcon) = when (contact.category) {
                "SPAM" -> Triple(Color(0xFFFDE8E8), Color(0xFFE02424), Icons.Default.ReportProblem)
                "BUSINESS" -> Triple(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary, Icons.Default.Business)
                else -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.secondary, Icons.Default.Person)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(badgeIcon, contentDescription = contact.category, tint = badgeTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (contact.category == "SPAM") Color(0xFFC81E1E) else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (contact.isBlocked) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Red)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("BLOCKED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (contact.category == "SPAM" && !contact.spamReason.isNullOrBlank()) {
                    Text(
                        text = "Reason: ${contact.spamReason}",
                        fontSize = 11.sp,
                        color = Color.Red.copy(alpha = 0.8f)
                    )
                }

                // Interactive vibe and assigned ringtone row
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (assignedMood != null) Color(0xFFBD00FF).copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { vibeDialogOpen = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (assignedMood != null) "VIBE: $assignedMood" else "+ Set Vibe",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (assignedMood != null) Color(0xFFBD00FF) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (ringtoneName != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🎵 $ringtoneName",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37)
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_contact_${contact.id}")) {
                Icon(Icons.Default.Delete, contentDescription = "Delete contact", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    // --- Interactive Vibe Picker Dialog ---
    if (vibeDialogOpen) {
        val vibeOptions = listOf("None", "Energetic 🎸", "Relaxed 🌊", "Romantic 💖", "Lofi Chill ☕", "Retro Beats 👾")
        AlertDialog(
            onDismissRequest = { vibeDialogOpen = false },
            title = { Text("Set Contact Music Vibe", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select a music vibe mood to associate with \"${contact.name}\":", fontSize = 12.sp)
                    vibeOptions.forEach { option ->
                        val isSelected = (assignedMood ?: "None") == option || (assignedMood == null && option == "None")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    viewModel.assignMoodTag(contact.phoneNumber, if (option == "None") "None" else option)
                                    vibeDialogOpen = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.assignMoodTag(contact.phoneNumber, if (option == "None") "None" else option)
                                    vibeDialogOpen = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { vibeDialogOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- Tab 3: SMS Spam Keywords ---
@Composable
fun KeywordsTab(keywords: List<SpamKeywordEntity>, viewModel: CallSmsViewModel) {
    var newKeywordText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "🛡️ local SMS Spam Filter Words",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Texts containing any match below are classified as Spam and silenced.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newKeywordText,
                    onValueChange = { newKeywordText = it },
                    label = { Text("New Block Trigger Word") },
                    placeholder = { Text("e.g. winner, loan, lottery") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("new_keyword_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newKeywordText.isBlank()) {
                            Toast.makeText(context, "Please enter a word", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.addSpamKeyword(newKeywordText)
                        newKeywordText = ""
                        keyboardController?.hide()
                    },
                    modifier = Modifier.height(56.dp).testTag("add_keyword_btn")
                ) {
                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (keywords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Block, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Keywords Configured", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Any SMS text containing these keywords is auto-blocked.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keywords, key = { it.id }) { kw ->
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Block, contentDescription = "Blocked keyword", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = kw.keyword,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { viewModel.removeSpamKeyword(kw) }, modifier = Modifier.testTag("delete_keyword_${kw.id}")) {
                                Icon(Icons.Default.Close, contentDescription = "Delete keyword", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Tab 4: Spam Logs ---
@Composable
fun LogsTab(logs: List<LogEntity>, viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "🛡️ Local Screening Audit Trail",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${logs.size} screenings handled locally",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        viewModel.testServiceCallerId(context, "+18005559999")
                        Toast.makeText(context, "Testing Local Caller ID Service...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("test_service_caller_id_btn")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Service", fontSize = 11.sp)
                }
                if (logs.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = { viewModel.clearLogs() },
                        modifier = Modifier.testTag("clear_logs_btn")
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Clear")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Activity Logs", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Incoming calls and SMS checks will log details here.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val (typeIcon, typeColor) = if (log.type == "CALL") {
                                        Icons.Default.Phone to MaterialTheme.colorScheme.primary
                                    } else {
                                        Icons.Default.Sms to MaterialTheme.colorScheme.secondary
                                    }
                                    Icon(typeIcon, contentDescription = log.type, tint = typeColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = log.type,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = typeColor
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dateFormat.format(Date(log.timestamp)),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                // Status Badge
                                val (statusBg, statusTextTint) = if (log.wasBlocked) {
                                    Color(0xFFFDE8E8) to Color(0xFFC81E1E)
                                } else if (log.isSpam) {
                                    Color(0xFFFEF08A) to Color(0xFF854D0E)
                                } else {
                                    Color(0xFFDEF7EC) to Color(0xFF03543F)
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(statusBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = log.actionTaken,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusTextTint
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = log.senderName ?: "Unknown Number",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = log.phoneNumber,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!log.messageBody.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = log.messageBody,
                                        fontSize = 12.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Simulation Overlay HUD: Calls with Neon Edge Glow & Wave Visualizers ---
@Composable
fun SimulatedCallHUD(simCall: SimulatedCall, onDismiss: () -> Unit) {
    // Collect ViewModel states
    val compositionLocalContext = LocalContext.current
    val viewModel: CallSmsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val playerState by viewModel.musicPlayerState.collectAsState()
    val amps by viewModel.audioWaveAmplitudes.collectAsState()
    val customRingtones by viewModel.customRingtones.collectAsState()
    val moodTags by viewModel.customMoodTags.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val isHoldMusicOn by viewModel.isHoldMusicOn.collectAsState()
    val isSelfHoldActive by viewModel.isSelfHoldActive.collectAsState()
    val isFlashAlertEnabled by viewModel.isFlashAlertEnabled.collectAsState()
    val ambientSound by viewModel.activeAmbientSound.collectAsState()
    val visualizerTheme by viewModel.visualizerTheme.collectAsState()

    // Determine Ringtone assigned
    val mappedTrackId = customRingtones[simCall.phoneNumber]
    val ringtoneName = viewModel.availableTracks.firstOrNull { it.id == mappedTrackId }?.title ?: "Cyberpunk Synth.mp3"

    // Animation values for Neon Edge Pulsing Glow
    val infiniteTransition = rememberInfiniteTransition(label = "Neon Glow")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 2f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow Width"
    )

    val (neonColor1, neonColor2) = when (visualizerTheme) {
        "Cyberpunk Amber" -> Color(0xFFFFB300) to Color(0xFFFF6F00)
        "Vaporwave Purple" -> Color(0xFFBD00FF) to Color(0xFFFF007F)
        "Matrix Emerald" -> Color(0xFF39FF14) to Color(0xFF00E676)
        "Sunset Crimson" -> Color(0xFFFF5722) to Color(0xFFE91E63)
        "Electric Blue" -> Color(0xFF2979FF) to Color(0xFF3D5AFE)
        else -> Color(0xFF00F2FE) to Color(0xFFBD00FF) // Cyber Neon Cyan
    }

    // Interactive Swipe Control offset state
    var swipeStateOffset by remember { mutableFloatStateOf(0f) }
    val maxSwipeDistance = 140f

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // OUTSIDE NEON EDGE GLOW CONTAINER (Phone 1 screen border look)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0B0B14)) // Sleek dark body
                    .border(
                        width = glowIntensity.dp,
                        brush = Brush.sweepGradient(
                            listOf(neonColor1, neonColor2, neonColor1)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Status
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ELYZARETH CALL MANAGER",
                            color = Color(0xFF00F2FE),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (simCall.isBlocked) Color.Red else Color(0xFF39FF14))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (simCall.isBlocked) "PROTECTION SHIELD ACTIVE" else "INCOMING ACTIVE SCREENING",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        if (isFlashAlertEnabled) {
                            val pulseAlpha = (amps.firstOrNull() ?: 0.5f).coerceIn(0.2f, 1.0f)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.alpha(pulseAlpha)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BEAT-SYNCED FLASH PULSING",
                                    color = Color(0xFFFFD700),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Large Avatar with pulsating visual ring
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        val avatarColor = if (simCall.category == "SPAM") Color(0xFFFF3333) else Color(0xFF00F2FE)
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(avatarColor.copy(alpha = 0.15f))
                                .border(2.dp, avatarColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (simCall.category == "SPAM") "🤖" else "👤",
                                fontSize = 48.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Caller Name and Number
                        Text(
                            text = simCall.contactName,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = simCall.phoneNumber,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )

                        val callerMood = moodTags[simCall.phoneNumber]
                        if (callerMood != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFBD00FF).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFBD00FF), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VIBE: $callerMood",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Display active ringtone
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Ringtone: \"$ringtoneName\"",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // --- FAST FOURIER TRANSFORM (FFT) AUDIO WAVE VISUALIZER ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .height(56.dp)
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            amps.forEach { amp ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .width(5.dp)
                                        .height((amp * 50).coerceAtLeast(3f).dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFFBD00FF), // Violet
                                                    Color(0xFF00F2FE)  // Neon Blue
                                                )
                                            )
                                        )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHoldMusicOn) "HOLD MUSIC ACTIVE: 7.1 ATMOSPHERE" else "LIVE TELEPHONY OSCILLOSCOPE",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.2.sp
                        )
                    }

                    // --- IN-CALL HARDWARE CONTROL BUTTONS ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // MUTE
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color(0xFFFF3333) else Color.White.copy(alpha = 0.1f))
                                    .clickable { viewModel.toggleMute() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Mute", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        // HOLD MUSIC
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isHoldMusicOn) Color(0xFFBD00FF) else Color.White.copy(alpha = 0.1f))
                                    .clickable { viewModel.toggleHoldMusic() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Hold Music",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Hold Music", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        // SELF-HOLD (REVERSE HOLD MUSIC)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelfHoldActive) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.1f))
                                    .clickable { viewModel.updateSelfHold(!isSelfHoldActive) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhonePaused,
                                    contentDescription = "Self Hold",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Self-Hold", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        // BEAT-SYNCED FLASH ALERT
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isFlashAlertEnabled) Color(0xFFFFD700) else Color.White.copy(alpha = 0.1f))
                                    .clickable { viewModel.updateFlashAlert(!isFlashAlertEnabled) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isFlashAlertEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Beat Flash",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Beat Flash", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }

                        // SPEAKER
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isSpeakerOn) Color(0xFF39FF14) else Color.White.copy(alpha = 0.1f))
                                    .clickable { viewModel.toggleSpeaker() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Speaker",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Speaker", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }

                    // --- IN-CALL AMBIENT BACKGROUNDS FILTER ---
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AMBIENT TELEPHONY SOUNDSCAPE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.4f),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("None", "Rain 🌧️", "Café Noise ☕", "Lo-Fi Beats 🎧").forEach { sound ->
                                val cleanName = sound.substringBefore(" ")
                                val isSel = ambientSound == cleanName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF00F2FE) else Color.White.copy(alpha = 0.08f))
                                        .clickable { viewModel.selectAmbientSound(cleanName) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = sound,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    // ==========================================
                    // ACTION CONTROL SLIDERS (Swipe Left/Right)
                    // ==========================================
                    if (simCall.isBlocked) {
                        // If spammer blocked automatically
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF3333).copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .border(1.dp, Color(0xFFFF3333), RoundedCornerShape(12.dp))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛡️ AUTO-BLOCKED BY SPAM CORE", fontWeight = FontWeight.Bold, color = Color(0xFFFF5555), fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    TextButton(onClick = onDismiss) {
                                        Text("DISMISS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Interactive Drag Gesture Slider (Swipe Left to Decline, Swipe Right to Answer)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                        ) {
                            // Guideline visual layout indicators
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 16.dp).clickable {
                                        Toast.makeText(compositionLocalContext, "Call Declined", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DECLINE", color = Color.Red.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 16.dp).clickable {
                                        Toast.makeText(compositionLocalContext, "Simulated call connected successfully!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    }
                                ) {
                                    Text("ANSWER", color = Color(0xFF39FF14).copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color(0xFF39FF14), modifier = Modifier.size(16.dp))
                                }
                            }

                            // Dynamic Draggable Glowing Central Slider Anchor!
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFF00F2FE), Color(0xFFBD00FF))
                                        )
                                    )
                                    .clickable {
                                        // Simple click fallback for emulator usability
                                        Toast.makeText(compositionLocalContext, "Connected Call via Core Tap!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Swipe Controller",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Dynamic Simulation Overlay HUD: SMS Interception ---
@Composable
fun SimulatedSmsHUD(simSms: SimulatedSms, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("sms_hud_overlay")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header badge
                val headerBg = if (simSms.isSpam) Color(0xFFFDE8E8) else Color(0xFFDEF7EC)
                val headerTextTint = if (simSms.isSpam) Color(0xFF9B1C1C) else Color(0xFF03543F)
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(headerBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (simSms.isSpam) Icons.Default.Lock else Icons.Default.MarkChatRead,
                        contentDescription = null,
                        tint = headerTextTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (simSms.isSpam) "🛡️ SMS SPAM INTERCEPTED" else "💬 INCOMING SAFE SMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = headerTextTint
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Chat sender layout
                Text(
                    text = simSms.senderName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = simSms.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message Text Bubble
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (simSms.isSpam) {
                                Color.Red.copy(alpha = 0.05f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                        .border(
                            1.dp,
                            if (simSms.isSpam) Color.Red.copy(alpha = 0.15f) else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = simSms.body,
                        fontSize = 13.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detailed reasoning explanation
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Filter explanation: ${simSms.reason}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action controls
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (simSms.isSpam) Color.Red else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("close_sms_hud_btn")
                ) {
                    Text(if (simSms.isSpam) "Acknowledge Block" else "Open Message Chat")
                }
            }
        }
    }
}
