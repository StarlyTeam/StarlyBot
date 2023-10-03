package kr.starly.discordbot.entity;

import kr.starly.discordbot.enums.MCVersion;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public record PluginFile(
        @NotNull File file,
        @NotNull Plugin plugin,
        @NotNull MCVersion mcVersion,
        @NotNull String version
) {}