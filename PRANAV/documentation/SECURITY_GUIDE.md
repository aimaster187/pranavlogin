# PRANAV Security Architecture Guide

1. **Zero-Trust Firestore Rules**: Every owner document resides under `/users/{userId}/...` with `request.auth != null && request.auth.uid == userId`, immutable `ownerUid`/`createdAt`, server timestamps (`<= request.time`), and explicit rejection of any raw `botToken` field (`!('botToken' in request.resource.data)`).
2. **AES-256-GCM Bot Token Vault**: Telegram tokens entered in PRANAV Admin are sent only to the backend's `POST /api/v1/bots`, verified live via Telegram `getMe`, and encrypted using AES-256-GCM derived from `VAULT_MASTER_KEY`. Neither Firestore nor Android logs ever store raw tokens.
3. **Hardware-Backed Device Identity**: `PranavDeviceIdentity` uses Android Keystore EC P-256 (`SHA256withECDSA`) + a random installation UUID. IMEI, MAC addresses, and persistent hardware serial numbers are never collected.
4. **Anti-Replay Challenge Auth**: `POST /api/v1/client/challenge` issues single-use nonces that expire in 5 minutes and are deleted immediately upon verification.
5. **Authorization Boundary**: PRANAV tooling refuses DRM removal, anti-tamper circumvention, or license bypass.
