package com.devlab.wecombot.crypto;

/**
 * 企微加解密异常（错误码与微信官方示例保持一致）
 */
public class AesException extends Exception {

    public static final int OK = 0;
    /** 签名验证错误 */
    public static final int ValidateSignatureError = -40001;
    /** xml解析失败 */
    public static final int ParseXmlError = -40002;
    /** sha加密生成签名失败 */
    public static final int ComputeSignatureError = -40003;
    /** AESKey 非法 */
    public static final int IllegalAesKey = -40004;
    /** corpid 校验错误 */
    public static final int ValidateCorpidError = -40005;
    /** AES 加密失败 */
    public static final int EncryptAESError = -40006;
    /** AES 解密失败 */
    public static final int DecryptAESError = -40007;
    /** 解密后得到的buffer非法 */
    public static final int IllegalBuffer = -40008;
    /** base64加密异常 */
    public static final int EncodeAESError = -40009;

    private final int code;

    public AesException(int code) {
        super(getMessage(code));
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    private static String getMessage(int code) {
        switch (code) {
            case ValidateSignatureError: return "签名验证错误(-40001)";
            case ParseXmlError:          return "xml解析失败(-40002)";
            case ComputeSignatureError:  return "sha加密生成签名失败(-40003)";
            case IllegalAesKey:          return "AESKey非法，必须为43位字符串(-40004)";
            case ValidateCorpidError:    return "corpid校验错误(-40005)";
            case EncryptAESError:        return "AES加密失败(-40006)";
            case DecryptAESError:        return "AES解密失败(-40007)";
            case IllegalBuffer:          return "解密后得到的buffer非法(-40008)";
            case EncodeAESError:         return "base64加密异常(-40009)";
            default: return "未知错误(" + code + ")";
        }
    }
}
