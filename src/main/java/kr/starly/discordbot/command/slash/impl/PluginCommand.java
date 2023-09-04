package kr.starly.discordbot.command.slash.impl;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.entity.PluginInfo;
import kr.starly.discordbot.repository.PluginRepository;
import kr.starly.discordbot.repository.impl.MongoPluginInfoRepository;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import org.bson.Document;

import java.util.List;

@BotSlashCommand(
        command = "플러그인",
        description = "플러그인을 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "등록",
                        description = "플러그인을 등록합니다.",
                        names = {"플러그인이름", "버전", "다운로드링크", "위키링크", "영상링크"},
                        optionType = {OptionType.STRING, OptionType.STRING, OptionType.STRING, OptionType.STRING, OptionType.STRING},
                        optionDescription = {"플러그인 이름", "버전", "다운로드 링크", "위키 링크", "영상 링크"},
                        required = {true, true, true, true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "삭제",
                        description = "플러그인을 삭제합니다.",
                        names = {"플러그인이름"},
                        optionType = {OptionType.STRING},
                        optionDescription = {"플러그인 이름"},
                        required = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "수정",
                        description = "플러그인 정보를 수정합니다.",
                        names = {"플러그인이름", "버전", "다운로드링크", "위키링크", "영상링크"},
                        optionType = {OptionType.STRING, OptionType.STRING, OptionType.STRING, OptionType.STRING, OptionType.STRING},
                        optionDescription = {"플러그인 이름", "버전 (선택)", "다운로드 링크 (선택)", "위키 링크 (선택)", "영상 링크 (선택)"},
                        required = {true, false, false, false, false}
                ),
                @BotSlashCommand.SubCommand(
                        name = "목록",
                        description = "등록된 플러그인 목록을 보여줍니다."
                ),
                @BotSlashCommand.SubCommand(
                        name = "정보",
                        description = "특정 플러그인의 정보를 보여줍니다.",
                        names = {"플러그인이름"},
                        optionType = {OptionType.STRING},
                        optionDescription = {"조회할 플러그인 이름"},
                        required = {true}
                )
        }
)
public class PluginCommand implements DiscordSlashCommand {

    private final PluginService pluginService;
    private final PluginRepository pluginInfoRepository;

    public PluginCommand() {
        ConfigProvider configProvider = ConfigProvider.getInstance();
        String DB_CONNECTION_STRING = configProvider.getString("DB_HOST");
        String DB_NAME = configProvider.getString("DB_DATABASE");
        String DB_COLLECTION_PLUGIN = configProvider.getString("DB_COLLECTION_PLUGIN");

        MongoClient mongoClient = MongoClients.create(DB_CONNECTION_STRING);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        MongoCollection<Document> collection = database.getCollection(DB_COLLECTION_PLUGIN);

        pluginInfoRepository = new MongoPluginInfoRepository(collection);
        this.pluginService = new PluginService(pluginInfoRepository);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
        }
        String subCommand = event.getSubcommandName();

        switch (subCommand) {
            case "등록" -> registerPlugin(event);
            case "삭제" -> deletePlugin(event);
            case "수정" -> modifyPlugin(event);
            case "목록" -> listPlugins(event);
            case "정보" -> showPluginInfo(event);
            default -> event.reply("알 수 없는 서브 커맨드입니다.").setEphemeral(true).queue();
        }
    }

    private void registerPlugin(SlashCommandInteractionEvent event) {
        String pluginName = event.getOption("플러그인이름").getAsString();
        String version = event.getOption("버전").getAsString();
        String downloadLink = event.getOption("다운로드링크").getAsString();
        String wikiLink = event.getOption("위키링크").getAsString();
        String videoLink = event.getOption("영상링크").getAsString();

        PluginInfo pluginInfo = new PluginInfo(pluginName, version, downloadLink, wikiLink, videoLink);
        pluginService.registerPlugin(pluginInfo);

        event.reply(pluginName + " 플러그인이 등록되었습니다.").queue();
    }

    private void deletePlugin(SlashCommandInteractionEvent event) {
        String pluginName = event.getOption("플러그인이름").getAsString();
        PluginInfo existingPlugin = pluginInfoRepository.findByName(pluginName);

        if (existingPlugin == null) {
            event.reply(pluginName + " 플러그인은 존재하지 않습니다.").queue();
            return;
        }
        pluginService.removePlugin(pluginName);
        event.reply(pluginName + " 플러그인이 삭제되었습니다.").queue();
    }

    private void modifyPlugin(SlashCommandInteractionEvent event) {
        String pluginName = event.getOption("플러그인이름").getAsString();
        PluginInfo existingPlugin = pluginInfoRepository.findByName(pluginName);

        if (existingPlugin == null) {
            event.reply(pluginName + " 플러그인은 존재하지 않습니다.").queue();
            return;
        }

        String version = event.getOption("버전") != null ? event.getOption("버전").getAsString() : null;
        String downloadLink = event.getOption("다운로드링크") != null ? event.getOption("다운로드링크").getAsString() : null;
        String wikiLink = event.getOption("위키링크") != null ? event.getOption("위키링크").getAsString() : null;
        String videoLink = event.getOption("영상링크") != null ? event.getOption("영상링크").getAsString() : null;

        String updatedVersion = version != null ? version : existingPlugin.version();
        String updatedDownloadLink = downloadLink != null ? downloadLink : existingPlugin.downloadLink();
        String updatedWikiLink = wikiLink != null ? wikiLink : existingPlugin.wikiLink();
        String updatedVideoLink = videoLink != null ? videoLink : existingPlugin.videoLink();

        PluginInfo updatedPluginInfo = new PluginInfo(pluginName, updatedVersion, updatedDownloadLink, updatedWikiLink, updatedVideoLink);
        pluginService.modifyPlugin(updatedPluginInfo);
        event.reply(pluginName + " 플러그인 정보가 수정되었습니다.").queue();
    }

    private void listPlugins(SlashCommandInteractionEvent event) {
        List<PluginInfo> plugins = pluginService.getAllPlugins();
        StringBuilder replyMessage = new StringBuilder("등록된 플러그인 목록:\n");
        for (PluginInfo plugin : plugins) {
            replyMessage.append("- ").append(plugin.pluginName()).append(" (버전: ").append(plugin.version()).append(")\n");
        }
        event.reply(replyMessage.toString()).queue();
    }

    private void showPluginInfo(SlashCommandInteractionEvent event) {
        String pluginName = event.getOption("플러그인이름").getAsString();
        PluginInfo pluginInfo = pluginService.getPluginInfo(pluginName);
        if (pluginInfo == null) {
            event.reply(pluginName + " 플러그인은 존재하지 않습니다.").queue();
            return;
        }
        String pluginDetails = String.format(
                "플러그인 이름: %s\n" +
                        "버전: %s\n" +
                        "다운로드 링크: %s\n" +
                        "위키 링크: %s\n" +
                        "영상 링크: %s\n",
                pluginInfo.pluginName(),
                pluginInfo.version(),
                pluginInfo.downloadLink(),
                pluginInfo.wikiLink(),
                pluginInfo.videoLink()
        );
        event.reply(pluginDetails).queue();
    }
}