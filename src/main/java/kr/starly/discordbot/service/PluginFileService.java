package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.repository.PluginFileRepository;

import java.io.File;
import java.io.IOException;
import java.util.List;

public record PluginFileService(PluginFileRepository pluginFileRepository) {

    public void saveData(File file, Plugin plugin, MCVersion mcVersion, String version) {
        PluginFile pluginFile = new PluginFile(file, plugin, mcVersion, version);

        try {
            pluginFileRepository.put(pluginFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<PluginFile> getData(String ENName) {
        return pluginFileRepository.findMany(ENName);
    }

    public List<PluginFile> getData(String ENName, String version) {
        return pluginFileRepository.findMany(ENName, version);
    }

    public PluginFile getData(String ENName, MCVersion mcVersion, String version) {
        return pluginFileRepository.findOne(ENName, mcVersion, version);
    }

    public void deleteData(String pluginNameEN) {
        pluginFileRepository.deleteMany(pluginNameEN);
    }

    public void deleteData(String pluginNameEN, String version) {
        pluginFileRepository.deleteMany(pluginNameEN, version);
    }

    public void deleteData(String pluginNameEN, MCVersion mcVersion, String version) {
        pluginFileRepository.deleteOne(pluginNameEN, mcVersion, version);
    }
}