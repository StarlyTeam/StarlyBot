package kr.starly.discordbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum TicketType {

    GENERAL("일반", "📰"),
    PAYMENT("구매", "💳"),
    PUNISHMENT("처벌", "📎");

    @Getter private static final Map<Long, TicketType> userTicketTypeMap = new HashMap<>();

    @Getter private final String name;
    @Getter private final String icon;
}