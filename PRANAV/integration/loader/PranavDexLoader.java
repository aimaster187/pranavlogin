package com.pranav.integration.loader;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.security.MessageDigest;

/**
 * Verified secondary DEX loader with SHA-256 verification and clean rollback fallback.
 * Prefer static multi-DEX packaging (classes2.dex) whenever modifying an authorized APK.
 * Use this helper when loading a bundled or staged pranav-sdk DEX file with checksum validation.
 */
public final class PranavDexLoader {
    private static final String TAG = "PranavDexLoader";

    private PranavDexLoader() {}

    public static boolean loadVerifiedAssetDex(
            Context context,
            String assetDexName,
            String expectedSha256Hex,
            String appCode,
            String endpointUrl
    ) {
        if (context == null) return false;
        File codeCache = new File(context.getCodeCacheDir(), "pranav_dex");
        File optimizedDir = new File(codeCache, "opt");
        if (!codeCache.exists() && !codeCache.mkdirs()) {
            Log.e(TAG, "Unable to create codeCache/pranav_dex directory");
            return false;
        }
        if (!optimizedDir.exists() && !optimizedDir.mkdirs()) {
            Log.e(TAG, "Unable to create optimized DEX directory");
            return false;
        }

        File targetDex = new File(codeCache, assetDexName);
        File backupDex = new File(codeCache, assetDexName + ".bak");

        try {
            if (targetDex.exists()) {
                copyFile(targetDex, backupDex);
            }
            try (InputStream is = context.getAssets().open(assetDexName);
                 FileOutputStream fos = new FileOutputStream(targetDex)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = is.read(buf)) > 0) {
                    fos.write(buf, 0, r);
                }
            }
            targetDex.setReadOnly();

            String actualSha = sha256Hex(targetDex);
            if (expectedSha256Hex != null && !expectedSha256Hex.isEmpty()
                    && !expectedSha256Hex.equalsIgnoreCase(actualSha)) {
                Log.e(TAG, "SHA-256 mismatch for " + assetDexName + "; rolling back.");
                rollback(targetDex, backupDex);
                return false;
            }

            DexClassLoader loader = new DexClassLoader(
                    targetDex.getAbsolutePath(),
                    optimizedDir.getAbsolutePath(),
                    null,
                    context.getClassLoader()
            );
            Class<?> sdkClass = loader.loadClass("com.pranav.sdk.PranavSdk");
            Method initMethod = sdkClass.getMethod("init", Context.class, String.class, String.class);
            initMethod.invoke(null, context.getApplicationContext(), appCode, endpointUrl);
            if (backupDex.exists()) backupDex.delete();
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Verified DEX load failed; executing rollback", t);
            rollback(targetDex, backupDex);
            return false;
        }
    }

    private static void rollback(File targetDex, File backupDex) {
        try {
            if (targetDex.exists()) targetDex.delete();
            if (backupDex.exists()) {
                copyFile(backupDex, targetDex);
            }
        } catch (Throwable ignored) {}
    }

    private static void copyFile(File src, File dst) throws Exception {
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
        }
    }

    private static String sha256Hex(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[8192];
            int r;
            while ((r = fis.read(buf)) > 0) {
                md.update(buf, 0, r);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
