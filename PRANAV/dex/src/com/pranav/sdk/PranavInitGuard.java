package com.pranav.sdk;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Prevents duplicate initialization across Application.onCreate(), attachBaseContext(),
 * or multiple Activity.onCreate() calls inside the same host process.
 */
public final class PranavInitGuard {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static volatile String activeAppCode = "";
    private static volatile String initEntryPoint = "UNINITIALIZED";
    private static volatile long initTimestampMs = 0L;

    private PranavInitGuard() {}

    public static boolean tryAcquire(String appCode, String entryPoint) {
        if (INITIALIZED.compareAndSet(false, true)) {
            activeAppCode = appCode == null ? "" : appCode;
            initEntryPoint = entryPoint == null ? "UNKNOWN" : entryPoint;
            initTimestampMs = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    public static String getActiveAppCode() {
        return activeAppCode;
    }

    public static String getInitEntryPoint() {
        return initEntryPoint;
    }

    public static long getInitTimestampMs() {
        return initTimestampMs;
    }

    public static void resetForRollback() {
        INITIALIZED.set(false);
        activeAppCode = "";
        initEntryPoint = "ROLLED_BACK";
        initTimestampMs = 0L;
    }
}
