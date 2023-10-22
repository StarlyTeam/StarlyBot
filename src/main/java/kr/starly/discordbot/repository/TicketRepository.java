package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.Ticket;
import org.bson.Document;

public interface TicketRepository {

    void save(Ticket ticketInfo);
    void updateRate(long channelId, byte rate);
    Ticket findByChannel(long channelId);
    Ticket findByDiscordId(long discordId);
    long getLastIndex();
    boolean isNotValidUser(long discordId);
    byte getAverageRate();

    MongoCollection<Document> getCollection();
}