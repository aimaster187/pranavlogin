package com.pranav.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.UUID;

/**
 * Privacy-preserving device identity using random installation UUID and
 * Android Keystore EC P-256 signed challenge-response. Never accesses IMEI or MAC.
 */
public final class PranavDeviceIdentity {
    private static final String PREFS_NAME = "pranav_sdk_identity";
    private static final String KEY_INSTALL_UUID = "install_uuid";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final String KEY_ALIAS = "pranav_device_ec_v1";

    private PranavDeviceIdentity() {}

    public static synchronized String getOrCreateInstallUuid(Context context) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String existing = prefs.getString(KEY_INSTALL_UUID, null);
        if (existing != null && existing.length() >= 8) {
            return existing;
        }
        String generated = UUID.randomUUID().toString();
        prefs.edit().putString(KEY_INSTALL_UUID, generated).apply();
        return generated;
    }

    public static synchronized PublicKey ensureKeystoreKeyPair() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            return keyStore.getCertificate(KEY_ALIAS).getPublicKey();
        }
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                KEYSTORE_PROVIDER
        );
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY
        )
                .setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1"))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build();
        kpg.initialize(spec);
        return kpg.generateKeyPair().getPublic();
    }

    public static String getPublicKeySpkiBase64() throws Exception {
        PublicKey pk = ensureKeystoreKeyPair();
        return Base64.encodeToString(pk.getEncoded(), Base64.NO_WRAP);
    }

    public static String getPublicKeySha256Hex() throws Exception {
        PublicKey pk = ensureKeystoreKeyPair();
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(pk.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String signChallenge(String challengeNonce) throws Exception {
        ensureKeystoreKeyPair();
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
        keyStore.load(null);
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(challengeNonce.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP);
    }

    public static String getDeviceLabel() {
        return Build.MANUFACTURER + " " + Build.MODEL + " (API " + Build.VERSION.SDK_INT + ")";
    }
}
