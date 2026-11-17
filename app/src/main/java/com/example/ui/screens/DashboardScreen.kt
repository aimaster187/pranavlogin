package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AdminSettingsState
import com.example.data.model.AdminEvent
import com.example.data.model.DexRelease
import com.example.data.model.HealthResponse
import com.example.data.model.LicenseRecord
import com.example.data.model.ManagedApp
import com.example.data.model.ManagedDevice
import com.example.data.model.OwnerWorkspace
import com.example.data.model.TelegramBotConfig
import com.example.data.model.UiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.AnimatedSystemCoreOrb
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HoloGradientButton
import com.example.ui.theme.LuminousCyan
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.RosePink
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill
import com.example.ui.theme.TelemetryLineChart

@Composable
fun DashboardScreen(
    ownerEmail: String,
    workspaceState: UiState<OwnerWorkspace?>,
    appsState: UiState<List<ManagedApp>>,
    devicesState: UiState<List<ManagedDevice>>,
    licensesState: UiState<List<LicenseRecord>>,
    botsState: UiState<List<TelegramBotConfig>>,
    dexState: UiState<List<DexRelease>>,
    eventsState: UiState<List<AdminEvent>>,
    settings: AdminSettingsState,
    backendHealth: HealthResponse?,
    backendError: String?,
    isCheckingHealth: Boolean,
    isOffline: Boolean,
    onCheckBackendHealth: () -> Unit,
    onToggleMaintenance: (Boolean, String) -> Unit,
    onToggleLockdown: (Boolean) -> Unit,
    onBootstrapWorkspace: () -> Unit,
    onNavigateToRoute: (String) -> Unit
) {
    val workspace = (workspaceState as? UiState.Success)?.data
    val apps = (appsState as? UiState.Success)?.data.orEmpty()
    val devices = (devicesState as? UiState.Success)?.data.orEmpty()
    val licenses = (licensesState as? UiState.Success)?.data.orEmpty()
    val bots = (botsState as? UiState.Success)?.data.orEmpty()
    val dexReleases = (dexState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentGradient = listOf(RoyalPurple, ElectricBlue, LuminousCyan, MetallicGold)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_pranav_hero_core),
                        contentDescription = stringResource(id = R.string.cd_hero_core),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedSystemCoreOrb(
                        isHealthy = !(workspace?.emergencyLockdown ?: false) && !isOffline,
                        lowEndMode = settings.lowEndFallbackMode
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("PRANAV Core", style = MaterialTheme.typography.headlineMedium)
                            StatusPill(
                                label = settings.environment.uppercase(),
                                color = ElectricBlue
                            )
                        }
                        Text(
                            text = "Root Owner: $ownerEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (dexReleases.isEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    HoloGradientButton(
                        text = "Initialize PRANAV Core DEX v1.4.0",
                        icon = Icons.Filled.Bolt,
                        onClick = onBootstrapWorkspace,
                        modifier = Modifier.testTag("bootstrap_core_button")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMiniCard(
                    title = "Managed Apps",
                    value = apps.size.toString(),
                    subtitle = "${apps.count { it.masterEnabled }} active",
                    icon = Icons.Filled.Apps,
                    accent = RoyalPurple,
                    modifier = Modifier.weight(1f)
                )
                KpiMiniCard(
                    title = "Devices",
                    value = devices.size.toString(),
                    subtitle = "${devices.count { it.approvalStatus == "APPROVED" }} approved",
                    icon = Icons.Filled.Devices,
                    accent = ElectricBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiMiniCard(
                    title = "Licenses & VIP",
                    value = licenses.size.toString(),
                    subtitle = "${licenses.count { it.status == "ACTIVE" }} active",
                    icon = Icons.Filled.Key,
                    accent = MetallicGold,
                    modifier = Modifier.weight(1f)
                )
                KpiMiniCard(
                    title = "Telegram Bots",
                    value = bots.size.toString(),
                    subtitle = "${bots.count { it.enabled }} online",
                    icon = Icons.Filled.SmartToy,
                    accent = LuminousCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                SectionHeader(
                    title = "Ecosystem Telemetry",
                    subtitle = "Live distribution across apps, devices, licenses, bots & releases",
                    trailing = {
                        StatusPill(
                            label = if (isOffline) "OFFLINE CACHE" else "FIRESTORE SYNCED",
                            color = if (isOffline) AmberWarning else EmeraldSuccess
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                val chartValues = listOf(
                    apps.size.toFloat().coerceAtLeast(0.2f),
                    devices.size.toFloat().coerceAtLeast(0.2f),
                    licenses.size.toFloat().coerceAtLeast(0.2f),
                    bots.size.toFloat().coerceAtLeast(0.2f),
                    dexReleases.size.toFloat().coerceAtLeast(0.2f),
                    events.size.toFloat().coerceAtLeast(0.2f)
                )
                TelemetryLineChart(
                    values = chartValues,
                    labels = listOf("Apps", "Devices", "Keys", "Bots", "DEX", "Events")
                )
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentGradient = listOf(ElectricBlue, LuminousCyan)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (backendHealth != null) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = if (backendHealth != null) EmeraldSuccess else AmberWarning
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Node.js Backend & VPS Health", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = settings.backendBaseUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onCheckBackendHealth,
                        enabled = !isCheckingHealth,
                        modifier = Modifier.testTag("ping_backend_button")
                    ) {
                        if (isCheckingHealth) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Ping backend")
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                when {
                    backendHealth != null -> {
                        Text(
                            text = "Status: ${backendHealth.status.uppercase()} • RAM ${backendHealth.memoryUsedMb}/${backendHealth.memoryTotalMb} MB • Workers: ${backendHealth.workerCount} • Load: ${backendHealth.loadAvg1m}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }
                    backendError != null -> {
                        Text(
                            text = "Endpoint check: $backendError (configure live VPS URL in Bots & Ops)",
                            style = MaterialTheme.typography.bodySmall,
                            color = AmberWarning
                        )
                    }
                    else -> {
                        Text(
                            text = "Tap refresh to run live /api/v1/health verification against your configured backend.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                accentGradient = listOf(RosePink, CrimsonError, MetallicGold)
            ) {
                SectionHeader(
                    title = "Emergency & Maintenance Controls",
                    subtitle = "Global circuit breakers for all managed APKs"
                )
                Spacer(Modifier.height(10.dp))
                val maintenanceOn = workspace?.masterMaintenance ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Global Maintenance Mode", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Blocks non-owner traffic across all managed apps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = maintenanceOn,
                        onCheckedChange = {
                            onToggleMaintenance(it, workspace?.maintenanceMessage ?: "Scheduled maintenance in progress.")
                        },
                        modifier = Modifier.testTag("global_maintenance_switch")
                    )
                }
                Spacer(Modifier.height(8.dp))
                val lockdownOn = workspace?.emergencyLockdown ?: false
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Emergency Kill-Switch", style = MaterialTheme.typography.titleSmall, color = if (lockdownOn) CrimsonError else MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "Suspends license issuance & forces security lock dialog",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = lockdownOn,
                        onCheckedChange = onToggleLockdown,
                        modifier = Modifier.testTag("emergency_lockdown_switch")
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onNavigateToRoute(ROUTE_INTEGRATION) },
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Inspect APK")
                    }
                    Button(
                        onClick = { onNavigateToRoute(ROUTE_APPS) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Manage Apps")
                    }
                }
            }
        }

        if (events.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Recent Audit & Ops Feed",
                    subtitle = "Latest verified owner actions and alerts"
                )
            }
            items(events.sortedByDescending { it.createdAt?.seconds ?: 0L }.take(5), key = { it.id }) { evt ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = if (evt.severity == "CRITICAL" || evt.severity == "WARNING") Icons.Filled.Warning else Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = when (evt.severity) {
                                    "CRITICAL" -> CrimsonError
                                    "WARNING" -> AmberWarning
                                    else -> ElectricBlue
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(evt.title, style = MaterialTheme.typography.titleSmall)
                                Text(evt.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusPill(label = evt.category, color = RoyalPurple)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        accentGradient = listOf(accent.copy(alpha = 0.45f), LuminousCyan.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = accent)
    }
}
