package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

enum class AppTab(
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val testTag: String
) {
    TRACKER("Bugün", Icons.Filled.Home, Icons.Outlined.Home, "tab_today"),
    PARTNER("Arkadaşım", Icons.Filled.People, Icons.Outlined.People, "tab_partner"),
    KAZA("Kaza", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_kaza"),
    REPORT("Karne", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "tab_report")
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.TRACKER) }

    // If on another tab and back pressed, return to TRACKER
    BackHandler(enabled = selectedTab != AppTab.TRACKER) {
        selectedTab = AppTab.TRACKER
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground),
        containerColor = WarmCreamBackground,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            // Clean Bottom Navigation Bar Matching Images 1 & 2
            Surface(
                color = PureWhite,
                shadowElevation = 4.dp,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, CardBorderColor)
            ) {
                NavigationBar(
                    containerColor = PureWhite,
                    tonalElevation = 0.dp
                ) {
                    AppTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = EmeraldPrimary,
                                selectedTextColor = EmeraldPrimary,
                                indicatorColor = Color.Transparent, // No pill, matches screenshot
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(WarmCreamBackground)
                .padding(paddingValues)
        ) {
            Crossfade(targetState = selectedTab, label = "tab_transition") { tab ->
                when (tab) {
                    AppTab.TRACKER -> TrackerScreen(
                        viewModel = viewModel,
                        onNavigateToPartner = { selectedTab = AppTab.PARTNER },
                        onRequestPermissions = onRequestPermissions
                    )
                    AppTab.PARTNER -> PartnerScreen(viewModel = viewModel)
                    AppTab.KAZA -> KazaScreen(viewModel = viewModel)
                    AppTab.REPORT -> ReportScreen(viewModel = viewModel)
                }
            }

            // In-App GitHub APK Update Dialog
            val availableUpdate by viewModel.availableUpdate.collectAsState()
            val downloadProgress by viewModel.downloadProgress.collectAsState()
            val updateDownloadError by viewModel.updateDownloadError.collectAsState()

            if (availableUpdate != null && availableUpdate?.hasUpdate == true) {
                com.example.ui.components.AppUpdateDialog(
                    updateInfo = availableUpdate!!,
                    downloadProgress = downloadProgress,
                    errorMessage = updateDownloadError,
                    onStartDownload = { viewModel.startApkUpdateDownload() },
                    onDismiss = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }
}
