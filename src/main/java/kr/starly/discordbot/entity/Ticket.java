package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.TicketType;
import org.jetbrains.annotations.NotNull;

public record Ticket(
        @NotNull long openBy,
        @NotNull long closedBy,
        @NotNull long channelId,
        @NotNull TicketType ticketType,
        @NotNull long index
) {}