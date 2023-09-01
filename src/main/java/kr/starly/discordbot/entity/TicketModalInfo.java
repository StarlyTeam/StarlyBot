package kr.starly.discordbot.entity;

import lombok.Getter;
import lombok.Setter;

public class TicketModalInfo {

    @Getter
    private String userId;

    @Getter
    private String title;

    @Getter
    private String message;

    @Getter
    @Setter
    private TicketType type;

    public TicketModalInfo(String userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
    }
}
