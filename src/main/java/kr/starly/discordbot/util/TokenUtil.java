package kr.starly.discordbot.util;

import java.security.SecureRandom;
import java.util.Base64;

public class TokenUtil {

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public static byte[] generateBytes() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return randomBytes;
    }

    public static String generateToken() {
        return base64Encoder.encodeToString(generateBytes());
    }
}