package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Plugin;

import java.util.List;


public interface PluginRepository {

    void save(Plugin pluginInfo);
    void deleteByPluginNameEN(String pluginNameEnglish);
    Plugin findByName(String pluginNameEnglish);
    List<Plugin> findAll();
}