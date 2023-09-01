package kr.starly.discordbot.repository.impl;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.entity.TicketModalInfo;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Message;

import java.io.BufferedWriter;
import java.io.File;

import java.io.IOException;
import java.util.List;


public class TicketInfoFileRepository {

    private TicketInfoFileRepository() {}

    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static final String TICKET_PATH = configManager.getString("TICKET_PATH");
    private static TicketInfoFileRepository instance;

    public static TicketInfoFileRepository getInstance() {
        if (instance == null) instance = new TicketInfoFileRepository();
        return instance;
    }


    @Setter
    private TicketModalInfo ticketInfo;

    public void save(TicketModalInfo ticketInfo, String channelId) {
        try {
            String path = TICKET_PATH + "\\" + ticketInfo.getType().name() + "\\" + channelId + "\\";
            File file = new File(path);
            file.mkdirs();

            BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(path + "info.txt", true));

            bw.write("title : " + ticketInfo.getTitle() + "\n");
            bw.write("message : " + ticketInfo.getMessage());

            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void save(TicketInfo ticketInfo, String channelId, List<Message> messages) {
        String path = TICKET_PATH + "\\" + ticketInfo.ticketType().name() + "\\" + channelId;

        try {
            File file = new File(path);
            file.mkdirs();

            BufferedWriter bw = new BufferedWriter(new java.io.FileWriter(path + "\\" + channelId + ".txt", true));

            messages.forEach(message -> {
                String content = message.getContentRaw();
                try {
                    bw.write("author : " + message.getAuthor() + " >> " + content);
                    bw.newLine();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
