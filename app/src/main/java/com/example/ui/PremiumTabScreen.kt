package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CallSmsViewModel

@Composable
fun PremiumTabScreen(viewModel: CallSmsViewModel) {
    val context = LocalContext.current
    var selectedPlan by remember { mutableIntStateOf(1) } // 0 = Monthly, 1 = Lifetime
    var isVipActive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        // VIP Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF14121E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFBD00FF), Color(0xFF00F2FE))),
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFFFFD700).copy(alpha = 0.12f),
                                    Color(0xFFBD00FF).copy(alpha = 0.08f),
                                    Color(0xFF0D0B14)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // VIP Crown Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700).copy(alpha = 0.18f))
                                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "VIP Crown",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ELYZARETH VIP STUDIO",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = Color(0xFFFFD700)
                        )

                        Text(
                            text = "Audiophile DSP Engine & Autonomous Telephony Shield",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isVipActive) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("VIP PASS ACTIVE", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Features Checklist
        item {
            Text(
                text = "Studio Exclusive Features",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VipFeatureRow(
                    icon = Icons.Default.MusicVideo,
                    title = "Unlimited VIP Ringtone Stem Loops",
                    description = "Extract vocal, bass, and beat stems to make custom contact ringtones.",
                    tint = Color(0xFF00F2FE)
                )
                VipFeatureRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Pro 64-Bit DSP Spatial Engine",
                    description = "Ultra-low latency equalizer with HRTF 3D surround sound.",
                    tint = Color(0xFFBD00FF)
                )
                VipFeatureRow(
                    icon = Icons.Default.Shield,
                    title = "Real-Time Telephony Threat Defense",
                    description = "Instant robo-caller drop and neural SMS phishing blocker.",
                    tint = Color(0xFFFF5252)
                )
                VipFeatureRow(
                    icon = Icons.Default.CloudSync,
                    title = "Encrypted Local & Cloud Vault Backup",
                    description = "Seamlessly backup contacts, customized EQ presets, and blocked spam lists.",
                    tint = Color(0xFFFFD700)
                )
            }
        }

        // Pricing Cards
        item {
            Text(
                text = "Choose Your Membership",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Monthly Plan
                MembershipCard(
                    title = "Monthly Pass",
                    price = "$4.99",
                    period = "/ month",
                    subtitle = "Billed monthly, cancel anytime",
                    isSelected = selectedPlan == 0,
                    badge = null,
                    onClick = { selectedPlan = 0 },
                    modifier = Modifier.weight(1f)
                )

                // Lifetime Plan
                MembershipCard(
                    title = "Lifetime VIP",
                    price = "$29.99",
                    period = "one-time",
                    subtitle = "All future updates & stems included",
                    isSelected = selectedPlan == 1,
                    badge = "BEST VALUE",
                    onClick = { selectedPlan = 1 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Action Buttons
        item {
            Button(
                onClick = {
                    isVipActive = true
                    Toast.makeText(context, "VIP Studio Activated! All features unlocked.", Toast.LENGTH_LONG).show()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD700)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("unlock_vip_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isVipActive) "Manage VIP Pass" else "Unlock Elyzareth VIP Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(
                onClick = {
                    isVipActive = true
                    Toast.makeText(context, "Purchases restored successfully.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().testTag("restore_purchases_btn")
            ) {
                Text(
                    text = "Restore Previous Purchases",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 100% Offline Guarantee Footer
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "100% Offline & Private. No audio or contact data is ever transmitted to external servers.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VipFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    tint: Color
) {
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
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun MembershipCard(
    title: String,
    price: String,
    period: String,
    subtitle: String,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1B182B) else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                badge?.let {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFD700))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = it,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " $period",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
