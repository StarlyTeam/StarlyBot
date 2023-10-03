package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;

public record ShortenLink(
        @NotNull String originUrl,
        @NotNull String shortenUrl
) {}