package kr.starly.discordbot.repository.impl;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.WarnInfo;
import kr.starly.discordbot.repository.WarnRepository;
import lombok.AllArgsConstructor;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@AllArgsConstructor
public class MongoWarnRepository implements WarnRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void recordWarn(WarnInfo warnInfo) {
        Document document = new Document();

        document.put("discordId", warnInfo.discordId());
        document.put("manager", warnInfo.manager());
        document.put("reason", warnInfo.reason());
        document.put("warn", getWarnByDiscordId(warnInfo.discordId()) + warnInfo.warn());
        document.put("date", warnInfo.date());

        collection.insertOne(document);
    }

    @Override
    public void removeWarn(WarnInfo warnInfo) {
        Document document = new Document();
        document.put("discordId", warnInfo.discordId());
        document.put("manager", warnInfo.manager());
        document.put("reason", warnInfo.reason());

        int warnCurrent = getWarnByDiscordId(warnInfo.discordId());
        document.put("warn", warnCurrent - warnInfo.warn());
        document.put("date", warnInfo.date());

        collection.insertOne(document);
    }

    @Override
    public int getWarnByDiscordId(long discordId) {
        Document query = new Document("discordId", discordId);
        FindIterable<Document> iterable = collection.find(query).sort(new Document("_id", -1)).limit(1);

        Document lastDocument = iterable.first();
        return lastDocument != null ? lastDocument.getInteger("warn") : 0;
    }

    @Override
    public List<WarnInfo> findAllByDiscordId(long discordId) {
        List<WarnInfo> warnInfoList = new ArrayList<>();
        Document searchDocument = new Document("discordId", discordId);

        for (Document document : collection.find(searchDocument)) {
            if (document == null) continue;

            long manager = document.getLong("manager");
            String reason = document.getString("reason");
            int warn = document.getInteger("warn");
            Date date = document.getDate("date");
            WarnInfo warnInfo = new WarnInfo(discordId, manager, reason, warn, date);
            warnInfoList.add(warnInfo);
        }
        return warnInfoList;
    }
}