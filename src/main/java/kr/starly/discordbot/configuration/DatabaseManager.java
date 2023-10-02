package kr.starly.discordbot.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.repository.*;
import kr.starly.discordbot.repository.impl.*;
import kr.starly.discordbot.service.*;
import lombok.Getter;
import org.bson.Document;

public class DatabaseManager {

    private DatabaseManager() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String DB_HOST = configProvider.getString("DB_HOST");
    private static final String DB_DATABASE = configProvider.getString("DB_DATABASE");
    private static final String DB_COLLECTION_USER = configProvider.getString("DB_COLLECTION_USER");
    private static final String DB_COLLECTION_TICKET = configProvider.getString("DB_COLLECTION_TICKET");
    private static final String DB_COLLECTION_WARN = configProvider.getString("DB_COLLECTION_WARN");
    private static final String DB_COLLECTION_SHORTEN_LINK = configProvider.getString("DB_COLLECTION_SHORTEN_LINK");
    private static final String DB_COLLECTION_BLACKLIST = configProvider.getString("DB_COLLECTION_BLACKLIST");

    private static final MongoClient mongoClient = MongoClients.create(DB_HOST);
    private static final MongoDatabase database = mongoClient.getDatabase(DB_DATABASE);

    private static final MongoCollection<Document> userCollection = database.getCollection(DB_COLLECTION_USER);
    private static final MongoCollection<Document> ticketCollection = database.getCollection(DB_COLLECTION_TICKET);
    private static final MongoCollection<Document> warnCollection = database.getCollection(DB_COLLECTION_WARN);
    private static final MongoCollection<Document> shortenLinkCollection = database.getCollection(DB_COLLECTION_SHORTEN_LINK);
    private static final MongoCollection<Document> blacklistCollection = database.getCollection(DB_COLLECTION_BLACKLIST);


    private static final UserInfoRepository userInfoRepository = new MongoUserInfoRepository(userCollection);
    private static final TicketRepository ticketRepository = new MongoTicketRepository(ticketCollection);
    private static final WarnRepository warnRepository = new MongoWarnRepository(warnCollection);
    private static final ShortenLinkRepository shortenLinkRepository = new MongoShortenLinkRepository(shortenLinkCollection);
    private static final BlacklistRepository blacklistRepository = new MongoBlacklistRepository(blacklistCollection);


    @Getter private static final UserInfoService userInfoService = new UserInfoService(userInfoRepository);
    @Getter private static final TicketService ticketService = new TicketService(ticketRepository);
    @Getter private static final WarnService warnService = new WarnService(warnRepository);
    @Getter private static final ShortenLinkService shortenLinkService = new ShortenLinkService(shortenLinkRepository);
    @Getter private static final BlacklistService blacklistService = new BlacklistService(blacklistRepository);
}