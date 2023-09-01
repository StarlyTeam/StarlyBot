package kr.starly.discordbot.service;


import kr.starly.discordbot.entity.TicketInfo;

import kr.starly.discordbot.entity.TicketModalInfo;
import kr.starly.discordbot.repository.TicketRepository;

import java.time.LocalDateTime;

public record TicketService(TicketRepository ticketInfoRepo) {

    public void save(TicketInfo ticketInfo) {
        ticketInfoRepo.save(ticketInfo);
    }


    public void update(String channelId) {
        ticketInfoRepo.updateCloseAtDate(channelId, LocalDateTime.now().toString());
    }

    public TicketInfo load(String channelId) {
        return ticketInfoRepo.findTicketInfoById(channelId);
    }
}