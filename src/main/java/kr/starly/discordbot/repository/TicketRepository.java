package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.TicketInfo;

public interface TicketRepository {

    void save(TicketInfo ticketInfo);
    void updateRate(long channelId, byte rate);
    TicketInfo findByChannel(long channelId);
    TicketInfo findByDiscordId(long discordId);
    long getLastIndex();
    boolean isNotValidUser(long discordId);
    byte getAverageRate();
}