# App Compatibility Guide

## Multi-DEX Naming Rules
- Target APKs on Android 5+ support multiple `classes*.dex` files natively (`classes.dex`, `classes2.dex`, `classes3.dex`, ...).
- Always place `pranav-sdk-1.4.0.dex` at the next sequential integer (`classes${N+1}.dex`) with NO gaps in numbering. Skipping numbers causes older Android ART/Dalvik loaders to stop scanning.

## Split APKs / App Bundles
- If an authorized target app is distributed as split APKs (`base.apk`, `split_config.arm64_v8a.apk`, ...), apply DEX + Hook changes strictly inside `base.apk` and re-sign **all** split APKs using the identical owner signing key (`apksigner`).

## `minSdk` & Architecture Compatibility
- PRANAV DEX targets `minSdk = 24` (`--min-api 24` via `d8`), Java 11 bytecode, and uses pure Android framework + Keystore APIs with zero native `.so` JNI binaries (100% ABI-independent across `arm64-v8a`, `armeabi-v7a`, `x86_64`).
