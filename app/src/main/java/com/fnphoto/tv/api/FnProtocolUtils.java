package com.fnphoto.tv.api;

import android.util.Base64;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class FnProtocolUtils {
    private static int index = 1;
    private static String backId = "0000000000000000";

    public static synchronized String generateReqId() {
        String t = String.format("%08x", System.currentTimeMillis() / 1000);
        String e = String.format("%04x", index++);
        // Format: t(8) + zeros(16) + e(4) = 28 hex chars
        // For initial requests, use zeros for backId
        return t + "0000000000000000" + e;
    }

    public static synchronized String generateReqIdWithBackId(String sessionBackId) {
        String t = String.format("%08x", System.currentTimeMillis() / 1000);
        String e = String.format("%04x", index++);
        // After login, use the backId from login response
        if (sessionBackId != null && sessionBackId.length() == 16) {
            return t + sessionBackId + e;
        }
        return t + "0000000000000000" + e;
    }

    public static void setBackId(String newBackId) {
        if (newBackId != null && newBackId.length() == 16) {
            backId = newBackId;
        }
    }

    public static String getBackId() {
        return backId;
    }

    /**
     * AES Encryption - Key must be 32 characters as per fnOS spec
     */
    public static String aesEncrypt(String data, String key, byte[] iv) throws Exception {
        // key must be exactly 32 chars (256 bits for AES-256)
        if (key.length() != 32) {
            throw new IllegalArgumentException("Key must be 32 characters, got: " + key.length());
        }
        
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    /**
     * RSA Encryption using PKCS1_v1_5 padding
     */
    public static String rsaEncrypt(String publicKeyStr, String plaintext) throws Exception {
        String cleanKey = publicKeyStr
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "")
                .trim();
        
        byte[] keyBytes = Base64.decode(cleanKey, Base64.DEFAULT);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes("UTF-8"));
        return Base64.encodeToString(encrypted, Base64.NO_WRAP);
    }

    /**
     * Generate random string with exact length
     * Used for AES key (must be 32 chars) and IV generation
     */
    public static String generateRandomString(int length) {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generate random IV (16 bytes)
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * Convert JSON to compact format (no spaces)
     * Equivalent to Python: json.dumps(data, separators=(',', ':'))
     */
    public static String toCompactJson(org.json.JSONObject json) {
        return json.toString(); // JSONObject.toString() produces compact format by default
    }
}
