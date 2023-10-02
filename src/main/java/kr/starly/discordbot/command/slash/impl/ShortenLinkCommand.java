package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.service.ShortenLinkService;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

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


    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
        }

        String subCommand = event.getSubcommandName();
        switch (subCommand) {
            case "생성" -> {
                String originUrl = event.getOption("원본링크").getAsString();
                String shortenUrl = event.getOption("단축링크").getAsString();

                ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
                if (shortenLinkService.getDataByShortenUrl(shortenUrl) != null) {
                    event.reply("해당 단축링크는 이미 존재합니다.").queue();
                    return;
                }

                shortenLinkService.saveData(originUrl, shortenUrl);
                event.reply("단축링크를 생성했습니다.").queue();
            }

            case "삭제" -> {
                OptionMapping originUrl = event.getOption("원본링크");
                OptionMapping shortenUrl = event.getOption("단축링크");

                if (originUrl == null && shortenUrl == null) {
                    event.reply("원본링크, 단축링크중 하나 이상은 입력해주세요.").queue();
                    return;
                }

                ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
                if ((
                        originUrl == null ?
                        shortenLinkService.getDataByShortenUrl(shortenUrl.getAsString())
                        : shortenLinkService.getDataByOriginUrl(originUrl.getAsString())
                ) == null) {
                    event.reply("해당 원본링크는 존재하지 않습니다.").queue();
                    return;
                }

                if (originUrl == null) {
                    shortenLinkService.deleteDataByShortenUrl(shortenUrl.getAsString());
                } else {
                    shortenLinkService.deleteDataByOriginUrl(originUrl.getAsString());
                }

                event.reply("단축링크를 삭제했습니다.").queue();
            }

            case "목록" -> {
                ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
                String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");
                String WEB_PORT = configProvider.getString("WEB_PORT");

                StringBuilder sb = new StringBuilder();
                shortenLinkService.getAllData().forEach(shortenLink -> {
                    sb
                            .append(WEB_ADDRESS + ":" + WEB_PORT + "/" + shortenLink.shortenUrl())
                            .append(" (" + shortenLink.originUrl() + ")")
                            .append("\n");
                });

                event.reply("**[단축링크 목록]**\n```\n" + sb + "```").queue();
            }
        }
    }
} // TODO : 메시지 디자인 작업