package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.Date;
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
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");
    private final String EMBED_COLOR_ERROR = configProvider.getString("EMBED_COLOR_ERROR");
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
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
                            .setColor(Color.decode(EMBED_COLOR_ERROR))
                            .setTitle("<a:loading:1141623256558866482> 오류 | 등록된 유저 <a:loading:1141623256558866482>")
                            .setDescription("> **" + target.getAsMention() + "님은 이미 블랙리스트에 등록되어 있습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                blacklistService.saveData(target.getIdLong(), event.getUser().getIdLong(), reason, new Date());

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 성공 | 블랙리스트 등록 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님을 블랙리스트에 등록하였습니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "해제" -> {
                User target = event.getOption("유저").getAsUser();

                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                if (blacklistService.getDataByUserId(target.getIdLong()) == null) {

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(Color.decode(EMBED_COLOR_ERROR))
                            .setTitle("<a:loading:1141623256558866482> 오류 | 미등록 유저 <a:loading:1141623256558866482>")
                            .setDescription("> **" + target.getAsMention() + "님은 블랙리스트에 등록되어 있지 않습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                blacklistService.deleteDataByUserId(target.getIdLong());

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 성공 | 블랙리스트 해제 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님을 블랙리스트에서 해제하였습니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "목록" -> {
                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                List<Blacklist> blacklists = blacklistService.getAllData();

                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 블랙리스트 목록 <a:loading:1141623256558866482>");

                StringBuilder descriptionBuilder = new StringBuilder("> **아래는 현재 블랙리스트에 등록된 유저들의 목록입니다.**\n\n");
                blacklists.forEach(blacklist -> descriptionBuilder
                        .append("> **유저:** ")
                        .append("<@")
                        .append(blacklist.userId())
                        .append(">\n> **사유: `")
                        .append(blacklist.reason() + "`**")
                        .append("\n\n"));

                embedBuilder.setDescription(descriptionBuilder.toString());
                event.replyEmbeds(embedBuilder.build()).queue();
            }
        }
    }
}