package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Plugin;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class PluginDataRepository {

    @Getter private static final PluginDataRepository instance = new PluginDataRepository();

    private final Map<Long, Plugin> sessionData = new HashMap<>();

    private PluginDataRepository() {}

    public Plugin getData(Long userId) {
        return sessionData.get(userId);
    }

    public void setData(Long userId, Plugin data) {
        sessionData.put(userId, data);
    }

    public void removeData(Long userId) {
        sessionData.remove(userId);
    }
}