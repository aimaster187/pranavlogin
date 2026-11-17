# Activity Lifecycle Hook Guide

## When to Use
Use `PranavActivityHook.onActivityCreated(...)` when the target APK uses the stock `android.app.Application` without a custom Application class, or when you want immediate dialog rendering inside the main launcher `Activity`.

## Java Example
```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    com.pranav.integration.hook.PranavActivityHook.onActivityCreated(
        this,
        "my_app_code",
        "https://api.pranavsmm.shop"
    );
}
```

## Duplicate-Init & Window Safety
- `PranavActivityHook` initializes `PranavSdk` safely once (`PranavInitGuard`) and dispatches UI dialogs only after checking `activity.isFinishing()` / `activity.isDestroyed()`.
