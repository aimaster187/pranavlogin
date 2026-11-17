package com.pranav.sdk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;

/**
 * Single reusable Universal Dialog Engine inside the PRANAV DEX runtime.
 * Supports NORMAL_UPDATE, FORCE_UPDATE, LICENSE_ACTIVATION, LICENSE_EXPIRY,
 * VIP, DEVICE_APPROVAL, DEVICE_BLOCK, CHANNEL_VERIFICATION, MAINTENANCE,
 * ANNOUNCEMENT, SECURITY_WARNING, SUPPORT, and CUSTOM.
 */
public final class PranavDialogEngine {
    private PranavDialogEngine() {}

    public static void render(final Activity activity, final JSONObject dialogJson) {
        if (activity == null || activity.isFinishing() || dialogJson == null) return;
        if (!dialogJson.optBoolean("enabled", true)) return;

        final String type = dialogJson.optString("dialogType", "ANNOUNCEMENT");
        final boolean forceUpdate = "FORCE_UPDATE".equalsIgnoreCase(type)
                || dialogJson.optBoolean("forceUpdateEnabled", false);
        final boolean dismissible = !forceUpdate && dialogJson.optBoolean("dismissible", true);
        final boolean laterAllowed = !forceUpdate && dialogJson.optBoolean("laterAllowed", true);

        final String title = dialogJson.optString("title", "PRANAV Notice");
        final String description = dialogJson.optString("description", "");
        final String primaryBtn = dialogJson.optString("primaryButtonText", forceUpdate ? "Update Now" : "Continue");
        final String primaryUrl = dialogJson.optString("primaryActionUrl", "");
        final String secondaryBtn = dialogJson.optString("secondaryButtonText", "Later");

        activity.runOnUiThread(() -> {
            if (activity.isFinishing()) return;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(title)
                    .setMessage(description)
                    .setCancelable(dismissible)
                    .setPositiveButton(primaryBtn, (d, which) -> {
                        if (primaryUrl != null && !primaryUrl.isEmpty()) {
                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(primaryUrl));
                                activity.startActivity(intent);
                            } catch (Exception ignored) {}
                        }
                        if (!forceUpdate) {
                            d.dismiss();
                        }
                    });

            if (laterAllowed) {
                builder.setNegativeButton(secondaryBtn, (d, which) -> d.dismiss());
            }

            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(dismissible);
            dialog.show();
        });
    }
}
