package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.WarnInfo;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.Color;
import java.util.Date;

@BotSlashCommand(
        command = "경고",
        description = "유저의 경고를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "추가",
                        description = "경고를 추가합니다.",
                        optionName = {"유저", "경고", "사유"},
                        optionType = {OptionType.USER, OptionType.INTEGER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "경고를 입력하세요.", "사유를 입력해 주세요."},
                        optionRequired = {true, true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "제거",
                        description = "경고를 제거합니다.",
                        optionName = {"유저", "경고", "사유"},
                        optionType = {OptionType.USER, OptionType.INTEGER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "경고를 입력하세요.", "사유를 입력해 주세요."},
                        optionRequired = {true, true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "설정",
                        description = "경고를 설정합니다.",
                        optionName = {"유저", "경고"},
                        optionType = {OptionType.USER, OptionType.INTEGER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "설정할 경고를 입력하세요.", "사유를 입력해 주세요."},
                        optionRequired = {true, true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "확인",
                        description = "경고를 확인합니다.",
                        optionName = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"확인할 유저를 선택하세요. (선택하지 않으면 자신의 경고를 확인합니다.)"},
                        optionRequired = {false}
                ),
                @BotSlashCommand.SubCommand(
                        name = "초기화",
                        description = "경고를 초기화합니다.",
                        optionName = {"유저", "사유"},
                        optionType = {OptionType.USER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "사유를 입력해 주세요."},
                        optionRequired = {true, true}
                )
        }
)
public class WarnCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configProvider.getString("EMBED_COLOR_ERROR");

    private final WarnService warnService = DatabaseConfig.getWarnService();

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subCommand = event.getSubcommandName();

        if ("확인".equals(subCommand)) {
            handleCheckCommand(event);
            return;
        }
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
        }

        MessageEmbed messageEmbed;

        switch (subCommand) {
            case "추가" -> {
                User userForAdd = event.getOption("유저").getAsUser();
                String userAvatarForAdd = event.getOption("유저").getAsUser().getAvatarUrl();
                String reason = event.getOption("사유").getAsString();
                int warnToAdd = getSafeIntFromOption(event.getOption("경고"));

                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 지급 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + userForAdd.getAsMention() + " 님에게 " + warnToAdd + "경고를 추가 하였습니다.** \n" +
                                "> 사유 : " + reason)
                        .setThumbnail(userAvatarForAdd)
                        .build();
                event.replyEmbeds(messageEmbed).queue();

                long manager = event.getUser().getIdLong();

                WarnInfo warnInfo = new WarnInfo(userForAdd.getIdLong(), manager, reason, warnToAdd, new Date(System.currentTimeMillis()));
                warnService.addWarn(warnInfo);
            }
            case "제거" -> {
                User userIdForRemove = event.getOption("유저").getAsUser();
                String userAvatarForRemove = event.getOption("유저").getAsUser().getAvatarUrl();
                int warnToRemove = getSafeIntFromOption(event.getOption("경고"));
                String reason = event.getOption("사유").getAsString();

                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:success:1141625729386287206> 제거 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + userIdForRemove.getAsMention() + ">님의 경고를" + warnToRemove + "만큼 제거하였습니다.** \n" +
                                "사유 > `" + reason + "`")
                        .setThumbnail(userAvatarForRemove)
                        .build();

                WarnInfo warnInfo = new WarnInfo(userIdForRemove.getIdLong(), event.getUser().getIdLong(), reason, warnToRemove, new Date(System.currentTimeMillis()));
                warnService.removeWarn(warnInfo);

                event.replyEmbeds(messageEmbed).queue();
            }
            case "설정" -> {
                User userIdForRemove = event.getOption("유저").getAsUser();

                String userAvatarForSet = event.getOption("유저").getAsUser().getAvatarUrl();
                int warnToSet = getSafeIntFromOption(event.getOption("경고"));
                String reason = event.getOption("사유").getAsString();

                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:success:1141625729386287206> 설정 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + userIdForRemove.getAsMention() + "님의 경고를 " + warnToSet + "로 설정 되었습니다.** \n" +
                                "사유 > " + reason)
                        .setThumbnail(userAvatarForSet)
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }
            case "확인" -> handleCheckCommand(event);
            case "초기화" -> {
                User userForRemove = event.getOption("유저").getAsUser();
                String userAvatarReset = event.getOption("유저").getAsUser().getAvatarUrl();

                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 초기화 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + userForRemove.getAsMention() + "님의 경고를 초기화 하였습니다.**")
                        .setThumbnail(userAvatarReset)
                        .build();


                event.replyEmbeds(messageEmbed).queue();
            }
            default -> {
                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:loading:1141623256558866482> 오류 | 경고 <a:loading:1141623256558866482>")
                        .setDescription("> **알 수 없는 명령입니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }
        }
    }

    private void handleCheckCommand(SlashCommandInteractionEvent event) {
        String userIdForCheck;

        if (event.getOption("유저") != null) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event.getChannel());
            }
            userIdForCheck = event.getOption("유저").getAsUser().getId();
        } else {
            userIdForCheck = event.getUser().getId();
        }

        String userAvatarCheck = event.getJDA().retrieveUserById(userIdForCheck).complete().getAvatarUrl();

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("<a:loading:1141623256558866482> 확인 | 경고 <a:loading:1141623256558866482>")
                .setDescription("> **<@" + userIdForCheck + ">님의 현재 경고: " + warnService.getWarn(Long.valueOf(userIdForCheck)) + "**")
                .setThumbnail(userAvatarCheck)
                .build();
        event.replyEmbeds(messageEmbed).queue();
    }

    private int getSafeIntFromOption(OptionMapping option) {
        long value = option.getAsLong();

        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return 0;
        }
        return (int) value;
    }
}