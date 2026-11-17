package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.AdminModuleCatalog
import com.example.data.model.ManagedApp
import com.example.data.model.UniversalDialog
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HoloGradientButton
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SectionHeader

@Composable
fun AppAndUpdateEditor(
    initial: ManagedApp,
    onDismiss: () -> Unit,
    onSave: (ManagedApp) -> Unit
) {
    var displayName by remember { mutableStateOf(initial.displayName) }
    var appCode by remember { mutableStateOf(initial.appCode) }
    var packageName by remember { mutableStateOf(initial.packageName) }
    var versionName by remember { mutableStateOf(initial.versionName) }
    var versionCodeText by remember { mutableStateOf(initial.versionCode.toString()) }
    var latestVersion by remember { mutableStateOf(initial.latestVersion) }
    var minSupportedVersion by remember { mutableStateOf(initial.minSupportedVersion) }
    var apkDownloadUrl by remember { mutableStateOf(initial.apkDownloadUrl) }
    var updateTitle by remember { mutableStateOf(initial.updateTitle) }
    var updateDescription by remember { mutableStateOf(initial.updateDescription) }
    var releaseNotes by remember { mutableStateOf(initial.releaseNotes) }
    var whatsNew by remember { mutableStateOf(initial.whatsNew) }
    var channelLink by remember { mutableStateOf(initial.channelLink) }
    var button1Text by remember { mutableStateOf(initial.button1Text) }
    var button2Text by remember { mutableStateOf(initial.button2Text) }
    var featureFlagsSummary by remember { mutableStateOf(initial.featureFlagsSummary) }
    var lifecycleStatus by remember { mutableStateOf(initial.lifecycleStatus) }
    var masterEnabled by remember { mutableStateOf(initial.masterEnabled) }
    var popupEnabled by remember { mutableStateOf(initial.popupEnabled) }
    var forceUpdateEnabled by remember { mutableStateOf(initial.forceUpdateEnabled) }
    var laterEnabled by remember { mutableStateOf(initial.laterEnabled) }
    var rollout by remember { mutableFloatStateOf(initial.rolloutPercent.toFloat()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = if (initial.id.isBlank()) "Register Managed App" else "Edit ${initial.displayName}",
            subtitle = "Multi-App isolation + Original Update Manager controls",
            trailing = { TextButton(onClick = onDismiss) { Text("Back") } }
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = displayName, onValueChange = { displayName = it }, label = { Text("App Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = appCode, onValueChange = { appCode = it }, label = { Text("App ID / Code") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = packageName, onValueChange = { packageName = it }, label = { Text("Android Package Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ACTIVE", "DISABLED", "SUSPENDED", "ARCHIVED").forEach { st ->
                    FilterChip(selected = lifecycleStatus == st, onClick = { lifecycleStatus = st }, label = { Text(st) })
                }
            }
        }

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = RoyalPurple)
                Spacer(Modifier.width(8.dp))
                Text("Original Update Manager Controls", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            EditorToggleRow("Master Update ON/OFF", masterEnabled) { masterEnabled = it }
            EditorToggleRow("Popup Dialog ON/OFF", popupEnabled) { popupEnabled = it }
            EditorToggleRow("Mandatory Force Update", forceUpdateEnabled) {
                forceUpdateEnabled = it
                if (it) laterEnabled = false
            }
            EditorToggleRow("Allow 'Later' Button", if (forceUpdateEnabled) false else laterEnabled, enabled = !forceUpdateEnabled) {
                laterEnabled = it
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = versionName, onValueChange = { versionName = it }, label = { Text("Current Ver") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = latestVersion, onValueChange = { latestVersion = it }, label = { Text("Latest Ver") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = minSupportedVersion, onValueChange = { minSupportedVersion = it }, label = { Text("Min Ver") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = versionCodeText, onValueChange = { versionCodeText = it }, label = { Text("Version Code (int)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = apkDownloadUrl, onValueChange = { apkDownloadUrl = it }, label = { Text("APK Download URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = updateTitle, onValueChange = { updateTitle = it }, label = { Text("Popup Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = updateDescription, onValueChange = { updateDescription = it }, label = { Text("Popup Description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = releaseNotes, onValueChange = { releaseNotes = it }, label = { Text("Release Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = whatsNew, onValueChange = { whatsNew = it }, label = { Text("What's New") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = channelLink, onValueChange = { channelLink = it }, label = { Text("Channel Link") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = button1Text, onValueChange = { button1Text = it }, label = { Text("Button 1") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = button2Text, onValueChange = { button2Text = it }, label = { Text("Button 2") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text("Staged Rollout: ${rollout.toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(value = rollout, onValueChange = { rollout = it }, valueRange = 0f..100f)
            OutlinedTextField(value = featureFlagsSummary, onValueChange = { featureFlagsSummary = it }, label = { Text("Feature Flags") }, modifier = Modifier.fillMaxWidth())
        }

        HoloGradientButton(
            text = "Save App & Update Policy",
            onClick = {
                onSave(
                    initial.copy(
                        appCode = appCode.trim().ifBlank { "app" },
                        packageName = packageName.trim().ifBlank { "com.pranav.app" },
                        displayName = displayName.trim().ifBlank { "PRANAV Client" },
                        versionName = versionName.trim().ifBlank { "1.0.0" },
                        versionCode = versionCodeText.toLongOrNull()?.coerceAtLeast(1L) ?: 1L,
                        lifecycleStatus = lifecycleStatus,
                        masterEnabled = masterEnabled,
                        popupEnabled = popupEnabled,
                        forceUpdateEnabled = forceUpdateEnabled,
                        laterEnabled = if (forceUpdateEnabled) false else laterEnabled,
                        latestVersion = latestVersion.trim(),
                        minSupportedVersion = minSupportedVersion.trim(),
                        apkDownloadUrl = apkDownloadUrl.trim(),
                        updateTitle = updateTitle.trim(),
                        updateDescription = updateDescription.trim(),
                        releaseNotes = releaseNotes.trim(),
                        whatsNew = whatsNew.trim(),
                        channelLink = channelLink.trim(),
                        button1Text = button1Text.trim(),
                        button2Text = button2Text.trim(),
                        rolloutPercent = rollout.toLong(),
                        featureFlagsSummary = featureFlagsSummary.trim()
                    )
                )
            }
        )
    }
}

@Composable
fun UniversalDialogEditor(
    initial: UniversalDialog,
    apps: List<ManagedApp>,
    onDismiss: () -> Unit,
    onSave: (UniversalDialog) -> Unit
) {
    var appId by remember { mutableStateOf(initial.appId) }
    var dialogType by remember { mutableStateOf(initial.dialogType) }
    var title by remember { mutableStateOf(initial.title) }
    var description by remember { mutableStateOf(initial.description) }
    var primaryBtn by remember { mutableStateOf(initial.primaryButtonText) }
    var primaryUrl by remember { mutableStateOf(initial.primaryActionUrl) }
    var secondaryBtn by remember { mutableStateOf(initial.secondaryButtonText) }
    var animationStyle by remember { mutableStateOf(initial.animationStyle) }
    var soundName by remember { mutableStateOf(initial.soundName) }
    var dismissible by remember { mutableStateOf(initial.dismissible) }
    var laterAllowed by remember { mutableStateOf(initial.laterAllowed) }
    var enabled by remember { mutableStateOf(initial.enabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Universal Dialog Designer",
            subtitle = "Configure any dialog category with full behavior overrides",
            trailing = { TextButton(onClick = onDismiss) { Text("Back") } }
        )

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Dialog Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            AdminModuleCatalog.dialogTypes.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { t ->
                        FilterChip(selected = dialogType == t, onClick = { dialogType = t }, label = { Text(t) })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = appId, onValueChange = { appId = it }, label = { Text("Target App ID (${apps.joinToString { it.id }.ifBlank { "global" }})") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = primaryBtn, onValueChange = { primaryBtn = it }, label = { Text("Primary Button") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = secondaryBtn, onValueChange = { secondaryBtn = it }, label = { Text("Secondary Button") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = primaryUrl, onValueChange = { primaryUrl = it }, label = { Text("Action URL") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = animationStyle, onValueChange = { animationStyle = it }, label = { Text("Animation") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = soundName, onValueChange = { soundName = it }, label = { Text("Sound") }, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            EditorToggleRow("Enabled", enabled) { enabled = it }
            EditorToggleRow("Dismissible", dismissible) { dismissible = it }
            EditorToggleRow("Allow Later", laterAllowed) { laterAllowed = it }
        }

        HoloGradientButton(
            text = "Save Universal Dialog",
            onClick = {
                onSave(
                    initial.copy(
                        appId = appId.trim().ifBlank { "global" },
                        dialogType = dialogType,
                        title = title.trim().ifBlank { "Notification" },
                        description = description.trim(),
                        primaryButtonText = primaryBtn.trim(),
                        primaryActionUrl = primaryUrl.trim(),
                        secondaryButtonText = secondaryBtn.trim(),
                        animationStyle = animationStyle.trim(),
                        soundName = soundName.trim(),
                        dismissible = dismissible,
                        laterAllowed = laterAllowed,
                        enabled = enabled
                    )
                )
            }
        )
    }
}

@Composable
private fun EditorToggleRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}
