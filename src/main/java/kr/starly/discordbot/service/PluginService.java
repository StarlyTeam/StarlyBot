package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;

import java.util.List;

public record PluginService(PluginRepository repository) {

    public void saveData(Plugin plugin) {
        repository.put(plugin);
    }

    public Plugin getDataByENName(String ENName) {
        return repository.findByENName(ENName);
    }

    public List<Plugin> getAllData() {
        return repository.findAll();
    }

    public void deleteDataByENName(String ENName) {
        repository.deleteByENName(ENName);
    }
}