#!/usr/bin/env node
/**
 * PRANAV Guided Authorized APK Inspector & Integration Assistant (CLI)
 * Inspects an authorized APK file (ZIP structure, binary AXML AndroidManifest.xml, classes*.dex,
 * duplicate PRANAV SDK markers, SHA-256 digest) and outputs compatibility diagnostics + hook snippets.
 * Strictly for authorized APK maintenance — refuses to bypass DRM, signature checks, or anti-tamper.
 */
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import zlib from "node:zlib";

function readZipEntries(buf) {
  const entries = [];
  // Locate End of Central Directory (0x06054b50)
  let eocd = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65557); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) {
      eocd = i;
      break;
    }
  }
  if (eocd === -1) throw new Error("Invalid APK/ZIP archive: EOCD not found");

  const cdCount = buf.readUInt16LE(eocd + 10);
  let pos = buf.readUInt32LE(eocd + 16);

  for (let i = 0; i < cdCount; i++) {
    if (buf.readUInt32LE(pos) !== 0x02014b50) break;
    const compMethod = buf.readUInt16LE(pos + 10);
    const compSize = buf.readUInt32LE(pos + 20);
    const nameLen = buf.readUInt16LE(pos + 28);
    const extraLen = buf.readUInt16LE(pos + 30);
    const commentLen = buf.readUInt16LE(pos + 32);
    const localHdrOff = buf.readUInt32LE(pos + 42);
    const name = buf.subarray(pos + 46, pos + 46 + nameLen).toString("utf8");
    pos += 46 + nameLen + extraLen + commentLen;

    entries.push({ name, compMethod, compSize, localHdrOff });
  }
  return entries;
}

function extractEntryBytes(buf, entry) {
  const lfh = entry.localHdrOff;
  if (buf.readUInt32LE(lfh) !== 0x04034b50) {
    throw new Error(`Corrupt local file header for ${entry.name}`);
  }
  const fnLen = buf.readUInt16LE(lfh + 26);
  const exLen = buf.readUInt16LE(lfh + 28);
  const dataStart = lfh + 30 + fnLen + exLen;
  const raw = buf.subarray(dataStart, dataStart + entry.compSize);
  if (entry.compMethod === 0) return raw;
  if (entry.compMethod === 8) return zlib.inflateRawSync(raw);
  throw new Error(`Unsupported compression method ${entry.compMethod} on ${entry.name}`);
}

function readAxmlString(data, offset, isUtf8) {
  if (offset < 0 || offset + 2 >= data.length) return "";
  try {
    if (isUtf8) {
      let p = offset;
      p += (data[p] & 0x80) !== 0 ? 2 : 1;
      let byteLen;
      if ((data[p] & 0x80) !== 0) {
        byteLen = ((data[p] & 0x7f) << 8) | data[p + 1];
        p += 2;
      } else {
        byteLen = data[p] & 0x7f;
        p += 1;
      }
      return data.subarray(p, p + byteLen).toString("utf8");
    }
    const charLen = data.readUInt16LE(offset);
    return data.subarray(offset + 2, offset + 2 + charLen * 2).toString("utf16le");
  } catch {
    return "";
  }
}

function parseManifest(data) {
  if (data.length < 8) return { packageName: "unknown", versionName: "1.0", applicationClass: null, firstActivity: null, permissions: [] };
  if (data.readUInt16LE(0) !== 0x0003) {
    const txt = data.toString("utf8");
    const pkg = txt.match(/package="([^"]+)"/)?.[1] || "unknown";
    const ver = txt.match(/android:versionName="([^"]+)"/)?.[1] || "1.0";
    const app = txt.match(/<application[^>]*android:name="([^"]+)"/)?.[1] || null;
    const act = txt.match(/<activity[^>]*android:name="([^"]+)"/)?.[1] || null;
    return { packageName: pkg, versionName: ver, applicationClass: app, firstActivity: act, permissions: [] };
  }

  const strings = [];
  let packageName = "unknown";
  let versionName = "1.0";
  let applicationClass = null;
  let firstActivity = null;
  const permissions = [];

  let pos = 8;
  while (pos + 8 <= data.length) {
    const chunkType = data.readUInt16LE(pos);
    const headerSize = data.readUInt16LE(pos + 2);
    const chunkSize = data.readUInt32LE(pos + 4);
    if (chunkSize <= 8 || pos + chunkSize > data.length) break;

    if (chunkType === 0x0001) {
      const stringCount = data.readUInt32LE(pos + 8);
      const flags = data.readUInt32LE(pos + 16);
      const isUtf8 = (flags & 0x100) !== 0;
      const stringsStart = pos + data.readUInt32LE(pos + 20);
      const offsetsBase = pos + headerSize;
      for (let i = 0; i < stringCount; i++) {
        const rel = data.readUInt32LE(offsetsBase + i * 4);
        strings.push(readAxmlString(data, stringsStart + rel, isUtf8));
      }
    } else if (chunkType === 0x0102) {
      const nameIdx = data.readInt32LE(pos + 20);
      const attrStart = data.readUInt16LE(pos + 24);
      const attrSize = Math.max(20, data.readUInt16LE(pos + 26));
      const attrCount = data.readUInt16LE(pos + 28);
      const tagName = strings[nameIdx] || "";
      const attrsBase = pos + 16 + attrStart;
      for (let a = 0; a < attrCount; a++) {
        const aOff = attrsBase + a * attrSize;
        if (aOff + 20 > data.length) break;
        const attrName = strings[data.readInt32LE(aOff + 4)] || "";
        const rawValIdx = data.readInt32LE(aOff + 8);
        const val = rawValIdx >= 0 ? strings[rawValIdx] : null;
        if (tagName === "manifest" && attrName === "package" && val) packageName = val;
        if (tagName === "manifest" && attrName === "versionName" && val) versionName = val;
        if (tagName === "application" && attrName === "name" && val) {
          applicationClass = val.startsWith(".") ? `${packageName}${val}` : val;
        }
        if (tagName === "activity" && attrName === "name" && val && !firstActivity) {
          firstActivity = val.startsWith(".") ? `${packageName}${val}` : val;
        }
        if (tagName === "uses-permission" && attrName === "name" && val) {
          permissions.push(val);
        }
      }
    }
    pos += chunkSize;
  }
  return { packageName, versionName, applicationClass, firstActivity, permissions };
}

export function inspectApkFile(apkPath) {
  const abs = path.resolve(apkPath);
  const buf = fs.readFileSync(abs);
  const sha256 = crypto.createHash("sha256").update(buf).digest("hex");
  const entries = readZipEntries(buf);

  const dexEntries = entries.filter((e) => /^classes\d*\.dex$/.test(e.name));
  const manifestEntry = entries.find((e) => e.name === "AndroidManifest.xml");
  const hasV2OrV3Sig = entries.some((e) => e.name.startsWith("META-INF/") && /\.(RSA|EC|DSA)$/.test(e.name));

  let hasPranavSdk = false;
  for (const de of dexEntries) {
    const dexBuf = extractEntryBytes(buf, de);
    if (dexBuf.includes("Lcom/pranav/sdk/PranavSdk;")) {
      hasPranavSdk = true;
    }
  }

  const manifest = manifestEntry
    ? parseManifest(extractEntryBytes(buf, manifestEntry))
    : { packageName: "unknown", versionName: "1.0", applicationClass: null, firstActivity: null, permissions: [] };

  const recommendedHook = manifest.applicationClass
    ? "APPLICATION_ON_CREATE"
    : manifest.firstActivity
      ? "ACTIVITY_ON_CREATE"
      : "APPLICATION_ATTACH_BASE_CONTEXT";

  return {
    file: path.basename(abs),
    sha256,
    packageName: manifest.packageName,
    versionName: manifest.versionName,
    applicationClass: manifest.applicationClass,
    launchActivityClass: manifest.firstActivity,
    permissions: manifest.permissions,
    dexFiles: dexEntries.map((d) => d.name),
    nextDexSlot: `classes${dexEntries.length + 1}.dex`,
    hasPranavSdk,
    hasSignatureFiles: hasV2OrV3Sig,
    recommendedHook,
    compatibilityVerdict: hasPranavSdk ? "PRANAV_ALREADY_PRESENT" : "READY_FOR_INTEGRATION"
  };
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(new URL(import.meta.url).pathname)) {
  const target = process.argv[2];
  if (!target) {
    console.error("Usage: node pranav_apk_inspector.js <path-to-authorized.apk>");
    process.exit(1);
  }
  console.log(JSON.stringify(inspectApkFile(target), null, 2));
}
