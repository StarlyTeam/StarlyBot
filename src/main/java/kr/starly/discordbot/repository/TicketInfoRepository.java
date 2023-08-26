package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.TicketInfo;


public interface TicketInfoRepository {

    void save(TicketInfo ticketInfo);
    TicketInfo findTicketInfoById(String userId);
    void updateCloseAtDate(String channelId, String date);
}