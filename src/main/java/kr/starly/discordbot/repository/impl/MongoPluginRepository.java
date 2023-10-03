package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class MongoPluginRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(Plugin plugin) {
        Document filter = new Document("ENName", plugin.ENName());

        Document document = new Document();
        document.put("ENName", plugin.ENName());
        document.put("KRName", plugin.KRName());
        document.put("emoji", plugin.emoji());
        document.put("wikiUrl", plugin.wikiUrl());
        document.put("iconUrl", plugin.iconUrl());
        document.put("videoUrl", plugin.videoUrl());
        document.put("gifUrl", plugin.gifUrl());
        document.put("dependency", plugin.dependency());
        document.put("manager", plugin.manager());
        document.put("version", plugin.version());
        document.put("price", plugin.price());

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
        Emoji emoji = document.get("emoji", Emoji.class);
        String wikiUrl = document.getString("wikiUrl");
        String iconUrl = document.getString("iconUrl");
        String videoUrl = document.getString("videoUrl");
        String gifUrl = document.getString("gifUrl");
        List<String> dependency = document.get("dependency", List.class);
        List<Long> manager = document.get("manager", List.class);
        Long buyerRole = document.getLong("buyerRole");
        String version = document.getString("version");
        int price = document.getInteger("price");

        return new Plugin(ENName, KRName, emoji, wikiUrl, iconUrl, videoUrl, gifUrl, dependency, manager, buyerRole, version, price);
    }
}