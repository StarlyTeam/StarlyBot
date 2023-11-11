package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Download;
import org.bson.Document;

import java.util.List;

public interface DownloadRepository {

    void put(Download download);
    Download findOne(String token);
    List<Download> findMany(long userId);
    List<Download> findMany(String userIp);
    List<Download> findAll();
    void deleteOne(String token);
    void deleteMany(long userId);

    MongoCollection<Document> getCollection();
}