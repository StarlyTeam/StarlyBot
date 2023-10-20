package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.User;

import org.bson.Document;
import java.util.List;

public interface UserRepository {

    void put(User user);
    User findByDiscordId(long discordId);
    List<User> findAll();

    MongoCollection<Document> getCollection();
}