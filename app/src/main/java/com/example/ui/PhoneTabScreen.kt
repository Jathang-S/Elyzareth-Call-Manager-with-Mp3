package com.example.ui

import android.content.Intent
import android.net.Uri
import android.provider.CallLog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.AdvancedCallLog
import com.example.data.DeviceContact
import com.example.repository.CallLogManager
import com.example.telecom.DefaultDialerManager
import com.example.viewmodel.CallSmsViewModel
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneTabScreen(
    viewModel: CallSmsViewModel,
    onOpenDialpad: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val advancedCallLogs by viewModel.advancedCallLogs.collectAsState()
    val deviceContacts by viewModel.deviceContacts.collectAsState()
    val currentFilter by viewModel.callFilter.collectAsState()

    var isDefaultDialer by remember {
        mutableStateOf(DefaultDialerManager.isDefaultDialer(context))
    }

    // Role launcher for system default dialer prompt (ROLE_DIALER)
    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
    }

    // Refresh default dialer status whenever the user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Recent Calls, 1 = Contacts

    // Filter call logs based on search query and selected filter chip
    val filteredLogs = remember(advancedCallLogs, searchQuery, currentFilter) {
        advancedCallLogs.filter { log ->
            // Search query filter
            val matchesQuery = if (searchQuery.isBlank()) true else {
                log.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                (log.contactName?.contains(searchQuery, ignoreCase = true) == true) ||
                log.locationLabel.contains(searchQuery, ignoreCase = true) ||
                log.carrierLabel.contains(searchQuery, ignoreCase = true) ||
                (log.spamWarning?.contains(searchQuery, ignoreCase = true) == true)
            }

            // Chip filter
            val matchesChip = when (currentFilter) {
                "Missed" -> log.callType == CallLog.Calls.MISSED_TYPE || log.callType == CallLog.Calls.REJECTED_TYPE
                "Contacts" -> !log.contactName.isNullOrBlank() && !log.isSpam
                "Non-spam" -> !log.isSpam
                else -> true // "All"
            }

            matchesQuery && matchesChip
        }
    }

    // Filter in-built device contacts based on search query
    val filteredContacts = remember(deviceContacts, searchQuery) {
        if (searchQuery.isBlank()) deviceContacts
        else deviceContacts.filter { contact ->
            contact.name.contains(searchQuery, ignoreCase = true) ||
            contact.phoneNumber.contains(searchQuery, ignoreCase = true) ||
            contact.typeLabel.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDialpad,
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("phone_fab_dialpad")
            ) {
                Icon(
                    imageVector = Icons.Default.Dialpad,
                    contentDescription = "Open Dialpad",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Phone & Dialer",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time Caller ID & local telephony shield",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.refreshCallLogsAndContacts()
                        Toast.makeText(context, "Refreshed call logs and contacts", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Android Telecom Default Dialer Qualification Card / Status
            if (!isDefaultDialer) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("default_dialer_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1B2234)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F2FE).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00F2FE).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneCallback,
                                    contentDescription = "Default Phone App",
                                    tint = Color(0xFF00F2FE),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Default Phone App",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Set as default dialer to qualify for direct Android Telecom calling.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val intent = DefaultDialerManager.createRequestRoleIntent(context)
                                if (intent != null) {
                                    try {
                                        roleLauncher.launch(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not launch role request", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Role request not available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F2FE)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("set_default_dialer_button")
                        ) {
                            Text(
                                text = "Set Default",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF00E676).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Default Phone Active",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Default Phone App Active",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, number, or carrier...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00F2FE),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("phone_search_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub Tab Switcher: Recents vs Contacts
            TabRow(
                selectedTabIndex = selectedSubTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                contentColor = Color(0xFF00F2FE),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    text = {
                        Text(
                            text = "Recent Calls (${filteredLogs.size})",
                            fontWeight = if (selectedSubTab == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
                Tab(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    text = {
                        Text(
                            text = "Contacts (${filteredContacts.size})",
                            fontWeight = if (selectedSubTab == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Filter Chips for Recent Calls (Matching Prompt Requirements)
            if (selectedSubTab == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filterOptions = listOf("All", "Missed", "Contacts", "Non-spam")
                    filterOptions.forEach { filter ->
                        val isSelected = currentFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCallFilter(filter) },
                            label = {
                                Text(
                                    text = filter,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00F2FE).copy(alpha = 0.2f),
                                selectedLabelColor = Color(0xFF00F2FE)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Color(0xFF00F2FE) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Sub Tab Content
            if (selectedSubTab == 0) {
                // Recent Calls List
                if (filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhoneMissed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Calls Found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Calls will appear here as you make and receive them.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            AdvancedCallLogItemCard(
                                log = log,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${log.phoneNumber}"))
                                    context.startActivity(intent)
                                },
                                onDelete = { viewModel.deleteAdvancedCallLog(log.id) },
                                onAnswerBusinessQuestion = { isBusiness ->
                                    viewModel.answerBusinessQuestion(log.id, isBusiness)
                                },
                                onReportSpam = {
                                    viewModel.reportCallLogSpam(log.id)
                                    Toast.makeText(context, "Reported ${log.phoneNumber} as spam", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            } else {
                // In-built Mobile Contacts List
                if (filteredContacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Device Contacts Found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Allow Contacts permission to view mobile contacts here.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredContacts, key = { it.id + it.phoneNumber }) { contact ->
                            DeviceContactItemCard(
                                contact = contact,
                                onCall = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phoneNumber}"))
                                    context.startActivity(intent)
                                },
                                onSms = {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${contact.phoneNumber}"))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Advanced Call Log item card matching the exact specifications:
 * - Circular Left Asset: photo, letter circle (e.g. 'A' for AI Robo), or spam/avatar icon
 * - Mid-Section Stacking (3 rows):
 *     Row 1: Contact Name (or Spam warning e.g. "Likely: Jaydeb Roy") + [HD] and Verified badges
 *     Row 2: Call direction arrow + Location/Label (Home, Mobile, India) + Relative timestamp (• 6 min ago)
 *     Row 3: Active SIM carrier name in accent text color (e.g. "airtel")
 * - Inline Context Card: "Was this a business?" with action buttons
 * - Right-Side Action: Clean phone receiver icon button for instant callback
 */
@Composable
fun AdvancedCallLogItemCard(
    log: AdvancedCallLog,
    onCall: () -> Unit,
    onDelete: () -> Unit,
    onAnswerBusinessQuestion: (Boolean) -> Unit,
    onReportSpam: () -> Unit
) {
    val avatarColors = remember {
        listOf(
            Color(0xFF00E676),
            Color(0xFF00F2FE),
            Color(0xFFFFB300),
            Color(0xFFBD00FF),
            Color(0xFFFF5252),
            Color(0xFF2979FF),
            Color(0xFF00E5FF)
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (log.isSpam) Color(0xFFFF5252).copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // --- 1. CIRCULAR LEFT ASSET ---
                val initialLetter = (log.contactName ?: log.phoneNumber).trim().firstOrNull()?.uppercaseChar() ?: '?'
                val letterColor = remember(log.contactName, log.phoneNumber) {
                    val hash = abs((log.contactName ?: log.phoneNumber).hashCode())
                    avatarColors[hash % avatarColors.size]
                }

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                log.isSpam -> Color(0xFFFF5252).copy(alpha = 0.15f)
                                log.photoUri != null -> Color.Transparent
                                !log.contactName.isNullOrBlank() -> letterColor.copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (log.photoUri != null) {
                        AsyncImage(
                            model = log.photoUri,
                            contentDescription = log.contactName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (log.isSpam) {
                        Icon(
                            imageVector = Icons.Default.SecurityUpdateWarning,
                            contentDescription = "Spam Warning",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(22.dp)
                        )
                    } else if (!log.contactName.isNullOrBlank()) {
                        Text(
                            text = initialLetter.toString(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = letterColor
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PersonOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // --- 2. MID-SECTION STACKING (3 ROWS) ---
                Column(modifier = Modifier.weight(1f)) {
                    // Row 1: Contact Name or Spam Warning + Badges
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (log.isSpam) {
                            Text(
                                text = log.spamWarning ?: "Likely: Spam Caller",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFFF5252),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else {
                            Text(
                                text = log.contactName ?: log.phoneNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        // Verified Business Badge
                        if (log.isVerifiedBusiness) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.15f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Verified",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }

                        // [HD] Badge
                        if (log.isHd) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF00F2FE).copy(alpha = 0.15f))
                                    .border(0.5.dp, Color(0xFF00F2FE).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "[HD]",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00F2FE)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Row 2: Call Direction Arrow + Location/Label + Relative Timestamp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val (directionIcon, directionColor) = when (log.callType) {
                            CallLog.Calls.MISSED_TYPE, CallLog.Calls.REJECTED_TYPE ->
                                Icons.AutoMirrored.Filled.CallMissed to Color(0xFFFF5252)
                            CallLog.Calls.OUTGOING_TYPE ->
                                Icons.AutoMirrored.Filled.CallMade to Color(0xFF00F2FE)
                            else ->
                                Icons.AutoMirrored.Filled.CallReceived to Color(0xFF00E676)
                        }

                        Icon(
                            imageVector = directionIcon,
                            contentDescription = null,
                            tint = directionColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = log.locationLabel,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )

                        Text(
                            text = CallLogManager.formatRelativeCallTime(log.date),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Row 3: Active SIM Card Carrier Name in Accent Text Color
                    Text(
                        text = log.carrierLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00F2FE)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // --- 3. RIGHT-SIDE ACTION BUTTONS ---
                IconButton(
                    onClick = onCall,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Call Back",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // --- 4. INLINE CONTEXT CARD (DYNAMIC BUSINESS INQUIRY) ---
            if ((log.isSpam || log.contactName == null) && !log.isBusinessQuestionAnswered) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Was this a business?",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(
                                onClick = { onAnswerBusinessQuestion(true) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Yes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F2FE))
                            }
                            TextButton(
                                onClick = { onAnswerBusinessQuestion(false) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("No", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(
                                onClick = onReportSpam,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Report Spam", fontSize = 11.sp, color = Color(0xFFFF5252))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * In-built Device Contact item card displaying photo/avatar, contact name, phone number & type,
 * and quick dial / SMS actions.
 */
@Composable
fun DeviceContactItemCard(
    contact: DeviceContact,
    onCall: () -> Unit,
    onSms: () -> Unit
) {
    val avatarColors = remember {
        listOf(
            Color(0xFF00E676),
            Color(0xFF00F2FE),
            Color(0xFFFFB300),
            Color(0xFFBD00FF),
            Color(0xFFFF5252),
            Color(0xFF2979FF)
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = contact.name.trim().firstOrNull()?.uppercaseChar() ?: '?'
            val letterColor = remember(contact.name) {
                val hash = abs(contact.name.hashCode())
                avatarColors[hash % avatarColors.size]
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (contact.photoUri != null) Color.Transparent
                        else letterColor.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (contact.photoUri != null) {
                    AsyncImage(
                        model = contact.photoUri,
                        contentDescription = contact.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = initial.toString(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = letterColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.phoneNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = contact.typeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00F2FE)
                    )
                }
            }

            IconButton(
                onClick = onCall,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676).copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onSms,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00F2FE).copy(alpha = 0.12f))
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "Message",
                    tint = Color(0xFF00F2FE),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
