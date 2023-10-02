package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Ticket;

public interface TicketRepository {

    void save(Ticket ticketInfo);
    void updateRate(long channelId, byte rate);
    Ticket findByChannel(long channelId);
    Ticket findByDiscordId(long discordId);
    long getLastIndex();
    boolean isNotValidUser(long discordId);
    byte getAverageRate();
}