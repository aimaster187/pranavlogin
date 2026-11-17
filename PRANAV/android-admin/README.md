# PRANAV Native Android Admin App

The native Kotlin + Jetpack Compose control console lives in `/app` so Gradle and Android Studio / AI Studio Streaming Emulator build and deploy it directly.

## Core Architecture
- **Authentication**: Single Root Owner via Google Sign-In (`CredentialManager` + `GetSignInWithGoogleOption` + `FirebaseAuth`).
- **Cloud Database**: Dedicated Firestore instance (`ai-studio-android-pranavad-0a07513d-88c9-492d-8858-e86bb8da7452`) with zero-trust owner isolation rules (`firestore.rules`).
- **UI**: Ice-White Glassmorphic 3D Material 3 theme (`Space Grotesk`, `Plus Jakarta Sans`, `JetBrains Mono`) with dark-mode toggle and low-end animation fallback.
- **Navigation**: `DashboardScreen` (Core), `AppsAndUpdateScreen` (Apps, Updates, Universal Dialogs), `IntegrationScreen` (SAF APK Inspector, DEX Releases, Hook Templates), `SecurityAndLicensesScreen` (Licenses, VIP, Devices), `BotsAndOpsScreen` (Telegram Multi-Bot Vault, Comms, 52 Admin Modules, VPS & Auth Settings).
