package kr.starly.discordbot.entity;

import java.util.List;

public record Plugin(
        String pluginNameEN,
        String pluginNameKR,
        String pluginWikiLink,
        String pluginVideoLink,
        String gifLink,
        List<String> dependency,
        List<Long> manager,
        int price
) {}