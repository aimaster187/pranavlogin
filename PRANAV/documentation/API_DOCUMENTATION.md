# PRANAV Backend REST API Reference

Base URL: `${API_BASE_URL}` (e.g. `https://api.pranavsmm.shop`)

## 1. Public Health & Client DEX Endpoints
- `GET /api/v1/health` — Returns `{ status, environment, uptimeSeconds, memoryUsedMb, memoryTotalMb, workerCount, loadAvg1m }`.
- `POST /api/v1/client/challenge` — Body `{ appCode, deviceId }`. Returns `{ ok, nonce, expiresInMs }`.
- `POST /api/v1/client/session` — Body `{ appCode, deviceId, versionName, keystorePublicKeyB64, challengeNonce, challengeSignatureB64 }`. Verifies ECDSA P-256 signature, isolates app policy, computes staged rollout, returns `{ allowed, app, license, dialogs[] }`.
- `POST /api/v1/client/license/activate` — Atomic single-device binding + VIP payment verification (`402` if unpaid, `409` if bound elsewhere).
- `POST /api/v1/client/license/transfer` — Enforces `MAX_LICENSE_TRANSFERS` and `DEFAULT_TRANSFER_COOLDOWN_HOURS` (`429` if cooldown active).
- `POST /api/v1/client/update-confirm` — Marks installed version on device record only after explicit client confirmation.

## 2. Owner-Authenticated Endpoints (`Authorization: Bearer <OWNER_API_KEY>`)
- `GET /api/v1/admin/overview`
- `POST /api/v1/admin/apps`
- `POST /api/v1/admin/licenses`
- `POST /api/v1/admin/workspace`
- `GET /api/v1/bots`, `POST /api/v1/bots` (verifies Telegram `getMe`, encrypts token in AES-256-GCM vault), `POST /api/v1/bots/:botId/action` (`START`/`STOP`/`RESTART`), `DELETE /api/v1/bots/:botId`
- `POST /api/v1/telegram/webhook/:botId` (requires `X-Telegram-Bot-Api-Secret-Token`)
