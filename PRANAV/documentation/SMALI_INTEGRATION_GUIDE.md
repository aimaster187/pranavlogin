# Smali Integration Guide

## Exact Templates
Ready-to-insert templates are maintained under `/PRANAV/integration/smali/`:
- `Application_onCreate_hook.smali`
- `Activity_onCreate_hook.smali`
- `Application_attachBaseContext_hook.smali`

## Register Safety Checklist
1. Inspect `.registers` or `.locals` at the top of the target `onCreate` method.
2. Notice the difference: `.locals N` declares local registers *excluding* parameter registers (`p0`, `p1`), whereas `.registers M` counts both locals and parameters.
3. Our templates use `v0`, `v1`, and `p0` (plus `p1` for `attachBaseContext`). Make sure `.locals` is at least `2` (or place the hook right before modifying live locals that reuse `v0`/`v1`).
4. Verify return types (`V` for `attach`, `attachEarly`, `onActivityCreated`, and `init`).
5. Never place an `invoke-static` before `invoke-super` inside lifecycle callbacks.
