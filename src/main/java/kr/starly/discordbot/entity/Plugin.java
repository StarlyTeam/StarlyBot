package kr.starly.discordbot.entity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record Plugin(
        @NotNull String ENName,
        @NotNull String KRName,
        @NotNull String emoji,
        @NotNull String wikiUrl,
        @NotNull String iconUrl,
        @Nullable String videoUrl,
        @NotNull String gifUrl,
        @NotNull List<String> dependency,
        @NotNull List<Long> manager,
        @Nullable Long buyerRole,
        @NotNull Long threadId,
        @NotNull String version,
        @NotNull int price
) {}