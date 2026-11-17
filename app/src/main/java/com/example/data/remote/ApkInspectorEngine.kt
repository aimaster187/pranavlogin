package com.example.data.remote

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.ApkInspectionReport
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.zip.ZipInputStream

class ApkInspectorEngine(private val context: Context) {

    fun inspectUri(uri: Uri): ApkInspectionReport {
        val displayName = resolveDisplayName(uri) ?: "selected_app.apk"
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Unable to open selected APK stream.")
        return stream.use { inspectStream(it, displayName) }
    }

    fun inspectFile(file: File): ApkInspectionReport {
        require(file.exists() && file.isFile) { "APK file does not exist: ${file.name}" }
        return file.inputStream().use { inspectStream(it, file.name) }
    }

    fun inspectStream(inputStream: InputStream, fileName: String): ApkInspectionReport {
        val apkBytes = inputStream.readBytes()
        require(apkBytes.size >= 4 && apkBytes[0] == 'P'.code.toByte() && apkBytes[1] == 'K'.code.toByte()) {
            "Selected file is not a valid APK/ZIP archive."
        }
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(apkBytes)
            .joinToString("") { "%02x".format(it) }

        val dexFiles = mutableListOf<String>()
        var manifestBytes: ByteArray? = null
        var hasPranavDexMarker = false

        ZipInputStream(apkBytes.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory) {
                    if (name.matches(Regex("^classes\\d*\\.dex$"))) {
                        dexFiles.add(name)
                        val dexData = readCurrentEntry(zis)
                        val dexText = String(dexData, Charsets.ISO_8859_1)
                        if (dexText.contains("Lcom/pranav/sdk/PranavSdk;")) {
                            hasPranavDexMarker = true
                        }
                    } else if (name == "AndroidManifest.xml") {
                        manifestBytes = readCurrentEntry(zis)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sortedDex = dexFiles.sortedWith(compareBy {
            if (it == "classes.dex") 1 else it.removePrefix("classes").removeSuffix(".dex").toIntOrNull() ?: 999
        })

        val parsedManifest = manifestBytes?.let { parseBinaryManifest(it) }
            ?: ParsedAxmlManifest(packageName = "unknown.package", versionName = "1.0", applicationClass = null, launchActivityClass = null)

        val recommendedHook = when {
            !parsedManifest.applicationClass.isNullOrBlank() -> "APPLICATION_ON_CREATE"
            !parsedManifest.launchActivityClass.isNullOrBlank() -> "ACTIVITY_ON_CREATE"
            else -> "APPLICATION_ATTACH_BASE_CONTEXT"
        }

        val notes = buildList {
            if (sortedDex.isEmpty()) add("WARNING: No classes*.dex found inside APK.")
            else add("Detected ${sortedDex.size} DEX file(s); next clean slot is classes${sortedDex.size + 1}.dex.")
            if (hasPranavDexMarker) add("WARNING: PRANAV DEX symbols already detected in archive — verify duplicate-init prevention.")
            else add("No duplicate PRANAV SDK symbols detected; safe for clean integration.")
            if (!parsedManifest.applicationClass.isNullOrBlank()) {
                add("Custom Application class found (${parsedManifest.applicationClass}) — hook Application.onCreate().")
            } else {
                add("No custom android:name Application declared — use PranavHostApplication or hook ${parsedManifest.launchActivityClass ?: "main Activity"}.")
            }
        }

        val targetClassSmali = (parsedManifest.applicationClass ?: parsedManifest.launchActivityClass ?: "com/pranav/host/MainActivity")
            .replace('.', '/')
        val appCode = parsedManifest.packageName.substringAfterLast('.').ifBlank { "app" }

        val smaliSnippet = """
            |# Target class: L$targetClassSmali;
            |.method public onCreate()V
            |    .locals 3
            |    invoke-super {p0}, Landroid/app/Application;->onCreate()V
            |    const-string v0, "$appCode"
            |    const-string v1, "https://api.pranavsmm.shop"
            |    invoke-static {p0, v0, v1}, Lcom/pranav/sdk/PranavSdk;->init(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;)V
            |    return-void
            |.end method
        """.trimMargin()

        val javaSnippet = """
            |// Separate Hook for ${parsedManifest.packageName}
            |public final class PranavHostHook {
            |    public static void initFromHost(android.content.Context ctx) {
            |        com.pranav.sdk.PranavSdk.init(ctx, "$appCode", "https://api.pranavsmm.shop");
            |    }
            |}
        """.trimMargin()

        val manifestSnippet = """
            |<!-- Add under <manifest> in AndroidManifest.xml if missing -->
            |<uses-permission android:name="android.permission.INTERNET" />
            |<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
            |<!-- Target package: ${parsedManifest.packageName} • Next DEX: classes${sortedDex.size + 1}.dex -->
        """.trimMargin()

        return ApkInspectionReport(
            fileName = fileName,
            packageName = parsedManifest.packageName,
            versionName = parsedManifest.versionName,
            applicationClass = parsedManifest.applicationClass,
            launchActivityClass = parsedManifest.launchActivityClass,
            dexFileCount = sortedDex.size,
            dexFileNames = sortedDex,
            hasPranavSdkDex = hasPranavDexMarker,
            recommendedHookPoint = recommendedHook,
            compatibilityVerdict = if (hasPranavDexMarker) "ALREADY_HOOKED" else "COMPATIBLE",
            sha256Digest = sha256,
            compatibilityNotes = notes,
            smaliSnippet = smaliSnippet,
            javaSnippet = javaSnippet,
            manifestSnippet = manifestSnippet
        )
    }

    private fun readCurrentEntry(zis: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        while (true) {
            val r = zis.read(buf)
            if (r <= 0) break
            out.write(buf, 0, r)
        }
        return out.toByteArray()
    }

    private fun resolveDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else uri.lastPathSegment
            }
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    private data class ParsedAxmlManifest(
        val packageName: String,
        val versionName: String,
        val applicationClass: String?,
        val launchActivityClass: String?
    )

    // Minimal real Android Binary XML (AXML) parser for manifest elements & attributes
    private fun parseBinaryManifest(data: ByteArray): ParsedAxmlManifest {
        if (data.size < 8) return ParsedAxmlManifest("unknown.package", "1.0", null, null)
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val fileType = buf.getShort(0).toInt() and 0xFFFF
        if (fileType != 0x0003) {
            // Plain text XML fallback for synthetic/uncompiled manifests
            val text = String(data, Charsets.UTF_8)
            val pkg = Regex("""package="([^"]+)"""").find(text)?.groupValues?.get(1) ?: "unknown.package"
            val ver = Regex("""android:versionName="([^"]+)"""").find(text)?.groupValues?.get(1) ?: "1.0"
            val app = Regex("""<application[^>]*android:name="([^"]+)"""").find(text)?.groupValues?.get(1)
            val act = Regex("""<activity[^>]*android:name="([^"]+)"""").find(text)?.groupValues?.get(1)
            return ParsedAxmlManifest(pkg, ver, app, act)
        }

        val strings = mutableListOf<String>()
        var packageName = "unknown.package"
        var versionName = "1.0"
        var applicationClass: String? = null
        var firstActivity: String? = null

        var pos = 8
        while (pos + 8 <= data.size) {
            val chunkType = buf.getShort(pos).toInt() and 0xFFFF
            val headerSize = buf.getShort(pos + 2).toInt() and 0xFFFF
            val chunkSize = buf.getInt(pos + 4)
            if (chunkSize <= 8 || pos + chunkSize > data.size) break

            when (chunkType) {
                0x0001 -> { // RES_STRING_POOL_TYPE
                    val stringCount = buf.getInt(pos + 8)
                    val flags = buf.getInt(pos + 16)
                    val isUtf8 = (flags and 0x100) != 0
                    val stringsStart = pos + buf.getInt(pos + 20)
                    val offsetsBase = pos + headerSize
                    for (i in 0 until stringCount) {
                        val rel = buf.getInt(offsetsBase + i * 4)
                        val strPos = stringsStart + rel
                        strings.add(readAxmlString(data, strPos, isUtf8))
                    }
                }
                0x0102 -> { // RES_XML_START_ELEMENT_TYPE
                    val nameIdx = buf.getInt(pos + 20)
                    val attrStart = buf.getShort(pos + 24).toInt() and 0xFFFF
                    val attrSize = (buf.getShort(pos + 26).toInt() and 0xFFFF).coerceAtLeast(20)
                    val attrCount = buf.getShort(pos + 28).toInt() and 0xFFFF
                    val tagName = strings.getOrNull(nameIdx).orEmpty()
                    val attrsBase = pos + 16 + attrStart
                    for (a in 0 until attrCount) {
                        val aOff = attrsBase + a * attrSize
                        if (aOff + 20 > data.size) break
                        val attrName = strings.getOrNull(buf.getInt(aOff + 4)).orEmpty()
                        val rawValIdx = buf.getInt(aOff + 8)
                        val strVal = if (rawValIdx >= 0) strings.getOrNull(rawValIdx) else null
                        if (tagName == "manifest") {
                            if (attrName == "package" && !strVal.isNullOrBlank()) packageName = strVal
                            if (attrName == "versionName" && !strVal.isNullOrBlank()) versionName = strVal
                        } else if (tagName == "application" && attrName == "name" && !strVal.isNullOrBlank()) {
                            applicationClass = normalizeClassName(packageName, strVal)
                        } else if (tagName == "activity" && attrName == "name" && !strVal.isNullOrBlank() && firstActivity == null) {
                            firstActivity = normalizeClassName(packageName, strVal)
                        }
                    }
                }
            }
            pos += chunkSize
        }

        return ParsedAxmlManifest(packageName, versionName, applicationClass, firstActivity)
    }

    private fun normalizeClassName(pkg: String, raw: String): String {
        return when {
            raw.startsWith(".") -> "$pkg$raw"
            !raw.contains(".") -> "$pkg.$raw"
            else -> raw
        }
    }

    private fun readAxmlString(data: ByteArray, offset: Int, isUtf8: Boolean): String {
        if (offset < 0 || offset + 2 >= data.size) return ""
        return try {
            if (isUtf8) {
                var p = offset
                if ((data[p].toInt() and 0x80) != 0) p += 2 else p += 1
                val byteLen = if ((data[p].toInt() and 0x80) != 0) {
                    val v = ((data[p].toInt() and 0x7F) shl 8) or (data[p + 1].toInt() and 0xFF)
                    p += 2
                    v
                } else {
                    val v = data[p].toInt() and 0x7F
                    p += 1
                    v
                }
                if (p + byteLen <= data.size) String(data, p, byteLen, Charsets.UTF_8) else ""
            } else {
                val charLen = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset].toInt() and 0xFF)
                val start = offset + 2
                val byteLen = charLen * 2
                if (start + byteLen <= data.size) String(data, start, byteLen, Charsets.UTF_16LE) else ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
