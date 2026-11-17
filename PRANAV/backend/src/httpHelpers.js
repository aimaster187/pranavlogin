import { safeEqual } from "./cryptoVault.js";

export function sendJson(res, statusCode, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "X-Content-Type-Options": "nosniff",
    "Cache-Control": "no-store"
  });
  res.end(body);
}

export async function readJsonBody(req, maxBytes = 1048576) {
  const chunks = [];
  let total = 0;
  for await (const chunk of req) {
    total += chunk.length;
    if (total > maxBytes) {
      throw Object.assign(new Error("Payload too large"), { statusCode: 413 });
    }
    chunks.push(chunk);
  }
  const raw = Buffer.concat(chunks).toString("utf8").trim();
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch {
    throw Object.assign(new Error("Invalid JSON request body"), { statusCode: 400 });
  }
}

export function requireOwnerAuth(req, config) {
  if (!config.ownerApiKey) {
    throw Object.assign(new Error("OWNER_API_KEY is not configured on backend server"), { statusCode: 503 });
  }
  const authHeader = String(req.headers.authorization || "");
  const token = authHeader.startsWith("Bearer ") ? authHeader.slice(7).trim() : "";
  if (!safeEqual(token, config.ownerApiKey)) {
    throw Object.assign(new Error("Unauthorized owner API credentials"), { statusCode: 401 });
  }
}
