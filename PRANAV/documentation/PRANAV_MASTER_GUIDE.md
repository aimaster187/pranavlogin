# PRANAV Master Owner Guide

## 1. Ecosystem Architecture
1. **PRANAV Android Admin App (`/app`)**: Single Root Owner control console using Google Sign-In + Firestore (`users/{uid}/...`) + REST integration with your Node.js VPS backend.
2. **PRANAV Core DEX (`PRANAV/dex`)**: Compiled standalone DEX payload (`pranav-sdk-1.4.0.dex`) added as the next `classesN.dex` slot inside authorized target APKs.
3. **Separate Hook Layer (`PRANAV/integration`)**: Thin Java/Kotlin/Smali entry points (`PranavApplicationHook`, `PranavActivityHook`, `PranavAttachBaseContextHook`, `PranavDexLoader`) preventing automatic/uncontrolled startup and guarding against duplicate initialization via `PranavInitGuard`.
4. **PRANAV Node.js Backend (`PRANAV/backend`)**: Handles Keystore ECDSA P-256 challenge-response, isolated per-app policies, single-device license binding & transfer cooldowns, update confirmations, and encrypted Telegram bot management.

## 2. Standard Owner Workflow
1. Sign in to **PRANAV Admin** with the Root Owner Google account.
2. Register your managed APK under **Apps** (`appCode`, `packageName`, version, update policy, dialogs).
3. Inspect your authorized target APK in **DEX & Hook -> APK Assistant** to auto-detect existing `classes*.dex`, custom `Application` vs launch `Activity`, and copy the tailored Smali/Java snippet.
4. Integrate `pranav-sdk-1.4.0.dex` and Hook into your authorized APK, re-sign with your owner signing key, and verify with `pranav_apk_inspector.js`.
5. Deploy `PRANAV/backend` to your Ubuntu VPS (`api.pranavsmm.shop`) and register live Telegram bots via **Bots & Ops** (`getMe` + AES-256-GCM vault).
