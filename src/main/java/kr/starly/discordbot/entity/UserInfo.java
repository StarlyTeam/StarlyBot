package kr.starly.discordbot.entity;

import java.time.LocalDateTime;

public record UserInfo(String discordId, String ip, LocalDateTime verifyDate, int point) {}

