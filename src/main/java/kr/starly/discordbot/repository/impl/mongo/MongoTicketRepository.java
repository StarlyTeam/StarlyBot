package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.repository.TicketRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@AllArgsConstructor
public class MongoTicketRepository implements TicketRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(Ticket ticketInfo) {
        Document document = new Document();

        document.put("openBy", ticketInfo.openBy());
        document.put("closedBy", ticketInfo.closedBy());
        document.put("channelId", ticketInfo.channelId());
        document.put("type", ticketInfo.ticketType());

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
    public Ticket findByChannel(long channelId) {
        Document query = new Document("channelId", channelId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastDocument = iterable.first();

        long openBy = lastDocument.getLong("openBy");
        long closedBy = lastDocument.getLong("closedBy");
        long index = lastDocument.getLong("index");

        TicketType ticketType = TicketType.valueOf((String) lastDocument.get("type"));

        return lastDocument != null ? new Ticket(openBy, closedBy, channelId, ticketType, index) : null;
    }

    @Override
    public Ticket findByDiscordId(long discordId) {
        Document query = new Document("openBy", discordId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastestDocument = iterable.first();
        if (lastestDocument == null) {
            return null;
        }

        long openBy = lastestDocument.getLong("openBy");
        long closedBy = lastestDocument.getLong("closedBy");
        long channelId = lastestDocument.getLong("channelId");
        long index = lastestDocument.getLong("index");

        TicketType ticketType = TicketType.valueOf((String) lastestDocument.get("type"));

        return new Ticket(openBy, closedBy, channelId, ticketType, index);
    }

    @Override
    public void deleteOne(long channelId) {
        Document filter = new Document("channelId", channelId);
        collection.deleteOne(filter);
    }

    @Override
    public void deleteMany(long discordId) {
        Document filter = new Document("openBy", discordId);
        collection.deleteMany(filter);
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