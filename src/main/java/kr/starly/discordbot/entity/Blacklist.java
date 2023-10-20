package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

public record Blacklist(
        @Nullable Long userId,
        @Nullable String ipAddress,
        @NotNull Long moderatorId,
        @NotNull String reason,
        @NotNull Date listedAt
) {}