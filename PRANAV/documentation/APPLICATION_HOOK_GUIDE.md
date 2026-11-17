# Application Lifecycle Hook Guide

## When to Use
Use `PranavApplicationHook` (`Application.onCreate()`) when the target APK defines a custom `<application android:name="...">` class, or `PranavAttachBaseContextHook` (`Application.attachBaseContext(Context)`) for MultiDex initialization.

## Java Example
```java
@Override
public void onCreate() {
    super.onCreate();
    com.pranav.integration.hook.PranavApplicationHook.attach(
        this,
        "my_app_code",
        "https://api.pranavsmm.shop"
    );
}
```

## Kotlin Example
```kotlin
override fun onCreate() {
    super.onCreate()
    PranavApplicationHook.attach(this, "my_app_code", "https://api.pranavsmm.shop")
}
```

## Notes
- Always call `super.onCreate()` or `super.attachBaseContext(base)` BEFORE calling PRANAV's hook.
- Never trigger dialog UI inside `Application.onCreate()` directly; pass an `Activity` later via `PranavSdk.checkSessionAndRenderDialogs(activity)`.
