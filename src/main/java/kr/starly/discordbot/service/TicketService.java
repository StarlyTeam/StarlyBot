package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.repository.TicketRepository;

public record TicketService(TicketRepository ticketRepository) {

    public void recordTicket(Ticket ticketInfo) {
        ticketRepository.save(ticketInfo);
    }

    public void updateRate(long channelId, byte rate) {
        ticketRepository.updateRate(channelId, rate);
    }

    public Ticket findByChannel(long channelId) {
        return ticketRepository.findByChannel(channelId);
    }

    public Ticket findByDiscordId(long discordId) {
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