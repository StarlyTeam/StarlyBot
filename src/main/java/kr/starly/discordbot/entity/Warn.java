package kr.starly.discordbot.entity;

import java.util.Date;

public record Warn(long discordId, long manager, String reason, int amount, Date date) {}