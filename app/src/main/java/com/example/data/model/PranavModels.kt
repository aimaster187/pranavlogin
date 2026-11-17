package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class OwnerWorkspace(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val masterMaintenance: Boolean = false,
    val maintenanceMessage: String = "Scheduled maintenance in progress.",
    val emergencyLockdown: Boolean = false,
    val domainHost: String = "pranavsmm.shop",
    val environment: String = "production",
    val enabledModules: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "ownerEmail" to ownerEmail,
        "masterMaintenance" to masterMaintenance,
        "maintenanceMessage" to maintenanceMessage,
        "emergencyLockdown" to emergencyLockdown,
        "domainHost" to domainHost,
        "environment" to environment,
        "enabledModules" to enabledModules,
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class ManagedApp(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val appCode: String = "app_1",
    val packageName: String = "com.pranav.client",
    val displayName: String = "PRANAV Managed App",
    val versionName: String = "1.0.0",
    val versionCode: Long = 100L,
    val lifecycleStatus: String = "ACTIVE",
    val masterEnabled: Boolean = true,
    val popupEnabled: Boolean = false,
    val forceUpdateEnabled: Boolean = false,
    val laterEnabled: Boolean = true,
    val latestVersion: String = "1.0.0",
    val minSupportedVersion: String = "1.0.0",
    val apkDownloadUrl: String = "",
    val updateTitle: String = "Update Available",
    val updateDescription: String = "A newer build is ready with performance and security improvements.",
    val releaseNotes: String = "Stability improvements and DEX hook telemetry.",
    val whatsNew: String = "Faster session handshake.",
    val channelLink: String = "",
    val button1Text: String = "Update Now",
    val button2Text: String = "Later",
    val rolloutPercent: Long = 100L,
    val featureFlagsSummary: String = "dialogEngine=true,deviceCheck=true",
    val dexReleaseVersion: String = "1.4.0",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("appCode", appCode)
        put("packageName", packageName)
        put("displayName", displayName)
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("lifecycleStatus", lifecycleStatus)
        put("masterEnabled", masterEnabled)
        put("popupEnabled", popupEnabled)
        put("forceUpdateEnabled", forceUpdateEnabled)
        put("laterEnabled", if (forceUpdateEnabled) false else laterEnabled)
        put("latestVersion", latestVersion)
        put("minSupportedVersion", minSupportedVersion)
        put("apkDownloadUrl", apkDownloadUrl)
        put("updateTitle", updateTitle)
        put("updateDescription", updateDescription)
        put("releaseNotes", releaseNotes)
        put("whatsNew", whatsNew)
        put("channelLink", channelLink)
        put("button1Text", button1Text)
        put("button2Text", button2Text)
        put("rolloutPercent", rolloutPercent)
        put("featureFlagsSummary", featureFlagsSummary)
        put("dexReleaseVersion", dexReleaseVersion)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class UniversalDialog(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val appId: String = "global",
    val dialogType: String = "NORMAL_UPDATE",
    val title: String = "Notification",
    val description: String = "",
    val primaryButtonText: String = "Continue",
    val primaryActionUrl: String = "",
    val secondaryButtonText: String = "Later",
    val animationStyle: String = "GLASS_SLIDE",
    val soundName: String = "DEFAULT_CHIME",
    val dismissible: Boolean = true,
    val laterAllowed: Boolean = true,
    val enabled: Boolean = true,
    val priority: Long = 10L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("appId", appId)
        put("dialogType", dialogType)
        put("title", title)
        put("description", description)
        put("primaryButtonText", primaryButtonText)
        put("primaryActionUrl", primaryActionUrl)
        put("secondaryButtonText", secondaryButtonText)
        put("animationStyle", animationStyle)
        put("soundName", soundName)
        put("dismissible", dismissible)
        put("laterAllowed", laterAllowed)
        put("enabled", enabled)
        put("priority", priority)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class ManagedDevice(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val appId: String = "global",
    val deviceLabel: String = "Android Device",
    val installUuid: String = "",
    val keystoreKeyHash: String = "",
    val approvalStatus: String = "PENDING",
    val androidApi: Long = 35L,
    val riskScore: Long = 0L,
    val transferCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("appId", appId)
        put("deviceLabel", deviceLabel)
        put("installUuid", installUuid)
        put("keystoreKeyHash", keystoreKeyHash)
        put("approvalStatus", approvalStatus)
        put("androidApi", androidApi)
        put("riskScore", riskScore)
        put("transferCount", transferCount)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class LicenseRecord(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val appId: String = "global",
    val licenseKey: String = "",
    val planTier: String = "VIP_PRO",
    val status: String = "UNUSED",
    val durationDays: Long = 30L,
    val graceDays: Long = 3L,
    val paymentVerified: Boolean = false,
    val transferCooldownHours: Long = 24L,
    val boundDeviceId: String = "",
    val expiresAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("appId", appId)
        put("licenseKey", licenseKey)
        put("planTier", planTier)
        put("status", status)
        put("durationDays", durationDays)
        put("graceDays", graceDays)
        put("paymentVerified", paymentVerified)
        put("transferCooldownHours", transferCooldownHours)
        put("boundDeviceId", boundDeviceId)
        put("expiresAtEpochMs", expiresAtEpochMs)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class TelegramBotConfig(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val botUsername: String = "",
    val botDisplayName: String = "",
    val roleType: String = "USER_BOT",
    val mode: String = "POLLING",
    val commandsCount: Long = 14L,
    val tokenSecretRef: String = "",
    val healthStatus: String = "REGISTERED",
    val enabled: Boolean = true,
    val rateLimitPerMin: Long = 120L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("botUsername", botUsername)
        put("botDisplayName", botDisplayName)
        put("roleType", roleType)
        put("mode", mode)
        put("commandsCount", commandsCount)
        put("tokenSecretRef", tokenSecretRef)
        put("healthStatus", healthStatus)
        put("enabled", enabled)
        put("rateLimitPerMin", rateLimitPerMin)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class DexRelease(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val versionName: String = "1.4.0",
    val versionCode: Long = 140L,
    val minSdk: Long = 24L,
    val targetSdk: Long = 36L,
    val sha256Checksum: String = "",
    val recommendedHook: String = "APPLICATION_ON_CREATE",
    val releaseNotes: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("versionName", versionName)
        put("versionCode", versionCode)
        put("minSdk", minSdk)
        put("targetSdk", targetSdk)
        put("sha256Checksum", sha256Checksum)
        put("recommendedHook", recommendedHook)
        put("releaseNotes", releaseNotes)
        put("active", active)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class AdminEvent(
    @DocumentId val id: String = "",
    val ownerUid: String = "",
    val appId: String = "global",
    val category: String = "AUDIT",
    val title: String = "",
    val message: String = "",
    val severity: String = "INFO",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toFirestoreMap(isNew: Boolean): Map<String, Any?> = buildMap {
        put("ownerUid", ownerUid)
        put("appId", appId)
        put("category", category)
        put("title", title)
        put("message", message)
        put("severity", severity)
        put("status", status)
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
        put("updatedAt", FieldValue.serverTimestamp())
    }
}

data class ApkInspectionReport(
    val fileName: String,
    val packageName: String,
    val versionName: String,
    val applicationClass: String?,
    val launchActivityClass: String?,
    val dexFileCount: Int,
    val dexFileNames: List<String>,
    val hasPranavSdkDex: Boolean,
    val recommendedHookPoint: String,
    val compatibilityVerdict: String,
    val sha256Digest: String,
    val compatibilityNotes: List<String>,
    val smaliSnippet: String,
    val javaSnippet: String,
    val manifestSnippet: String
)
