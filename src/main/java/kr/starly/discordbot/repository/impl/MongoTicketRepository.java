package kr.starly.discordbot.repository.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.enums.TicketStatus;
import kr.starly.discordbot.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

@AllArgsConstructor
public class MongoTicketRepository implements TicketRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(TicketInfo ticketInfo) {
        Document document = new Document();

        document.put("openBy", ticketInfo.openBy());
        document.put("closeBy", ticketInfo.closeBy());
        document.put("channelId", ticketInfo.channelId());
        document.put("type", ticketInfo.ticketStatus());

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();

        Document searchDocument = new Document("channelId", ticketInfo.channelId());
        if (collection.find(searchDocument).first() != null) {
            document.put("closeAt", dateFormat.format(date));

            collection.updateOne(searchDocument, new Document("$set", document));
        } else {
            document.put("openAt", dateFormat.format(date));
            document.put("closeAt", null);
            document.put("rate", null);
            document.put("index", getLastIndex());

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
        long closeBy = lastDocument.getLong("closeBy");

        TicketStatus ticketStatus = TicketStatus.valueOf((String) lastDocument.get("type"));

        return lastDocument != null ? new TicketInfo(openBy, closeBy, channelId, ticketStatus) : null;
    }

    @Override
    public TicketInfo findByDiscordId(long discordId) {
        Document query = new Document("openBy", discordId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastDocument = iterable.first();

        long openBy = lastDocument.getLong("openBy");
        long closeBy = lastDocument.getLong("closeBy");
        long channelId = lastDocument.getLong("channelId");

        TicketStatus ticketStatus = TicketStatus.valueOf((String) lastDocument.get("type"));

        return lastDocument != null ? new TicketInfo(openBy, closeBy, channelId, ticketStatus) : null;
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
        return lastDocument != null ? (lastDocument.getLong("index") + 1) : 1;
    }

    @Override
    public void delete(String channelId) {
        Document searchDocument = new Document("channelId", channelId);
        collection.deleteOne(searchDocument);
    }

    @Override
    public boolean isNotValidUser(long discordId) {
        Document lastDocument = collection.find(new Document("openBy", discordId)).sort(new Document("_id", -1)).first();
        if (lastDocument != null) {
            return lastDocument.getLong("closeBy") == 0;
        }
        return false;
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