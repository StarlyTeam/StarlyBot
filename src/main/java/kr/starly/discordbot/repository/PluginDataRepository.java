package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.PluginInfoDTO;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class PluginDataRepository {

    @Getter
    private static final PluginDataRepository instance = new PluginDataRepository();

    private final Map<Long, PluginInfoDTO> sessionData = new HashMap<>();

    private PluginDataRepository() {}

    public PluginInfoDTO getData(Long userId) {
        return sessionData.get(userId);
    }

    public void setData(Long userId, PluginInfoDTO data) {
        sessionData.put(userId, data);
    }

    public void removeData(Long userId) {
        sessionData.remove(userId);
    }
}