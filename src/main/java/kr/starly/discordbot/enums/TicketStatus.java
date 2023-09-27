package kr.starly.discordbot.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public enum TicketStatus {

    NORMAL_TICKET,
    QUESTION_TICKET,
    CONSULTING_TICKET,
    PURCHASE_INQUIRY_TICKET,
    USE_RESTRICTION_TICKET,
    BUG_REPORT_TICKET,
    BUG_REPORT_BUKKIT_TICKET,
    BUG_REPORT_ETC_TICKET,
    ETC_TICKET;

    @Getter
    private static final Map<Long, TicketStatus> userTicketStatusMap = new HashMap<>();
}