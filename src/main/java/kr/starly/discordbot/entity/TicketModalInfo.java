package kr.starly.discordbot.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
public class TicketModalInfo {

    private String userId;

    private String title;

    private String message;

    @Setter
    private TicketType type;

    public TicketModalInfo(String userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
    }
}
