package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.TicketInfo;


public interface TicketRepository {

    void save(TicketInfo ticketInfo);
    TicketInfo findTicketInfoById(String channelId);
    void updateCloseAtDate(String channelId, String date);
}