package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Ticket;
import lombok.Getter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TicketModalFileRepository {

    @Getter
    private static final TicketModalFileRepository instance = new TicketModalFileRepository();

    private final String local = System.getProperty("user.dir");

    private TicketModalFileRepository() {}

    public File getFile(Ticket ticket) {
        String filePath = generatePath(ticket);
        File dir = new File(local, filePath);
        File file = new File(dir.getPath(), "log.txt");

        return file.exists() ? file : null;
    }

    public boolean delete(Ticket ticket) {
        String filePath = generatePath(ticket);
        File file = new File(local, filePath);
        deleteDir(file);

        return true;
    }

    public void save(Ticket ticket, String str) {
        try {
            String filePath = generatePath(ticket);
            File dir = new File(local, filePath);

            if (!dir.exists()) dir.mkdirs();
            File file = new File(filePath, "log.txt");

            Path logFile = Paths.get(file.getAbsolutePath());
            BufferedWriter bufferedWriter = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);

            bufferedWriter.write(str);
            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    private String generatePath(Ticket ticket) {
        String nameByType = ticket.ticketStatus().getName();
        return "ticket/" + nameByType + "/" + ticket.index() + "-" + ticket.channelId() + "-" + nameByType + "/";
    }
}