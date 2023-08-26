package kr.starly.discordbot.service;


import kr.starly.discordbot.entity.TicketInfo;

import kr.starly.discordbot.repository.TicketInfoRepository;

import java.time.LocalDateTime;

public record TicketService(TicketInfoRepository ticketInfoRepo) {

    public void save(TicketInfo ticketInfo) {
        ticketInfoRepo.save(ticketInfo);
    }

    public TicketInfo load(String userId) {
        return ticketInfoRepo.findTicketInfoById(userId);
    }

    public void update(String userId) {
        ticketInfoRepo.updateCloseAtDate(userId, LocalDateTime.now().toString());
    }
}