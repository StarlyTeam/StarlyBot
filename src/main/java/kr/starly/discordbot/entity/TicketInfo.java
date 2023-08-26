package kr.starly.discordbot.entity;

import kr.starly.discordbot.listener.ticket.TicketType;

public record TicketInfo(String openBy, String channelId, TicketType ticketType, String openAt, String closeAt) {}
