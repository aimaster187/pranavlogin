package com.pranav.sampletarget;

import android.app.Application;
import com.pranav.integration.hook.PranavApplicationHook;

public class TargetApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PranavApplicationHook.attach(this, "sample_target", "https://api.pranavsmm.shop");
    }
}
