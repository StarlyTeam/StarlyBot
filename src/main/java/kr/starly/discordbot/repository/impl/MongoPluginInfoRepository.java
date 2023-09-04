package kr.starly.discordbot.repository.impl;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.PluginInfoDTO;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@SuppressWarnings("all")
public class MongoPluginInfoRepository implements PluginRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void save(PluginInfoDTO pluginInfo) {
        Document document = new Document();
        document.put("pluginNameEnglish", pluginInfo.getPluginNameEnglish());
        document.put("pluginNameKorean", pluginInfo.getPluginNameKorean());
        document.put("pluginWikiLink", pluginInfo.getPluginWikiLink());
        document.put("pluginVideoLink", pluginInfo.getPluginVideoLink());
        document.put("dependency", pluginInfo.getDependency());
        document.put("managers", pluginInfo.getManagers());
        document.put("gifLink", pluginInfo.getGifLink());

        Document searchDocument = new Document("pluginNameEnglish", pluginInfo.getPluginNameEnglish());
        if (collection.find(searchDocument).first() != null) {
            collection.updateOne(searchDocument, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public void remove(String pluginNameEnglish) {
        Document searchDocument = new Document("pluginNameEnglish", pluginNameEnglish);
        collection.deleteOne(searchDocument);
    }

    @Override
    public PluginInfoDTO findByName(String pluginNameEnglish) {
        Document document = collection.find(new Document("pluginNameEnglish", pluginNameEnglish)).first();
        if (document == null) return null;

        PluginInfoDTO dto = new PluginInfoDTO();
        dto.setPluginNameEnglish(document.getString("pluginNameEnglish"));
        dto.setPluginNameKorean(document.getString("pluginNameKorean"));
        dto.setPluginWikiLink(document.getString("pluginWikiLink"));
        dto.setPluginVideoLink(document.getString("pluginVideoLink"));
        dto.setDependency((List<String>) document.get("dependency"));
        dto.setManagers((List<String>) document.get("managers"));
        dto.setGifLink(document.getString("gifLink"));

        return dto;
    }

    @Override
    public List<PluginInfoDTO> findAll() {
        List<PluginInfoDTO> plugins = new ArrayList<>();
        for (Document document : collection.find()) {
            PluginInfoDTO dto = new PluginInfoDTO();
            dto.setPluginNameEnglish(document.getString("pluginNameEnglish"));
            dto.setPluginNameKorean(document.getString("pluginNameKorean"));
            dto.setPluginWikiLink(document.getString("pluginWikiLink"));
            dto.setPluginVideoLink(document.getString("pluginVideoLink"));
            dto.setDependency((List<String>) document.get("dependency"));
            dto.setManagers((List<String>) document.get("managers"));
            dto.setGifLink(document.getString("gifLink"));
            plugins.add(dto);
        }
        return plugins;
    }

    @Override
    public void update(PluginInfoDTO pluginInfo) {
        Document searchDocument = new Document("pluginNameEnglish", pluginInfo.getPluginNameEnglish());

        Document updateDocument = new Document();
        updateDocument.put("pluginNameKorean", pluginInfo.getPluginNameKorean());
        updateDocument.put("pluginWikiLink", pluginInfo.getPluginWikiLink());
        updateDocument.put("pluginVideoLink", pluginInfo.getPluginVideoLink());
        updateDocument.put("dependency", pluginInfo.getDependency());
        updateDocument.put("managers", pluginInfo.getManagers());
        updateDocument.put("gifLink", pluginInfo.getGifLink());

        Document setDocument = new Document("$set", updateDocument);

        collection.updateOne(searchDocument, setDocument);
    }
}