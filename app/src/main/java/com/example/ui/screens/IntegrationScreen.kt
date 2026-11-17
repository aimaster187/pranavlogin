package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ApkInspectionReport
import com.example.data.model.DexRelease
import com.example.data.model.UiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GlassCard
import com.example.ui.theme.HoloGradientButton
import com.example.ui.theme.RoyalPurple
import com.example.ui.theme.SectionHeader
import com.example.ui.theme.StatusPill

@Composable
fun IntegrationScreen(
    ownerUid: String,
    dexState: UiState<List<DexRelease>>,
    apkReport: ApkInspectionReport?,
    isInspectingApk: Boolean,
    apkInspectError: String?,
    onInspectApkUri: (Uri) -> Unit,
    onSaveDexRelease: (DexRelease) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val dexReleases = (dexState as? UiState.Success)?.data.orEmpty()
    val context = LocalContext.current

    val apkPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onInspectApkUri(uri)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("APK Assistant") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("DEX Releases") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Hook Templates") })
        }

        when (selectedTab) {
            0 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = "Guided APK Integration Assistant",
                                subtitle = "Zero-permission SAF picker • Reads real AXML manifest & DEX entries"
                            )
                            Spacer(Modifier.height(12.dp))
                            HoloGradientButton(
                                text = if (isInspectingApk) "Inspecting APK..." else "Select Authorized APK (.apk)",
                                icon = Icons.Filled.FolderOpen,
                                enabled = !isInspectingApk,
                                onClick = {
                                    apkPickerLauncher.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
                                },
                                modifier = Modifier.testTag("select_apk_button")
                            )
                            if (isInspectingApk) {
                                Spacer(Modifier.height(10.dp))
                                CircularProgressIndicator()
                            }
                            if (apkInspectError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(apkInspectError, style = MaterialTheme.typography.bodySmall, color = CrimsonError)
                            }
                        }
                    }

                    if (apkReport != null) {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(apkReport.fileName, style = MaterialTheme.typography.titleMedium)
                                        Text(apkReport.packageName, style = MaterialTheme.typography.labelMedium, color = ElectricBlue)
                                    }
                                    StatusPill(
                                        label = apkReport.compatibilityVerdict,
                                        color = if (apkReport.hasPranavSdkDex) AmberWarning else EmeraldSuccess
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Application class: ${apkReport.applicationClass ?: "Default android.app.Application"}", style = MaterialTheme.typography.bodySmall)
                                Text("Launch Activity: ${apkReport.launchActivityClass ?: "Not explicitly matched"}", style = MaterialTheme.typography.bodySmall)
                                Text("DEX layout (${apkReport.dexFileCount}): ${apkReport.dexFileNames.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                                Text("Recommended Hook: ${apkReport.recommendedHookPoint}", style = MaterialTheme.typography.bodySmall, color = RoyalPurple)
                                Text("APK SHA-256: ${apkReport.sha256Digest.take(24)}...", style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(8.dp))
                                apkReport.compatibilityNotes.forEach { note ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (note.startsWith("WARNING")) Icons.Filled.Warning else Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = if (note.startsWith("WARNING")) AmberWarning else EmeraldSuccess,
                                            modifier = Modifier.padding(end = 6.dp)
                                        )
                                        Text(note, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }

                        item {
                            CodeSnippetCard(
                                title = "App-Specific Smali Hook (MT Manager)",
                                code = apkReport.smaliSnippet,
                                onCopy = { copyText(context, "PRANAV Smali Hook", apkReport.smaliSnippet) }
                            )
                        }
                        item {
                            CodeSnippetCard(
                                title = "App-Specific Java Hook",
                                code = apkReport.javaSnippet,
                                onCopy = { copyText(context, "PRANAV Java Hook", apkReport.javaSnippet) }
                            )
                        }
                        item {
                            CodeSnippetCard(
                                title = "Manifest Integration Snippet",
                                code = apkReport.manifestSnippet,
                                onCopy = { copyText(context, "PRANAV Manifest", apkReport.manifestSnippet) }
                            )
                        }
                    } else {
                        item {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Text("18-Step Authorized APK Integration Workflow", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                val steps = listOf(
                                    "1. Select authorized APK via SAF picker",
                                    "2-5. Inspect ZIP, binary AndroidManifest.xml & classes*.dex",
                                    "6-7. Verify compatibility & select PRANAV DEX release",
                                    "8-11. Generate app-specific Hook & check duplicate init",
                                    "12-13. Rebuild APK & sign with owner signing key",
                                    "14-18. Verify registration, update/dialog/license & report"
                                )
                                steps.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                }
            }

            1 -> {
                var newVer by remember { mutableStateOf("1.4.0") }
                var newCode by remember { mutableStateOf("140") }
                var sha256Input by remember { mutableStateOf("18e0a788743ef6f18a889cfc41cbda7d25a4ea5a5b0a28745876fa3e967d6786") }
                var shaError by remember { mutableStateOf<String?>(null) }
                var hookPref by remember { mutableStateOf("APPLICATION_ON_CREATE") }
                var notes by remember { mutableStateOf("Universal DEX release with Keystore EC P-256 handshake & duplicate-init guard.") }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Publish Verified PRANAV DEX Release", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = newVer, onValueChange = { newVer = it }, label = { Text("Version") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = newCode, onValueChange = { newCode = it }, label = { Text("Code") }, modifier = Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = sha256Input,
                                onValueChange = {
                                    sha256Input = it.trim()
                                    shaError = null
                                },
                                label = { Text("Compiled DEX SHA-256 (64 hex chars from build-dex.sh)") },
                                isError = shaError != null,
                                supportingText = shaError?.let { msg -> { Text(msg, color = CrimsonError) } },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("APPLICATION_ON_CREATE", "ACTIVITY_ON_CREATE", "ATTACH_BASE_CONTEXT").forEach { h ->
                                    FilterChip(selected = hookPref == h, onClick = { hookPref = h }, label = { Text(h.substringBefore("_")) })
                                }
                            }
                            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Release Notes") }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val normalizedSha = sha256Input.trim().lowercase()
                                    if (!normalizedSha.matches(Regex("^[0-9a-f]{64}$"))) {
                                        shaError = "Enter a valid 64-character SHA-256 hex digest from build-dex.sh."
                                        return@Button
                                    }
                                    onSaveDexRelease(
                                        DexRelease(
                                            ownerUid = ownerUid,
                                            versionName = newVer.trim().ifBlank { "1.4.0" },
                                            versionCode = newCode.toLongOrNull() ?: 140L,
                                            minSdk = 24L,
                                            targetSdk = 36L,
                                            sha256Checksum = normalizedSha,
                                            recommendedHook = hookPref,
                                            releaseNotes = notes.trim(),
                                            active = true
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RoyalPurple, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Register DEX Release")
                            }
                        }
                    }
                    items(dexReleases, key = { it.id }) { rel ->
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("PRANAV DEX v${rel.versionName} (#${rel.versionCode})", style = MaterialTheme.typography.titleMedium)
                                StatusPill(label = if (rel.active) "ACTIVE" else "ARCHIVED", color = if (rel.active) EmeraldSuccess else AmberWarning)
                            }
                            Text("MinSDK ${rel.minSdk} • TargetSDK ${rel.targetSdk} • Hook: ${rel.recommendedHook}", style = MaterialTheme.typography.labelSmall, color = ElectricBlue)
                            Text(rel.releaseNotes, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        CodeSnippetCard(
                            title = "1. Separate Application Lifecycle Hook (Java)",
                            code = """
                                |public class PranavApplicationHook {
                                |    public static void attach(Application app, String appCode, String endpoint) {
                                |        PranavSdk.init(app.getApplicationContext(), appCode, endpoint);
                                |    }
                                |}
                            """.trimMargin(),
                            onCopy = { copyText(context, "Application Hook", "PranavApplicationHook.attach(...)") }
                        )
                    }
                    item {
                        CodeSnippetCard(
                            title = "2. Separate Activity Lifecycle Hook (Java)",
                            code = """
                                |public class PranavActivityHook {
                                |    public static void onActivityCreated(Activity activity, String appCode, String endpoint) {
                                |        PranavSdk.init(activity.getApplicationContext(), appCode, endpoint);
                                |        PranavSdk.checkSessionAndRenderDialogs(activity);
                                |    }
                                |}
                            """.trimMargin(),
                            onCopy = { copyText(context, "Activity Hook", "PranavActivityHook.onActivityCreated(...)") }
                        )
                    }
                    item {
                        CodeSnippetCard(
                            title = "3. MT Manager Smali Insertion Example",
                            code = """
                                |.method public onCreate()V
                                |    .locals 3
                                |    invoke-super {p0}, Landroid/app/Application;->onCreate()V
                                |    const-string v0, "my_app_code"
                                |    const-string v1, "https://api.pranavsmm.shop"
                                |    invoke-static {p0, v0, v1}, Lcom/pranav/sdk/PranavSdk;->init(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
                                |    return-void
                                |.end method
                            """.trimMargin(),
                            onCopy = { copyText(context, "Smali Hook", "invoke-static {p0, v0, v1}, Lcom/pranav/sdk/PranavSdk;->init(...)") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeSnippetCard(
    title: String,
    code: String,
    onCopy: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onCopy) {
                Icon(Icons.Filled.ContentCopy, contentDescription = "Copy snippet", tint = ElectricBlue)
            }
        }
        Text(
            text = code,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun copyText(context: Context, label: String, text: String) {
    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cb?.setPrimaryClip(ClipData.newPlainText(label, text))
}
