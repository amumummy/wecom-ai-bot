package com.devlab.wecombot.crypto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * 签名计算：对 token/timestamp/nonce/encrypt 四参数做字典序排序后拼接，再取 SHA-1
 */
public final class SHA1 {

    private SHA1() {
    }

    public static String getSignature(String token, String timestamp, String nonce, String encrypt)
            throws AesException {
        try {
            String[] array = new String[]{token, timestamp, nonce, encrypt};
            Arrays.sort(array);
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                sb.append(s);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new AesException(AesException.ComputeSignatureError);
        }
    }
}
