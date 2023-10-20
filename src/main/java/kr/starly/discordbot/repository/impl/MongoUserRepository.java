package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@AllArgsConstructor
@Getter
public class MongoUserRepository implements UserRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(User user) {
        Document filter = new Document("discordId", user.discordId());

        Document document = new Document();
        document.put("discordId", user.discordId());
        document.put("ip", user.ip());
        document.put("verifiedAt", user.verifiedAt());
        document.put("point", user.point());

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public User findByDiscordId(long discordId) {
        Document filter = new Document("discordId", discordId);
        Document document = collection.find(filter).first();
        return parseUser(document);
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        for (Document document : collection.find()) {
            users.add(parseUser(document));
        }

        return users;
    }

    private User parseUser(Document document) {
        if (document == null) return null;

        long discordId = document.getLong("discordId");
        String ip = document.getString("ip");
        Date verifiedAt = document.getDate("verifiedAt");
        int point = document.getInteger("point");

        return new User(discordId, ip, verifiedAt, point);
    }
}