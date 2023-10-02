package kr.starly.discordbot.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public enum TicketType {

    NORMAL_TICKET("일반"),
    QUESTION_TICKET("질문"),
    CONSULTING_TICKET("상담"),
    PURCHASE_INQUIRY_TICKET("구매"),
    USE_RESTRICTION_TICKET("이용제한"),
    BUG_REPORT_TICKET("버그"),
    BUG_REPORT_BUKKIT_TICKET("플러그인 버그"),
    BUG_REPORT_ETC_TICKET("기타 버그"),
    ETC_TICKET("기타");

    @Getter private final String name;
    @Getter private static final Map<Long, TicketType> userTicketStatusMap = new HashMap<>();
}