package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class MongoPluginRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Plugin plugin) {
        Document filter = new Document("ENName", plugin.getENName());

        Document document = new Document();
        document.put("ENName", plugin.getENName());
        document.put("KRName", plugin.getKRName());
        document.put("emoji", plugin.getEmoji());
        document.put("wikiUrl", plugin.getWikiUrl());
        document.put("iconUrl", plugin.getIconUrl());
        document.put("videoUrl", plugin.getVideoUrl());
        document.put("gifUrl", plugin.getGifUrl());
        document.put("dependency", plugin.getDependency());
        document.put("manager", plugin.getManager());
        document.put("buyerRole", plugin.getBuyerRole());
        document.put("threadId", plugin.getThreadId());
        document.put("version", plugin.getVersion());
        document.put("price", plugin.getPrice());

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
        return parsePlugin(document);
    }

    @Override
    public List<Plugin> findAll() {
        List<Plugin> plugins = new ArrayList<>();
        for (Document document : collection.find()) {
            plugins.add(parsePlugin(document));
        }

        return plugins;
    }

    @Override
    public void deleteByENName(String ENName) {
        Document filter = new Document("ENName", ENName);
        collection.deleteOne(filter);
    }

    @SuppressWarnings("all")
    private Plugin parsePlugin(Document document) {
        if (document == null) return null;

        String ENName = document.getString("ENName");
        String KRName = document.getString("KRName");
        String emoji = document.get("emoji", String.class);
        String wikiUrl = document.getString("wikiUrl");
        String iconUrl = document.getString("iconUrl");
        String videoUrl = document.getString("videoUrl");
        String gifUrl = document.getString("gifUrl");
        List<String> dependency = document.get("dependency", List.class);
        List<Long> manager = document.get("manager", List.class);
        Long buyerRole = document.getLong("buyerRole");
        Long postId = document.getLong("threadId");
        String version = document.getString("version");
        int price = document.getInteger("price");

        return new Plugin(ENName, KRName, emoji, wikiUrl, iconUrl, videoUrl, gifUrl, dependency, manager, buyerRole, postId, version, price);
    }
}