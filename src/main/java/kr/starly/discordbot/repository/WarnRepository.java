package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Warn;
import org.bson.Document;

import java.util.Date;
import java.util.List;

public interface WarnRepository {

    void put(Warn warn);
    List<Warn> findMany(long discordId);
    void deleteOne(long discordId, Date date);
    void deleteMany(long discordId);

    MongoCollection<Document> getCollection();
}