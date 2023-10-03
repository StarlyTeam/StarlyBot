package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public record User(
        @NotNull Long discordId,
        @NotNull String ip,
        @NotNull Date verifiedAt,
        @NotNull int point
) {}