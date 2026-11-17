# Integration Troubleshooting & Rollback Guide

## Common Issues & Fixes
1. **`ClassNotFoundException: com.pranav.sdk.PranavSdk`**:
   - Ensure `pranav-sdk-1.4.0.dex` was added with no gap in `classesN.dex` numbering (e.g., `classes.dex` -> `classes2.dex`, not `classes3.dex` while `classes2.dex` is missing).
2. **`VerifyError` on target `onCreate()` after Smali edit**:
   - Check `.locals` register count; increment by `2` if `v0`/`v1` exceed existing local count, and ensure `invoke-super` remains BEFORE PRANAV hook calls.
3. **`INSTALL_PARSE_FAILED_NO_CERTIFICATES` on install**:
   - Run `zipalign -f -p 4` BEFORE `apksigner sign`, and verify with `apksigner verify --verbose`.
4. **Clean Rollback Procedure**:
   - Remove the single `invoke-static` Hook line from the host `Application`/`Activity` Smali (or delete `classes2.dex` if unused) and re-sign with your backup keystore.
