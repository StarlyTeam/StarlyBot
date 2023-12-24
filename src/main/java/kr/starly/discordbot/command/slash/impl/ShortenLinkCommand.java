package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.service.ShortenLinkService;
import kr.starly.discordbot.util.FileUploadUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;

@BotSlashCommand(
        command = "단축링크",
        description = "단축링크를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "생성",
                        description = "단축링크를 생성합니다.",
                        optionName = {"원본링크", "단축링크"},
                        optionType = {OptionType.STRING, OptionType.STRING},
                        optionDescription = {"단축될 링크를 입력하세요.", "단축된 링크를 입력하세요."},
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "삭제",
                        description = "단축링크를 삭제합니다.",
                        optionName = {"원본링크", "단축링크"},
                        optionType = {OptionType.STRING, OptionType.STRING},
                        optionDescription = {"삭제할 원본링크를 입력하세요.", "삭제할 단축링크를 입력하세요."},
                        optionRequired = {false, false}
                ),
                @BotSlashCommand.SubCommand(
                        name = "목록",
                        description = "단축링크 목록을 확인합니다."
                )
        }
)
public class ShortenLinkCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String subCommand = event.getSubcommandName();
        switch (subCommand) {
            case "생성" -> {
                String originUrl = event.getOption("원본링크").getAsString();
                String shortenUrl = event.getOption("단축링크").getAsString();

                ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
                if (shortenLinkService.getDataByShortenUrl(shortenUrl) != null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 단축링크 <a:loading:1168266572847128709>")
                            .setDescription("> **해당 단축링크는 이미 존재합니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                shortenLinkService.saveData(originUrl, shortenUrl);
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 단축링크 <a:success:1168266537262657626>")
                        .setDescription("> **단축링크를 생성하였습니다.**\n" +
                                "> **`" + originUrl + "` ↔ `" + shortenUrl + "`**"
                        )
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "삭제" -> {
                OptionMapping originUrl = event.getOption("원본링크");
                OptionMapping shortenUrl = event.getOption("단축링크");

                if (originUrl == null && shortenUrl == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 단축링크 <a:loading:1168266572847128709>")
                            .setDescription("> **원본링크, 단축링크 중 하나 이상은 입력해 주세요.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
                if ((
                        originUrl == null ?
                        shortenLinkService.getDataByShortenUrl(shortenUrl.getAsString())
                        : shortenLinkService.getDataByOriginUrl(originUrl.getAsString())
                ) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 단축링크 <a:loading:1168266572847128709>")
                            .setDescription("> **해당 단축링크는 존재하지 않습니다..**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                if (originUrl == null) {
                    shortenLinkService.deleteDataByShortenUrl(shortenUrl.getAsString());
                } else {
                    shortenLinkService.deleteDataByOriginUrl(originUrl.getAsString());
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 단축링크 <a:success:1168266537262657626>")
                        .setDescription("> **단축링크를 삭제하였습니다.**\n" +
                                "> **" + originUrl + " ↔ " + shortenUrl + "**"
                        )
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "목록" -> {
                StringBuilder sb = new StringBuilder();
                List<ShortenLink> shortenLinks = DatabaseManager.getShortenLinkService().getAllData();
                shortenLinks.forEach(shortenLink -> {
                    sb
                            .append("%s%s [%s]".formatted(WEB_ADDRESS, shortenLink.shortenUrl(), shortenLink.originUrl()))
                            .append("\n");
                });

                event.replyFiles(FileUploadUtil.createFileUpload(sb.toString())).queue();
            }
        }
    }
}