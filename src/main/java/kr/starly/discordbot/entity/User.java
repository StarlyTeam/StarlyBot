package kr.starly.discordbot.entity;

import kr.starly.discordbot.rank.entity.Rank;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.List;

public record User(
        @NotNull Long discordId,
        @NotNull String ip,
        @NotNull Date verifiedAt,
        @NotNull int point,
        @NotNull List<Rank> rank
) {}