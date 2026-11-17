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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AdminSettingsState
import com.example.data.model.AdminEvent
import com.example.data.model.AdminModuleCatalog
import com.example.data.model.OwnerWorkspace
import com.example.data.model.TelegramBotConfig
import com.example.data.model.UiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HoloGradientButton
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.StatusPill

@Composable
fun BotsAndOpsScreen(
    botsState: UiState<List<TelegramBotConfig>>,
    eventsState: UiState<List<AdminEvent>>,
    workspaceState: UiState<OwnerWorkspace?>,
    settings: AdminSettingsState,
    onAddBot: (username: String, displayName: String, token: String, roleType: String, mode: String, rateLimit: Long) -> Unit,
    onBotAction: (TelegramBotConfig, String) -> Unit,
    onDeleteBot: (TelegramBotConfig) -> Unit,
    onSendCommsEvent: (appId: String, category: String, title: String, message: String, severity: String) -> Unit,
    onResolveEvent: (AdminEvent) -> Unit,
    onToggleModule: (String, Boolean) -> Unit,
    onSaveBackendConfig: (baseUrl: String, env: String, secret: String, domainHost: String) -> Unit,
    onSetDarkTheme: (Boolean) -> Unit,
    onSetLowEndFallback: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val bots = (botsState as? UiState.Success)?.data.orEmpty()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val workspace = (workspaceState as? UiState.Success)?.data

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Bots (${bots.size})") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Comms") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Modules") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("VPS & Auth") })
        }

        when (selectedTab) {
            0 -> {
                var botUsername by remember { mutableStateOf("") }
                var botDisplay by remember { mutableStateOf("") }
                var botToken by remember { mutableStateOf("") }
                var roleType by remember { mutableStateOf("USER_BOT") }
                var mode by remember { mutableStateOf("POLLING") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_pranav_bot_banner),
                                    contentDescription = stringResource(id = R.string.cd_bot_banner),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Telegram Multi-Bot Manager", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Unlimited dynamic bots. Tokens are dispatched only to the backend encrypted vault and never stored in Firestore or client logs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = botUsername, onValueChange = { botUsername = it }, label = { Text("Bot Username (e.g. pranav_user_bot)") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = botDisplay, onValueChange = { botDisplay = it }, label = { Text("Display Name") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = botToken,
                                onValueChange = { botToken = it },
                                label = { Text("Bot Token (Sent only to encrypted backend vault)") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = roleType == "USER_BOT", onClick = { roleType = "USER_BOT" }, label = { Text("USER_BOT (14 cmds)") })
                                FilterChip(selected = roleType == "OWNER_BOT", onClick = { roleType = "OWNER_BOT" }, label = { Text("OWNER_BOT (25 cmds)") })
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = mode == "POLLING", onClick = { mode = "POLLING" }, label = { Text("POLLING") })
                                FilterChip(selected = mode == "WEBHOOK", onClick = { mode = "WEBHOOK" }, label = { Text("WEBHOOK") })
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    onAddBot(botUsername, botDisplay, botToken, roleType, mode, 120L)
                                    botToken = ""
                                },
                                modifier = Modifier.fillMaxWidth().testTag("add_bot_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Register & Validate Bot (getMe)")
                            }
                        }
                    }

                    items(bots, key = { it.id }) { bot ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${bot.botDisplayName} (@${bot.botUsername})", style = MaterialTheme.typography.titleMedium)
                                    Text("${bot.roleType} • ${bot.mode} • ${bot.commandsCount} cmds • Ref: ${bot.tokenSecretRef}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                                }
                                StatusPill(label = bot.healthStatus, color = if (bot.enabled) EmeraldSuccess else AmberWarning)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { onBotAction(bot, "START") }) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = "Start bot", tint = EmeraldSuccess)
                                }
                                IconButton(onClick = { onBotAction(bot, "STOP") }) {
                                    Icon(Icons.Filled.Stop, contentDescription = "Stop bot", tint = AmberWarning)
                                }
                                IconButton(onClick = { onBotAction(bot, "RESTART") }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Restart bot", tint = ElectricBlue)
                                }
                                IconButton(onClick = { onDeleteBot(bot) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete bot", tint = CrimsonError)
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                var title by remember { mutableStateOf("") }
                var body by remember { mutableStateOf("") }
                var category by remember { mutableStateOf("ANNOUNCEMENT") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Announcements, Notifications & Support Tickets", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("ANNOUNCEMENT", "NOTIFICATION", "SUPPORT").forEach { c ->
                                    FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) })
                                }
                            }
                            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (title.isNotBlank()) {
                                        onSendCommsEvent("global", category, title.trim(), body.trim(), "INFO")
                                        title = ""
                                        body = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White)
                            ) { Text("Dispatch $category") }
                        }
                    }
                    items(events, key = { it.id }) { evt ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(evt.title, style = MaterialTheme.typography.titleSmall)
                                    Text(evt.message, style = MaterialTheme.typography.bodySmall)
                                }
                                StatusPill(label = evt.status, color = if (evt.status == "RESOLVED") EmeraldSuccess else RoyalPurple)
                            }
                            if (evt.status == "OPEN") {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    OutlinedButton(onClick = { onResolveEvent(evt) }) { Text("Mark Resolved") }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                var moduleFilter by remember { mutableStateOf("") }
                val filteredModules = remember(moduleFilter) {
                    AdminModuleCatalog.allModules.filter {
                        moduleFilter.isBlank() ||
                            it.title.contains(moduleFilter, ignoreCase = true) ||
                            it.category.contains(moduleFilter, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = moduleFilter,
                            onValueChange = { moduleFilter = it },
                            label = { Text("Filter all ${AdminModuleCatalog.allModules.size} PRANAV Admin modules") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    items(filteredModules, key = { it.key }) { mod ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${mod.title} [${mod.category}]", style = MaterialTheme.typography.titleSmall)
                                    Text(mod.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = settings.isModuleEnabled(mod.key),
                                    onCheckedChange = { onToggleModule(mod.key, it) }
                                )
                            }
                        }
                    }
                }
            }

            3 -> {
                var baseUrl by remember(settings.backendBaseUrl) { mutableStateOf(settings.backendBaseUrl) }
                var env by remember(settings.environment) { mutableStateOf(settings.environment) }
                var domainHost by remember(workspace?.domainHost) { mutableStateOf(workspace?.domainHost ?: "pranavsmm.shop") }
                var apiSecret by remember(settings.ownerApiSecret) { mutableStateOf(settings.ownerApiSecret) }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Backend, Domain & Environment Configuration", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("development", "staging", "production").forEach { e ->
                                    FilterChip(selected = env == e, onClick = { env = e }, label = { Text(e) })
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("API Base URL") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = domainHost, onValueChange = { domainHost = it }, label = { Text("Primary Domain Host") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = apiSecret,
                                onValueChange = { apiSecret = it },
                                label = { Text("Owner API Bearer Key (Encrypted in DataStore)") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(10.dp))
                            HoloGradientButton(
                                text = "Save Backend & Domain Settings",
                                onClick = { onSaveBackendConfig(baseUrl, env, apiSecret, domainHost) }
                            )
                        }
                    }

                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Visual & Performance Preferences", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Midnight Dark Theme", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = settings.darkThemeEnabled, onCheckedChange = onSetDarkTheme)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Low-End Device Fallback", style = MaterialTheme.typography.bodyMedium)
                                    Text("Disables continuous core pulse animations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = settings.lowEndFallbackMode, onCheckedChange = onSetLowEndFallback)
                            }
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("sign_out_button")
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null, tint = CrimsonError)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(id = R.string.sign_out), color = CrimsonError)
                        }
                    }
                }
            }
        }
    }
}
