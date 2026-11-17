package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ManagedApp
import com.example.data.model.UiState
import com.example.data.model.UniversalDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun AppsAndUpdateScreen(
    ownerUid: String,
    appsState: UiState<List<ManagedApp>>,
    dialogsState: UiState<List<UniversalDialog>>,
    onSaveApp: (ManagedApp) -> Unit,
    onDeleteApp: (ManagedApp) -> Unit,
    onSaveDialog: (UniversalDialog) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var editingApp by remember { mutableStateOf<ManagedApp?>(null) }
    var editingDialog by remember { mutableStateOf<UniversalDialog?>(null) }
    var previewDialog by remember { mutableStateOf<UniversalDialog?>(null) }
    var confirmDeleteApp by remember { mutableStateOf<ManagedApp?>(null) }

    val apps = (appsState as? UiState.Success)?.data.orEmpty()
    val dialogs = (dialogsState as? UiState.Success)?.data.orEmpty()

    if (editingApp != null) {
        BackHandler { editingApp = null }
        AppAndUpdateEditor(
            initial = editingApp!!,
            onDismiss = { editingApp = null },
            onSave = {
                onSaveApp(it)
                editingApp = null
            }
        )
        return
    }

    if (editingDialog != null) {
        BackHandler { editingDialog = null }
        UniversalDialogEditor(
            initial = editingDialog!!,
            apps = apps,
            onDismiss = { editingDialog = null },
            onSave = {
                onSaveDialog(it)
                editingDialog = null
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Apps & Updates (${apps.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Universal Dialogs (${dialogs.size})") })
        }

        if (selectedTab == 0) {
            val filtered = remember(apps, searchQuery, statusFilter) {
                apps.filter { app ->
                    val matchesQuery = searchQuery.isBlank() ||
                        app.displayName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true) ||
                        app.appCode.contains(searchQuery, ignoreCase = true)
                    val matchesStatus = statusFilter == "ALL" || app.lifecycleStatus == statusFilter
                    matchesQuery && matchesStatus
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Multi-App & Update Manager",
                        subtitle = "Isolated per-app config, staged rollout & force update",
                        trailing = {
                            Button(
                                onClick = {
                                    editingApp = ManagedApp(
                                        ownerUid = ownerUid,
                                        appCode = "app_${apps.size + 1}",
                                        packageName = "com.pranav.client${apps.size + 1}",
                                        displayName = "Managed App ${apps.size + 1}"
                                    )
                                },
                                modifier = Modifier.testTag("create_app_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("New App")
                            }
                        }
                    )
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search apps by name, code, or package") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().testTag("apps_search_input"),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ALL", "ACTIVE", "DISABLED", "SUSPENDED", "ARCHIVED").forEach { st ->
                            FilterChip(
                                selected = statusFilter == st,
                                onClick = { statusFilter = st },
                                label = { Text(st) }
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No managed apps found", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Tap 'New App' to register your first authorized PRANAV managed APK with isolated update, license, and dialog policies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { app ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(app.displayName, style = MaterialTheme.typography.titleMedium)
                                    Text(app.packageName, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                                }
                                StatusPill(
                                    label = app.lifecycleStatus,
                                    color = when (app.lifecycleStatus) {
                                        "ACTIVE" -> EmeraldSuccess
                                        "SUSPENDED" -> AmberWarning
                                        else -> CrimsonError
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "v${app.versionName} -> Latest v${app.latestVersion} (Min v${app.minSupportedVersion}) • Rollout ${app.rolloutPercent}% • DEX ${app.dexReleaseVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Master", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = app.masterEnabled,
                                        onCheckedChange = { onSaveApp(app.copy(masterEnabled = it)) }
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Force Update", style = MaterialTheme.typography.labelLarge)
                                    Spacer(Modifier.width(6.dp))
                                    Switch(
                                        checked = app.forceUpdateEnabled,
                                        onCheckedChange = {
                                            onSaveApp(
                                                app.copy(
                                                    forceUpdateEnabled = it,
                                                    laterEnabled = if (it) false else app.laterEnabled
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { editingApp = app }) {
                                    Icon(Icons.Filled.Edit, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Configure Update & App")
                                }
                                IconButton(onClick = { confirmDeleteApp = app }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete app", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Universal Dialog Engine",
                        subtitle = "Single engine for update, VIP, license, device, maintenance & custom dialogs",
                        trailing = {
                            Button(
                                onClick = {
                                    editingDialog = UniversalDialog(
                                        ownerUid = ownerUid,
                                        appId = apps.firstOrNull()?.id ?: "global",
                                        dialogType = "NORMAL_UPDATE",
                                        title = "New Release Available",
                                        description = "Upgrade to unlock the latest stability and VIP improvements."
                                    )
                                },
                                modifier = Modifier.testTag("create_dialog_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("New Dialog")
                            }
                        }
                    )
                }

                if (dialogs.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No universal dialogs configured", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Create reusable dialogs for Normal/Force Updates, License Activation, VIP Upgrades, Device Approvals, Channel Verification, or Maintenance.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(dialogs, key = { it.id }) { dlg ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(dlg.title, style = MaterialTheme.typography.titleMedium)
                                    Text("${dlg.dialogType} • App: ${dlg.appId} • Priority ${dlg.priority}", style = MaterialTheme.typography.labelSmall, color = RoyalPurple)
                                }
                                Switch(
                                    checked = dlg.enabled,
                                    onCheckedChange = { onSaveDialog(dlg.copy(enabled = it)) }
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(dlg.description, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { previewDialog = dlg }) {
                                    Icon(Icons.Filled.Preview, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Preview")
                                }
                                Spacer(Modifier.width(8.dp))
                                TextButton(onClick = { editingDialog = dlg }) {
                                    Text("Edit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewDialog != null) {
        val d = previewDialog!!
        AlertDialog(
            onDismissRequest = { if (d.dismissible) previewDialog = null },
            icon = { Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = RoyalPurple) },
            title = { Text(d.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(label = d.dialogType, color = ElectricBlue)
                    Text(d.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "Anim: ${d.animationStyle} • Sound: ${d.soundName} • Dismissible: ${d.dismissible}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { previewDialog = null }) {
                    Text(d.primaryButtonText.ifBlank { "Continue" })
                }
            },
            dismissButton = if (d.laterAllowed) {
                {
                    TextButton(onClick = { previewDialog = null }) {
                        Text(d.secondaryButtonText.ifBlank { "Later" })
                    }
                }
            } else null
        )
    }

    if (confirmDeleteApp != null) {
        val target = confirmDeleteApp!!
        AlertDialog(
            onDismissRequest = { confirmDeleteApp = null },
            title = { Text("Delete ${target.displayName}?") },
            text = { Text("This removes the app configuration (${target.packageName}) from Firestore. Confirm permanent deletion?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteApp(target)
                        confirmDeleteApp = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonError, contentColor = Color.White)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteApp = null }) { Text("Cancel") }
            }
        )
    }
}
