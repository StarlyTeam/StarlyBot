package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Verify;
import org.bson.Document;

import java.util.List;

public interface VerifyRepository {

    void put(Verify download);
    Verify findOne(String token);
    List<Verify> findMany(long userId);
    List<Verify> findMany(String  userIp);
    List<Verify> findAll();
    void deleteOne(String token);
    void deleteMany(long userId);

    MongoCollection<Document> getCollection();
}