package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.repository.TicketRepository;

public record TicketService(TicketRepository repository) {

    public void recordTicket(Ticket ticketInfo) {
        repository.save(ticketInfo);
    }

    public void updateRate(long channelId, byte rate) {
        repository.updateRate(channelId, rate);
    }

    public Ticket findByChannel(long channelId) {
        return repository.findByChannel(channelId);
    }

    public Ticket findByDiscordId(long discordId) {
        return repository.findByDiscordId(discordId);
    }

    public void deleteDataByChannelId(long channelId) {
        repository.deleteOne(channelId);
    }

    public void deleteDataByDiscordId(long discordId) {
        repository.deleteMany(discordId);
    }

    public long getLastIndex() {
        return repository().getLastIndex();
    }

    public boolean isNotValidUser(long discordId) {
        return repository.isNotValidUser(discordId);
    }

    public byte getAverageRate() {
        return repository.getAverageRate();
    }
}