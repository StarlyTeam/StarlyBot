package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.repository.PluginFileRepository;
import kr.starly.discordbot.service.PluginService;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class MongoPluginFileRepository implements PluginFileRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(PluginFile pluginFile) throws IOException {
        Document filter = new Document();
        filter.put("ENName", pluginFile.getPlugin().getENName());
        filter.put("mcVersion", pluginFile.getMcVersion());
        filter.put("version", pluginFile.getVersion());

        Document document = new Document(filter);
        document.put("filePath", pluginFile.getFile().getCanonicalPath());

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN) {
        Document filter = new Document("ENName", pluginNameEN);

        List<PluginFile> pluginFiles = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            pluginFiles.add(parsePluginFile(document));
        }

        return pluginFiles;
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN, String version) {
        Document filter = new Document();
        filter.put("ENName", pluginNameEN);
        filter.put("version", version);

        List<PluginFile> pluginFiles = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            pluginFiles.add(parsePluginFile(document));
        }

        return pluginFiles;
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN, MCVersion mcVersion) {
        Document filter = new Document();
        filter.put("ENName", pluginNameEN);
        filter.put("mcVersion", mcVersion);

        List<PluginFile> pluginFiles = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            pluginFiles.add(parsePluginFile(document));
        }

        return pluginFiles;
    }

    @Override
    public PluginFile findOne(String ENName, MCVersion mcVersion, String version) {
        Document filter = new Document();
        filter.put("ENName", ENName);
        filter.put("mcVersion", mcVersion);
        filter.put("version", version);

        Document document = collection.find(filter).first();
        return parsePluginFile(document);
    }

    @Override
    public void deleteMany(String ENName) {
        Document filter = new Document("ENName", ENName);

        collection.deleteMany(filter);
    }

    @Override
    public void deleteMany(String ENName, String version) {
        Document filter = new Document();
        filter.put("ENName", ENName);
        filter.put("version", version);

        collection.deleteMany(filter);
    }

    @Override
    public void deleteMany(String ENName, MCVersion mcVersion) {
        Document filter = new Document();
        filter.put("ENName", ENName);
        filter.put("mcVersion", mcVersion);

        collection.deleteMany(filter);
    }

    @Override
    public void deleteOne(String ENName, MCVersion mcVersion, String version) {
        Document filter = new Document();
        filter.put("ENName", ENName);
        filter.put("mcVersion", mcVersion);
        filter.put("version", version);

        collection.deleteOne(filter);
    }

    private PluginFile parsePluginFile(Document document) {
        if (document == null) return null;
        PluginService pluginService = DatabaseManager.getPluginService();

        File file = new File(document.getString("filePath"));
        Plugin plugin = pluginService.getDataByENName(document.getString("ENName"));
        MCVersion mcVersion = MCVersion.valueOf(document.getString("mcVersion"));
        String version = document.getString("version");

        return new PluginFile(file, plugin, mcVersion, version);
    }
}
