# Ecosystem Troubleshooting Guide

- **Google Sign-In returns cancelled / `NoCredentialException`**:
  - Verify a Google account is logged in on the Android device/emulator.
  - Ensure your debug/release keystore SHA-1 matches `certificate_hash` in `app/google-services.json`.
- **Backend health check fails in Dashboard**:
  - Confirm VPS systemd unit (`systemctl status pranav-backend`) and HTTPS certificate on `api.pranavsmm.shop`.
  - Confirm `backendBaseUrl` and `OWNER_API_KEY` are saved under **Bots & Ops -> VPS & Auth**.
- **Telegram bot fails `getMe` registration**:
  - Check BotFather token validity and outbound HTTPS access from VPS to `api.telegram.org`.
