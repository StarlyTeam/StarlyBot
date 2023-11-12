package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.repository.VerifyRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MongoVerifyRepository implements VerifyRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Verify download) {
        Document filter = new Document("token", download.getToken());
        Document document = download.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Verify findOne(String token) {
        Document filter = new Document("token", token);
        Document document = collection.find(filter).first();
        return Verify.deserialize(document);
    }

    @Override
    public List<Verify> findMany(long userId) {
        Document filter = new Document("userId", userId);
        return collection.find(filter)
                .map(Verify::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<Verify> findMany(String  userIp) {
        Document filter = new Document("userIp", userIp);
        return collection.find(filter)
                .map(Verify::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<Verify> findAll() {
        return collection.find()
                .map(Verify::deserialize)
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