package com.devlab.wecombot.crypto;

import java.util.Arrays;

/**
 * PKCS7 填补：企微按 32 字节块填补
 */
public final class PKCS7Encoder {

    private static final int BLOCK_SIZE = 32;

    private PKCS7Encoder() {
    }

    /**
     * 标准PKCS#7补位：填充 amountToPad 个值为 amountToPad 的字节（与企微服务端实现对称）
     */
    public static byte[] encode(int count) {
        int amountToPad = BLOCK_SIZE - (count % BLOCK_SIZE);
        if (amountToPad == 0) {
            amountToPad = BLOCK_SIZE;
        }
        byte[] pad = new byte[amountToPad];
        Arrays.fill(pad, (byte) amountToPad);
        return pad;
    }

    /** 删除解密后明文的 PKCS7 补位 */
    public static byte[] decode(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1];
        if (pad < 1 || pad > BLOCK_SIZE) {
            pad = 0;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }
}
