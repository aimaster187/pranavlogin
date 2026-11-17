# AndroidManifest.xml Configuration Guide

See `/PRANAV/integration/manifest/AndroidManifest_snippet.xml` for the drop-in snippet.

## Required Elements
1. **Permissions**:
   - `<uses-permission android:name="android.permission.INTERNET" />`
   - `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
   No storage permissions (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE`) or device identifiers (`READ_PHONE_STATE`) are needed or allowed.
2. **Cleartext Traffic**: Keep `android:usesCleartextTraffic="false"` in production (`https://api.pranavsmm.shop`).
3. **Application Name**: Preserve any existing `android:name` on `<application>` and hook its `onCreate()`. Do NOT blindly overwrite an existing custom Application class with a wrapper or you will break target app dependencies (Dagger/Hilt/Firebase/etc.).
