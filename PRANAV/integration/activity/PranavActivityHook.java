package com.pranav.integration.hook;

import android.app.Activity;
import com.pranav.sdk.PranavSdk;

/**
 * Separate Activity Lifecycle Hook for host apps without a custom Application class
 * or where UI dialog rendering is bound directly to the launcher Activity.
 */
public final class PranavActivityHook {
    private PranavActivityHook() {}

    public static void onActivityCreated(Activity activity, String appCode, String backendBaseUrl) {
        if (activity == null) return;
        PranavSdk.initWithEntryPoint(
                activity.getApplicationContext(),
                appCode,
                backendBaseUrl,
                "ACTIVITY_ON_CREATE"
        );
        PranavSdk.checkSessionAndRenderDialogs(activity);
    }
}
