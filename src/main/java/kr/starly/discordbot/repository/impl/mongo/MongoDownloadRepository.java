package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Download;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MongoDownloadRepository implements kr.starly.discordbot.repository.DownloadRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Download download) {
        Document filter = new Document("token", download.getToken());
        Document document = download.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Download findOne(String token) {
        Document filter = new Document("token", token);
        return Download.deserialize(collection.find(filter).first());
    }

    @Override
    public List<Download> findMany(long userId) {
        Document filter = new Document("userId", userId);
        return collection.find(filter)
                .map(Download::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<Download> findMany(String userIp) {
        Document filter = new Document("userIp", userIp);
        return collection.find(filter)
                .map(Download::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<Download> findAll() {
        return collection.find()
                .map(Download::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public void deleteOne(String token) {
        Document filter = new Document("token", token);
        collection.deleteOne(filter);
    }

    @Override
    public void deleteMany(long userId) {
        Document filter = new Document("userId", userId);
        collection.deleteMany(filter);
    }
}