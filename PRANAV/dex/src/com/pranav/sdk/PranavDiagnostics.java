package com.pranav.sdk;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import org.json.JSONObject;

public final class PranavDiagnostics {
    private PranavDiagnostics() {}

    public static JSONObject collect(Context context, String appCode) {
        JSONObject obj = new JSONObject();
        try {
            Context appCtx = context.getApplicationContext();
            boolean hasInternet = appCtx.checkCallingOrSelfPermission(android.Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED;
            obj.put("sdkVersion", "1.4.0");
            obj.put("sdkVersionCode", 140);
            obj.put("packageName", appCtx.getPackageName());
            obj.put("appCode", appCode);
            obj.put("androidSdkInt", Build.VERSION.SDK_INT);
            obj.put("hasInternetPermission", hasInternet);
            obj.put("initialized", PranavInitGuard.isInitialized());
            obj.put("initEntryPoint", PranavInitGuard.getInitEntryPoint());
            obj.put("installUuid", PranavDeviceIdentity.getOrCreateInstallUuid(appCtx));
            try {
                obj.put("keystoreKeyHash", PranavDeviceIdentity.getPublicKeySha256Hex());
                obj.put("keystoreReady", true);
            } catch (Exception e) {
                obj.put("keystoreReady", false);
                obj.put("keystoreError", e.getMessage());
            }
        } catch (Exception ignored) {
        }
        return obj;
    }
}
