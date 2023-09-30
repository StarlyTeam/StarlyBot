package kr.starly.discordbot.entity;

import java.util.Date;

public record WarnInfo(long discordId, long manager, String reason, int warn, Date date) { }
