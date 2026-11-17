import crypto from "node:crypto";
import { readJsonBody, sendJson } from "../httpHelpers.js";
import { sha256Hex, verifyDeviceChallengeSignature } from "../cryptoVault.js";

function compareSemver(a, b) {
  const pa = String(a || "0.0.0").split(".").map((x) => parseInt(x, 10) || 0);
  const pb = String(b || "0.0.0").split(".").map((x) => parseInt(x, 10) || 0);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const va = pa[i] || 0;
    const vb = pb[i] || 0;
    if (va < vb) return -1;
    if (va > vb) return 1;
  }
  return 0;
}

function inRolloutBucket(deviceId, appCode, rolloutPercent) {
  const pct = Number(rolloutPercent ?? 100);
  if (pct >= 100) return true;
  if (pct <= 0) return false;
  const hash = sha256Hex(`${deviceId}:${appCode}`);
  const bucket = parseInt(hash.slice(0, 4), 16) % 100;
  return bucket < pct;
}

export async function handleClientRoutes(req, res, pathname, { config, storage }) {
  if (req.method === "POST" && pathname === "/api/v1/client/challenge") {
    const body = await readJsonBody(req);
    const nonce = crypto.randomBytes(24).toString("base64url");
    storage.state.nonces[nonce] = {
      appCode: String(body.appCode || ""),
      deviceId: String(body.deviceId || ""),
      createdAtMs: Date.now()
    };
    storage.save();
    return sendJson(res, 200, { ok: true, nonce, expiresInMs: 300000 });
  }

  if (req.method === "POST" && (pathname === "/api/v1/client/session" || pathname === "/api/v1/client/handshake")) {
    const body = await readJsonBody(req);
    const appCode = String(body.appCode || body.packageName || "").trim();
    const deviceId = String(body.deviceId || body.installUuid || "").trim();
    const versionName = String(body.versionName || "1.0.0").trim();
    const keystoreHash = String(body.keystoreHash || "").trim();
    const keystorePubKey = String(body.keystorePublicKeyB64 || "").trim();
    const challengeNonce = String(body.challengeNonce || "").trim();
    const challengeSig = String(body.challengeSignatureB64 || "").trim();

    if (!appCode || !deviceId) {
      return sendJson(res, 400, { ok: false, error: "appCode and deviceId are required" });
    }

    // Optional challenge signature check when nonce + public key are provided
    let signatureVerified = false;
    if (challengeNonce && challengeSig && keystorePubKey) {
      const nonceEntry = storage.state.nonces[challengeNonce];
      if (!nonceEntry) {
        return sendJson(res, 403, { ok: false, error: "Invalid or expired challenge nonce" });
      }
      delete storage.state.nonces[challengeNonce];
      if (!verifyDeviceChallengeSignature(keystorePubKey, challengeNonce, challengeSig)) {
        return sendJson(res, 403, { ok: false, error: "Keystore ECDSA P-256 signature verification failed" });
      }
      signatureVerified = true;
    }

    const app = storage.resolveIsolatedApp(appCode);
    if (!app) {
      return sendJson(res, 404, { ok: false, error: `Isolated app not found: ${appCode}` });
    }

    // Upsert device tracking strictly isolated per appId
    let device = storage.state.devices.find((d) => d.installUuid === deviceId && d.appId === app.id);
    if (!device) {
      device = {
        id: `dev_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
        appId: app.id,
        deviceLabel: String(body.deviceModel || "Android Client"),
        installUuid: deviceId,
        keystoreKeyHash: keystoreHash,
        approvalStatus: "APPROVED",
        androidApi: Number(body.androidApi || 35),
        riskScore: 0,
        transferCount: 0,
        installedVersion: versionName,
        lastSeenMs: Date.now()
      };
      storage.state.devices.push(device);
    } else {
      device.lastSeenMs = Date.now();
      if (keystoreHash && device.keystoreKeyHash && device.keystoreKeyHash !== keystoreHash) {
        device.riskScore = Math.min(100, Number(device.riskScore || 0) + 40);
      }
    }
    storage.save();

    if (storage.state.workspace.emergencyLockdown) {
      return sendJson(res, 200, {
        ok: true,
        allowed: false,
        lockdown: true,
        signatureVerified,
        dialogs: [{
          dialogType: "SECURITY_LOCK",
          title: "Security Lockdown Active",
          description: "Remote access has been temporarily locked by the Root Owner.",
          dismissible: false,
          laterAllowed: false
        }]
      });
    }

    if (storage.state.workspace.masterMaintenance || app.lifecycleStatus !== "ACTIVE" || !app.masterEnabled) {
      return sendJson(res, 200, {
        ok: true,
        allowed: false,
        maintenance: true,
        signatureVerified,
        dialogs: [{
          dialogType: "MAINTENANCE",
          title: "Service Maintenance",
          description: storage.state.workspace.maintenanceMessage || "Application temporarily paused for maintenance.",
          dismissible: false,
          laterAllowed: false
        }]
      });
    }

    if (device.approvalStatus === "BLOCKED") {
      return sendJson(res, 403, {
        ok: false,
        allowed: false,
        error: "Device is blocked by owner policy",
        dialogs: [{
          dialogType: "SECURITY_LOCK",
          title: "Device Suspended",
          description: "This device installation has been blocked.",
          dismissible: false,
          laterAllowed: false
        }]
      });
    }

    const belowMin = app.minSupportedVersion && compareSemver(versionName, app.minSupportedVersion) < 0;
    const hasNewer = app.latestVersion && compareSemver(versionName, app.latestVersion) < 0;
    const eligibleRollout = inRolloutBucket(deviceId, app.appCode, app.rolloutPercent);
    const shouldForce = Boolean(app.forceUpdateEnabled || belowMin);
    const shouldShowUpdate = Boolean(app.popupEnabled && hasNewer && eligibleRollout);

    const dialogs = [...storage.getDialogsForApp(app.id, app.appCode)];
    if (shouldForce || shouldShowUpdate) {
      dialogs.unshift({
        dialogType: shouldForce ? "FORCE_UPDATE" : "NORMAL_UPDATE",
        title: app.updateTitle || `Update ${app.displayName}`,
        description: app.updateDescription || `Version ${app.latestVersion} is ready.`,
        primaryButtonText: app.button1Text || "Update Now",
        primaryActionUrl: app.apkDownloadUrl || "",
        secondaryButtonText: shouldForce ? "" : (app.button2Text || "Later"),
        dismissible: !shouldForce,
        laterAllowed: shouldForce ? false : Boolean(app.laterEnabled),
        latestVersion: app.latestVersion
      });
    }

    const activeLic = storage.state.licenses.find(
      (l) => (l.appId === app.id || l.appId === "global") && l.boundDeviceId === deviceId && l.status === "ACTIVE"
    );

    return sendJson(res, 200, {
      ok: true,
      allowed: true,
      signatureVerified,
      app: {
        id: app.id,
        appCode: app.appCode,
        packageName: app.packageName,
        latestVersion: app.latestVersion,
        minSupportedVersion: app.minSupportedVersion,
        forceUpdateEnabled: shouldForce,
        laterEnabled: shouldForce ? false : Boolean(app.laterEnabled),
        rolloutEligible: eligibleRollout
      },
      license: activeLic ? {
        licenseKey: activeLic.licenseKey,
        planTier: activeLic.planTier,
        status: activeLic.status,
        paymentVerified: Boolean(activeLic.paymentVerified),
        expiresAtEpochMs: activeLic.expiresAtEpochMs
      } : null,
      dialogs
    });
  }

  if (req.method === "POST" && pathname === "/api/v1/client/license/activate") {
    const body = await readJsonBody(req);
    const key = String(body.licenseKey || "").trim();
    const deviceId = String(body.deviceId || body.installUuid || "").trim();
    const appCode = String(body.appCode || "").trim();
    if (!key || !deviceId) {
      return sendJson(res, 400, { ok: false, error: "licenseKey and deviceId are required" });
    }
    const app = appCode ? storage.resolveIsolatedApp(appCode) : null;
    const lic = storage.state.licenses.find((l) => l.licenseKey === key);
    if (!lic) return sendJson(res, 404, { ok: false, error: "License key not found" });

    if (lic.appId !== "global" && app && lic.appId !== app.id && lic.appId !== app.appCode) {
      return sendJson(res, 403, { ok: false, error: "License is isolated to a different application" });
    }
    if (lic.status === "REVOKED" || lic.status === "EXPIRED" || lic.status === "SUSPENDED") {
      return sendJson(res, 403, { ok: false, error: `License status is ${lic.status}` });
    }
    if (String(lic.planTier || "").startsWith("VIP") && !lic.paymentVerified) {
      return sendJson(res, 402, { ok: false, error: "VIP entitlement requires verified payment before activation" });
    }
    if (lic.boundDeviceId && lic.boundDeviceId !== deviceId) {
      return sendJson(res, 409, { ok: false, error: "Single-device license already bound to another device" });
    }

    lic.boundDeviceId = deviceId;
    lic.status = "ACTIVE";
    lic.activatedAtMs = lic.activatedAtMs || Date.now();
    if (!lic.expiresAtEpochMs) {
      lic.expiresAtEpochMs = Date.now() + Number(lic.durationDays || 30) * 86_400_000;
    }
    storage.save();
    return sendJson(res, 200, { ok: true, status: lic.status, planTier: lic.planTier, boundDeviceId: lic.boundDeviceId });
  }

  if (req.method === "POST" && pathname === "/api/v1/client/license/transfer") {
    const body = await readJsonBody(req);
    const key = String(body.licenseKey || "").trim();
    const newDeviceId = String(body.newDeviceId || "").trim();
    const lic = storage.state.licenses.find((l) => l.licenseKey === key);
    if (!lic || !newDeviceId) return sendJson(res, 404, { ok: false, error: "License or newDeviceId missing" });

    const transferCount = Number(lic.transferCount || 0);
    if (transferCount >= config.maxLicenseTransfers) {
      return sendJson(res, 429, { ok: false, error: "Maximum device transfers exceeded" });
    }
    const cooldownHours = Number(lic.transferCooldownHours ?? config.defaultCooldownHours);
    if (lic.lastTransferAtMs && Date.now() - Number(lic.lastTransferAtMs) < cooldownHours * 3600000) {
      return sendJson(res, 429, { ok: false, error: `Transfer cooldown (${cooldownHours}h) active` });
    }

    lic.boundDeviceId = newDeviceId;
    lic.transferCount = transferCount + 1;
    lic.lastTransferAtMs = Date.now();
    storage.save();
    return sendJson(res, 200, { ok: true, boundDeviceId: newDeviceId, transferCount: lic.transferCount });
  }

  if (req.method === "POST" && pathname === "/api/v1/client/update-confirm") {
    const body = await readJsonBody(req);
    const appCode = String(body.appCode || "").trim();
    const deviceId = String(body.deviceId || "").trim();
    const installedVersion = String(body.installedVersion || "").trim();
    if (!appCode || !deviceId || !installedVersion) {
      return sendJson(res, 400, { ok: false, error: "appCode, deviceId, and installedVersion are required" });
    }
    const app = storage.resolveIsolatedApp(appCode);
    if (!app) return sendJson(res, 404, { ok: false, error: "App not found" });

    const dev = storage.state.devices.find((d) => d.installUuid === deviceId && d.appId === app.id);
    if (dev) {
      dev.installedVersion = installedVersion;
      dev.updateConfirmedAtMs = Date.now();
    }
    storage.state.updateConfirmations.push({
      appId: app.id,
      deviceId,
      installedVersion,
      confirmedAtMs: Date.now()
    });
    storage.save();
    return sendJson(res, 200, { ok: true, installedVersion });
  }

  return false;
}
