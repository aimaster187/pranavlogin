import crypto from "node:crypto";

/**
 * Derives a 32-byte AES-256 key from VAULT_MASTER_KEY env variable.
 * Refuses empty master keys in production.
 */
function deriveAesKey(masterSecret) {
  if (!masterSecret || masterSecret.length < 16) {
    throw new Error("VAULT_MASTER_KEY must be configured (>= 16 characters) before encrypting bot tokens.");
  }
  return crypto.createHash("sha256").update(masterSecret, "utf8").digest();
}

export function encryptSecret(plaintext, masterSecret) {
  const key = deriveAesKey(masterSecret);
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv("aes-256-gcm", key, iv);
  const encrypted = Buffer.concat([cipher.update(plaintext, "utf8"), cipher.final()]);
  const tag = cipher.getAuthTag();
  return `${iv.toString("base64")}:${tag.toString("base64")}:${encrypted.toString("base64")}`;
}

export function decryptSecret(envelope, masterSecret) {
  const key = deriveAesKey(masterSecret);
  const parts = String(envelope || "").split(":");
  if (parts.length !== 3) {
    throw new Error("Corrupt encrypted vault payload.");
  }
  const [ivB64, tagB64, dataB64] = parts;
  const decipher = crypto.createDecipheriv("aes-256-gcm", key, Buffer.from(ivB64, "base64"));
  decipher.setAuthTag(Buffer.from(tagB64, "base64"));
  const decrypted = Buffer.concat([
    decipher.update(Buffer.from(dataB64, "base64")),
    decipher.final()
  ]);
  return decrypted.toString("utf8");
}

export function safeEqual(a, b) {
  const bufA = Buffer.from(String(a || ""), "utf8");
  const bufB = Buffer.from(String(b || ""), "utf8");
  if (bufA.length === 0 || bufA.length !== bufB.length) return false;
  return crypto.timingSafeEqual(bufA, bufB);
}

/**
 * Verifies SHA256withECDSA signature produced by Android Keystore EC P-256 keypair.
 */
export function verifyDeviceChallengeSignature(publicKeyB64, challengeNonce, signatureB64) {
  try {
    const spkiDer = Buffer.from(publicKeyB64, "base64");
    const pubKey = crypto.createPublicKey({ key: spkiDer, format: "der", type: "spki" });
    const sigBuf = Buffer.from(signatureB64, "base64");
    return crypto.verify("sha256", Buffer.from(challengeNonce, "utf8"), pubKey, sigBuf);
  } catch {
    return false;
  }
}

export function sha256Hex(input) {
  return crypto.createHash("sha256").update(input).digest("hex");
}

export function redactSensitiveText(text) {
  return String(text || "").replace(/\b\d{6,12}:[A-Za-z0-9_-]{20,}\b/g, "[REDACTED_BOT_TOKEN]");
}
