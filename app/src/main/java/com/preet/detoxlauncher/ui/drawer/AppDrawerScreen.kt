package com.preet.detoxlauncher.ui.drawer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.preet.detoxlauncher.data.model.AppInfo
import com.preet.detoxlauncher.data.repository.LaunchDecision
import com.preet.detoxlauncher.ui.components.AppIconView
import com.preet.detoxlauncher.ui.components.DelayCountdownDialog
import com.preet.detoxlauncher.ui.components.IntentionalPromptDialog
import com.preet.detoxlauncher.ui.components.SearchBarComponent
import com.preet.detoxlauncher.ui.components.SoftBlockDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerScreen(
    viewModel: AppDrawerViewModel,
    onNavigateToIconCustomization: (packageName: String) -> Unit,
    onNavigateToDetoxRule: (packageName: String) -> Unit,
    onStrictBlockRequired: (packageName: String, reason: String, resetTime: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var renamingApp by remember { mutableStateOf<AppInfo?>(null) }
    var renameText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AppDrawerEvent.OpenStrictBlock -> {
                    onStrictBlockRequired(event.packageName, event.reason, event.resetTime)
                }
                is AppDrawerEvent.OpenAppInfo -> {
                    openSystemAppInfo(context, event.packageName)
                }
                is AppDrawerEvent.ShowMessage -> {}
            }
        }
    }

    // Modal Friction Dialogs
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
                    onFinished = { viewModel.onDelayFinished() }
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        // 1. Search Bar
        SearchBarComponent(
            query = state.searchQuery,
            onQueryChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Category Filter Chips
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.categories) { category ->
                val isSelected = category == state.selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. App List / Empty State
        if (state.filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No applications found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try adjusting your search query or category filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                items(
                    items = state.filteredApps,
                    key = { it.packageName }
                ) { appInfo ->
                    AppDrawerRow(
                        appInfo = appInfo,
                        onClick = { viewModel.onAppClicked(appInfo) },
                        onLongClick = { viewModel.selectAppForContext(appInfo) }
                    )
                }
            }
        }
    }

    // Context Menu Bottom Sheet on Long Press
    state.selectedAppForContext?.let { selectedApp ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectAppForContext(null) },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIconView(
                        drawable = selectedApp.iconDrawable,
                        label = selectedApp.displayLabel,
                        size = 48.dp
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = selectedApp.displayLabel,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = selectedApp.packageName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                ContextActionItem(
                    icon = if (selectedApp.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (selectedApp.isFavorite) "Remove from Favorites" else "Add to Favorites",
                    onClick = { viewModel.toggleFavorite(selectedApp) }
                )

                ContextActionItem(
                    icon = Icons.Default.Edit,
                    label = "Rename Application",
                    onClick = {
                        renamingApp = selectedApp
                        renameText = selectedApp.displayLabel
                        viewModel.selectAppForContext(null)
                    }
                )

                ContextActionItem(
                    icon = Icons.Default.Image,
                    label = "Customize Icon",
                    onClick = {
                        viewModel.selectAppForContext(null)
                        onNavigateToIconCustomization(selectedApp.packageName)
                    }
                )

                ContextActionItem(
                    icon = Icons.Default.Block,
                    label = "Configure Detox Limits",
                    onClick = {
                        viewModel.selectAppForContext(null)
                        onNavigateToDetoxRule(selectedApp.packageName)
                    }
                )

                ContextActionItem(
                    icon = Icons.Default.VisibilityOff,
                    label = "Hide from Launcher",
                    onClick = { viewModel.hideApp(selectedApp) }
                )

                ContextActionItem(
                    icon = Icons.Default.Info,
                    label = "System App Info",
                    onClick = {
                        viewModel.selectAppForContext(null)
                        openSystemAppInfo(context, selectedApp.packageName)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Dialog
    renamingApp?.let { appToRename ->
        AlertDialog(
            onDismissRequest = { renamingApp = null },
            title = { Text("Rename Application") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("App Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameApp(appToRename, renameText)
                    renamingApp = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingApp = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppDrawerRow(
    appInfo: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconView(
            drawable = appInfo.iconDrawable,
            label = appInfo.displayLabel,
            size = 46.dp
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appInfo.displayLabel,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (appInfo.isFavorite) {
                Text(
                    text = "Favorite",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ContextActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun openSystemAppInfo(context: Context, packageName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Graceful handling
    }
}
