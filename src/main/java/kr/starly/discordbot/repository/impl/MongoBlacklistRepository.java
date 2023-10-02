package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.repository.BlacklistRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@SuppressWarnings("all")
public class MongoBlacklistRepository implements BlacklistRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Blacklist blacklist) {
        Document filter = new Document("userId", blacklist.userId());

        Document document = new Document();
        document.put("userId", blacklist.userId());
        document.put("moderatorId", blacklist.moderatorId());
        document.put("reason", blacklist.reason());
        document.put("listedAt", blacklist.listedAt());

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public void deleteByUserId(long userId) {
        Document filter = new Document("userId", userId);
        collection.deleteOne(filter);
    }

    @Override
    public Blacklist findByUserId(long userId) {
        Document filter = new Document("userId", userId);
        Document document = collection.find(filter).first();
        return parseBlacklist(document);
    }

    @Override
    public List<Blacklist> findAll() {
        List<Blacklist> blacklists = new ArrayList<>();
        for (Document document : collection.find()) {
            blacklists.add(parseBlacklist(document));
        }

        return blacklists;
    }

    private Blacklist parseBlacklist(Document document) {
        if (document == null) return null;

        long userId = document.getLong("userId");
        long moderatorId = document.getLong("moderatorId");
        String reason = document.getString("reason");
        Date listedAt = document.getDate("listedAt");
        return new Blacklist(userId, moderatorId, reason, listedAt);
    }
}