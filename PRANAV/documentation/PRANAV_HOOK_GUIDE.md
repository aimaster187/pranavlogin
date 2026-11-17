# PRANAV Hook Architecture & Selection Guide

> **Core Invariant**: Copying `pranav-sdk-1.4.0.dex` into an APK never initializes the SDK automatically. An explicit Hook MUST invoke `PranavSdk.init(...)` / `PranavSdk.initWithEntryPoint(...)` once.

## Choosing the Correct Lifecycle Hook Point

| Target APK Characteristics | Recommended Hook | Template File |
|---|---|---|
| Defines custom `<application android:name="...">` | `Application.onCreate()` | `PRANAV/integration/application/PranavApplicationHook.{java,kt}` |
| MultiDex or crowded `Application.onCreate()` | `Application.attachBaseContext()` | `PRANAV/integration/attach/PranavAttachBaseContextHook.java` |
| Uses default `android.app.Application` and you prefer minimal Manifest edits | Launch `Activity.onCreate()` | `PRANAV/integration/activity/PranavActivityHook.{java,kt}` |
| Deferred / verified asset DEX loading | `PranavDexLoader.loadVerifiedAssetDex()` | `PRANAV/integration/loader/PranavDexLoader.java` |

## Duplicate-Initialization Prevention
`PranavInitGuard` uses an `AtomicBoolean` and stores the first initialized entry point string (`APPLICATION_ON_CREATE`, `ACTIVITY_ON_CREATE`, or `ATTACH_BASE_CONTEXT`). Subsequent hook invocations in the same process safely no-op and record a diagnostic counter instead of re-initializing Keystore keys or repeating network handshakes.
