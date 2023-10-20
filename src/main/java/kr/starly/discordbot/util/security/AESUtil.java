package kr.starly.discordbot.util.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AESUtil {

    private AESUtil() {
    }

    public static String encode(String plainText, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
        IvParameterSpec IV = new IvParameterSpec(key.substring(0, 16).getBytes());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IV);

        byte[] plainBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return new String(plainBytes);
    }


    public static String decode(String encodedText, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        IvParameterSpec IV = new IvParameterSpec(key.substring(0, 16).getBytes());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IV);

        byte[] encodedBytes = encodedText.getBytes();
        return new String(cipher.doFinal(encodedBytes), StandardCharsets.UTF_8);
    }
}