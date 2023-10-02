package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

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
                    event.reply(target.getAsMention() + "님은 이미 블랙리스트에 등록되어 있습니다.").queue();
                    return;
                }

                blacklistService.saveData(target.getIdLong(), event.getUser().getIdLong(), reason, new Date());

                event.reply(target.getAsMention() + "님을 블랙리스트에 등록했습니다.").queue();
            }

            case "해제" -> {
                User target = event.getOption("유저").getAsUser();

                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                if (blacklistService.getDataByUserId(target.getIdLong()) == null) {
                    event.reply(target.getAsMention() + "님은 이미 블랙리스트에 등록되어 있지 않습니다.").queue();
                    return;
                }

                blacklistService.deleteDataByUserId(target.getIdLong());

                event.reply(target.getAsMention() + "님을 블랙리스트에서 해제했습니다.").queue();
            }

            case "목록" -> {
                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                List<Blacklist> blacklists = blacklistService.getAllData();

                StringBuilder sb = new StringBuilder();
                blacklists.forEach(blacklist -> {
                    sb
                            .append("<@" + blacklist.userId() + "> ")
                            .append("| 사유: " + blacklist.reason())
                            .append("\n");
                });

                event.reply("**[블랙리스트 목록]**\n" + sb).queue();
            }
        }
    }
} // TODO : 메시지 디자인 작업