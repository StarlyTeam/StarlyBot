package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.MCVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
@AllArgsConstructor
public class PluginFile {

        @NotNull File file;
        @NotNull Plugin plugin;
        @NotNull MCVersion mcVersion;
        @NotNull String version;

        public void updateFile(@NotNull File file) {
            this.file = file;
        }

        public void updatePlugin(@NotNull Plugin plugin) {
            this.plugin = plugin;
        }

        public void updateMCVersion(@NotNull MCVersion mcVersion) {
            this.mcVersion = mcVersion;
        }

        public void updateVersion(@NotNull String version) {
            this.version = version;
        }

        public Document serialize() {
            Document document = new Document();
            document.put("file", file.getAbsolutePath());
            document.put("plugin", plugin.serialize());
            document.put("mcVersion", mcVersion.toString());
            document.put("version", version);
            return document;
        }

        public static PluginFile deserialize(Document document) {
            if (document == null) return null;

            File file = new File(document.getString("file"));
            Plugin plugin = Plugin.deserialize(document.get("plugin", Document.class));
            MCVersion mcVersion = MCVersion.valueOf(document.getString("mcVersion"));
            String version = document.getString("version");
            return new PluginFile(file, plugin, mcVersion, version);
        }
}