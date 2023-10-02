package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.repository.ShortenLinkRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@SuppressWarnings("all")
public class MongoShortenLinkRepository implements ShortenLinkRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(ShortenLink shortenLink) {
        Document document = new Document();
        document.put("originUrl", shortenLink.originUrl());
        document.put("shortenUrl", shortenLink.shortenUrl());

        if (findByOriginUrl(shortenLink.originUrl()) != null) {
            Document filter = new Document("originUrl", shortenLink.originUrl());
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public ShortenLink findByOriginUrl(String originUrl) {
        Document filter = new Document("originUrl", originUrl);

        Document document = collection.find(filter).first();
        if (document == null) return null;

        String shortenUrl = document.getString("shortenUrl");
        return new ShortenLink(originUrl, shortenUrl);
    }

    @Override
    public ShortenLink findByShortenUrl(String shortenUrl) {
        Document filter = new Document("shortenUrl", shortenUrl);
        Document document = collection.find(filter).first();
        return parseShortenLink(document);
    }

    @Override
    public List<ShortenLink> findAll() {
        List<ShortenLink> shortenLinks = new ArrayList<>();
        for (Document document : collection.find()) {
            shortenLinks.add(parseShortenLink(document));
        }

        return shortenLinks;
    }

    @Override
    public void deleteByOriginUrl(String originUrl) {
        Document filter = new Document("originUrl", originUrl);
        collection.deleteOne(filter);
    }

    @Override
    public void deleteByShortenUrl(String shortenUrl) {
        Document filter = new Document("shortenUrl", shortenUrl);
        collection.deleteOne(filter);
    }

    private ShortenLink parseShortenLink(Document document) {
        if (document == null) return null;

        String originUrl = document.getString("originUrl");
        String shortenUrl = document.getString("shortenUrl");
        return new ShortenLink(originUrl, shortenUrl);
    }
}