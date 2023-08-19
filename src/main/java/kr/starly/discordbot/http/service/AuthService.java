package kr.starly.discordbot.http.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthService {

    private static AuthService instance;

    public static AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    private AuthService() {}

    private final Map<String, String> userTokens = new HashMap<>();
    private final Map<String, Instant> tokenCreationTimes = new HashMap<>();

    private static final long TOKEN_EXPIRATION_TIME_MINUTES = 30;

    public String generateToken(String userId) {
        String token = UUID.randomUUID().toString();
        userTokens.put(userId, token);

        tokenCreationTimes.put(userId, Instant.now());

        return token;
    }

    public boolean validateToken(String userId, String token) {
        String storedToken = userTokens.get(userId);

        if (storedToken == null || !storedToken.equals(token)) {
            return false;
        }

        Instant creationTime = tokenCreationTimes.get(userId);
        Instant expirationTime = creationTime.plusSeconds(TOKEN_EXPIRATION_TIME_MINUTES * 60);

        if (Instant.now().isAfter(expirationTime)) {
            userTokens.remove(userId);
            tokenCreationTimes.remove(userId);
            return false;
        }

        return true;
    }

    public void removeTokenForUser(String userId) {
        userTokens.remove(userId);
        tokenCreationTimes.remove(userId);
    }
}
