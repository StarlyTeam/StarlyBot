package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.repository.PluginRepository;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;

import java.util.List;

public record PluginService(PluginRepository pluginRepository) {

    public void saveData(
            String ENName,
            String KRName,
            UnicodeEmoji emoji,
            String wikiUrl,
            String iconUrl,
            String videoUrl,
            String gifUrl,
            List<String> dependency,
            List<Long> manager,
            long buyerRole,
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
                version,
                price
        );
        pluginRepository.save(plugin);
    }

    public Plugin getDataByENName(String ENName) {
        return pluginRepository.findByENName(ENName);
    }

    public List<Plugin> getAllData() {
        return pluginRepository.findAll();
    }

    public void deleteDataByENName(String ENName) {
        pluginRepository.deleteByENName(ENName);
    }
}