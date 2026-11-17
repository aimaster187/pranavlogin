package com.example.data.remote

import com.example.data.model.AdminEvent
import com.example.data.model.DexRelease
import com.example.data.model.LicenseRecord
import com.example.data.model.ManagedApp
import com.example.data.model.ManagedDevice
import com.example.data.model.OwnerWorkspace
import com.example.data.model.TelegramBotConfig
import com.example.data.model.UniversalDialog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PranavFirestoreRepository(
    private val db: FirebaseFirestore
) {
    private val est = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE

    private fun userRoot(ownerUid: String) = db.collection("users").document(ownerUid)

    fun observeWorkspace(ownerUid: String): Flow<Result<OwnerWorkspace?>> = callbackFlow {
        val reg = userRoot(ownerUid).collection("workspace").document("primary")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }
                if (snap == null || !snap.exists()) {
                    trySend(Result.success(null))
                } else {
                    @Suppress("UNCHECKED_CAST")
                    val rawModules = snap.get("enabledModules") as? Map<String, Boolean> ?: emptyMap()
                    val ws = OwnerWorkspace(
                        id = snap.id,
                        ownerUid = snap.getString("ownerUid") ?: ownerUid,
                        ownerEmail = snap.getString("ownerEmail") ?: "",
                        masterMaintenance = snap.getBoolean("masterMaintenance") ?: false,
                        maintenanceMessage = snap.getString("maintenanceMessage") ?: "Scheduled maintenance in progress.",
                        emergencyLockdown = snap.getBoolean("emergencyLockdown") ?: false,
                        domainHost = snap.getString("domainHost") ?: "pranavsmm.shop",
                        environment = snap.getString("environment") ?: "production",
                        enabledModules = rawModules,
                        updatedAt = snap.getTimestamp("updatedAt", est)
                    )
                    trySend(Result.success(ws))
                }
            }
        awaitClose { reg.remove() }
    }

    suspend fun upsertWorkspace(ownerUid: String, workspace: OwnerWorkspace) {
        userRoot(ownerUid).collection("workspace").document("primary")
            .set(workspace.copy(ownerUid = ownerUid).toFirestoreMap())
            .await()
    }

    fun observeApps(ownerUid: String): Flow<Result<List<ManagedApp>>> =
        observeList(ownerUid, "apps") { doc ->
            ManagedApp(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                appCode = doc.getString("appCode") ?: "",
                packageName = doc.getString("packageName") ?: "",
                displayName = doc.getString("displayName") ?: "",
                versionName = doc.getString("versionName") ?: "1.0.0",
                versionCode = doc.getLong("versionCode") ?: 100L,
                lifecycleStatus = doc.getString("lifecycleStatus") ?: "ACTIVE",
                masterEnabled = doc.getBoolean("masterEnabled") ?: true,
                popupEnabled = doc.getBoolean("popupEnabled") ?: false,
                forceUpdateEnabled = doc.getBoolean("forceUpdateEnabled") ?: false,
                laterEnabled = doc.getBoolean("laterEnabled") ?: true,
                latestVersion = doc.getString("latestVersion") ?: "1.0.0",
                minSupportedVersion = doc.getString("minSupportedVersion") ?: "1.0.0",
                apkDownloadUrl = doc.getString("apkDownloadUrl") ?: "",
                updateTitle = doc.getString("updateTitle") ?: "",
                updateDescription = doc.getString("updateDescription") ?: "",
                releaseNotes = doc.getString("releaseNotes") ?: "",
                whatsNew = doc.getString("whatsNew") ?: "",
                channelLink = doc.getString("channelLink") ?: "",
                button1Text = doc.getString("button1Text") ?: "Update Now",
                button2Text = doc.getString("button2Text") ?: "Later",
                rolloutPercent = doc.getLong("rolloutPercent") ?: 100L,
                featureFlagsSummary = doc.getString("featureFlagsSummary") ?: "",
                dexReleaseVersion = doc.getString("dexReleaseVersion") ?: "1.4.0",
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveApp(ownerUid: String, app: ManagedApp): String {
        val col = userRoot(ownerUid).collection("apps")
        val isNew = app.id.isBlank()
        val ref = if (isNew) col.document() else col.document(app.id)
        ref.set(app.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    suspend fun deleteApp(ownerUid: String, appId: String) {
        userRoot(ownerUid).collection("apps").document(appId).delete().await()
    }

    fun observeDialogs(ownerUid: String): Flow<Result<List<UniversalDialog>>> =
        observeList(ownerUid, "dialogs") { doc ->
            UniversalDialog(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                appId = doc.getString("appId") ?: "global",
                dialogType = doc.getString("dialogType") ?: "NORMAL_UPDATE",
                title = doc.getString("title") ?: "",
                description = doc.getString("description") ?: "",
                primaryButtonText = doc.getString("primaryButtonText") ?: "Continue",
                primaryActionUrl = doc.getString("primaryActionUrl") ?: "",
                secondaryButtonText = doc.getString("secondaryButtonText") ?: "Later",
                animationStyle = doc.getString("animationStyle") ?: "GLASS_SLIDE",
                soundName = doc.getString("soundName") ?: "DEFAULT_CHIME",
                dismissible = doc.getBoolean("dismissible") ?: true,
                laterAllowed = doc.getBoolean("laterAllowed") ?: true,
                enabled = doc.getBoolean("enabled") ?: true,
                priority = doc.getLong("priority") ?: 10L,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveDialog(ownerUid: String, dialog: UniversalDialog): String {
        val col = userRoot(ownerUid).collection("dialogs")
        val isNew = dialog.id.isBlank()
        val ref = if (isNew) col.document() else col.document(dialog.id)
        ref.set(dialog.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    fun observeDevices(ownerUid: String): Flow<Result<List<ManagedDevice>>> =
        observeList(ownerUid, "devices") { doc ->
            ManagedDevice(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                appId = doc.getString("appId") ?: "global",
                deviceLabel = doc.getString("deviceLabel") ?: "Android Device",
                installUuid = doc.getString("installUuid") ?: "",
                keystoreKeyHash = doc.getString("keystoreKeyHash") ?: "",
                approvalStatus = doc.getString("approvalStatus") ?: "PENDING",
                androidApi = doc.getLong("androidApi") ?: 35L,
                riskScore = doc.getLong("riskScore") ?: 0L,
                transferCount = doc.getLong("transferCount") ?: 0L,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveDevice(ownerUid: String, device: ManagedDevice): String {
        val col = userRoot(ownerUid).collection("devices")
        val isNew = device.id.isBlank()
        val ref = if (isNew) col.document() else col.document(device.id)
        ref.set(device.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    fun observeLicenses(ownerUid: String): Flow<Result<List<LicenseRecord>>> =
        observeList(ownerUid, "licenses") { doc ->
            LicenseRecord(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                appId = doc.getString("appId") ?: "global",
                licenseKey = doc.getString("licenseKey") ?: "",
                planTier = doc.getString("planTier") ?: "VIP_PRO",
                status = doc.getString("status") ?: "UNUSED",
                durationDays = doc.getLong("durationDays") ?: 30L,
                graceDays = doc.getLong("graceDays") ?: 3L,
                paymentVerified = doc.getBoolean("paymentVerified") ?: false,
                transferCooldownHours = doc.getLong("transferCooldownHours") ?: 24L,
                boundDeviceId = doc.getString("boundDeviceId") ?: "",
                expiresAtEpochMs = doc.getLong("expiresAtEpochMs") ?: 0L,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveLicense(ownerUid: String, license: LicenseRecord): String {
        val col = userRoot(ownerUid).collection("licenses")
        val isNew = license.id.isBlank()
        val ref = if (isNew) col.document() else col.document(license.id)
        ref.set(license.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    fun observeBots(ownerUid: String): Flow<Result<List<TelegramBotConfig>>> =
        observeList(ownerUid, "bots") { doc ->
            TelegramBotConfig(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                botUsername = doc.getString("botUsername") ?: "",
                botDisplayName = doc.getString("botDisplayName") ?: "",
                roleType = doc.getString("roleType") ?: "USER_BOT",
                mode = doc.getString("mode") ?: "POLLING",
                commandsCount = doc.getLong("commandsCount") ?: 14L,
                tokenSecretRef = doc.getString("tokenSecretRef") ?: "",
                healthStatus = doc.getString("healthStatus") ?: "REGISTERED",
                enabled = doc.getBoolean("enabled") ?: true,
                rateLimitPerMin = doc.getLong("rateLimitPerMin") ?: 120L,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveBot(ownerUid: String, bot: TelegramBotConfig): String {
        val col = userRoot(ownerUid).collection("bots")
        val isNew = bot.id.isBlank()
        val ref = if (isNew) col.document() else col.document(bot.id)
        ref.set(bot.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    suspend fun deleteBot(ownerUid: String, botId: String) {
        userRoot(ownerUid).collection("bots").document(botId).delete().await()
    }

    fun observeDexReleases(ownerUid: String): Flow<Result<List<DexRelease>>> =
        observeList(ownerUid, "dexReleases") { doc ->
            DexRelease(
                id = doc.id,
                ownerUid = doc.getString("ownerUid") ?: ownerUid,
                versionName = doc.getString("versionName") ?: "1.4.0",
                versionCode = doc.getLong("versionCode") ?: 140L,
                minSdk = doc.getLong("minSdk") ?: 24L,
                targetSdk = doc.getLong("targetSdk") ?: 36L,
                sha256Checksum = doc.getString("sha256Checksum") ?: "",
                recommendedHook = doc.getString("recommendedHook") ?: "APPLICATION_ON_CREATE",
                releaseNotes = doc.getString("releaseNotes") ?: "",
                active = doc.getBoolean("active") ?: true,
                createdAt = doc.getTimestamp("createdAt", est),
                updatedAt = doc.getTimestamp("updatedAt", est)
            )
        }

    suspend fun saveDexRelease(ownerUid: String, release: DexRelease): String {
        val col = userRoot(ownerUid).collection("dexReleases")
        val isNew = release.id.isBlank()
        val ref = if (isNew) col.document() else col.document(release.id)
        ref.set(release.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    fun observeEvents(ownerUid: String): Flow<Result<List<AdminEvent>>> = callbackFlow {
        val reg = userRoot(ownerUid).collection("events")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty().map { doc ->
                    AdminEvent(
                        id = doc.id,
                        ownerUid = doc.getString("ownerUid") ?: ownerUid,
                        appId = doc.getString("appId") ?: "global",
                        category = doc.getString("category") ?: "AUDIT",
                        title = doc.getString("title") ?: "",
                        message = doc.getString("message") ?: "",
                        severity = doc.getString("severity") ?: "INFO",
                        status = doc.getString("status") ?: "OPEN",
                        createdAt = doc.getTimestamp("createdAt", est),
                        updatedAt = doc.getTimestamp("updatedAt", est)
                    )
                }
                trySend(Result.success(list))
            }
        awaitClose { reg.remove() }
    }

    suspend fun recordEvent(ownerUid: String, event: AdminEvent): String {
        val col = userRoot(ownerUid).collection("events")
        val isNew = event.id.isBlank()
        val ref = if (isNew) col.document() else col.document(event.id)
        ref.set(event.copy(ownerUid = ownerUid).toFirestoreMap(isNew = isNew)).await()
        return ref.id
    }

    private fun <T> observeList(
        ownerUid: String,
        collectionName: String,
        mapper: (DocumentSnapshot) -> T
    ): Flow<Result<List<T>>> = callbackFlow {
        val reg = userRoot(ownerUid).collection(collectionName)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(Result.failure(err))
                    return@addSnapshotListener
                }
                val items = snap?.documents.orEmpty().map(mapper)
                trySend(Result.success(items))
            }
        awaitClose { reg.remove() }
    }
}
