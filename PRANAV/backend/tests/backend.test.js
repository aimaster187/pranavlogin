import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import crypto from "node:crypto";
import { createPranavServer } from "../src/server.js";
import { decryptSecret } from "../src/cryptoVault.js";
import { OWNER_BOT_COMMANDS, USER_BOT_COMMANDS } from "../src/telegramManager.js";

const MASTER_SECRET = "pranav_test_vault_master_secret_key_2026";
const OWNER_KEY = "pranav_owner_bearer_secret_2026";

let tempDir;
let instance;
let baseUrl;

test.before(async () => {
  tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "pranav-backend-test-"));
  const stubFetch = async (url) => {
    if (String(url).includes("/getMe")) {
      return new Response(
        JSON.stringify({ ok: true, result: { id: 987654321, username: "pranav_verified_bot", first_name: "PRANAV Bot" } }),
        { status: 200, headers: { "Content-Type": "application/json" } }
      );
    }
    return new Response(JSON.stringify({ ok: true, result: {} }), { status: 200 });
  };

  instance = createPranavServer({
    dataDir: tempDir,
    fetchImpl: stubFetch,
    env: {
      NODE_ENV: "test",
      PORT: "0",
      OWNER_API_KEY: OWNER_KEY,
      VAULT_MASTER_KEY: MASTER_SECRET
    }
  });
  await new Promise((resolve) => instance.server.listen(0, "127.0.0.1", resolve));
  const addr = instance.server.address();
  baseUrl = `http://127.0.0.1:${addr.port}`;
});

test.after(async () => {
  if (instance) await instance.stop();
  if (tempDir) fs.rmSync(tempDir, { recursive: true, force: true });
});

test("Health endpoint returns real OS metrics and admin auth blocks unauthenticated access", async () => {
  const healthRes = await fetch(`${baseUrl}/api/v1/health`);
  assert.equal(healthRes.status, 200);
  const health = await healthRes.json();
  assert.equal(health.status, "ok");
  assert.ok(health.memoryTotalMb > 0);

  const denied = await fetch(`${baseUrl}/api/v1/admin/overview`);
  assert.equal(denied.status, 401);
});

test("Multi-app isolation, ECDSA P-256 Keystore handshake, force update, and update confirmation", async () => {
  // Register 2 isolated apps
  const app1Res = await fetch(`${baseUrl}/api/v1/admin/apps`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      id: "app_alpha",
      appCode: "alpha",
      packageName: "com.pranav.alpha",
      displayName: "Alpha App",
      latestVersion: "2.0.0",
      minSupportedVersion: "1.5.0",
      forceUpdateEnabled: true,
      laterEnabled: true
    })
  });
  assert.equal(app1Res.status, 200);
  const app1Data = await app1Res.json();
  assert.equal(app1Data.app.laterEnabled, false);

  await fetch(`${baseUrl}/api/v1/admin/apps`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      id: "app_beta",
      appCode: "beta",
      packageName: "com.pranav.beta",
      displayName: "Beta App",
      latestVersion: "1.0.0"
    })
  });

  // Challenge + Keystore EC P-256 signing
  const { publicKey, privateKey } = crypto.generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  const pubSpkiB64 = publicKey.export({ type: "spki", format: "der" }).toString("base64");

  const chRes = await fetch(`${baseUrl}/api/v1/client/challenge`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "alpha", deviceId: "dev-uuid-1" })
  });
  const ch = await chRes.json();
  assert.ok(ch.nonce);

  const signatureB64 = crypto.sign("sha256", Buffer.from(ch.nonce, "utf8"), privateKey).toString("base64");

  const hsRes = await fetch(`${baseUrl}/api/v1/client/session`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      appCode: "alpha",
      deviceId: "dev-uuid-1",
      versionName: "1.2.0",
      keystorePublicKeyB64: pubSpkiB64,
      challengeNonce: ch.nonce,
      challengeSignatureB64: signatureB64
    })
  });
  assert.equal(hsRes.status, 200);
  const hs = await hsRes.json();
  assert.equal(hs.signatureVerified, true);
  assert.equal(hs.app.forceUpdateEnabled, true);
  assert.equal(hs.app.laterEnabled, false);

  // Replay same nonce must fail with 403
  const replayRes = await fetch(`${baseUrl}/api/v1/client/session`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      appCode: "alpha",
      deviceId: "dev-uuid-1",
      keystorePublicKeyB64: pubSpkiB64,
      challengeNonce: ch.nonce,
      challengeSignatureB64: signatureB64
    })
  });
  assert.equal(replayRes.status, 403);

  // Update is only marked installed on explicit update-confirm
  const devBefore = instance.storage.state.devices.find((d) => d.installUuid === "dev-uuid-1");
  assert.equal(devBefore.installedVersion, "1.2.0");

  const confirmRes = await fetch(`${baseUrl}/api/v1/client/update-confirm`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "alpha", deviceId: "dev-uuid-1", installedVersion: "2.0.0" })
  });
  assert.equal(confirmRes.status, 200);
  const devAfter = instance.storage.state.devices.find((d) => d.installUuid === "dev-uuid-1");
  assert.equal(devAfter.installedVersion, "2.0.0");
});

test("Single-device license binding, VIP payment verification, transfer cooldown, and app isolation", async () => {
  await fetch(`${baseUrl}/api/v1/admin/licenses`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      appId: "app_alpha",
      licenseKey: "PRNV-UNPAID-VIP",
      planTier: "VIP_PRO",
      paymentVerified: false
    })
  });

  const unpaidRes = await fetch(`${baseUrl}/api/v1/client/license/activate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "alpha", licenseKey: "PRNV-UNPAID-VIP", deviceId: "dev-uuid-1" })
  });
  assert.equal(unpaidRes.status, 402);

  await fetch(`${baseUrl}/api/v1/admin/licenses`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      appId: "app_alpha",
      licenseKey: "PRNV-PAID-VIP",
      planTier: "VIP_PRO",
      paymentVerified: true,
      transferCooldownHours: 24
    })
  });

  // Cross-app isolation check: beta cannot activate alpha's license
  const crossAppRes = await fetch(`${baseUrl}/api/v1/client/license/activate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "beta", licenseKey: "PRNV-PAID-VIP", deviceId: "dev-uuid-1" })
  });
  assert.equal(crossAppRes.status, 403);

  // Legitimate activation binds to dev-uuid-1
  const okAct = await fetch(`${baseUrl}/api/v1/client/license/activate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "alpha", licenseKey: "PRNV-PAID-VIP", deviceId: "dev-uuid-1" })
  });
  assert.equal(okAct.status, 200);

  // Second device blocked (409)
  const secondDev = await fetch(`${baseUrl}/api/v1/client/license/activate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ appCode: "alpha", licenseKey: "PRNV-PAID-VIP", deviceId: "dev-uuid-2" })
  });
  assert.equal(secondDev.status, 409);

  // Transfer succeeds once then hits 24h cooldown (429)
  const tr1 = await fetch(`${baseUrl}/api/v1/client/license/transfer`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ licenseKey: "PRNV-PAID-VIP", newDeviceId: "dev-uuid-2" })
  });
  assert.equal(tr1.status, 200);

  const tr2 = await fetch(`${baseUrl}/api/v1/client/license/transfer`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ licenseKey: "PRNV-PAID-VIP", newDeviceId: "dev-uuid-3" })
  });
  assert.equal(tr2.status, 429);
});

test("Telegram Multi-Bot Vault encrypts token, executes /start CONTINUE Main Menu, 14 User & 25 Owner commands", async () => {
  assert.equal(USER_BOT_COMMANDS.length, 14);
  assert.equal(OWNER_BOT_COMMANDS.length, 25);

  const rawToken = "123456789:AAH_TEST_FAKE_TELEGRAM_TOKEN_VALUE";
  const regRes = await fetch(`${baseUrl}/api/v1/bots`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      botUsername: "pranav_verified_bot",
      botDisplayName: "PRANAV Owner Ops",
      botToken: rawToken,
      roleType: "OWNER_BOT",
      mode: "WEBHOOK",
      rateLimitPerMin: 120
    })
  });
  assert.equal(regRes.status, 200);
  const reg = await regRes.json();
  assert.equal(reg.ok, true);
  assert.match(reg.tokenSecretRef, /^vault:\/\/bots\//);

  // Confirm encrypted vault holds ciphertext rather than raw token
  const storedCipher = instance.storage.state.vault[reg.botId];
  assert.ok(!storedCipher.includes(rawToken));
  assert.equal(decryptSecret(storedCipher, MASTER_SECRET), rawToken);

  // Public list must not leak botToken or webhookSecret
  const listRes = await fetch(`${baseUrl}/api/v1/bots`, {
    headers: { Authorization: `Bearer ${OWNER_KEY}` }
  });
  const listJson = await listRes.json();
  assert.equal(listJson.bots[0].botToken, undefined);
  assert.equal(listJson.bots[0].webhookSecret, undefined);

  // Test /start -> CONTINUE -> Main Menu callback flow
  const botRecord = instance.storage.state.bots.find((b) => b.id === reg.botId);
  const startReply = instance.botManager.processTextCommand(botRecord, "/start", "owner_tg");
  assert.equal(startReply.replyMarkup.inline_keyboard[0][0].callback_data, "PRANAV_CONTINUE");

  const menuReply = instance.botManager.processCallbackQuery(botRecord, "PRANAV_CONTINUE");
  assert.ok(menuReply.text.includes("Main Menu"));
  assert.equal(menuReply.replyMarkup.inline_keyboard.length, 3);

  const ownerReply = instance.botManager.processTextCommand(botRecord, "/health", "owner_tg");
  assert.ok(ownerReply.text.includes("Apps:"));
});

test("Admin sync endpoints manage dialogs, devices, events, and app deletion", async () => {
  const dlgRes = await fetch(`${baseUrl}/api/v1/admin/dialogs`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      id: "dlg_sync_1",
      appId: "app_beta",
      dialogType: "ANNOUNCEMENT",
      title: "Beta Channel Ready",
      description: "New telemetry enabled."
    })
  });
  assert.equal(dlgRes.status, 200);
  const dlgJson = await dlgRes.json();
  assert.equal(dlgJson.dialog.title, "Beta Channel Ready");

  const devRes = await fetch(`${baseUrl}/api/v1/admin/devices`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      id: "dev_sync_1",
      appId: "app_beta",
      deviceLabel: "Pixel 9 Owner",
      installUuid: "uuid-sync-1",
      approvalStatus: "APPROVED"
    })
  });
  assert.equal(devRes.status, 200);
  assert.equal((await devRes.json()).device.approvalStatus, "APPROVED");

  const evtRes = await fetch(`${baseUrl}/api/v1/admin/events`, {
    method: "POST",
    headers: { Authorization: `Bearer ${OWNER_KEY}`, "Content-Type": "application/json" },
    body: JSON.stringify({ appId: "app_beta", category: "AUDIT", title: "Sync Check", message: "OK" })
  });
  assert.equal(evtRes.status, 200);

  const delRes = await fetch(`${baseUrl}/api/v1/admin/apps/app_beta`, {
    method: "DELETE",
    headers: { Authorization: `Bearer ${OWNER_KEY}` }
  });
  assert.equal(delRes.status, 200);
  assert.equal(instance.storage.state.apps.some((a) => a.id === "app_beta"), false);
});
