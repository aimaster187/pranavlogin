import http from "node:http";
import path from "node:path";
import { loadConfig } from "./config.js";
import { PranavStorage } from "./storage.js";
import { TelegramMultiBotManager } from "./telegramManager.js";
import { startBackgroundScheduler } from "./workers/scheduler.js";
import { sendJson } from "./httpHelpers.js";
import { handleClientRoutes } from "./routes/client.js";
import { handleAdminRoutes } from "./routes/admin.js";
import { handleBotRoutes } from "./routes/bots.js";

export function createPranavServer(overrides = {}) {
  const config = loadConfig({ ...process.env, ...overrides.env });
  const storage = new PranavStorage(overrides.dataDir || config.dataDir);
  const botManager = new TelegramMultiBotManager({
    config,
    storage,
    fetchImpl: overrides.fetchImpl || fetch
  });
  const scheduler = startBackgroundScheduler({ config, storage });
  const startedAtMs = Date.now();

  const server = http.createServer(async (req, res) => {
    try {
      const urlObj = new URL(req.url || "/", `http://${req.headers.host || "localhost"}`);
      const pathname = urlObj.pathname;

      if (req.method === "GET" && pathname === "/api/v1/health") {
        const sys = scheduler.getSystemMetrics();
        return sendJson(res, 200, {
          status: "ok",
          environment: config.environment,
          uptimeSeconds: Math.floor((Date.now() - startedAtMs) / 1000),
          memoryUsedMb: sys.memoryUsedMb,
          memoryTotalMb: sys.memoryTotalMb,
          workerCount: storage.state.bots.filter((b) => b.enabled).length,
          loadAvg1m: sys.loadAvg1m
        });
      }

      if ((await handleClientRoutes(req, res, pathname, { config, storage })) !== false) return;
      if ((await handleAdminRoutes(req, res, pathname, { config, storage })) !== false) return;
      if ((await handleBotRoutes(req, res, pathname, { config, storage, botManager })) !== false) return;

      sendJson(res, 404, { ok: false, error: `Route not found: ${pathname}` });
    } catch (err) {
      const code = err?.statusCode || 500;
      sendJson(res, code, { ok: false, error: err?.message || "Internal server error" });
    }
  });

  const stop = () => new Promise((resolve) => {
    scheduler.stop();
    botManager.stopAll();
    server.close(() => resolve());
  });

  return { server, config, storage, botManager, scheduler, stop };
}

if (process.argv[1] && path.resolve(process.argv[1]) === path.resolve(new URL(import.meta.url).pathname)) {
  const instance = createPranavServer();
  instance.server.listen(instance.config.port, instance.config.host, () => {
    console.log(
      `[PRANAV Backend] listening on ${instance.config.host}:${instance.config.port} (${instance.config.environment})`
    );
  });
}
