package kr.starly.discordbot.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum UploadStatus {

    NONE,
    GIF_UPLOADED,
    FILE_UPLOADED;

    @Getter
    private static final Map<Long, UploadStatus> userUploadStatus = new HashMap<>();
}