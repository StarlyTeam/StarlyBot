package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.MCVersion;
import lombok.AllArgsConstructor;
import lombok.Getter;
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
}