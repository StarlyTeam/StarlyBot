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

    @Nullable private Boolean isSuccess;
    @Getter @NotNull  private Date createdAt;
    @Getter @Nullable private Date usedAt;
    @Getter @NotNull private Date expireAt;

    public boolean isUsed() {
        return usedAt != null;
    }
    public Boolean isSuccess() {
        return isSuccess;
    }
    public boolean isExpired() {
        return new Date().after(expireAt);
    }

    public void updateIsSuccess(Boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public void updateUsedAt(Date usedAt) {
        this.usedAt = usedAt;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("token", token);
        document.put("pluginFile", pluginFile.serialize());
        document.put("userId", userId);
        document.put("isSuccess", isSuccess);
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
        Boolean isSuccess = document.getBoolean("isSuccess");
        Date createdAt = document.getDate("createdAt");
        Date usedAt = document.getDate("usedAt");
        Date expireAt = document.getDate("expireAt");

        return new Download(token, file, userId, isSuccess, createdAt, usedAt, expireAt);
    }
}