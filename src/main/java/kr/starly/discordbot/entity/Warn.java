package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public record Warn(
        @NotNull long discordId,
        @NotNull long manager,
        @NotNull String reason,
        @NotNull int amount,
        @NotNull Date date
) {}