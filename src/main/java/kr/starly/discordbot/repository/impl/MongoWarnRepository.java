package kr.starly.discordbot.repository.impl;

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
    public List<Warn> findByDiscordId(long discordId) {
        Document filter = new Document("discordId", discordId);

        List<Warn> warnList = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            warnList.add(parseWarn(document));
        }

        return warnList;
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