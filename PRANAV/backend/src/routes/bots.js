import { readJsonBody, requireOwnerAuth, sendJson } from "../httpHelpers.js";
import { safeEqual } from "../cryptoVault.js";

export async function handleBotRoutes(req, res, pathname, { config, storage, botManager }) {
  // Public Telegram webhook receiver protected by per-bot secret token header
  const webhookMatch = pathname.match(/^\/api\/v1\/telegram\/webhook\/([^/]+)$/);
  if (req.method === "POST" && webhookMatch) {
    const botId = webhookMatch[1];
    const bot = storage.state.bots.find((b) => b.id === botId);
    if (!bot) return sendJson(res, 404, { ok: false, error: "Bot not registered" });

    const secretHeader = String(req.headers["x-telegram-bot-api-secret-token"] || "");
    if (!safeEqual(secretHeader, bot.webhookSecret)) {
      return sendJson(res, 401, { ok: false, error: "Invalid Telegram webhook secret" });
    }
    const update = await readJsonBody(req);
    const result = await botManager.handleIncomingUpdate(botId, update);
    return sendJson(res, 200, { ok: true, result });
  }

  if (req.method === "GET" && pathname === "/api/v1/bots") {
    requireOwnerAuth(req, config);
    // Strip webhookSecret before sending bot list
    const safeBots = storage.state.bots.map(({ webhookSecret, ...rest }) => rest);
    return sendJson(res, 200, { ok: true, bots: safeBots });
  }

  if (req.method === "POST" && pathname === "/api/v1/bots") {
    requireOwnerAuth(req, config);
    const body = await readJsonBody(req);
    try {
      const reg = await botManager.registerBot(body);
      return sendJson(res, 200, reg);
    } catch (err) {
      return sendJson(res, 400, { ok: false, message: err.message });
    }
  }

  const actionMatch = pathname.match(/^\/api\/v1\/bots\/([^/]+)\/action$/);
  if (req.method === "POST" && actionMatch) {
    requireOwnerAuth(req, config);
    const body = await readJsonBody(req);
    const resObj = botManager.performAction(actionMatch[1], body.action);
    return sendJson(res, 200, resObj);
  }

  const deleteMatch = pathname.match(/^\/api\/v1\/bots\/([^/]+)$/);
  if (req.method === "DELETE" && deleteMatch) {
    requireOwnerAuth(req, config);
    return sendJson(res, 200, botManager.deleteBot(deleteMatch[1]));
  }

  return false;
}
