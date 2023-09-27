package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.TicketInfo;

import java.util.List;

public interface TicketRepository {

    void save(TicketInfo ticketInfo);

    void updateRate(long channelId, byte rate);

    TicketInfo findByChannel(long channelId);

    TicketInfo findByDiscordId(long discordId);

    void delete(String channelId);

    long getLastIndex();

    boolean isNotValidUser(long discordId);

    byte getAverageRate();
}