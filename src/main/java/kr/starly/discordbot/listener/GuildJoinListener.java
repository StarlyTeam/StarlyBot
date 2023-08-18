package kr.starly.discordbot.listener;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@BotEvent
public class GuildJoinListener extends ListenerAdapter {

    private final MongoClient mongoClient;
    private final MongoDatabase database;
    private final MongoCollection<Document> collection;

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String DB_HOST = configManager.getString("DB_HOST");
    private final String DB_DATABASE = configManager.getString("DB_DATABASE");
    private final String DB_COLLECTION_USER = configManager.getString("DB_COLLECTION_USER");

    public GuildJoinListener() {
        this.mongoClient = MongoClients.create(DB_HOST);
        this.database = mongoClient.getDatabase(DB_DATABASE);
        this.collection = database.getCollection(DB_COLLECTION_USER);
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        String discordId = event.getGuild().getId();
        LocalDateTime currentTime = LocalDateTime.now();

        Document existingDocument = collection.find(new Document("discord-id", discordId)).first();
        if (existingDocument == null) {
            Document document = new Document()
                    .append("discord-id", discordId)
                    .append("ip", "")
                    .append("join-date", currentTime);

            collection.insertOne(document);
            LOGGER.severe("서버에 처음 접속하였으므로 데이터를 추가했습니다: " + discordId);
        } else {
            LOGGER.severe("이미 데이터베이스에 존재하는 유저입니다: " + discordId);
        }
    }
}
