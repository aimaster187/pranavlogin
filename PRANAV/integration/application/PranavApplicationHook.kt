package com.pranav.integration.hook

import android.app.Application
import com.pranav.sdk.PranavSdk

object PranavApplicationHook {
    @JvmStatic
    fun attach(application: Application, appCode: String, backendBaseUrl: String) {
        PranavSdk.initWithEntryPoint(
            application.applicationContext,
            appCode,
            backendBaseUrl,
            "APPLICATION_ON_CREATE"
        )
    }
}
