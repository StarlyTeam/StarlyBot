package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.PluginInfo;
import kr.starly.discordbot.entity.TicketInfo;

import java.util.List;

public interface TicketInfoRepo {

    public void add(TicketInfo ticketInfo);

    public void delete(String id);

    TicketInfo findById(String id);



}
