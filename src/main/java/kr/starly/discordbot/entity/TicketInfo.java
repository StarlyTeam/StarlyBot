package kr.starly.discordbot.entity;

public record TicketInfo(String openBy, String channelId, TicketType ticketType, String openAt, String closeAt) {}
