package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.service.PluginFileService;
import net.dv8tion.jda.api.entities.Message;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginFileUtil {

    private PluginFileUtil() {}

    public static List<String> uploadPluginFile(Plugin plugin, List<Message.Attachment> attachments) {
        PluginFileService pluginFileService = DatabaseManager.getPluginFileService();

        List<String> errors = new ArrayList<>();
        for (Message.Attachment attachment : attachments) {
            String fileName = attachment.getFileName();
            if (!List.of("jar", "zip").contains(attachment.getFileExtension())) {
                errors.add("플러그인 파일은 .jar 또는 .zip 형식의 파일만 업로드 가능합니다. [" + fileName + "]");
                continue;
            }

            String[] fileNameSplit = fileName.replace("." + attachment.getFileExtension(), "").split("-");
            if (fileNameSplit.length != 2) {
                errors.add("플러그인 파일명이 올바르지 않습니다. 파일명을 확인해 주세요. [" + fileName + "]");
                continue;
            }

            String mcVersion = fileNameSplit[0];
            String version = fileNameSplit[1];

            File pluginFile = new File(".\\plugin\\%s\\%s.%s".formatted(plugin.getENName(), mcVersion + "-" + version, attachment.getFileExtension()));
            File pluginDir = pluginFile.getParentFile();

            if (!pluginDir.exists()) pluginDir.mkdirs();
            if (!pluginFile.exists()) {
                try {
                    pluginFile.createNewFile();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            attachment.getProxy().downloadToFile(pluginFile);

            pluginFileService.saveData(pluginFile, plugin, MCVersion.valueOf(mcVersion), version);
        }
        return errors;
    }
}