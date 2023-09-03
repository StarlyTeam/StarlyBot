package kr.starly.discordbot.configuration;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class YamlConfiguration extends FileConfiguration {

    public static YamlConfiguration load(String filePath) {
        YamlConfiguration configuration = new YamlConfiguration();

        ensureFileExists(filePath);

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8)) {
            Yaml yaml = new Yaml();
            configuration.configData = (Map<String, Object>) yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return configuration;
    }

    public void save(String filePath) {
        try (FileWriter fileWriter = new FileWriter(filePath)) {
            Yaml yaml = new Yaml();
            yaml.dump(configData, fileWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void ensureFileExists(String filePath) {
        File file = new File(filePath);

        if (!file.exists()) {
            copyFromResources(filePath);
        }
    }

    private static void copyFromResources(String filePath) {
        try (InputStream inputStream = YamlConfiguration.class.getResourceAsStream("/config.yml")) {
            Files.copy(inputStream, Paths.get(filePath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}