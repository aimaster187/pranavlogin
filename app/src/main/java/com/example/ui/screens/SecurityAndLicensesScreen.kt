package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.LicenseRecord
import com.example.data.model.ManagedApp
import com.example.data.model.ManagedDevice
import com.example.data.model.UiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusPill

@Composable
fun SecurityAndLicensesScreen(
    appsState: UiState<List<ManagedApp>>,
    devicesState: UiState<List<ManagedDevice>>,
    licensesState: UiState<List<LicenseRecord>>,
    onRegisterDevice: (appId: String, label: String, apiLevel: Long) -> Unit,
    onUpdateDeviceApproval: (ManagedDevice, String) -> Unit,
    onGenerateLicense: (appId: String, tier: String, days: Long, graceDays: Long, paymentVerified: Boolean, cooldownHours: Long) -> Unit,
    onUpdateLicenseStatus: (LicenseRecord, String, Long) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val apps = (appsState as? UiState.Success)?.data.orEmpty()
    val devices = (devicesState as? UiState.Success)?.data.orEmpty()
    val licenses = (licensesState as? UiState.Success)?.data.orEmpty()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Licenses & VIP (${licenses.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Devices & Risk (${devices.size})") })
        }

        if (selectedTab == 0) {
            var selectedTier by remember { mutableStateOf("VIP_PRO") }
            var durationDaysText by remember { mutableStateOf("30") }
            var graceDaysText by remember { mutableStateOf("3") }
            var cooldownHoursText by remember { mutableStateOf("24") }
            var paymentVerified by remember { mutableStateOf(true) }
            var targetAppId by remember { mutableStateOf(apps.firstOrNull()?.id ?: "global") }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = MetallicGold)
                            Spacer(Modifier.width(8.dp))
                            Text("License & VIP Generator", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("STANDARD", "VIP_PRO", "VIP_ELITE").forEach { tier ->
                                FilterChip(selected = selectedTier == tier, onClick = { selectedTier = tier }, label = { Text(tier) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = targetAppId,
                            onValueChange = { targetAppId = it },
                            label = { Text("App ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = durationDaysText, onValueChange = { durationDaysText = it }, label = { Text("Days") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = graceDaysText, onValueChange = { graceDaysText = it }, label = { Text("Grace") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = cooldownHoursText, onValueChange = { cooldownHoursText = it }, label = { Text("Cooldown h") }, modifier = Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Payment Verified", style = MaterialTheme.typography.bodyMedium)
                                Text("VIP keys require verified payment before activation", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = paymentVerified, onCheckedChange = { paymentVerified = it })
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                onGenerateLicense(
                                    targetAppId.trim().ifBlank { "global" },
                                    selectedTier,
                                    durationDaysText.toLongOrNull()?.coerceIn(1L, 3650L) ?: 30L,
                                    graceDaysText.toLongOrNull()?.coerceIn(0L, 90L) ?: 3L,
                                    paymentVerified,
                                    cooldownHoursText.toLongOrNull()?.coerceIn(0L, 8760L) ?: 24L
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("generate_license_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Generate Single-Device License Key")
                        }
                    }
                }

                items(licenses, key = { it.id }) { lic ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lic.licenseKey, style = MaterialTheme.typography.titleSmall, color = ElectricBlue)
                            StatusPill(
                                label = lic.status,
                                color = when (lic.status) {
                                    "ACTIVE" -> EmeraldSuccess
                                    "UNUSED" -> RoyalPurple
                                    "SUSPENDED" -> AmberWarning
                                    else -> CrimsonError
                                }
                            )
                        }
                        Text(
                            text = "${lic.planTier} • ${lic.durationDays}d (grace ${lic.graceDays}d) • Payment: ${if (lic.paymentVerified) "VERIFIED" else "UNVERIFIED"} • Transfer Cooldown: ${lic.transferCooldownHours}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (lic.boundDeviceId.isNotBlank()) {
                            Text("Bound Device: ${lic.boundDeviceId}", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cb?.setPrimaryClip(ClipData.newPlainText("PRANAV License", lic.licenseKey))
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Copy")
                            }
                            TextButton(onClick = { onUpdateLicenseStatus(lic, "ACTIVE", 0L) }) { Text("Activate") }
                            TextButton(onClick = { onUpdateLicenseStatus(lic, "ACTIVE", 30L) }) { Text("+30d") }
                            TextButton(onClick = { onUpdateLicenseStatus(lic, "SUSPENDED", 0L) }) { Text("Suspend") }
                            TextButton(onClick = { onUpdateLicenseStatus(lic, "REVOKED", 0L) }) { Text("Revoke", color = CrimsonError) }
                        }
                    }
                }
            }
        } else {
            var deviceLabel by remember { mutableStateOf("Pixel 9 Pro Test") }
            var deviceAppId by remember { mutableStateOf(apps.firstOrNull()?.id ?: "global") }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text("Register Device (UUID + Keystore Identity)", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Never uses IMEI or MAC address. Uses random installation UUID + Android Keystore key fingerprint.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = deviceLabel, onValueChange = { deviceLabel = it }, label = { Text("Device Label") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = deviceAppId, onValueChange = { deviceAppId = it }, label = { Text("App ID") }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onRegisterDevice(deviceAppId.trim().ifBlank { "global" }, deviceLabel.trim(), 35L) },
                            modifier = Modifier.fillMaxWidth().testTag("register_device_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Register Installation Identity")
                        }
                    }
                }

                items(devices, key = { it.id }) { dev ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dev.deviceLabel, style = MaterialTheme.typography.titleMedium)
                                Text("UUID: ${dev.installUuid}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                Text("Keystore SHA: ${dev.keystoreKeyHash.take(24)}...", style = MaterialTheme.typography.labelSmall)
                            }
                            StatusPill(
                                label = dev.approvalStatus,
                                color = when (dev.approvalStatus) {
                                    "APPROVED" -> EmeraldSuccess
                                    "PENDING" -> AmberWarning
                                    else -> CrimsonError
                                }
                            )
                        }
                        Text(
                            text = "App: ${dev.appId} • API ${dev.androidApi} • Risk Score: ${dev.riskScore}/100 • Transfers: ${dev.transferCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = {
                                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                cb?.setPrimaryClip(ClipData.newPlainText("PRANAV Device ID", dev.installUuid))
                            }) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy device ID")
                            }
                            IconButton(onClick = { onUpdateDeviceApproval(dev, "APPROVED") }) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Approve device", tint = EmeraldSuccess)
                            }
                            IconButton(onClick = { onUpdateDeviceApproval(dev, "PENDING") }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Reset pending", tint = AmberWarning)
                            }
                            IconButton(onClick = { onUpdateDeviceApproval(dev, "BLOCKED") }) {
                                Icon(Icons.Filled.Block, contentDescription = "Block device", tint = CrimsonError)
                            }
                        }
                    }
                }
            }
        }
    }
}
