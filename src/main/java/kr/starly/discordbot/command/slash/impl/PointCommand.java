package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.UserInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;

@BotSlashCommand(
        command = "포인트",
        description = "유저의 포인트를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "지급",
                        description = "포인트를 지급합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "지급할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "제거",
                        description = "포인트를 제거합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "제거할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "설정",
                        description = "포인트를 설정합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "설정할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "확인",
                        description = "포인트를 확인합니다.",
                        names = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"유저를 선택하세요."},
                        required = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "초기화",
                        description = "포인트를 초기화합니다.",
                        names = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"유저를 선택하세요."},
                        required = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "순위",
                        description = "포인트 순위를 확인합니다."
                )
        }
)
public class PointCommand extends DiscordSlashCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subCommand = event.getSubcommandName();
        EmbedBuilder embedBuilder;

        switch (subCommand) {
            case "지급" -> {
                String userIdForAdd = event.getOption("유저").getAsUser().getId();
                String userAvatarForAdd = event.getOption("유저").getAsUser().getAvatarUrl();
                int pointToAdd = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().addPoint(userIdForAdd, pointToAdd);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 지급 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **<@" + userIdForAdd + ">님에게 " + pointToAdd + "포인트를 지급하였습니다.**")
                        .setThumbnail(userAvatarForAdd);
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            case "제거" -> {
                String userIdForRemove = event.getOption("유저").getAsUser().getId();
                String userAvatarForRemove = event.getOption("유저").getAsUser().getAvatarUrl();
                int pointToRemove = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().removePoint(userIdForRemove, pointToRemove);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:success:1141625729386287206> 제거 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **<@" + userIdForRemove + ">님의 " + pointToRemove + "포인트를 제거하였습니다.**")
                        .setThumbnail(userAvatarForRemove);
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            case "설정" -> {
                String userIdForSet = event.getOption("유저").getAsUser().getId();
                String userAvatarForSet = event.getOption("유저").getAsUser().getAvatarUrl();
                int pointToSet = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().setPoint(userIdForSet, pointToSet);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:success:1141625729386287206> 설정 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **<@" + userIdForSet + ">님의 포인트를 " + pointToSet + "로 설정되었습니다.**")
                        .setThumbnail(userAvatarForSet);
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            case "확인" -> {
                String userIdForCheck = event.getOption("유저").getAsUser().getId();
                String userAvatarCheck = event.getOption("유저").getAsUser().getAvatarUrl();
                int currentPoint = DatabaseConfig.getUserInfoService().getPoint(userIdForCheck);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 확인 | 포인트 <a:loading:1141623256558866482>")
                        .setDescription("> **<@" + userIdForCheck + ">님의 현재 포인트: " + currentPoint + "**")
                        .setThumbnail(userAvatarCheck);
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            case "초기화" -> {
                String userIdForReset = event.getOption("유저").getAsUser().getId();
                String userAvatarReset = event.getOption("유저").getAsUser().getAvatarUrl();
                DatabaseConfig.getUserInfoService().setPoint(userIdForReset, 0);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 초기화 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **<@" + userIdForReset + ">님의 포인트를 초기화하였습니다.**")
                        .setThumbnail(userAvatarReset);
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            case "순위" -> {
                List<UserInfo> topUsers = DatabaseConfig.getUserInfoService().getTopUsersByPoints(10);
                StringBuilder rankMessage = new StringBuilder();
                int rank = 1;
                for (UserInfo user : topUsers) {
                    String username = event.getJDA().getUserById(user.discordId()).getName();
                    rankMessage.append("> ")
                            .append("**" + rank)
                            .append(".")
                            .append(" " + username + " ")
                            .append(": ")
                            .append(user.point())
                            .append(" 포인트**\n");
                    rank++;
                }
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 순위 | 포인트 <a:loading:1141623256558866482>")
                        .setDescription(rankMessage)
                        .setThumbnail("https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=568&height=568");
                event.replyEmbeds(embedBuilder.build()).queue();
            }
            default -> {
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:loading:1141623256558866482> 오류 | 포인트 <a:loading:1141623256558866482>")
                        .setDescription("> **알 수 없는 명령입니다.**");
                event.replyEmbeds(embedBuilder.build()).queue();
            }
        }
    }

    private int getSafeIntFromOption(OptionMapping option) {
        long value = option.getAsLong();
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return 0;
        }
        return (int) value;
    }
}