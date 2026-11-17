# PRANAV DEX Build & Release Pipeline

## Building `pranav-sdk-1.4.0.dex`
Requirements: JDK 11+, Android SDK (`android.jar` and `d8`).

```bash
bash PRANAV/dex/build-dex.sh
```

What `build-dex.sh` does:
1. Compiles all Java sources under `PRANAV/dex/src/com/pranav/sdk/` with `javac --release 11`.
2. Converts bytecode into Dalvik executable format via `d8 --min-api 24`.
3. Outputs `PRANAV/dex/build/pranav-sdk-1.4.0.dex` and prints its real SHA-256 checksum.

## Release Tracking
Update `PRANAV/dex/dex-releases.json` and publish the matching release version + SHA-256 in **PRANAV Admin -> DEX & Hook -> DEX Releases** so host apps and diagnostics match expected checksums.
