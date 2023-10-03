package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Plugin;

import java.util.List;


public interface PluginRepository {

    void save(Plugin plugin);
    Plugin findByENName(String ENName);
    List<Plugin> findAll();
    void deleteByENName(String ENName);
}