package kr.starly.discordbot.entity;

import lombok.Data;

import java.util.List;

@Data
public class PluginInfoDTO {

    private String pluginNameEnglish;
    private String pluginNameKorean;
    private String pluginWikiLink;
    private String pluginVideoLink;
    private List<String> dependency;
    private List<String> managers;
    private String gifLink;
}
