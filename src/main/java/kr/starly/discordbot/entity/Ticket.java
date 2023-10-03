package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.TicketType;

public record Ticket(
        long openBy,
        long closedBy,
        long channelId,
        TicketType ticketStatus,
        long index
) {}