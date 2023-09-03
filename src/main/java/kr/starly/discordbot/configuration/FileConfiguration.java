package kr.starly.discordbot.configuration;

import java.util.HashMap;
import java.util.Map;

public class FileConfiguration {

    protected Map<String, Object> configData;

    public FileConfiguration() {
        this.configData = new HashMap<>();
    }

    public Object get(String key) {
        return configData.get(key);
    }

    public String getString(String key) {
        Object value = get(key);
        return (value instanceof String) ? (String) value : null;
    }

    public int getInt(String key) {
        Object value = get(key);
        return (value instanceof Integer) ? (Integer) value : 0;
    }

    public boolean getBoolean(String key) {
        Object value = get(key);
        return (value instanceof Boolean) ? (Boolean) value : false;
    }

    public double getDouble(String key) {
        Object value = get(key);
        return (value instanceof Double) ? (Double) value : 0.0;
    }

    public void set(String key, Object value) {
        configData.put(key, value);
    }
}