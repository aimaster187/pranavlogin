# PRANAV Ecosystem — Root Architecture

PRANAV is a single-owner multi-app governance, remote update/dialog orchestration, hardware-backed device & license security, DEX + Hook SDK, and Telegram multi-bot ecosystem.

## Deliverables Layout
- `/app` — **Native PRANAV Android Admin App** (Kotlin + Jetpack Compose, Material 3, Firebase Auth + Firestore, SAF APK Inspector, 52 Admin Modules)
- `/PRANAV/dex` — **PRANAV Core DEX SDK** (`PranavSdk`, `PranavInitGuard`, `PranavDeviceIdentity`, `PranavDiagnostics`, `PranavApiClient`, `PranavDialogEngine`, `build-dex.sh`, `dex-releases.json`)
- `/PRANAV/integration` — **Separate Hook Templates & APK Assistant** (`PranavApplicationHook`, `PranavActivityHook`, `PranavAttachBaseContextHook`, `PranavDexLoader`, Smali templates, Manifest snippets, `pranav_apk_inspector.js`)
- `/PRANAV/backend` — **Modular Node.js Backend** (REST API, AES-256-GCM bot token vault, Keystore EC P-256 verification, Telegram Multi-Bot Manager, expiry/health scheduler)
- `/PRANAV/deployment` — **Ubuntu VPS + Nginx + Certbot + systemd** production configs (`pranavsmm.shop` subdomains)
- `/PRANAV/documentation` — **MT Manager, Hook, DEX, API, Security & Deployment Guides**
- `/PRANAV/tests` — **End-to-End Automated Test Runners & Authorized Sample APK Verification**
