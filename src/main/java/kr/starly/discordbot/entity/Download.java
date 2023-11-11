package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@Getter
@AllArgsConstructor
public class Download {

    @NotNull private String token;
    @NotNull private PluginFile file;
    @NotNull private Long userId;
    @Nullable private String userIp;

    @NotNull private Boolean isUsed;
    @Nullable private Boolean isSuccess;
    @NotNull private Boolean isExpired;
    @NotNull  private Date createdAt;
    @Nullable private Date usedAt;
    @NotNull private Date expireAt;

    public void updateIsUsed(Boolean isUsed) {
        this.isUsed = isUsed;
    }

    public void updateIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public void updateIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    public void updateUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("token", token);
        document.put("file", file.serialize());
        document.put("userId", userId);
        document.put("userIp", userIp);
        document.put("isUsed", isUsed);
        document.put("isSuccess", isSuccess);
        document.put("isExpired", isExpired);
        document.put("createdAt", createdAt);
        document.put("usedAt", usedAt);
        document.put("expireAt", expireAt);
        return document;
    }


    public static Download deserialize(Document document) {
        String token = document.getString("token");
        PluginFile file = PluginFile.deserialize((Document) document.get("file"));
        Long userId = document.getLong("userId");
        String userIp = document.getString("userIp");
        Boolean isUsed = document.getBoolean("isUsed");
        Boolean isSuccess = document.getBoolean("isSuccess");
        Boolean isExpired = document.getBoolean("isExpired");
        Date createdAt = document.getDate("createdAt");
        Date usedAt = document.getDate("usedAt");
        Date expireAt = document.getDate("expireAt");
        return new Download(token, file, userId, userIp, isUsed, isSuccess, isExpired, createdAt, usedAt, expireAt);
    }
}