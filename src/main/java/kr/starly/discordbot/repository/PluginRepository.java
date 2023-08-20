package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.PluginInfo;

import java.util.List;

public interface PluginRepository {

    void save(PluginInfo pluginInfo);
    void remove(String pluginName);
    PluginInfo findByName(String pluginName);
    List<PluginInfo> findAll();
    void update(PluginInfo pluginInfo);
}