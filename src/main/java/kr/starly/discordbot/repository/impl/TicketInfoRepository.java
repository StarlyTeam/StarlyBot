package kr.starly.discordbot.repository.impl;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;

import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.listener.ticket.TicketType;
import lombok.AllArgsConstructor;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.eq;

@AllArgsConstructor
public class TicketInfoRepository implements kr.starly.discordbot.repository.TicketInfoRepository {

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

    public TicketInfo findTicketInfoById(String userId) {
        Document document = collection.find(new Document("openBy", userId)).first();
        if (document == null) return null;

        return new TicketInfo(
                document.getString("openBy"),
                document.getString("channelId"),
                TicketType.valueOf(document.getString("type")),
                document.getString("openAt"),
                document.getString("closeAt")
        );
    }


    public Document getFieldById(String id) {
        Bson projectionFields = Projections.fields(
                Projections.include("openBy", id),
                Projections.excludeId());

        Document doc = collection.find(eq("title", "The Room"))
                .projection(projectionFields)
                .sort(Sorts.descending("imdb.rating"))
                .first();
        return doc;
    }
}