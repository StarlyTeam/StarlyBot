package kr.starly.discordbot.repository;

import com.google.gson.Gson;

import kr.starly.discordbot.entity.Ticket;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Message;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TicketFileRepository {

    @Getter
    private static final TicketFileRepository instance = new TicketFileRepository();

    private final String local = System.getProperty("user.dir");

    private TicketFileRepository() {}

    public void save(List<Message> messageHistory, Ticket ticket) {
        try {
            String filePath = generatePath(ticket);
            File dir = new File(local, filePath);

            if (!dir.exists()) dir.mkdirs();
            File file = new File(filePath, "message.json");

            Path logFile = Paths.get(file.getAbsolutePath());
            BufferedWriter bufferedWriter = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8);

            Gson gson = new Gson();

            List<Map<String, Object>> messageData = new ArrayList<>();
            Map<String, Object> messageObj = new HashMap<>();

            AtomicInteger folderIndex = new AtomicInteger(0);

            messageHistory.forEach(message -> {
                Map<String, Object> messageMap = new HashMap<>();
                Map<String, Object> authorMap = new HashMap<>();
                Map<String, Object> contentMap = new HashMap<>();

                authorMap.put("authorID", message.getAuthor().getId());
                authorMap.put("authorGlobalName", message.getAuthor().getGlobalName());
                authorMap.put("authorName", message.getAuthor().getName());

                messageMap.put("author", authorMap);

                List<Message.Attachment> attachments = message.getAttachments();

                if (attachments.isEmpty())
                    contentMap.put("contentDisplay", message.getContentDisplay());

                folderIndex.getAndIncrement();

                attachments.forEach(
                        attachment -> {
                            int index = attachments.indexOf(attachment);

                            File attachmentFile = new File(filePath + "/attachments/" + folderIndex);
                            attachmentFile.mkdirs();

                            String fileName = index + "." + attachment.getFileExtension();

                            attachment.downloadToFile(new File(attachmentFile.getPath(), fileName));
                            contentMap.put("filePath", attachmentFile.getPath() + "\\" + fileName);
                        }
                );

                contentMap.put("createdTime", message.getTimeCreated().toString());
                messageMap.put("message", contentMap);
                messageObj.put(message.getId(), messageMap);
            });

            messageData.add(messageObj);

            bufferedWriter.write(gson.toJson(messageData));
            bufferedWriter.flush();
            bufferedWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String generatePath(Ticket ticket) {
        String nameByType = ticket.ticketStatus().getName();
        return "ticket/" + nameByType + "/" + ticket.index() + "-" + ticket.channelId() + "-" + nameByType;
    }
}
