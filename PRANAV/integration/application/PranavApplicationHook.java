package com.pranav.integration.hook;

import android.app.Application;
import com.pranav.sdk.PranavSdk;

/**
 * Separate Application Lifecycle Hook for host apps that define a custom Application class.
 * Attach inside Application.onCreate() after super.onCreate().
 */
public final class PranavApplicationHook {
    private PranavApplicationHook() {}

    public static void attach(Application application, String appCode, String backendBaseUrl) {
        if (application == null) return;
        PranavSdk.initWithEntryPoint(
                application.getApplicationContext(),
                appCode,
                backendBaseUrl,
                "APPLICATION_ON_CREATE"
        );
    }
}
