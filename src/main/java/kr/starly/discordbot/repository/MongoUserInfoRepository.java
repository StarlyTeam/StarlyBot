package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.UserInfo;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;

@AllArgsConstructor
public class MongoUserInfoRepository implements UserInfoRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(UserInfo userInfo) {
        Document document = new Document();
        document.put("discord-id", userInfo.discordId());
        document.put("ip", userInfo.ip());
        document.put("verify-date", userInfo.verifyDate());
        collection.insertOne(document);
    }

    @Override
    public UserInfo findByDiscordId(String takenDiscordId) {
        Document document = collection.find(new Document("discord-id", takenDiscordId)).first();
        if (document != null) {
            String discordId = document.getString("discord-id");
            String ip = document.getString("ip");
            LocalDateTime verifyData = document.getDate("verify-date").toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            UserInfo userInfo = new UserInfo(discordId, ip, verifyData);
            return userInfo;
        }
        return null;
    }
}
