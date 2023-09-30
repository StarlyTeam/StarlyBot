package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.repository.TicketRepository;

public record TicketInfoService(TicketRepository ticketRepository) {

    public void recordTicketInfo(TicketInfo ticketInfo) {
        ticketRepository.save(ticketInfo);
    }

    public void updateRate(long channelId, byte rate) {
        ticketRepository.updateRate(channelId, rate);
    }

    public TicketInfo findByChannel(long channelId) {
        return ticketRepository.findByChannel(channelId);
    }

    public TicketInfo findByDiscordId(long discordId) {
        return ticketRepository.findByDiscordId(discordId);
    }

    public long getLastIndex() {
        return ticketRepository().getLastIndex();
    }

    public boolean isNotValidUser(long discordId) {
        return ticketRepository.isNotValidUser(discordId);
    }

    public byte getAverageRate() {
        return ticketRepository.getAverageRate();
    }
}