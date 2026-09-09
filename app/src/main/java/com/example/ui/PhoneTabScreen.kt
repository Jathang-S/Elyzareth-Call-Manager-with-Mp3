package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallLogEntity
import com.example.data.ContactEntity
import com.example.viewmodel.CallSmsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneTabScreen(
    viewModel: CallSmsViewModel,
    onOpenDialpad: () -> Unit
) {
    val context = LocalContext.current
    val callLogs by viewModel.allCallLogs.collectAsState()
    val contacts by viewModel.allContacts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Recent Calls, 1 = Contacts
    var showAddContactDialog by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    val filteredLogs = remember(callLogs, searchQuery) {
        if (searchQuery.isBlank()) callLogs
        else callLogs.filter {
            it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
            (it.callerName?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) contacts
        else contacts.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.phoneNumber.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenDialpad,
                containerColor = Color(0xFF00F2FE),
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
                        text = "Local telephony shield & caller identity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedSubTab == 1) {
                    IconButton(
                        onClick = { showAddContactDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF00F2FE).copy(alpha = 0.15f))
                            .testTag("add_contact_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = "Add Contact",
                            tint = Color(0xFF00F2FE)
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or number...") },
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

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(12.dp))

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
                                text = "No Recent Calls",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Simulate a call from dialpad or wait for incoming calls.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Call History",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { viewModel.clearCallLogs() },
                            modifier = Modifier.testTag("clear_call_history_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 11.sp)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(filteredLogs, key = { it.id }) { log ->
                            CallLogItemCard(
                                log = log,
                                onCall = {
                                    viewModel.simulateIncomingCall(log.phoneNumber)
                                    Toast.makeText(context, "Simulating call from ${log.phoneNumber}", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.deleteCallLog(log.id) },
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            } else {
                // Contacts List
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
                                text = "No Contacts Found",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 72.dp)
                    ) {
                        items(filteredContacts, key = { it.id }) { contact ->
                            ContactItemCard(
                                contact = contact,
                                onCall = {
                                    viewModel.simulateIncomingCall(contact.phoneNumber)
                                    Toast.makeText(context, "Calling ${contact.name}...", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = { viewModel.removeContact(contact) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AddContactModalDialog(
            onDismiss = { showAddContactDialog = false },
            onSave = { name, number, category, isBlocked ->
                viewModel.addContact(number, name, category, null, isBlocked)
                showAddContactDialog = false
                Toast.makeText(context, "Contact saved locally", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CallLogItemCard(
    log: CallLogEntity,
    onCall: () -> Unit,
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
                if (log.isSpam) Color(0xFFFF5252).copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Direction / Type Icon
            val (icon, tint) = when {
                log.isSpam -> Icons.Default.ReportProblem to Color(0xFFFF5252)
                log.callType == "MISSED" -> Icons.AutoMirrored.Filled.CallMissed to Color(0xFFFF5252)
                log.callType == "OUTGOING" -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF00F2FE)
                else -> Icons.AutoMirrored.Filled.CallReceived to Color(0xFF00E676)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = log.callType, tint = tint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.callerName ?: log.phoneNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (log.isSpam) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFF5252).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SPAM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.phoneNumber,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                if (log.durationSeconds > 0) {
                    Text(
                        text = "Duration: ${log.durationSeconds / 60}m ${log.durationSeconds % 60}s",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Action Buttons
            IconButton(
                onClick = onCall,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Delete Log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    onCall: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when (contact.category) {
                    "SPAM" -> Color(0xFFFF5252).copy(alpha = 0.3f)
                    "BUSINESS" -> Color(0xFF00F2FE).copy(alpha = 0.3f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                },
                RoundedCornerShape(14.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (badgeBg, badgeTint, badgeIcon) = when (contact.category) {
                "SPAM" -> Triple(Color(0xFFFF5252).copy(alpha = 0.15f), Color(0xFFFF5252), Icons.Default.ReportProblem)
                "BUSINESS" -> Triple(Color(0xFF00F2FE).copy(alpha = 0.15f), Color(0xFF00F2FE), Icons.Default.Business)
                else -> Triple(Color(0xFFBD00FF).copy(alpha = 0.15f), Color(0xFFBD00FF), Icons.Default.Person)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(badgeIcon, contentDescription = contact.category, tint = badgeTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = contact.category,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTint
                        )
                    }
                }

                Text(
                    text = contact.phoneNumber,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onCall, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF00E676), modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun AddContactModalDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, category: String, isBlocked: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("PERSONAL") }
    var isBlocked by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Local Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_name_input")
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+1 (555) 000-0000") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_contact_phone_input")
                )

                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("PERSONAL", "BUSINESS", "SPAM").forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = {
                                category = cat
                                if (cat == "SPAM") isBlocked = true
                            },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isBlocked,
                        onCheckedChange = { isBlocked = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Auto-block & silence calls", fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onSave(name, number, category, isBlocked)
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
