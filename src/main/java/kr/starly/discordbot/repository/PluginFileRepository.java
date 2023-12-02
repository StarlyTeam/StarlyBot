package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import org.bson.Document;

import java.io.IOException;
import java.util.List;

public interface PluginFileRepository {

    void put(PluginFile pluginFile) throws IOException;
    List<PluginFile> findMany(String ENName);
    List<PluginFile> findMany(String ENName, String version);
    List<PluginFile> findMany(String ENName, MCVersion mcVersion);
    PluginFile findOne(String ENName, MCVersion mcVersion, String version);
    void deleteMany(String ENName);
    void deleteMany(String ENName, String version);
    void deleteMany(String ENName, MCVersion mcVersion);
    void deleteOne(String ENName, MCVersion mcVersion, String version);

    MongoCollection<Document> getCollection();
}