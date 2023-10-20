package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;

import java.util.List;

public record PluginService(PluginRepository repository) {

    public void saveData(
            String ENName,
            String KRName,
            String emoji,
            String wikiUrl,
            String iconUrl,
            String videoUrl,
            String gifUrl,
            List<String> dependency,
            List<Long> manager,
            long buyerRole,
            long threadId,
            String version,
            int price
    ) {
        Plugin plugin = new Plugin(
                ENName,
                KRName,
                emoji,
                wikiUrl,
                iconUrl,
                videoUrl,
                gifUrl,
                dependency,
                manager,
                buyerRole,
                threadId,
                version,
                price
        );
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