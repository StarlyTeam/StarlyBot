package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Blacklist;
import org.bson.Document;

import java.util.List;

public interface BlacklistRepository {

    void put(Blacklist blacklist);
    Blacklist findByUserId(long userId);
    Blacklist findByIpAddress(String ipAddress);
    List<Blacklist> findAll();
    void deleteByUserId(long userId);
    void deleteByIpAddress(String ipAddress);

    MongoCollection<Document> getCollection();
}