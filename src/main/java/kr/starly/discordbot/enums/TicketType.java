package kr.starly.discordbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum TicketType {
    GENERAL("일반"),
    QUESTION("질문"),
    CONSULTING("상담"),
    PAYMENT("구매"),
    PUNISHMENT("처벌"),
    ERROR("버그"),
    PLUGIN_ERROR("플러그인 버그"),
    OTHER_ERROR("기타 버그"),
    OTHER("기타");

    @Getter private static final Map<Long, TicketType> userTicketTypeMap = new HashMap<>();

    @Getter private final String name;
}