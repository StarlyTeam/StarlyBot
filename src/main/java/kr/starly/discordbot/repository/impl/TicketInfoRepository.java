package kr.starly.discordbot.repository.impl;


import com.mongodb.client.MongoCollection;

import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.entity.TicketType;
import kr.starly.discordbot.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

@AllArgsConstructor
public class TicketInfoRepository implements TicketRepository {

    private final MongoCollection<Document> collection;


    @Override
    public void save(TicketInfo ticketInfo) {
        Document document = new Document();

        document.put("openBy", ticketInfo.openBy());
        document.put("channelId", ticketInfo.channelId());
        document.put("type", ticketInfo.ticketType());
        document.put("openAt", ticketInfo.openAt());
        document.put("closeAt", ticketInfo.closeAt());

        collection.insertOne(document);
    }

    public void updateCloseAtDate(String channelId, String date) {
        Document document = collection.find(new Document("channelId", channelId)).first();

        Document searchDocument = new Document("channelId", channelId);

        Document updateDocument = new Document();

        updateDocument.put("openBy", document.getString("openBy"));
        updateDocument.put("channelId", document.getString("channelId"));
        updateDocument.put("type", TicketType.valueOf(document.getString("type")));
        updateDocument.put("openAt", document.getString("openAt"));

        updateDocument.put("closeAt", date);

        Document setDocument = new Document("$set", updateDocument);

        collection.updateOne(searchDocument, setDocument);
    }

    public TicketInfo findTicketInfoById(String channelId) {
        Document document = collection.find(new Document("channelId", channelId)).first();
        if (document == null) return null;

        return new TicketInfo(
                document.getString("openBy"),
                document.getString("channelId"),
                TicketType.valueOf(document.getString("type")),
                document.getString("openAt"),
                document.getString("closeAt")
        );
    }
}