package kr.starly.discordbot.configuration;

public class ConfigProvider {

    private static ConfigProvider instance;
    private final YamlConfiguration config;

    private ConfigProvider() {
        this.config = YamlConfiguration.load("config.yml");
    }

    public static ConfigProvider getInstance() {
        if (instance == null) {
            instance = new ConfigProvider();
        }
        return instance;
    }

    public String getString(String key) {
        return config.getString(key);
    }

    public int getInt(String key) {
        return config.getInt(key);
    }

    public boolean getBoolean(String key) {
        return config.getBoolean(key);
    }

    public double getDouble(String key) {
        return config.getDouble(key);
    }
}