package kr.starly.discordbot.repository.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
public class MongoTicketRepository implements TicketRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(TicketInfo ticketInfo) {
        Document document = new Document();

        document.put("openBy", ticketInfo.openBy());
        document.put("closedBy", ticketInfo.closedBy());
        document.put("channelId", ticketInfo.channelId());
        document.put("type", ticketInfo.ticketStatus());

        Date date = new Date();

        Document searchDocument = new Document("channelId", ticketInfo.channelId());
        if (collection.find(searchDocument).first() != null) {
            document.put("closedAt", date);
            collection.updateOne(searchDocument, new Document("$set", document));
        } else {
            document.put("openAt", date);
            document.put("closedAt", null);
            document.put("rate", null);
            document.put("index", getLastIndex() + 1);

            Document newDocument = new Document();
            newDocument.putAll(document);
            collection.insertOne(newDocument);
        }
    }

    @Override
    public TicketInfo findByChannel(long channelId) {
        Document query = new Document("channelId", channelId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastDocument = iterable.first();

        long openBy = lastDocument.getLong("openBy");
        long closedBy = lastDocument.getLong("closedBy");
        long index = lastDocument.getLong("index");

        TicketType ticketStatus = TicketType.valueOf((String) lastDocument.get("type"));

        return lastDocument != null ? new TicketInfo(openBy, closedBy, channelId, ticketStatus, index) : null;
    }

    @Override
    public TicketInfo findByDiscordId(long discordId) {
        Document query = new Document("openBy", discordId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastDocument = iterable.first();

        if (lastDocument == null) {
            return null;
        }

        long openBy = lastDocument.getLong("openBy");
        long closedBy = lastDocument.getLong("closedBy");
        long channelId = lastDocument.getLong("channelId");
        long index = lastDocument.getLong("index");

        TicketType ticketStatus = TicketType.valueOf((String) lastDocument.get("type"));

        return new TicketInfo(openBy, closedBy, channelId, ticketStatus, index);
    }

    @Override
    public void updateRate(long channelId, byte rate) {
        Document searchDocument = new Document("channelId", channelId);
        Document document = new Document();
        document.put("rate", rate);
        collection.updateOne(searchDocument, new Document("$set", document));
    }

    @Override
    public long getLastIndex() {
        Document lastDocument = collection.find().sort(new Document("_id", -1)).first();
        return lastDocument != null ? (lastDocument.getLong("index")) : 0;
    }

    @Override
    public boolean isNotValidUser(long discordId) {
        Document lastDocument = collection.find(new Document("openBy", discordId)).sort(new Document("_id", -1)).first();
        if (lastDocument != null) {
            return lastDocument.getLong("closedBy") == 0;
        }

        return true;
    }

    @Override
    public byte getAverageRate() {
        List<Integer> rateList = new ArrayList<>();

        for (Document document : collection.find()) {
            if (document.get("rate") != null) {
                int rate = document.getInteger("rate");
                rateList.add(rate);
            }

        }
        return calculateAverage(rateList);
    }

    private byte calculateAverage(List<Integer> marks) {
        return (byte) marks.stream()
                .mapToDouble(d -> d)
                .average()
                .orElse(0.0);
    }
}