package com.pranav.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * PRANAV Universal DEX SDK Entrypoint (v1.4.0).
 * Never runs automatically on DEX copy; must be initialized via a separate Hook.
 */
public final class PranavSdk {
    private static final String TAG = "PranavSdk";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static volatile String configuredBaseUrl = "";
    private static volatile String configuredAppCode = "";
    private static volatile JSONObject lastHandshake = null;

    private PranavSdk() {}

    public static void init(Context context, String appCode, String baseUrl) {
        initWithEntryPoint(context, appCode, baseUrl, "DIRECT_INIT");
    }

    public static void initWithEntryPoint(Context context, String appCode, String baseUrl, String entryPoint) {
        if (context == null) return;
        if (!PranavInitGuard.tryAcquire(appCode, entryPoint)) {
            Log.w(TAG, "Duplicate PRANAV init ignored (already initialized at "
                    + PranavInitGuard.getInitEntryPoint() + ")");
            return;
        }
        configuredAppCode = appCode == null ? "" : appCode;
        configuredBaseUrl = baseUrl == null ? "" : baseUrl;

        final Context appCtx = context.getApplicationContext();
        EXECUTOR.execute(() -> performHandshake(appCtx));
    }

    private static void performHandshake(Context appCtx) {
        try {
            if (configuredBaseUrl.isEmpty()) return;
            PranavApiClient client = new PranavApiClient(configuredBaseUrl);
            String uuid = PranavDeviceIdentity.getOrCreateInstallUuid(appCtx);
            String keyHash = PranavDeviceIdentity.getPublicKeySha256Hex();
            String spki = PranavDeviceIdentity.getPublicKeySpkiBase64();

            PackageInfo pkg = appCtx.getPackageManager().getPackageInfo(appCtx.getPackageName(), 0);
            JSONObject challengeReq = new JSONObject();
            challengeReq.put("appCode", configuredAppCode);
            challengeReq.put("packageName", appCtx.getPackageName());
            challengeReq.put("deviceId", uuid);
            challengeReq.put("installUuid", uuid);

            JSONObject challengeRes = client.postJson("/api/v1/client/challenge", challengeReq);
            String nonce = challengeRes.optString("nonce", challengeRes.optString("challenge", ""));
            if (!nonce.isEmpty()) {
                String signature = PranavDeviceIdentity.signChallenge(nonce);
                JSONObject sessionReq = new JSONObject();
                sessionReq.put("appCode", configuredAppCode);
                sessionReq.put("packageName", appCtx.getPackageName());
                sessionReq.put("deviceId", uuid);
                sessionReq.put("installUuid", uuid);
                sessionReq.put("deviceModel", PranavDeviceIdentity.getDeviceLabel());
                sessionReq.put("androidApi", Build.VERSION.SDK_INT);
                sessionReq.put("keystoreHash", keyHash);
                sessionReq.put("keystorePublicKeyB64", spki);
                sessionReq.put("challengeNonce", nonce);
                sessionReq.put("challengeSignatureB64", signature);
                sessionReq.put("versionName", pkg.versionName != null ? pkg.versionName : "1.0.0");
                lastHandshake = client.postJson("/api/v1/client/session", sessionReq);
            }
        } catch (Exception e) {
            Log.e(TAG, "PRANAV handshake failed: " + e.getMessage());
        }
    }

    public static void confirmUpdateInstalled(final Context context, final String installedVersion) {
        if (context == null || configuredBaseUrl.isEmpty()) return;
        final Context appCtx = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                PranavApiClient client = new PranavApiClient(configuredBaseUrl);
                String uuid = PranavDeviceIdentity.getOrCreateInstallUuid(appCtx);
                JSONObject body = new JSONObject();
                body.put("appCode", configuredAppCode);
                body.put("deviceId", uuid);
                body.put("installedVersion", installedVersion);
                client.postJson("/api/v1/client/update-confirm", body);
            } catch (Exception e) {
                Log.e(TAG, "Update confirmation failed: " + e.getMessage());
            }
        });
    }

    public static void checkSessionAndRenderDialogs(final Activity activity) {
        if (activity == null || lastHandshake == null) return;
        JSONObject updateDialog = lastHandshake.optJSONObject("updateDialog");
        if (updateDialog != null) {
            PranavDialogEngine.render(activity, updateDialog);
            return;
        }
        JSONArray dialogs = lastHandshake.optJSONArray("dialogs");
        if (dialogs != null && dialogs.length() > 0) {
            PranavDialogEngine.render(activity, dialogs.optJSONObject(0));
        }
    }

    public static JSONObject getLastHandshake() {
        return lastHandshake;
    }

    public static JSONObject getDiagnostics(Context context) {
        return PranavDiagnostics.collect(context, configuredAppCode);
    }
}
