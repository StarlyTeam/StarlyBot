package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@AllArgsConstructor
public class Download {

    @Getter @NotNull private String token;
    @Getter @NotNull private PluginFile pluginFile;
    @Getter @NotNull private Long userId;
    @Getter @Nullable private String userIp;

    @NotNull private Boolean isUsed;
    @Nullable private Boolean isSuccess;
    @NotNull private Boolean isExpired;
    @Getter @NotNull  private Date createdAt;
    @Getter @Nullable private Date usedAt;
    @Getter @NotNull private Date expireAt;

    public Boolean isUsed() {
        return isUsed;
    }
    public Boolean isSuccess() {
        return isSuccess;
    }
    public Boolean isExpired() {
        return new Date().after(expireAt);
    }

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
        document.put("pluginFile", pluginFile.serialize());
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
        if (document == null) return null;

        String token = document.getString("token");
        PluginFile file = PluginFile.deserialize(document.get("pluginFile", Document.class));
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