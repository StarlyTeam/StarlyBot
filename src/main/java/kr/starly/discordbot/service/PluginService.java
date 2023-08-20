package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.PluginInfo;
import kr.starly.discordbot.repository.PluginRepository;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class PluginService {

    private final PluginRepository pluginRepository;

    public void registerPlugin(PluginInfo pluginInfo) {
        pluginRepository.save(pluginInfo);
    }

    public void modifyPlugin(PluginInfo updatedPluginInfo) {
        pluginRepository.update(updatedPluginInfo);
    }

    public void removePlugin(String pluginName) {
        pluginRepository.remove(pluginName);
    }

    public PluginInfo getPluginInfo(String pluginName) {
        return pluginRepository.findByName(pluginName);
    }

    public List<PluginInfo> getAllPlugins() {
        return pluginRepository.findAll();
    }
}