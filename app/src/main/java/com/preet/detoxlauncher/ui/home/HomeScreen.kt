package com.preet.detoxlauncher.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.repository.LaunchDecision
import com.preet.detoxlauncher.ui.components.AppIconView
import com.preet.detoxlauncher.ui.components.ClockWidget
import com.preet.detoxlauncher.ui.components.DelayCountdownDialog
import com.preet.detoxlauncher.ui.components.IntentionalPromptDialog
import com.preet.detoxlauncher.ui.components.SoftBlockDialog
import com.preet.detoxlauncher.ui.components.UsageSummaryCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onStrictBlockRequired: (packageName: String, reason: String, resetTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refreshUsageMetrics()
        viewModel.events.collect { event ->
            when (event) {
                is HomeUiEvent.OpenStrictBlock -> {
                    onStrictBlockRequired(event.packageName, event.reason, event.resetTime)
                }
                is HomeUiEvent.ShowToast -> {}
            }
        }
    }

    // Modal Friction Overlays
    val pending = state.pendingLaunchApp
    when (val decision = state.activeDecision) {
        is LaunchDecision.RequireIntentionalPrompt -> {
            if (pending != null) {
                IntentionalPromptDialog(
                    appName = pending.displayLabel,
                    onDismiss = { viewModel.dismissPendingLaunch() },
                    onConfirm = { reason -> viewModel.onIntentionalReasonConfirmed(reason) }
                )
            }
        }
        is LaunchDecision.RequireDelay -> {
            if (pending != null) {
                DelayCountdownDialog(
                    appName = pending.displayLabel,
                    totalSeconds = decision.delaySeconds,
                    onCancel = { viewModel.dismissPendingLaunch() },
                    onFinished = { viewModel.onDelayCountdownFinished() }
                )
            }
        }
        is LaunchDecision.SoftBlock -> {
            if (pending != null) {
                SoftBlockDialog(
                    appName = pending.displayLabel,
                    usedMinutes = decision.currentUsageMinutes,
                    limitMinutes = decision.limitMinutes,
                    onCancel = { viewModel.dismissPendingLaunch() },
                    onContinueAnyway = { viewModel.onSoftBlockContinueAnyway() }
                )
            }
        }
        else -> {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onDragEnd = {
                        if (totalDragY < -120f) {
                            // Swiped up -> Open App Drawer
                            onOpenDrawer()
                        } else if (totalDragY > 150f) {
                            // Swiped down -> Expand Notification Shade
                            expandNotificationShade(context)
                        }
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                    }
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Settings Gear Icon at top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Main Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper: Digital Clock & Motivational Message
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ClockWidget(
                    clockSize = state.settings.clockSize,
                    isClockVisible = state.settings.isClockVisible,
                    isDateVisible = state.settings.isDateVisible
                )

                if (state.settings.motivationalMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "“${state.settings.motivationalMessage}”",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Middle: Wellbeing & Screen Time Gauge
            if (state.settings.showUsageSummaryCard) {
                UsageSummaryCard(
                    screenTimeMinutes = state.todayScreenTimeMinutes,
                    targetMinutes = state.settings.dailyScreenTimeTargetMinutes,
                    unlockCount = state.todayUnlockCount,
                    onClick = onOpenAnalytics
                )
            }

            // Lower: Favorite Apps Row & Swipe Up Indicator
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (state.favoriteApps.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        state.favoriteApps.take(5).forEach { appInfo ->
                            FavoriteAppItem(
                                appInfo = appInfo,
                                onClick = { viewModel.onAppClicked(appInfo) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Swipe-up gesture prompt
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onOpenDrawer() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Swipe up for apps",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ALL APPS",
                        style = MaterialTheme.typography.bodySmall.copy(
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteAppItem(
    appInfo: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIconView(
            drawable = appInfo.iconDrawable,
            label = appInfo.displayLabel,
            size = 54.dp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = appInfo.displayLabel,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Expands the notification shade via reflection on StatusBarManager.
 */
private fun expandNotificationShade(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        // Graceful fallback
    }
}
