package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HealthResponse(
    val status: String = "ok",
    val environment: String = "production",
    val uptimeSeconds: Long = 0L,
    val memoryUsedMb: Int = 0,
    val memoryTotalMb: Int = 0,
    val workerCount: Int = 0,
    val loadAvg1m: String = "0.00"
)

@JsonClass(generateAdapter = true)
data class RegisterBotRequest(
    val botUsername: String,
    val botDisplayName: String,
    val botToken: String,
    val roleType: String,
    val mode: String,
    val rateLimitPerMin: Long
)

@JsonClass(generateAdapter = true)
data class RegisterBotResponse(
    val ok: Boolean = false,
    val botId: String = "",
    val botUsername: String = "",
    val tokenSecretRef: String = "",
    val verifiedTelegramId: Long? = null,
    val healthStatus: String = "ACTIVE",
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class BotActionRequest(
    val action: String
)

@JsonClass(generateAdapter = true)
data class GenericApiResponse(
    val ok: Boolean = false,
    val message: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncWorkspaceRequest(
    val masterMaintenance: Boolean,
    val maintenanceMessage: String,
    val emergencyLockdown: Boolean
)

@JsonClass(generateAdapter = true)
data class SyncAppRequest(
    val id: String,
    val appCode: String,
    val packageName: String,
    val displayName: String,
    val lifecycleStatus: String,
    val masterEnabled: Boolean,
    val popupEnabled: Boolean,
    val forceUpdateEnabled: Boolean,
    val laterEnabled: Boolean,
    val latestVersion: String,
    val minSupportedVersion: String,
    val apkDownloadUrl: String,
    val updateTitle: String,
    val updateDescription: String,
    val releaseNotes: String,
    val channelLink: String,
    val button1Text: String,
    val button2Text: String,
    val rolloutPercent: Long
)

@JsonClass(generateAdapter = true)
data class SyncDialogRequest(
    val id: String,
    val appId: String,
    val dialogType: String,
    val title: String,
    val description: String,
    val primaryButtonText: String,
    val primaryActionUrl: String,
    val secondaryButtonText: String,
    val animationStyle: String,
    val soundName: String,
    val dismissible: Boolean,
    val laterAllowed: Boolean,
    val enabled: Boolean,
    val priority: Long
)

@JsonClass(generateAdapter = true)
data class SyncDeviceRequest(
    val id: String,
    val appId: String,
    val deviceLabel: String,
    val installUuid: String,
    val keystoreKeyHash: String,
    val approvalStatus: String,
    val androidApi: Long,
    val riskScore: Long,
    val transferCount: Long
)

@JsonClass(generateAdapter = true)
data class SyncLicenseRequest(
    val id: String,
    val appId: String,
    val licenseKey: String,
    val planTier: String,
    val status: String,
    val durationDays: Long,
    val graceDays: Long,
    val paymentVerified: Boolean,
    val transferCooldownHours: Long,
    val boundDeviceId: String,
    val expiresAtEpochMs: Long
)

@JsonClass(generateAdapter = true)
data class SyncEventRequest(
    val appId: String,
    val category: String,
    val title: String,
    val message: String,
    val severity: String
)
