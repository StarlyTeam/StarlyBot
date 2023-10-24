package kr.starly.discordbot.repository.impl;

import lombok.Getter;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;

public class TicketUserDataRepository {
    @Getter
    private static final TicketUserDataRepository instance = new TicketUserDataRepository();

    @Getter
    private HashMap<Long, User> modalListHashMap = new HashMap<>();

    private TicketUserDataRepository() {}

    public void registerUser(long discordId, User user) {
        modalListHashMap.put(discordId, user);
    }

    public User retrieveUser(long discordId) {
        return modalListHashMap.get(discordId);
    }

    public boolean contains(long discordId) {
        return modalListHashMap.containsKey(discordId);
    }

    public void deleteUser(long discordId) {
        modalListHashMap.remove(discordId);
    }
}