package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.TicketModalInfo;
import kr.starly.discordbot.entity.TicketType;

import java.util.HashMap;
import java.util.Map;

public class TicketModalService {

    private final Map<String, TicketModalInfo> ticketModalInfoMap = new HashMap<>();

    private static TicketModalService instance;

    private TicketModalService() {}

    public static TicketModalService getInstance() {
        if (instance == null) instance = new TicketModalService();
        return instance;
    }

    public void replaceModalInfo(String id, TicketModalInfo ticketModalInfo) {
        if (ticketModalInfoMap.containsKey(id))
            this.ticketModalInfoMap.replace(id, ticketModalInfo);
    }

    public void setTicketType(String id, TicketType type) {
        if (!ticketModalInfoMap.containsKey(id)) {
            TicketModalInfo ticketModalInfo = new TicketModalInfo(id, null, null);
            ticketModalInfo.setType(type);
            ticketModalInfoMap.put(id, ticketModalInfo);
        }
    }

    public TicketModalInfo getTicketModalInfo(String id) {
        if (ticketModalInfoMap.containsKey(id)) {
            return this.ticketModalInfoMap.get(id);
        }
        return null;
    }

    public void removeById(String id) {
        if (ticketModalInfoMap.containsKey(id))
            this.ticketModalInfoMap.remove(id);
    }
}