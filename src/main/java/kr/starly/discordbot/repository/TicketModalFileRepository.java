package kr.starly.discordbot.repository;

import kr.starly.discordbot.enums.TicketStatus;
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

    public File getFile(long channelId, TicketStatus ticketStatus) {
        String filePath = "ticket/" + ticketStatus.name().replace("_TICKET", "").replace("_", "-").toLowerCase() + "/" + channelId + "/log.txt";
        File file = new File(local, filePath);

        return file.exists() ? file : null;
    }

    public boolean delete(long channelId, TicketStatus ticketStatus) {
        String filePath = "ticket/" + ticketStatus.name().replace("_TICKET", "").replace("_", "-").toLowerCase() + "/" + channelId + "/log.txt";
        File file = new File(local, filePath);

        if (!file.exists()) return false;
        file.delete();

        return true;
    }

    public void save(TicketStatus type, long channelId, String str) {
        try {
            String filePath = "ticket/" + type.name().replace("_TICKET", "").replace("_", "-").toLowerCase() + "/" + channelId + "/";
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
}