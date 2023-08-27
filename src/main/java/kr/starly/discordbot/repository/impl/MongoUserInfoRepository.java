package kr.starly.discordbot.repository.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.UserInfo;
import kr.starly.discordbot.repository.UserInfoRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class MongoUserInfoRepository implements UserInfoRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(UserInfo userInfo) {
        Document searchDocument = new Document("discord-id", userInfo.discordId());

        Document updateDocument = new Document();
        updateDocument.put("ip", userInfo.ip());
        updateDocument.put("verify-date", userInfo.verifyDate());
        updateDocument.put("point", userInfo.point());

        Document setDocument = new Document("$set", updateDocument);

        if (collection.find(searchDocument).first() != null) {
            collection.updateOne(searchDocument, setDocument);
        } else {
            Document newDocument = new Document();
            newDocument.put("discord-id", userInfo.discordId());
            newDocument.putAll(updateDocument);
            collection.insertOne(newDocument);
        }
    }

    @Override
    public UserInfo findByDiscordId(String takenDiscordId) {
        Document document = collection.find(new Document("discord-id", takenDiscordId)).first();
        if (document != null) {
            String discordId = document.getString("discord-id");
            String ip = document.getString("ip");
            LocalDateTime verifyData = document.getDate("verify-date").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            int point = document.getInteger("point");

            return new UserInfo(discordId, ip, verifyData, point);
        }
        return null;
    }

    @Override
    public void updatePoint(String discordId, int newPoint) {
        Document searchDocument = new Document("discord-id", discordId);
        Document setDocument = new Document("$set", new Document("point", newPoint));
        collection.updateOne(searchDocument, setDocument);
    }

    @Override
    public List<UserInfo> getTopUsersByPoints(int limit) {
        List<UserInfo> topUsers = new ArrayList<>();

        FindIterable<Document> documents = collection.find()
                .sort(new Document("point", -1))
                .limit(limit);

        for (Document document : documents) {
            String discordId = document.getString("discord-id");
            String ip = document.getString("ip");
            LocalDateTime verifyDate = document.getDate("verify-date").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            int point = document.getInteger("point");

            UserInfo userInfo = new UserInfo(discordId, ip, verifyDate, point);
            topUsers.add(userInfo);
        }

        return topUsers;
    }
}