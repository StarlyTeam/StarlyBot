package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.ShortenLink;

import org.bson.Document;
import java.util.List;

public interface ShortenLinkRepository {

    void put(ShortenLink shortenLink);
    void deleteByOriginUrl(String originUrl);
    void deleteByShortenUrl(String shortenUrl);
    ShortenLink findByOriginUrl(String originUrl);
    ShortenLink findByShortenUrl(String shortenUrl);
    List<ShortenLink> findAll();

    MongoCollection<Document> getCollection();
}