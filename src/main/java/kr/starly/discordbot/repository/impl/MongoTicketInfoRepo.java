package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginInfo;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.repository.TicketInfoRepo;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class MongoTicketInfoRepo implements TicketInfoRepo {

    private final MongoCollection<Document> collection;

    public MongoTicketInfoRepo(MongoCollection<Document> collection) {
        this.collection = collection;
    }


    @Override
    public void add(TicketInfo ticketInfo) {
        Document document = new Document("ticket", "ticketInfo");

        document.put("id", ticketInfo.id());
        document.put("title", ticketInfo.title());
        document.put("description", ticketInfo.description());
        document.put("date", ticketInfo.date());

        Document searchDocument = new Document("ticket", ticketInfo.title());
        if (collection.find(searchDocument).first() != null) {
            collection.updateOne(searchDocument, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public void delete(String id) {
        Document searchDocument = new Document(id, id);
        collection.deleteOne(searchDocument);
    }

    @Override
    public TicketInfo findById(String id) {
        Document document = collection.find(new Document("id", id)).first();
        if (document == null) return null;

        return new TicketInfo(
                document.getString("id"),
                document.getString("title"),
                document.getString("description"),
                document.getString("date")
        );
    }
}
