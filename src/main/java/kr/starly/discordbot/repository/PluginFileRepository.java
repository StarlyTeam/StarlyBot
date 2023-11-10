package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import org.bson.Document;

import java.io.IOException;
import java.util.List;

public interface PluginFileRepository {

    void put(PluginFile pluginFile) throws IOException;
    List<PluginFile> findMany(String pluginNameEN);
    List<PluginFile> findMany(String pluginNameEN, String version);
    List<PluginFile> findMany(String pluginNameEN, MCVersion mcVersion);
    PluginFile findOne(String pluginNameEN, MCVersion mcVersion, String version);
    void deleteMany(String pluginNameEN);
    void deleteMany(String pluginNameEN, String version);
    void deleteMany(String pluginNameEN, MCVersion mcVersion);
    void deleteOne(String pluginNameEN, MCVersion mcVersion, String version);

    MongoCollection<Document> getCollection();
}