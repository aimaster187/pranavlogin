import path from "node:path";

export function loadConfig(env = process.env) {
  return {
    port: Number(env.PORT || 8080),
    host: env.HOST || "0.0.0.0",
    environment: env.NODE_ENV || "development",
    primaryDomain: env.PRIMARY_DOMAIN || "pranavsmm.shop",
    apiBaseUrl: env.API_BASE_URL || "http://127.0.0.1:8080",
    botWebhookBaseUrl: env.BOT_WEBHOOK_BASE_URL || "https://bot.pranavsmm.shop",
    ownerApiKey: env.OWNER_API_KEY || "",
    vaultMasterKey: env.VAULT_MASTER_KEY || "",
    dataDir: path.resolve(env.DATA_DIR || "./data"),
    telegramApiBase: env.TELEGRAM_API_BASE || "https://api.telegram.org",
    workerIntervalMs: Number(env.WORKER_INTERVAL_MS || 30000),
    defaultCooldownHours: Number(env.DEFAULT_TRANSFER_COOLDOWN_HOURS || 24),
    maxLicenseTransfers: Number(env.MAX_LICENSE_TRANSFERS || 3)
  };
}
