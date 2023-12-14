package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.repository.WarnRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Getter
@AllArgsConstructor
public class MongoWarnRepository implements WarnRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Warn warn) {
        Document document = new Document();

        document.put("discordId", warn.discordId());
        document.put("manager", warn.manager());
        document.put("reason", warn.reason());
        document.put("amount", warn.amount());
        document.put("date", warn.date());

        collection.insertOne(document);
    }

    @Override
    public List<Warn> findMany(long discordId) {
        Document filter = new Document("discordId", discordId);
        return collection.find(filter)
                .map(this::parseWarn)
                .into(new ArrayList<>());
    }

    @Override
    public void deleteOne(long discordId, Date date) {
        Document filter = new Document();
        filter.put("discordId", discordId);
        filter.put("date", date);

        collection.deleteOne(filter);
    }

    @Override
    public void deleteMany(long discordId) {
        Document filter = new Document("discordId", discordId);
        collection.deleteMany(filter);
    }

    private Warn parseWarn(Document document) {
        if (document == null) return null;

        long discordId = document.getLong("discordId");
        long manager = document.getLong("manager");
        String reason = document.getString("reason");
        int amount = document.getInteger("amount");
        Date date = document.getDate("date");
        return new Warn(discordId, manager, reason, amount, date);
    }
}