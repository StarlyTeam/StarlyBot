package kr.starly.discordbot.entity;

import java.util.Date;

public record User(long discordId, String ip, Date verifiedAt, int point) {}