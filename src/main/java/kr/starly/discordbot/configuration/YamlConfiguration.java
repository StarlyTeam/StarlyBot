package kr.starly.discordbot.configuration;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class YamlConfiguration extends FileConfiguration {

    public static YamlConfiguration load(String filePath) {
        YamlConfiguration configuration = new YamlConfiguration();

        ensureFileExists(filePath);

        try (FileReader fileReader = new FileReader(filePath)) {
            Yaml yaml = new Yaml();
            configuration.configData = (Map<String, Object>) yaml.load(fileReader);
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