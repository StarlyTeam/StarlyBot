package kr.starly.discordbot.repository.impl;

import kr.starly.discordbot.entity.PluginFile;

import java.util.HashMap;
import java.util.Map;

public class DownloadTokenRepository {

    private static DownloadTokenRepository instance;

    public static DownloadTokenRepository getInstance() {
        if (instance == null) instance = new DownloadTokenRepository();
        return instance;
    }

    private DownloadTokenRepository() {}

    private final Map<String, PluginFile> tokenMap = new HashMap<>();

    public void put(String token, PluginFile pluginFile) {
        tokenMap.put(token, pluginFile);
    }

    public PluginFile get(String token) {
        return tokenMap.get(token);
    }

    public PluginFile remove(String token) {
        return tokenMap.remove(token);
    }
}