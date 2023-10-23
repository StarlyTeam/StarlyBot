package kr.starly.discordbot.util.security;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class AESUtil {

    private AESUtil() {}

    public static String encode(String plainText, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec IV = new IvParameterSpec(new String(key.getBytes(), 0, 15).getBytes());

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IV);

            byte[] plainBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(plainBytes);
        } catch (Exception ex) {
            ex.printStackTrace();

            return "{MAL}{ENCODING_ERROR}" + plainText;
        }
    }

    public static String decode(String encodedText, String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            IvParameterSpec IV = new IvParameterSpec(new String(key.getBytes(), 0, 15).getBytes());

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IV);

            byte[] encodedBytes = Base64.getDecoder().decode(encodedText);
            return new String(cipher.doFinal(encodedBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            ex.printStackTrace();
            return "{MAL}{DECODING_ERROR}" + encodedText;
        }
    }
}