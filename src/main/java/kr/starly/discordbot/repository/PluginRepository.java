package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.PluginInfoDTO;

import java.util.List;


public interface PluginRepository {

    void save(PluginInfoDTO pluginInfo);
    void remove(String pluginNameEnglish);
    PluginInfoDTO findByName(String pluginNameEnglish);
    List<PluginInfoDTO> findAll();
    void update(PluginInfoDTO pluginInfo);
}