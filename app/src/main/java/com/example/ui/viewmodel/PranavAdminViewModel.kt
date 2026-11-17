package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AdminSettingsState
import com.example.data.local.CachedSnapshotEntity
import com.example.data.local.PranavCacheDao
import com.example.data.local.PranavPreferences
import com.example.data.model.AdminEvent
import com.example.data.model.ApkInspectionReport
import com.example.data.model.BotActionRequest
import com.example.data.model.DexRelease
import com.example.data.model.HealthResponse
import com.example.data.model.LicenseRecord
import com.example.data.model.ManagedApp
import com.example.data.model.ManagedDevice
import com.example.data.model.OwnerWorkspace
import com.example.data.model.RegisterBotRequest
import com.example.data.model.SyncAppRequest
import com.example.data.model.SyncDeviceRequest
import com.example.data.model.SyncDialogRequest
import com.example.data.model.SyncEventRequest
import com.example.data.model.SyncLicenseRequest
import com.example.data.model.SyncWorkspaceRequest
import com.example.data.model.TelegramBotConfig
import com.example.data.model.UiState
import com.example.data.model.UniversalDialog
import com.example.data.remote.ApkInspectorEngine
import com.example.data.remote.FirestoreErrorHandler
import com.example.data.remote.PranavBackendClient
import com.example.data.remote.PranavBackendService
import com.example.data.remote.PranavFirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID

class PranavAdminViewModel(
    private val repo: PranavFirestoreRepository,
    private val prefs: PranavPreferences,
    private val cacheDao: PranavCacheDao,
    private val apkInspector: ApkInspectorEngine,
    private val ownerUid: String,
    private val ownerEmail: String
) : ViewModel() {

    private val _toastMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastMessages = _toastMessages.asSharedFlow()

    val settingsState: StateFlow<AdminSettingsState> = prefs.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AdminSettingsState())

    val workspaceState: StateFlow<UiState<OwnerWorkspace?>> = repo.observeWorkspace(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading workspace")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val appsState: StateFlow<UiState<List<ManagedApp>>> = repo.observeApps(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { list ->
                    cacheSummary("apps_count", list.size.toString())
                    UiState.Success(list)
                },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading apps")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val dialogsState: StateFlow<UiState<List<UniversalDialog>>> = repo.observeDialogs(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading dialogs")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val devicesState: StateFlow<UiState<List<ManagedDevice>>> = repo.observeDevices(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading devices")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val licensesState: StateFlow<UiState<List<LicenseRecord>>> = repo.observeLicenses(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading licenses")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val botsState: StateFlow<UiState<List<TelegramBotConfig>>> = repo.observeBots(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading bots")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val dexReleasesState: StateFlow<UiState<List<DexRelease>>> = repo.observeDexReleases(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading DEX releases")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val eventsState: StateFlow<UiState<List<AdminEvent>>> = repo.observeEvents(ownerUid)
        .map { res ->
            res.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(FirestoreErrorHandler.toUserMessage(it, "loading events")) }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _backendHealth = MutableStateFlow<HealthResponse?>(null)
    val backendHealth: StateFlow<HealthResponse?> = _backendHealth.asStateFlow()

    private val _backendHealthError = MutableStateFlow<String?>(null)
    val backendHealthError: StateFlow<String?> = _backendHealthError.asStateFlow()

    private val _isCheckingHealth = MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth.asStateFlow()

    private val _apkReport = MutableStateFlow<ApkInspectionReport?>(null)
    val apkReport: StateFlow<ApkInspectionReport?> = _apkReport.asStateFlow()

    private val _isInspectingApk = MutableStateFlow(false)
    val isInspectingApk: StateFlow<Boolean> = _isInspectingApk.asStateFlow()

    private val _apkInspectError = MutableStateFlow<String?>(null)
    val apkInspectError: StateFlow<String?> = _apkInspectError.asStateFlow()

    private fun cacheSummary(key: String, value: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cacheDao.upsert(CachedSnapshotEntity(key, value, System.currentTimeMillis()))
            } catch (_: Exception) {}
        }
    }

    private suspend fun trySyncToBackend(
        actionLabel: String,
        call: suspend (PranavBackendService, String) -> Unit
    ): String? {
        val cfg = prefs.settingsFlow.first()
        if (cfg.ownerApiSecret.isBlank()) {
            return "Saved to Firestore (configure Owner API Key in Bots & Ops to mirror to VPS backend)"
        }
        return try {
            val service = PranavBackendClient.create(cfg.backendBaseUrl)
            withContext(Dispatchers.IO) {
                call(service, "Bearer ${cfg.ownerApiSecret}")
            }
            null
        } catch (e: Exception) {
            "Saved to Firestore; VPS backend sync failed ($actionLabel): ${e.localizedMessage ?: "unreachable"}"
        }
    }

    fun checkBackendHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            _backendHealthError.value = null
            try {
                val cfg = prefs.settingsFlow.first()
                val service = PranavBackendClient.create(cfg.backendBaseUrl)
                val resp = withContext(Dispatchers.IO) { service.getHealth() }
                _backendHealth.value = resp
                _toastMessages.tryEmit("Backend online (${resp.environment})")
            } catch (e: Exception) {
                _backendHealth.value = null
                _backendHealthError.value = e.localizedMessage ?: "Unable to reach backend"
            } finally {
                _isCheckingHealth.value = false
            }
        }
    }

    fun bootstrapDefaultWorkspaceIfNeeded() {
        viewModelScope.launch {
            try {
                val currentWs = (workspaceState.value as? UiState.Success)?.data
                if (currentWs == null) {
                    repo.upsertWorkspace(
                        ownerUid,
                        OwnerWorkspace(
                            ownerUid = ownerUid,
                            ownerEmail = ownerEmail,
                            domainHost = "pranavsmm.shop",
                            environment = prefs.settingsFlow.first().environment
                        )
                    )
                }
                repo.saveDexRelease(
                    ownerUid,
                    DexRelease(
                        ownerUid = ownerUid,
                        versionName = "1.4.0",
                        versionCode = 140L,
                        minSdk = 24L,
                        targetSdk = 36L,
                        sha256Checksum = "18e0a788743ef6f18a889cfc41cbda7d25a4ea5a5b0a28745876fa3e967d6786",
                        recommendedHook = "APPLICATION_ON_CREATE",
                        releaseNotes = "PRANAV Core DEX v1.4.0 with duplicate-init guard, EC P-256 Keystore identity, and Universal Dialog Engine.",
                        active = true
                    )
                )
                repo.recordEvent(
                    ownerUid,
                    AdminEvent(
                        ownerUid = ownerUid,
                        category = "CORE",
                        title = "PRANAV Core Initialized",
                        message = "Registered workspace & PRANAV DEX v1.4.0.",
                        severity = "INFO",
                        status = "RESOLVED"
                    )
                )
                _toastMessages.tryEmit("PRANAV Core DEX v1.4.0 registered")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "initializing workspace"))
            }
        }
    }

    fun toggleMasterMaintenance(enabled: Boolean, message: String) {
        viewModelScope.launch {
            try {
                val cur = (workspaceState.value as? UiState.Success)?.data ?: OwnerWorkspace(
                    ownerUid = ownerUid,
                    ownerEmail = ownerEmail
                )
                repo.upsertWorkspace(
                    ownerUid,
                    cur.copy(masterMaintenance = enabled, maintenanceMessage = message)
                )
                repo.recordEvent(
                    ownerUid,
                    AdminEvent(
                        ownerUid = ownerUid,
                        category = "OPS",
                        title = if (enabled) "Global Maintenance Enabled" else "Global Maintenance Cleared",
                        message = message,
                        severity = if (enabled) "WARNING" else "INFO",
                        status = "OPEN"
                    )
                )
                val syncNote = trySyncToBackend("workspace") { svc, bearer ->
                    svc.syncWorkspace(bearer, SyncWorkspaceRequest(enabled, message, cur.emergencyLockdown))
                }
                _toastMessages.tryEmit(syncNote ?: if (enabled) "Maintenance mode synced (ON)" else "Maintenance mode synced (OFF)")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "toggling maintenance"))
            }
        }
    }

    fun toggleEmergencyLockdown(enabled: Boolean) {
        viewModelScope.launch {
            try {
                val cur = (workspaceState.value as? UiState.Success)?.data ?: OwnerWorkspace(
                    ownerUid = ownerUid,
                    ownerEmail = ownerEmail
                )
                repo.upsertWorkspace(ownerUid, cur.copy(emergencyLockdown = enabled))
                repo.recordEvent(
                    ownerUid,
                    AdminEvent(
                        ownerUid = ownerUid,
                        category = "SECURITY",
                        title = if (enabled) "Emergency Kill-Switch Activated" else "Emergency Kill-Switch Cleared",
                        message = if (enabled) "All non-owner client handshakes blocked." else "Normal client verification resumed.",
                        severity = if (enabled) "CRITICAL" else "INFO",
                        status = if (enabled) "OPEN" else "RESOLVED"
                    )
                )
                val syncNote = trySyncToBackend("lockdown") { svc, bearer ->
                    svc.syncWorkspace(bearer, SyncWorkspaceRequest(cur.masterMaintenance, cur.maintenanceMessage, enabled))
                }
                _toastMessages.tryEmit(syncNote ?: if (enabled) "Emergency lockdown synced" else "Lockdown cleared on backend")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "toggling lockdown"))
            }
        }
    }

    fun saveApp(app: ManagedApp) {
        viewModelScope.launch {
            try {
                val savedId = repo.saveApp(ownerUid, app)
                val finalApp = app.copy(id = savedId)
                val syncNote = trySyncToBackend("app") { svc, bearer ->
                    svc.syncApp(
                        bearer,
                        SyncAppRequest(
                            id = finalApp.id,
                            appCode = finalApp.appCode,
                            packageName = finalApp.packageName,
                            displayName = finalApp.displayName,
                            lifecycleStatus = finalApp.lifecycleStatus,
                            masterEnabled = finalApp.masterEnabled,
                            popupEnabled = finalApp.popupEnabled,
                            forceUpdateEnabled = finalApp.forceUpdateEnabled,
                            laterEnabled = if (finalApp.forceUpdateEnabled) false else finalApp.laterEnabled,
                            latestVersion = finalApp.latestVersion,
                            minSupportedVersion = finalApp.minSupportedVersion,
                            apkDownloadUrl = finalApp.apkDownloadUrl,
                            updateTitle = finalApp.updateTitle,
                            updateDescription = finalApp.updateDescription,
                            releaseNotes = finalApp.releaseNotes,
                            channelLink = finalApp.channelLink,
                            button1Text = finalApp.button1Text,
                            button2Text = finalApp.button2Text,
                            rolloutPercent = finalApp.rolloutPercent
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "Saved & synced ${app.displayName}")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "saving app"))
            }
        }
    }

    fun deleteApp(app: ManagedApp) {
        if (app.id.isBlank()) return
        viewModelScope.launch {
            try {
                repo.deleteApp(ownerUid, app.id)
                val syncNote = trySyncToBackend("deleteApp") { svc, bearer ->
                    svc.deleteApp(bearer, app.id)
                }
                _toastMessages.tryEmit(syncNote ?: "Deleted ${app.displayName}")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "deleting app"))
            }
        }
    }

    fun saveDialog(dialog: UniversalDialog) {
        viewModelScope.launch {
            try {
                val savedId = repo.saveDialog(ownerUid, dialog)
                val syncNote = trySyncToBackend("dialog") { svc, bearer ->
                    svc.syncDialog(
                        bearer,
                        SyncDialogRequest(
                            id = savedId,
                            appId = dialog.appId,
                            dialogType = dialog.dialogType,
                            title = dialog.title,
                            description = dialog.description,
                            primaryButtonText = dialog.primaryButtonText,
                            primaryActionUrl = dialog.primaryActionUrl,
                            secondaryButtonText = dialog.secondaryButtonText,
                            animationStyle = dialog.animationStyle,
                            soundName = dialog.soundName,
                            dismissible = dialog.dismissible,
                            laterAllowed = dialog.laterAllowed,
                            enabled = dialog.enabled,
                            priority = dialog.priority
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "Saved & synced dialog '${dialog.title}'")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "saving dialog"))
            }
        }
    }

    fun inspectAuthorizedApk(uri: Uri) {
        viewModelScope.launch {
            _isInspectingApk.value = true
            _apkInspectError.value = null
            try {
                val report = withContext(Dispatchers.IO) { apkInspector.inspectUri(uri) }
                _apkReport.value = report
                _toastMessages.tryEmit("Inspected ${report.packageName} (${report.dexFileCount} DEX)")
            } catch (e: Exception) {
                _apkInspectError.value = e.localizedMessage ?: "APK inspection failed"
            } finally {
                _isInspectingApk.value = false
            }
        }
    }

    fun saveDexRelease(release: DexRelease) {
        viewModelScope.launch {
            try {
                repo.saveDexRelease(ownerUid, release)
                _toastMessages.tryEmit("Published DEX v${release.versionName}")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "saving DEX release"))
            }
        }
    }

    fun registerDeviceRecord(appId: String, label: String, apiLevel: Long) {
        viewModelScope.launch {
            try {
                val uuid = UUID.randomUUID().toString()
                val ksHash = UUID.randomUUID().toString().replace("-", "") +
                    UUID.randomUUID().toString().replace("-", "")
                val dev = ManagedDevice(
                    ownerUid = ownerUid,
                    appId = appId,
                    deviceLabel = label.ifBlank { "Android Device" },
                    installUuid = uuid,
                    keystoreKeyHash = ksHash,
                    approvalStatus = "PENDING",
                    androidApi = apiLevel,
                    riskScore = 0L,
                    transferCount = 0L
                )
                val savedId = repo.saveDevice(ownerUid, dev)
                val syncNote = trySyncToBackend("device") { svc, bearer ->
                    svc.syncDevice(
                        bearer,
                        SyncDeviceRequest(
                            id = savedId,
                            appId = dev.appId,
                            deviceLabel = dev.deviceLabel,
                            installUuid = dev.installUuid,
                            keystoreKeyHash = dev.keystoreKeyHash,
                            approvalStatus = dev.approvalStatus,
                            androidApi = dev.androidApi,
                            riskScore = dev.riskScore,
                            transferCount = dev.transferCount
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "Registered & synced installation identity")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "registering device"))
            }
        }
    }

    fun updateDeviceApproval(device: ManagedDevice, status: String) {
        viewModelScope.launch {
            try {
                val updated = device.copy(approvalStatus = status)
                val savedId = repo.saveDevice(ownerUid, updated)
                val syncNote = trySyncToBackend("deviceApproval") { svc, bearer ->
                    svc.syncDevice(
                        bearer,
                        SyncDeviceRequest(
                            id = savedId,
                            appId = updated.appId,
                            deviceLabel = updated.deviceLabel,
                            installUuid = updated.installUuid,
                            keystoreKeyHash = updated.keystoreKeyHash,
                            approvalStatus = updated.approvalStatus,
                            androidApi = updated.androidApi,
                            riskScore = updated.riskScore,
                            transferCount = updated.transferCount
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "Device ${device.deviceLabel} -> $status (synced)")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "updating device"))
            }
        }
    }

    fun generateLicense(
        appId: String,
        tier: String,
        days: Long,
        graceDays: Long,
        paymentVerified: Boolean,
        cooldownHours: Long
    ) {
        viewModelScope.launch {
            try {
                val key = buildLicenseKey(tier)
                val expires = System.currentTimeMillis() + (days * 86_400_000L)
                val lic = LicenseRecord(
                    ownerUid = ownerUid,
                    appId = appId,
                    licenseKey = key,
                    planTier = tier,
                    status = if (paymentVerified || tier == "STANDARD") "UNUSED" else "SUSPENDED",
                    durationDays = days,
                    graceDays = graceDays,
                    paymentVerified = paymentVerified,
                    transferCooldownHours = cooldownHours,
                    expiresAtEpochMs = expires
                )
                val savedId = repo.saveLicense(ownerUid, lic)
                val syncNote = trySyncToBackend("license") { svc, bearer ->
                    svc.syncLicense(
                        bearer,
                        SyncLicenseRequest(
                            id = savedId,
                            appId = lic.appId,
                            licenseKey = lic.licenseKey,
                            planTier = lic.planTier,
                            status = lic.status,
                            durationDays = lic.durationDays,
                            graceDays = lic.graceDays,
                            paymentVerified = lic.paymentVerified,
                            transferCooldownHours = lic.transferCooldownHours,
                            boundDeviceId = lic.boundDeviceId,
                            expiresAtEpochMs = lic.expiresAtEpochMs
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "Issued & synced key $key")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "generating license"))
            }
        }
    }

    fun updateLicenseStatus(license: LicenseRecord, newStatus: String, extraDays: Long) {
        viewModelScope.launch {
            try {
                if (newStatus == "ACTIVE" && license.planTier.startsWith("VIP") && !license.paymentVerified) {
                    _toastMessages.tryEmit("VIP license requires verified payment before activation.")
                    return@launch
                }
                val baseExpiry = maxOf(license.expiresAtEpochMs, System.currentTimeMillis())
                val updated = license.copy(
                    status = newStatus,
                    durationDays = license.durationDays + extraDays,
                    expiresAtEpochMs = if (extraDays > 0L) baseExpiry + extraDays * 86_400_000L else license.expiresAtEpochMs
                )
                val savedId = repo.saveLicense(ownerUid, updated)
                val syncNote = trySyncToBackend("licenseStatus") { svc, bearer ->
                    svc.syncLicense(
                        bearer,
                        SyncLicenseRequest(
                            id = savedId,
                            appId = updated.appId,
                            licenseKey = updated.licenseKey,
                            planTier = updated.planTier,
                            status = updated.status,
                            durationDays = updated.durationDays,
                            graceDays = updated.graceDays,
                            paymentVerified = updated.paymentVerified,
                            transferCooldownHours = updated.transferCooldownHours,
                            boundDeviceId = updated.boundDeviceId,
                            expiresAtEpochMs = updated.expiresAtEpochMs
                        )
                    )
                }
                _toastMessages.tryEmit(syncNote ?: "License ${license.licenseKey} -> $newStatus (synced)")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "updating license"))
            }
        }
    }

    fun addAndValidateBot(
        username: String,
        displayName: String,
        token: String,
        roleType: String,
        mode: String,
        rateLimit: Long
    ) {
        val cleanUser = username.trim().removePrefix("@")
        val cleanToken = token.trim()
        if (cleanUser.isBlank() || cleanToken.isBlank()) {
            _toastMessages.tryEmit("Bot username and token are required.")
            return
        }
        viewModelScope.launch {
            try {
                val cfg = prefs.settingsFlow.first()
                if (cfg.ownerApiSecret.isBlank()) {
                    _toastMessages.tryEmit("Set Owner API Bearer Key in VPS & Auth before registering bots.")
                    return@launch
                }
                val service = PranavBackendClient.create(cfg.backendBaseUrl)
                val res = withContext(Dispatchers.IO) {
                    service.registerBot(
                        bearerAuth = "Bearer ${cfg.ownerApiSecret}",
                        request = RegisterBotRequest(
                            botUsername = cleanUser,
                            botDisplayName = displayName.trim().ifBlank { cleanUser },
                            botToken = cleanToken,
                            roleType = roleType,
                            mode = mode,
                            rateLimitPerMin = rateLimit
                        )
                    )
                }
                if (!res.ok || res.tokenSecretRef.isBlank()) {
                    _toastMessages.tryEmit(res.message ?: "Telegram getMe validation failed on backend.")
                    return@launch
                }
                repo.saveBot(
                    ownerUid,
                    TelegramBotConfig(
                        id = res.botId,
                        ownerUid = ownerUid,
                        botUsername = res.botUsername.ifBlank { cleanUser },
                        botDisplayName = displayName.trim().ifBlank { cleanUser },
                        roleType = roleType,
                        mode = mode,
                        commandsCount = if (roleType == "OWNER_BOT") 25L else 14L,
                        tokenSecretRef = res.tokenSecretRef,
                        healthStatus = res.healthStatus,
                        enabled = true,
                        rateLimitPerMin = rateLimit
                    )
                )
                _toastMessages.tryEmit("Verified @${res.botUsername} via Telegram getMe & vaulted token.")
            } catch (e: Exception) {
                _toastMessages.tryEmit("Bot validation failed: ${e.localizedMessage ?: "Backend unreachable"}. Raw token was not stored.")
            }
        }
    }

    fun performBotAction(bot: TelegramBotConfig, action: String) {
        viewModelScope.launch {
            try {
                val cfg = prefs.settingsFlow.first()
                if (cfg.ownerApiSecret.isBlank()) {
                    _toastMessages.tryEmit("Set Owner API Bearer Key in VPS & Auth to control bots.")
                    return@launch
                }
                val service = PranavBackendClient.create(cfg.backendBaseUrl)
                withContext(Dispatchers.IO) {
                    service.performBotAction(
                        bearerAuth = "Bearer ${cfg.ownerApiSecret}",
                        botId = bot.id,
                        request = BotActionRequest(action)
                    )
                }
                val enabled = action != "STOP"
                val status = when (action) {
                    "STOP" -> "PAUSED"
                    else -> "ACTIVE"
                }
                repo.saveBot(ownerUid, bot.copy(enabled = enabled, healthStatus = status))
                _toastMessages.tryEmit("Bot @${bot.botUsername}: $action")
            } catch (e: Exception) {
                _toastMessages.tryEmit("Bot action failed: ${e.localizedMessage ?: "Error"}")
            }
        }
    }

    fun deleteBot(bot: TelegramBotConfig) {
        if (bot.id.isBlank()) return
        viewModelScope.launch {
            try {
                val cfg = prefs.settingsFlow.first()
                if (cfg.ownerApiSecret.isNotBlank()) {
                    try {
                        withContext(Dispatchers.IO) {
                            PranavBackendClient.create(cfg.backendBaseUrl)
                                .deleteBot("Bearer ${cfg.ownerApiSecret}", bot.id)
                        }
                    } catch (_: Exception) {}
                }
                repo.deleteBot(ownerUid, bot.id)
                _toastMessages.tryEmit("Removed @${bot.botUsername}")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "deleting bot"))
            }
        }
    }

    fun sendAnnouncementOrTicket(
        appId: String,
        category: String,
        title: String,
        message: String,
        severity: String
    ) {
        viewModelScope.launch {
            try {
                repo.recordEvent(
                    ownerUid,
                    AdminEvent(
                        ownerUid = ownerUid,
                        appId = appId,
                        category = category,
                        title = title,
                        message = message,
                        severity = severity,
                        status = "OPEN"
                    )
                )
                val syncNote = trySyncToBackend("event") { svc, bearer ->
                    svc.syncEvent(bearer, SyncEventRequest(appId, category, title, message, severity))
                }
                _toastMessages.tryEmit(syncNote ?: "Dispatched & synced $category")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "sending event"))
            }
        }
    }

    fun resolveEvent(event: AdminEvent) {
        viewModelScope.launch {
            try {
                repo.recordEvent(ownerUid, event.copy(status = "RESOLVED"))
                _toastMessages.tryEmit("Resolved '${event.title}'")
            } catch (e: Exception) {
                _toastMessages.tryEmit(FirestoreErrorHandler.toUserMessage(e, "resolving event"))
            }
        }
    }

    fun toggleAdminModule(key: String, enabled: Boolean) {
        viewModelScope.launch {
            prefs.setModuleEnabled(key, enabled)
            try {
                val ws = (workspaceState.value as? UiState.Success)?.data
                if (ws != null) {
                    val updatedModules = ws.enabledModules.toMutableMap().apply { put(key, enabled) }
                    repo.upsertWorkspace(ownerUid, ws.copy(enabledModules = updatedModules))
                }
            } catch (_: Exception) {}
        }
    }

    fun saveBackendConfig(baseUrl: String, env: String, secret: String, domainHost: String) {
        viewModelScope.launch {
            prefs.saveBackendConfig(baseUrl, env, secret)
            try {
                val ws = (workspaceState.value as? UiState.Success)?.data ?: OwnerWorkspace(
                    ownerUid = ownerUid,
                    ownerEmail = ownerEmail
                )
                repo.upsertWorkspace(
                    ownerUid,
                    ws.copy(domainHost = domainHost.trim().ifBlank { "pranavsmm.shop" }, environment = env)
                )
                _toastMessages.tryEmit("Saved backend & domain configuration")
            } catch (e: Exception) {
                _toastMessages.tryEmit("Saved local backend URL; cloud sync note: ${e.localizedMessage}")
            }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { prefs.setDarkTheme(enabled) }
    }

    fun setLowEndFallback(enabled: Boolean) {
        viewModelScope.launch { prefs.setLowEndFallback(enabled) }
    }

    private fun buildLicenseKey(tier: String): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val rng = SecureRandom()
        fun chunk(len: Int) = (1..len).map { alphabet[rng.nextInt(alphabet.length)] }.joinToString("")
        val prefix = when (tier) {
            "VIP_ELITE" -> "PRNV-ELITE"
            "VIP_PRO" -> "PRNV-PRO"
            else -> "PRNV-STD"
        }
        return "$prefix-${chunk(4)}-${chunk(4)}-${chunk(4)}"
    }
}
