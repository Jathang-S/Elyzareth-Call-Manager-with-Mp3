package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.LogEntity
import com.example.viewmodel.CallSmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesTabScreen(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    val allLogs by viewModel.allLogs.collectAsState()
    val spamKeywords by viewModel.allSpamKeywords.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Personal, 2 = Spam Shielded
    var showComposeDialog by remember { mutableStateOf(false) }
    var showKeywordsDialog by remember { mutableStateOf(false) }

    // Check RECEIVE_SMS permission
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasSmsPermission = isGranted
        if (isGranted) {
            Toast.makeText(context, "SMS interceptor activated", Toast.LENGTH_SHORT).show()
        }
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    // Filter SMS logs
    val smsLogs = remember(allLogs, searchQuery, selectedFilter) {
        allLogs.filter { it.type == "SMS" || it.messageBody != null }
            .filter { log ->
                when (selectedFilter) {
                    1 -> !log.isSpam
                    2 -> log.isSpam
                    else -> true
                }
            }
            .filter { log ->
                if (searchQuery.isBlank()) true
                else {
                    log.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                    (log.senderName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (log.messageBody?.contains(searchQuery, ignoreCase = true) == true)
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showComposeDialog = true },
                containerColor = Color(0xFF00F2FE),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("messages_fab_compose")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "Compose or Test SMS",
                    modifier = Modifier.size(24.dp)
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
            // Screen Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real-time SMS screening & spam filtering",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showKeywordsDialog = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFBD00FF).copy(alpha = 0.15f))
                        .testTag("keywords_manager_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Spam Keywords",
                        tint = Color(0xFFBD00FF)
                    )
                }
            }

            // 1. Permission Alert Card (if missing RECEIVE_SMS)
            if (!hasSmsPermission) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color(0xFFFF9800).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = "Warning",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS Interceptor Inactive",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Grant SMS permission to automatically inspect incoming phishing messages.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECEIVE_SMS) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Activate", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search messages, senders, text...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    .testTag("messages_search_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == 0,
                    onClick = { selectedFilter = 0 },
                    label = { Text("All (${allLogs.count { it.type == "SMS" || it.messageBody != null }})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == 1,
                    onClick = { selectedFilter = 1 },
                    label = { Text("Personal", fontSize = 12.sp) },
                    leadingIcon = if (selectedFilter == 1) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )
                FilterChip(
                    selected = selectedFilter == 2,
                    onClick = { selectedFilter = 2 },
                    label = { Text("Spam Shielded", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF5252).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFFF5252)
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Messages List
            if (smsLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Inbox Clean",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Incoming physical SMS or simulated tests will appear here.",
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 72.dp)
                ) {
                    items(smsLogs, key = { it.id }) { log ->
                        MessageItemCard(
                            log = log,
                            onDelete = { viewModel.deleteLog(log.id) },
                            dateFormat = dateFormat
                        )
                    }
                }
            }
        }
    }

    // Compose / Test SMS Dialog
    if (showComposeDialog) {
        ComposeTestSmsDialog(
            viewModel = viewModel,
            onDismiss = { showComposeDialog = false }
        )
    }

    // Spam Keywords Manager Dialog
    if (showKeywordsDialog) {
        SpamKeywordsDialog(
            viewModel = viewModel,
            keywords = spamKeywords,
            onDismiss = { showKeywordsDialog = false }
        )
    }
}

@Composable
fun MessageItemCard(
    log: LogEntity,
    onDelete: () -> Unit,
    dateFormat: SimpleDateFormat
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (log.isSpam) Color(0xFFFF5252).copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (log.isSpam) Color(0xFFFF5252).copy(alpha = 0.15f)
                                else Color(0xFF00F2FE).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.isSpam) Icons.Default.ReportProblem else Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (log.isSpam) Color(0xFFFF5252) else Color(0xFF00F2FE),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = log.senderName ?: log.phoneNumber,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (log.isSpam) Color(0xFFFF5252).copy(alpha = 0.18f)
                                else Color(0xFF00E676).copy(alpha = 0.18f)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (log.isSpam) "SPAM SHIELD" else "DELIVERED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (log.isSpam) Color(0xFFFF5252) else Color(0xFF00E676)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            Text(
                text = log.messageBody ?: "(Empty message)",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (log.isSpam) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFF5252).copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Trigger: ${log.actionTaken}",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color(0xFFFF5252)
                    )
                }
            }
        }
    }
}

@Composable
fun ComposeTestSmsDialog(
    viewModel: CallSmsViewModel,
    onDismiss: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("+18005550199") }
    var messageBody by remember { mutableStateOf("Congratulations! You won a $1,000 gift card. Click bit.ly/claim to verify.") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Screen or Test Incoming SMS", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Simulate an incoming SMS to test local spam keyword detection and auto-classification.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Sender Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("compose_test_sms_phone")
                )
                OutlinedTextField(
                    value = messageBody,
                    onValueChange = { messageBody = it },
                    label = { Text("SMS Message Body") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("compose_test_sms_body")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (phoneNumber.isNotBlank() && messageBody.isNotBlank()) {
                        viewModel.simulateIncomingSms(phoneNumber, messageBody)
                        Toast.makeText(context, "Testing SMS screening...", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analyze & Screen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SpamKeywordsDialog(
    viewModel: CallSmsViewModel,
    keywords: List<com.example.data.SpamKeywordEntity>,
    onDismiss: () -> Unit
) {
    var newKeyword by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SMS Spam Keywords (${keywords.size})", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Texts containing these keywords will be classified as Spam and silenced locally.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newKeyword,
                        onValueChange = { newKeyword = it },
                        placeholder = { Text("e.g. loan, winner, lottery") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("dialog_new_keyword_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newKeyword.isNotBlank()) {
                                viewModel.addSpamKeyword(newKeyword.trim())
                                newKeyword = ""
                                Toast.makeText(context, "Keyword added", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(keywords, key = { it.id }) { kw ->
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = kw.keyword,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                                IconButton(
                                    onClick = { viewModel.removeSpamKeyword(kw) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove keyword",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFF5252)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
