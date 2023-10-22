package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.List;
import java.util.stream.StreamSupport;

@Getter
@AllArgsConstructor
public class MongoPluginRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Plugin plugin) {
        Document filter = new Document("ENName", plugin.getENName());
        Document document = plugin.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Plugin findByENName(String ENName) {
        Document filter = new Document("ENName", ENName);
        Document document = collection.find(filter).first();
        return Plugin.deserialize(document);
    }

    @Override
    public List<Plugin> findAll() {
        return StreamSupport.stream(collection.find().spliterator(), false)
                .map(Plugin::deserialize)
                .toList();
    }

    @Override
    public void deleteByENName(String ENName) {
        Document filter = new Document("ENName", ENName);
        collection.deleteOne(filter);
    }
}