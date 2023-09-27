package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.TicketStatus;

public record TicketInfo(long openBy, long closeBy, long channelId, TicketStatus ticketStatus) {}