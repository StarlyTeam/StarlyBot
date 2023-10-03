package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public record Blacklist(
        @NotNull Long userId,
        @NotNull Long moderatorId,
        @NotNull String reason,
        @NotNull Date listedAt
) {}