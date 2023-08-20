package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginInfo;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class MongoPluginInfoRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(PluginInfo pluginInfo) {
        Document document = new Document();
        document.put("pluginName", pluginInfo.pluginName());
        document.put("version", pluginInfo.version());
        document.put("downloadLink", pluginInfo.downloadLink());
        document.put("wikiLink", pluginInfo.wikiLink());
        document.put("videoLink", pluginInfo.videoLink());

        Document searchDocument = new Document("pluginName", pluginInfo.pluginName());
        if (collection.find(searchDocument).first() != null) {
            collection.updateOne(searchDocument, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public void remove(String pluginName) {
        Document searchDocument = new Document("pluginName", pluginName);
        collection.deleteOne(searchDocument);
    }

    @Override
    public PluginInfo findByName(String pluginName) {
        Document document = collection.find(new Document("pluginName", pluginName)).first();
        if (document == null) return null;

        return new PluginInfo(
                document.getString("pluginName"),
                document.getString("version"),
                document.getString("downloadLink"),
                document.getString("wikiLink"),
                document.getString("videoLink")
        );
    }

    @Override
    public List<PluginInfo> findAll() {
        List<PluginInfo> plugins = new ArrayList<>();
        for (Document document : collection.find()) {
            plugins.add(new PluginInfo(
                    document.getString("pluginName"),
                    document.getString("version"),
                    document.getString("downloadLink"),
                    document.getString("wikiLink"),
                    document.getString("videoLink")
            ));
        }
        return plugins;
    }

    @Override
    public void update(PluginInfo pluginInfo) {
        Document searchDocument = new Document("pluginName", pluginInfo.pluginName());

        Document updateDocument = new Document();
        updateDocument.put("version", pluginInfo.version());
        updateDocument.put("downloadLink", pluginInfo.downloadLink());
        updateDocument.put("wikiLink", pluginInfo.wikiLink());
        updateDocument.put("videoLink", pluginInfo.videoLink());

        Document setDocument = new Document("$set", updateDocument);

        collection.updateOne(searchDocument, setDocument);
    }
}