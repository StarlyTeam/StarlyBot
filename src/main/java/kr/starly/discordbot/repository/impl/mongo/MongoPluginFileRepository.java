package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.repository.PluginFileRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class MongoPluginFileRepository implements PluginFileRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(PluginFile pluginFile) throws IOException {
        Document filter = new Document();
        filter.put("ENName", pluginFile.getPlugin().getENName());
        filter.put("mcVersion", pluginFile.getMcVersion());
        filter.put("version", pluginFile.getVersion());

        Document document = pluginFile.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN) {
        Document filter = new Document("ENName", pluginNameEN);

        return collection.find(filter)
                .map(PluginFile::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN, String version) {
        Document filter = new Document();
        filter.put("ENName", pluginNameEN);
        filter.put("version", version);

        return collection.find(filter)
                .map(PluginFile::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<PluginFile> findMany(String pluginNameEN, MCVersion mcVersion) {
        Document filter = new Document();
        filter.put("ENName", pluginNameEN);
        filter.put("mcVersion", mcVersion);

        return collection.find(filter)
                .map(PluginFile::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public PluginFile findOne(String ENName, MCVersion mcVersion, String version) {
        Document filter = new Document();
        filter.put("ENName", ENName);
        filter.put("mcVersion", mcVersion);
        filter.put("version", version);

        Document document = collection.find(filter).first();
        return PluginFile.deserialize(document);
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
}
