package com.pranav.integration.hook

import android.app.Activity
import com.pranav.sdk.PranavSdk

object PranavActivityHook {
    @JvmStatic
    fun onActivityCreated(activity: Activity, appCode: String, backendBaseUrl: String) {
        PranavSdk.initWithEntryPoint(
            activity.applicationContext,
            appCode,
            backendBaseUrl,
            "ACTIVITY_ON_CREATE"
        )
        PranavSdk.checkSessionAndRenderDialogs(activity)
    }
}
