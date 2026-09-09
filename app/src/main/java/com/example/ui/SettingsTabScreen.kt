package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.CallSmsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTabScreen(viewModel: CallSmsViewModel) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    val categories = listOf(
        "Equalizer DSP" to Icons.Default.Equalizer,
        "Background Studio" to Icons.Default.Wallpaper,
        "Scanner & Security" to Icons.Default.Security,
        "Themes & Styles" to Icons.Default.Palette,
        "Local Audio" to Icons.Default.MusicNote,
        "About & FAQ" to Icons.Default.HelpOutline
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Audio Lab & Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Hardware DSP equalizer, wallpapers, and spam rules",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Horizontal Category Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            contentColor = Color(0xFF00F2FE),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            categories.forEachIndexed { index, (name, icon) ->
                Tab(
                    selected = selectedCategory == index,
                    onClick = { selectedCategory = index },
                    modifier = Modifier.testTag("settings_tab_$index"),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategory == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Section Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedCategory) {
                0 -> EqualizerSection(viewModel = viewModel)
                1 -> BackgroundStudioSection(viewModel = viewModel)
                2 -> ScannerSection(viewModel = viewModel)
                3 -> VisualizerThemeSection(viewModel = viewModel)
                4 -> LocalMp3Section(viewModel = viewModel)
                else -> AboutAndHelpSection(viewModel = viewModel)
            }
        }
    }
}
