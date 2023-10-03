package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@SuppressWarnings("all")
public class MongoPluginRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(Plugin pluginInfo) {
        Document filter = new Document("pluginNameEN", pluginInfo.pluginNameEN());

        Document document = new Document();
        document.put("pluginNameEN", pluginInfo.pluginNameEN());
        document.put("pluginNameKR", pluginInfo.pluginNameKR());
        document.put("pluginWikiLink", pluginInfo.pluginWikiLink());
        document.put("pluginVideoLink", pluginInfo.pluginVideoLink());
        document.put("gifLink", pluginInfo.gifLink());
        document.put("dependency", pluginInfo.dependency());
        document.put("manager", pluginInfo.manager());

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Plugin findByName(String pluginNameEnglish) {
        Document filter = new Document("pluginNameEN", pluginNameEnglish);
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
    public void deleteByPluginNameEN(String pluginNameEnglish) {
        Document filter = new Document("pluginNameEN", pluginNameEnglish);
        collection.deleteOne(filter);
    }

    private Plugin parsePlugin(Document document) {
        if (document == null) return null;

        String pluginNameEN = document.getString("pluginNameEN");
        String pluginNameKR = document.getString("pluginNameKR");
        String pluginWikiLink = document.getString("pluginWikiLink");
        String pluginVideoLink = document.getString("pluginVideoLink");
        String gifLink = document.getString("gifLink");
        List<String> dependency = (List<String>) document.get("dependency");
        List<Long> manager = (List<Long>) document.get("manager");
        int price = document.getInteger("price");

        return new Plugin(pluginNameEN, pluginNameKR, pluginWikiLink, pluginVideoLink, gifLink, dependency, manager, price);
    }
}