package kr.starly.discordbot.entity;

import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record Plugin(
        @NotNull String ENName,
        @NotNull String KRName,
        @NotNull UnicodeEmoji emoji,
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