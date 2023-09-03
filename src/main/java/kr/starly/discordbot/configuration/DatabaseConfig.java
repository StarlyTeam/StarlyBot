package kr.starly.discordbot.configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.repository.impl.MongoUserInfoRepository;
import kr.starly.discordbot.repository.UserInfoRepository;

import kr.starly.discordbot.repository.impl.TicketInfoRepository;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.service.UserInfoService;
import lombok.Getter;
import org.bson.Document;

public class DatabaseConfig {

    private DatabaseConfig() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String DB_HOST = configProvider.getString("DB_HOST");
    private static final String DB_DATABASE = configProvider.getString("DB_DATABASE");
    private static final String DB_COLLECTION_USER = configProvider.getString("DB_COLLECTION_USER");


    private static final String DB_COLLECTION_TICKET = configProvider.getString("DB_COLLECTION_TICKET");

    private static final MongoClient mongoClient = MongoClients.create(DB_HOST);
    private static final MongoDatabase database = mongoClient.getDatabase(DB_DATABASE);
    private static final MongoCollection<Document> userCollection = database.getCollection(DB_COLLECTION_USER);


    private static final MongoCollection<Document> ticketCollection = database.getCollection(DB_COLLECTION_TICKET);

    private static final UserInfoRepository userInfoRepository = new MongoUserInfoRepository(userCollection);


    @Getter
    private static final TicketInfoRepository ticketInfoRepository = new TicketInfoRepository(ticketCollection);

    @Getter
    private static final UserInfoService userInfoService = new UserInfoService(userInfoRepository);

    @Getter
    private static final TicketService ticketService = new TicketService(ticketInfoRepository);
}