package com.pranav.integration.hook;

import android.app.Application;
import android.content.Context;
import com.pranav.sdk.PranavSdk;

/**
 * Early attachBaseContext Hook for MultiDex / custom ContextWrapper host applications.
 */
public final class PranavAttachBaseContextHook {
    private PranavAttachBaseContextHook() {}

    public static void attachEarly(Application application, Context base, String appCode, String backendBaseUrl) {
        Context target = base != null ? base : (application != null ? application.getApplicationContext() : null);
        if (target == null) return;
        PranavSdk.initWithEntryPoint(target, appCode, backendBaseUrl, "ATTACH_BASE_CONTEXT");
    }
}
