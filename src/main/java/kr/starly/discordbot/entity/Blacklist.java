package kr.starly.discordbot.entity;

import java.util.Date;

public record Blacklist(long userId, long moderatorId, String reason, Date listedAt) {}