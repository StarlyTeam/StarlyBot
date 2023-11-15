package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.util.FileUploadUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@BotSlashCommand(
        command = "블랙리스트",
        description = "블랙리스트를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "등록",
                        description = "유저를 블랙리스트에 등록합니다.",
                        optionName = {"유저", "사유"},
                        optionType = {OptionType.MENTIONABLE, OptionType.STRING},
                        optionDescription = {"블랙리스트에 등록할 유저를 입력하세요.", "블랙리스트에 등록할 사유를 입력하세요."},
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "해제",
                        description = "유저를 블랙리스트에서 해제합니다.",
                        optionName = {"유저"},
                        optionType = {OptionType.MENTIONABLE},
                        optionDescription = {"블랙리스트에서 해제할 유저를 입력하세요."},
                        optionRequired = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "목록",
                        description = "블랙리스트 목록을 확인합니다."
                )
        }
)
public class BlacklistCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String subCommand = event.getSubcommandName();
        switch (subCommand) {
            case "등록" -> {
                User target = event.getOption("유저").getAsUser();
                String reason = event.getOption("사유").getAsString();

                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                if (blacklistService.getDataByUserId(target.getIdLong()) != null) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 등록된 유저 <a:loading:1168266572847128709>")
                            .setDescription("> **" + target.getAsMention() + "님은 이미 블랙리스트에 등록되어 있습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                blacklistService.saveData(target.getIdLong(), null, event.getUser().getIdLong(), reason);

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 블랙리스트 등록 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님을 블랙리스트에 등록하였습니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "해제" -> {
                User target = event.getOption("유저").getAsUser();

                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                if (blacklistService.getDataByUserId(target.getIdLong()) == null) {

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 미등록 유저 <a:loading:1168266572847128709>")
                            .setDescription("> **" + target.getAsMention() + "님은 블랙리스트에 등록되어 있지 않습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                blacklistService.deleteDataByUserId(target.getIdLong());

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 블랙리스트 해제 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님을 블랙리스트에서 해제하였습니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "목록" -> {
                StringBuilder sb = new StringBuilder();
                List<Blacklist> blacklists = DatabaseManager.getBlacklistService().getAllData();
                blacklists.forEach(blacklist -> sb
                        .append("디스코드: %s | IP: %s [%s, %s]".formatted(blacklist.userId(), blacklist.ipAddress(), blacklist.reason(), DATE_FORMAT.format(blacklist.listedAt())))
                        .append("\n"));

                event.replyFiles(FileUploadUtil.createFileUpload(sb.toString())).queue();
            }
        }
    }
}