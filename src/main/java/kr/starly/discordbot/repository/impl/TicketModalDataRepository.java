package kr.starly.discordbot.repository.impl;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TicketModalDataRepository {

    @Getter
    private static final TicketModalDataRepository instance = new TicketModalDataRepository();

    @Getter
    private HashMap<Long, List<String>> modalListHashMap = new HashMap<>();

    private TicketModalDataRepository() {}

    public boolean registerModalData(Long channelId, String... data) {
        if (data.length == 0) return false;

        modalListHashMap.put(channelId, List.of(data));
        return true;
    }

    public List<String> retrieveModalData(Long channelId) {
        return new ArrayList<>(modalListHashMap.get(channelId));
    }

    public boolean removeModalData(Long channelId) {
        if (!modalListHashMap.containsKey(channelId)) return false;
        modalListHashMap.remove(channelId);
        return true;
    }
}