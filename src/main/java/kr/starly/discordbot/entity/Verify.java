package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@Getter
@AllArgsConstructor
public class Verify {

    @NotNull private final String token;
    @NotNull private final long userId;
    @NotNull private final String userIp;

    @NotNull private final boolean isDMSent;
    @NotNull private final Date verifiedAt;

    public Document serialize() {
        Document document = new Document();
        document.put("token", token);
        document.put("userId", userId);
        document.put("userIp", userIp);
        document.put("isDMSent", isDMSent);
        document.put("verifiedAt", verifiedAt);

        return document;
    }


    public static Verify deserialize(Document document) {
        String token = document.getString("token");
        long userId = document.getLong("userId");
        String userIp = document.getString("userIp");
        boolean isDMSent = document.getBoolean("isDMSent");
        Date verifiedAt = document.getDate("verifiedAt");

        return new Verify(token, userId, userIp, isDMSent, verifiedAt);
    }
}