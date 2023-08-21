package kr.starly.discordbot.service;


import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.repository.TicketInfoRepo;
import lombok.AllArgsConstructor;


@AllArgsConstructor
public class TicketService {

    private final TicketInfoRepo ticketInfoRepo;

    public void registerPlugin(TicketInfo ticketInfo) {
        ticketInfoRepo.add(ticketInfo);
    }



    public void removePlugin(String pluginName) {
        ticketInfoRepo.delete(pluginName);
    }

    public TicketInfo getPluginInfo(String id) {
        return ticketInfoRepo.findById(id);
    }

}
