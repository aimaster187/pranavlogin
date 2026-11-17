import crypto from "node:crypto";
import { decryptSecret, encryptSecret, redactSensitiveText } from "./cryptoVault.js";

export const USER_BOT_COMMANDS = [
  "start", "help", "menu", "status", "license", "activate", "vip",
  "device", "transfer", "update", "changelog", "channel", "support", "ping"
];

export const OWNER_BOT_COMMANDS = [
  "owner", "health", "apps", "appinfo", "maintenance", "lockdown", "genkey",
  "vipgrant", "revokekey", "extendkey", "devices", "approvedev", "blockdev",
  "transferreset", "bots", "botstart", "botstop", "botrestart", "broadcast",
  "tickets", "resolve", "updatecfg", "forceupdate", "audit", "backup"
];

export class TelegramMultiBotManager {
  constructor({ config, storage, fetchImpl = fetch }) {
    this.config = config;
    this.storage = storage;
    this.fetch = fetchImpl;
    this.pollTimers = new Map();
    this.offsets = new Map();
    this.rateWindows = new Map();
  }

  async verifyTokenWithTelegram(botToken) {
    const url = `${this.config.telegramApiBase}/bot${botToken}/getMe`;
    const res = await this.fetch(url, { method: "GET" });
    const data = await res.json();
    if (!res.ok || !data?.ok || !data?.result?.id) {
      throw new Error(redactSensitiveText(data?.description || "Telegram getMe verification failed"));
    }
    return data.result;
  }

  async registerBot({ botUsername, botDisplayName, botToken, roleType, mode, rateLimitPerMin }) {
    const cleanToken = String(botToken || "").trim();
    if (!cleanToken) throw new Error("Bot token is required.");
    const me = await this.verifyTokenWithTelegram(cleanToken);
    const botId = `bot_${me.id}`;
    const verifiedUsername = me.username || String(botUsername || "").replace(/^@/, "");
    const tokenSecretRef = `vault://bots/${botId}`;

    const encryptedEnvelope = encryptSecret(cleanToken, this.config.vaultMasterKey);
    this.storage.state.vault[botId] = encryptedEnvelope;

    const record = {
      id: botId,
      telegramBotId: me.id,
      botUsername: verifiedUsername,
      botDisplayName: botDisplayName || me.first_name || verifiedUsername,
      roleType: roleType === "OWNER_BOT" ? "OWNER_BOT" : "USER_BOT",
      mode: mode === "WEBHOOK" ? "WEBHOOK" : "POLLING",
      commandsCount: roleType === "OWNER_BOT" ? OWNER_BOT_COMMANDS.length : USER_BOT_COMMANDS.length,
      tokenSecretRef,
      healthStatus: "ACTIVE",
      enabled: true,
      rateLimitPerMin: Math.max(10, Number(rateLimitPerMin || 120)),
      webhookSecret: crypto.randomBytes(16).toString("hex"),
      updatedAtMs: Date.now()
    };

    const idx = this.storage.state.bots.findIndex((b) => b.id === botId);
    if (idx >= 0) this.storage.state.bots[idx] = record;
    else this.storage.state.bots.push(record);
    this.storage.appendEvent("BOT", "Telegram Bot Registered", `@${verifiedUsername} (${record.roleType}) verified via getMe`);

    if (record.mode === "POLLING" && record.enabled && this.config.environment !== "test") {
      this.startPolling(botId);
    }

    return {
      ok: true,
      botId,
      botUsername: verifiedUsername,
      tokenSecretRef,
      verifiedTelegramId: me.id,
      healthStatus: record.healthStatus
    };
  }

  performAction(botId, action) {
    const bot = this.storage.state.bots.find((b) => b.id === botId);
    if (!bot) throw Object.assign(new Error("Bot not found"), { statusCode: 404 });

    const upper = String(action || "").toUpperCase();
    if (upper === "STOP") {
      bot.enabled = false;
      bot.healthStatus = "PAUSED";
      this.stopPolling(botId);
    } else if (upper === "START" || upper === "RESTART") {
      if (upper === "RESTART") this.stopPolling(botId);
      bot.enabled = true;
      bot.healthStatus = "ACTIVE";
      if (bot.mode === "POLLING" && this.config.environment !== "test") {
        this.startPolling(botId);
      }
    } else {
      throw Object.assign(new Error(`Unsupported bot action: ${action}`), { statusCode: 400 });
    }
    bot.updatedAtMs = Date.now();
    this.storage.appendEvent("BOT", `Bot ${upper}`, `@${bot.botUsername} -> ${bot.healthStatus}`);
    return { ok: true, status: bot.healthStatus };
  }

  deleteBot(botId) {
    this.stopPolling(botId);
    this.storage.state.bots = this.storage.state.bots.filter((b) => b.id !== botId);
    delete this.storage.state.vault[botId];
    this.storage.save();
    return { ok: true };
  }

  checkRateLimit(bot, senderId) {
    const key = `${bot.id}:${senderId}`;
    const now = Date.now();
    const bucket = (this.rateWindows.get(key) || []).filter((ts) => now - ts < 60000);
    if (bucket.length >= (bot.rateLimitPerMin || 120)) {
      return false;
    }
    bucket.push(now);
    this.rateWindows.set(key, bucket);
    return true;
  }

  async handleIncomingUpdate(botId, update) {
    const bot = this.storage.state.bots.find((b) => b.id === botId);
    if (!bot || !bot.enabled) return { handled: false, reason: "bot_inactive" };

    if (update.callback_query) {
      const cq = update.callback_query;
      const senderId = String(cq.from?.id || "anon");
      if (!this.checkRateLimit(bot, senderId)) return { handled: false, reason: "rate_limited" };
      const reply = this.processCallbackQuery(bot, cq.data || "");
      await this.sendTelegramAnswer(botId, cq.message?.chat?.id, reply.text, reply.replyMarkup, cq.id);
      return { handled: true, type: "callback", response: reply };
    }

    const msg = update.message;
    if (!msg || !msg.text) return { handled: false, reason: "no_text" };
    const senderId = String(msg.from?.id || "anon");
    if (!this.checkRateLimit(bot, senderId)) return { handled: false, reason: "rate_limited" };

    const reply = this.processTextCommand(bot, msg.text.trim(), senderId);
    await this.sendTelegramAnswer(botId, msg.chat?.id, reply.text, reply.replyMarkup, null);
    return { handled: true, type: "command", response: reply };
  }

  processCallbackQuery(bot, callbackData) {
    if (callbackData === "PRANAV_CONTINUE" || callbackData === "menu_main") {
      return {
        text: `PRANAV Main Menu (${bot.roleType})\nSelect an operational action:`,
        replyMarkup: {
          inline_keyboard: [
            [
              { text: "License Status", callback_data: "menu_license" },
              { text: "VIP Status", callback_data: "menu_vip" }
            ],
            [
              { text: "Device Info", callback_data: "menu_device" },
              { text: "Check Update", callback_data: "menu_update" }
            ],
            [
              { text: "Transfer Key", callback_data: "menu_transfer" },
              { text: "Support Ticket", callback_data: "menu_support" }
            ]
          ]
        }
      };
    }

    const map = {
      menu_license: "License Check: Send `/activate <LICENSE_KEY> <INSTALL_UUID>` or `/license <LICENSE_KEY>`.",
      menu_vip: "VIP Verification: Send `/vip <LICENSE_KEY>`. VIP unlocks only after verified payment.",
      menu_device: "Device Binding: PRANAV uses install UUID + Keystore EC P-256 identity. Send `/device <INSTALL_UUID>`.",
      menu_update: `Latest Managed Apps: ${this.storage.state.apps.length} app(s) registered. Send \`/update <APP_CODE>\`.`,
      menu_transfer: `Single-Device Transfer: Max ${this.config.maxLicenseTransfers} transfers, ${this.config.defaultCooldownHours}h cooldown. Send \`/transfer <KEY> <NEW_UUID>\`.`,
      menu_support: "Support Desk: Send `/support <your issue message>` to create a tracked ticket."
    };
    return {
      text: map[callbackData] || "Action acknowledged. Return to Main Menu anytime.",
      replyMarkup: {
        inline_keyboard: [[{ text: "Back to Main Menu", callback_data: "menu_main" }]]
      }
    };
  }

  processTextCommand(bot, text, senderId) {
    const parts = text.split(/\s+/);
    const rawCmd = parts[0].toLowerCase().replace(/^\//, "").split("@")[0];
    const args = parts.slice(1);

    if (rawCmd === "start") {
      return {
        text: `Welcome to ${bot.botDisplayName} powered by PRANAV Core.\nTap CONTINUE below to open the interactive Main Menu.`,
        replyMarkup: {
          inline_keyboard: [[{ text: "CONTINUE", callback_data: "PRANAV_CONTINUE" }]]
        }
      };
    }

    if (rawCmd === "menu") {
      return this.processCallbackQuery(bot, "menu_main");
    }

    if (USER_BOT_COMMANDS.includes(rawCmd)) {
      return { text: this.executeUserCommand(rawCmd, args, senderId) };
    }

    if (OWNER_BOT_COMMANDS.includes(rawCmd)) {
      if (bot.roleType !== "OWNER_BOT") {
        return { text: "Access denied: Owner commands are isolated to OWNER_BOT instances." };
      }
      return { text: this.executeOwnerCommand(rawCmd, args) };
    }

    return { text: "Unknown command. Send /help or /menu to view available commands." };
  }

  executeUserCommand(cmd, args, senderId) {
    const state = this.storage.state;
    switch (cmd) {
      case "help":
        return `User Bot Commands (${USER_BOT_COMMANDS.length}): ` + USER_BOT_COMMANDS.map((c) => `/${c}`).join(", ");
      case "ping":
        return `Pong • PRANAV Core active (${state.apps.length} managed apps)`;
      case "status":
        return `Workspace Maintenance: ${state.workspace.masterMaintenance ? "ON" : "OFF"} | Lockdown: ${state.workspace.emergencyLockdown ? "ON" : "OFF"}`;
      case "license": {
        const key = args[0];
        const lic = state.licenses.find((l) => l.licenseKey === key);
        return lic ? `Key ${lic.licenseKey}: ${lic.status} (${lic.planTier}) Bound=${lic.boundDeviceId || "UNBOUND"}` : "Provide a valid key: /license <KEY>";
      }
      case "activate": {
        const [key, uuid] = args;
        if (!key || !uuid) return "Usage: /activate <KEY> <INSTALL_UUID>";
        const lic = state.licenses.find((l) => l.licenseKey === key);
        if (!lic) return "License key not found.";
        if (lic.boundDeviceId && lic.boundDeviceId !== uuid) return "License is already bound to another device.";
        if (lic.planTier.startsWith("VIP") && !lic.paymentVerified) return "VIP payment verification required before activation.";
        lic.boundDeviceId = uuid;
        lic.status = "ACTIVE";
        this.storage.save();
        return `Activated ${lic.licenseKey} for device ${uuid}.`;
      }
      case "vip": {
        const lic = state.licenses.find((l) => l.licenseKey === args[0]);
        if (!lic) return "Usage: /vip <KEY>";
        return `VIP Tier=${lic.planTier}, PaymentVerified=${lic.paymentVerified}, Status=${lic.status}`;
      }
      case "device": {
        const dev = state.devices.find((d) => d.installUuid === args[0]);
        return dev ? `Device ${dev.deviceLabel}: ${dev.approvalStatus} | Risk ${dev.riskScore}/100` : "Usage: /device <INSTALL_UUID>";
      }
      case "transfer": {
        const [key, newUuid] = args;
        const lic = state.licenses.find((l) => l.licenseKey === key);
        if (!lic || !newUuid) return "Usage: /transfer <KEY> <NEW_INSTALL_UUID>";
        const count = Number(lic.transferCount || 0);
        if (count >= this.config.maxLicenseTransfers) return "Transfer limit reached.";
        const lastTransfer = Number(lic.lastTransferAtMs || 0);
        const cooldownMs = Number(lic.transferCooldownHours || this.config.defaultCooldownHours) * 3600000;
        if (lastTransfer && Date.now() - lastTransfer < cooldownMs) return "Transfer cooldown still active.";
        lic.boundDeviceId = newUuid;
        lic.transferCount = count + 1;
        lic.lastTransferAtMs = Date.now();
        this.storage.save();
        return `Transferred ${lic.licenseKey} cleanly to ${newUuid}.`;
      }
      case "update": {
        const app = state.apps.find((a) => a.appCode === args[0] || a.packageName === args[0]) || state.apps[0];
        return app ? `${app.displayName}: Latest v${app.latestVersion} (Force=${app.forceUpdateEnabled})` : "No apps published yet.";
      }
      case "changelog": {
        const app = state.apps.find((a) => a.appCode === args[0]) || state.apps[0];
        return app ? `${app.displayName} Notes: ${app.releaseNotes}` : "No changelog available.";
      }
      case "channel": {
        const app = state.apps.find((a) => a.channelLink) || state.apps[0];
        return app?.channelLink ? `Official Channel: ${app.channelLink}` : "No official channel configured.";
      }
      case "support": {
        const msg = args.join(" ").trim();
        if (!msg) return "Usage: /support <describe your issue>";
        const evt = this.storage.appendEvent("SUPPORT", `Telegram Ticket (${senderId})`, msg, "INFO");
        return `Ticket ${evt.id} created and queued for Root Owner.`;
      }
      default:
        return "Command processed.";
    }
  }

  executeOwnerCommand(cmd, args) {
    const state = this.storage.state;
    switch (cmd) {
      case "owner":
        return `Owner Commands (${OWNER_BOT_COMMANDS.length}): ` + OWNER_BOT_COMMANDS.map((c) => `/${c}`).join(", ");
      case "health":
        return `Apps:${state.apps.length} Devices:${state.devices.length} Keys:${state.licenses.length} Bots:${state.bots.length}`;
      case "apps":
        return state.apps.map((a) => `${a.appCode}(${a.lifecycleStatus})`).join(", ") || "No apps registered.";
      case "appinfo": {
        const app = this.storage.resolveIsolatedApp(args[0]);
        return app ? `${app.displayName} [${app.packageName}] v${app.latestVersion} Force=${app.forceUpdateEnabled}` : "App not found.";
      }
      case "maintenance": {
        const mode = (args[0] || "").toLowerCase() === "on";
        state.workspace.masterMaintenance = mode;
        this.storage.save();
        return `Master maintenance set to ${mode ? "ON" : "OFF"}.`;
      }
      case "lockdown": {
        const mode = (args[0] || "").toLowerCase() === "on";
        state.workspace.emergencyLockdown = mode;
        this.storage.save();
        return `Emergency lockdown set to ${mode ? "ON" : "OFF"}.`;
      }
      case "genkey": {
        const tier = (args[0] || "STANDARD").toUpperCase();
        const key = `PRNV-${crypto.randomBytes(4).toString("hex").toUpperCase()}`;
        state.licenses.push({
          id: `lic_${Date.now()}`,
          appId: args[1] || "global",
          licenseKey: key,
          planTier: tier,
          status: tier.startsWith("VIP") ? "SUSPENDED" : "UNUSED",
          paymentVerified: !tier.startsWith("VIP"),
          transferCooldownHours: this.config.defaultCooldownHours,
          transferCount: 0
        });
        this.storage.save();
        return `Generated key: ${key} (${tier})`;
      }
      case "vipgrant": {
        const lic = state.licenses.find((l) => l.licenseKey === args[0]);
        if (!lic) return "Key not found.";
        lic.paymentVerified = true;
        lic.planTier = "VIP_PRO";
        lic.status = "ACTIVE";
        this.storage.save();
        return `Verified payment & granted VIP_PRO on ${lic.licenseKey}.`;
      }
      case "revokekey": {
        const lic = state.licenses.find((l) => l.licenseKey === args[0]);
        if (!lic) return "Key not found.";
        lic.status = "REVOKED";
        this.storage.save();
        return `Revoked ${lic.licenseKey}.`;
      }
      case "extendkey": {
        const lic = state.licenses.find((l) => l.licenseKey === args[0]);
        const days = Number(args[1] || 30);
        if (!lic) return "Key not found.";
        lic.durationDays = Number(lic.durationDays || 0) + days;
        this.storage.save();
        return `Extended ${lic.licenseKey} by ${days}d.`;
      }
      case "devices":
        return `Registered Devices: ${state.devices.length} (Approved: ${state.devices.filter((d) => d.approvalStatus === "APPROVED").length})`;
      case "approvedev": {
        const dev = state.devices.find((d) => d.installUuid === args[0]);
        if (!dev) return "Device not found.";
        dev.approvalStatus = "APPROVED";
        this.storage.save();
        return `Approved device ${dev.installUuid}.`;
      }
      case "blockdev": {
        const dev = state.devices.find((d) => d.installUuid === args[0]);
        if (!dev) return "Device not found.";
        dev.approvalStatus = "BLOCKED";
        this.storage.save();
        return `Blocked device ${dev.installUuid}.`;
      }
      case "transferreset": {
        const lic = state.licenses.find((l) => l.licenseKey === args[0]);
        if (!lic) return "License not found.";
        lic.transferCount = 0;
        lic.lastTransferAtMs = 0;
        this.storage.save();
        return `Reset transfer cooldown for ${lic.licenseKey}.`;
      }
      case "bots":
        return state.bots.map((b) => `@${b.botUsername}[${b.healthStatus}]`).join(", ") || "No bots.";
      case "botstart":
        this.performAction(args[0], "START");
        return `Started ${args[0]}`;
      case "botstop":
        this.performAction(args[0], "STOP");
        return `Stopped ${args[0]}`;
      case "botrestart":
        this.performAction(args[0], "RESTART");
        return `Restarted ${args[0]}`;
      case "broadcast": {
        const msg = args.join(" ");
        this.storage.appendEvent("ANNOUNCEMENT", "Owner Bot Broadcast", msg || "Update notice", "INFO");
        return "Broadcast queued in audit & comms feed.";
      }
      case "tickets":
        return `Open tickets: ${state.events.filter((e) => e.category === "SUPPORT" && e.status === "OPEN").length}`;
      case "resolve": {
        const evt = state.events.find((e) => e.id === args[0]);
        if (!evt) return "Ticket not found.";
        evt.status = "RESOLVED";
        this.storage.save();
        return `Resolved ticket ${evt.id}.`;
      }
      case "updatecfg": {
        const app = this.storage.resolveIsolatedApp(args[0]);
        if (!app || !args[1]) return "Usage: /updatecfg <APP_CODE> <LATEST_VER>";
        app.latestVersion = args[1];
        this.storage.save();
        return `Updated latest version of ${app.appCode} -> ${app.latestVersion}`;
      }
      case "forceupdate": {
        const app = this.storage.resolveIsolatedApp(args[0]);
        if (!app) return "Usage: /forceupdate <APP_CODE> <on|off>";
        app.forceUpdateEnabled = (args[1] || "").toLowerCase() === "on";
        if (app.forceUpdateEnabled) app.laterEnabled = false;
        this.storage.save();
        return `Force update on ${app.appCode}: ${app.forceUpdateEnabled}`;
      }
      case "audit":
        return `Latest ${Math.min(3, state.events.length)} events: ` + state.events.slice(0, 3).map((e) => e.title).join(" | ");
      case "backup":
        this.storage.save();
        return `Atomic snapshot saved cleanly to ${this.storage.dbFile}`;
      default:
        return "Owner command processed.";
    }
  }

  async sendTelegramAnswer(botId, chatId, text, replyMarkup, callbackQueryId) {
    if (this.config.environment === "test" || !chatId) return;
    const envelope = this.storage.state.vault[botId];
    if (!envelope) return;
    const token = decryptSecret(envelope, this.config.vaultMasterKey);
    if (callbackQueryId) {
      await this.fetch(`${this.config.telegramApiBase}/bot${token}/answerCallbackQuery`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ callback_query_id: callbackQueryId })
      }).catch(() => {});
    }
    await this.fetch(`${this.config.telegramApiBase}/bot${token}/sendMessage`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        chat_id: chatId,
        text,
        reply_markup: replyMarkup
      })
    }).catch(() => {});
  }

  startPolling(botId) {
    if (this.pollTimers.has(botId)) return;
    const timer = setInterval(async () => {
      const bot = this.storage.state.bots.find((b) => b.id === botId);
      if (!bot || !bot.enabled || bot.mode !== "POLLING") {
        this.stopPolling(botId);
        return;
      }
      try {
        const envelope = this.storage.state.vault[botId];
        if (!envelope) return;
        const token = decryptSecret(envelope, this.config.vaultMasterKey);
        const offset = this.offsets.get(botId) || 0;
        const res = await this.fetch(`${this.config.telegramApiBase}/bot${token}/getUpdates?timeout=5&offset=${offset}`);
        const json = await res.json();
        if (json?.ok && Array.isArray(json.result)) {
          for (const upd of json.result) {
            this.offsets.set(botId, Math.max(this.offsets.get(botId) || 0, upd.update_id + 1));
            await this.handleIncomingUpdate(botId, upd);
          }
        }
      } catch (err) {
        bot.lastError = redactSensitiveText(err?.message || "poll_error");
      }
    }, 5000);
    timer.unref();
    this.pollTimers.set(botId, timer);
  }

  stopPolling(botId) {
    const t = this.pollTimers.get(botId);
    if (t) {
      clearInterval(t);
      this.pollTimers.delete(botId);
    }
  }

  stopAll() {
    for (const botId of this.pollTimers.keys()) {
      this.stopPolling(botId);
    }
  }
}
