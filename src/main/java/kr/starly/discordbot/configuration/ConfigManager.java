package kr.starly.discordbot.configuration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

public class ConfigManager {

    private static ConfigManager instance;

    public static ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    private ConfigManager() {}

    private JsonObject config;

    public void loadConfig() {
        try (Reader fileReader = new InputStreamReader(getClass().getResourceAsStream("/config.json"))) {
            config = JsonParser.parseReader(fileReader).getAsJsonObject();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public String getString(String key) {
        return config.get(key).getAsString();
    }

    public int getInt(String key) {
        return config.get(key).getAsInt();
    }
}