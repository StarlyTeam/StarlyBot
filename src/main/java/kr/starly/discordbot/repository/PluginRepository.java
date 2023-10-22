package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Plugin;
import org.bson.Document;

import java.util.List;


public interface PluginRepository {

    void put(Plugin plugin);
    Plugin findByENName(String ENName);
    List<Plugin> findAll();
    void deleteByENName(String ENName);

    MongoCollection<Document> getCollection();
}