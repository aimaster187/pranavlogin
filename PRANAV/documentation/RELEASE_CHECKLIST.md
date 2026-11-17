# PRANAV Production Release Checklist

1. [x] Verify Android Admin compilation (`gradle :app:assembleDebug` / `compile_applet`).
2. [x] Run Robolectric & Unit test suite (`gradle :app:testDebugUnitTest`).
3. [x] Run Firestore zero-trust rules tests (`firestore.test.js`) and deploy `firestore.rules`.
4. [x] Run PRANAV Node.js backend, isolation, Keystore EC P-256 & Telegram bot tests (`node --test PRANAV/backend/tests/backend.test.js`).
5. [x] Build PRANAV Core DEX (`build-dex.sh`), attach as `classes2.dex` + `PranavApplicationHook` into an authorized sample APK, verify `apksigner`, and inspect via `pranav_apk_inspector.js`.
6. [ ] Deploy `PRANAV/backend` + Nginx + Certbot to live Ubuntu VPS (`pranavsmm.shop`, `admin`, `api`, `bot`).
7. [ ] Enter live `OWNER_API_KEY` + BotFather tokens inside PRANAV Admin Console (`Bots & Ops`).
