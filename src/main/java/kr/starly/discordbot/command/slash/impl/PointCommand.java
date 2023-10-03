package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@BotSlashCommand(
        command = "포인트",
        description = "유저의 포인트를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "지급",
                        description = "포인트를 지급합니다.",
                        optionName = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "지급할 포인트를 입력하세요."},
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "제거",
                        description = "포인트를 제거합니다.",
                        optionName = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "제거할 포인트를 입력하세요."},
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "설정",
                        description = "포인트를 설정합니다.",
                        optionName = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "설정할 포인트를 입력하세요."},
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "확인",
                        description = "포인트를 확인합니다.",
                        optionName = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"확인할 유저를 선택하세요. (선택하지 않으면 자신의 포인트를 확인합니다.)"},
                        optionRequired = {false}
                ),
                @BotSlashCommand.SubCommand(
                        name = "초기화",
                        description = "포인트를 초기화합니다.",
                        optionName = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"유저를 선택하세요."},
                        optionRequired = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "순위",
                        description = "포인트 순위를 확인합니다."
                )
        }
)
public class PointCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subCommand = event.getSubcommandName();

        if ("확인".equals(subCommand)) {
            handleCheckCommand(event);
            return;
        }

        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        MessageEmbed messageEmbed;

        switch (subCommand) {
            case "지급" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToAdd = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().addPoint(target.getIdLong(), pointToAdd);
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 지급 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님에게 " + pointToAdd + "포인트를 지급하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "제거" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToRemove = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().removePoint(target.getIdLong(), pointToRemove);
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:success:1141625729386287206> 제거 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님의 " + pointToRemove + "포인트를 제거하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "설정" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToSet = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().setPoint(target.getIdLong(), pointToSet);
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:success:1141625729386287206> 설정 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님의 포인트를 " + pointToSet + "로 설정되었습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "초기화" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                DatabaseManager.getUserService().setPoint(target.getIdLong(), 0);
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 초기화 완료 | 포인트 <a:success:1141625729386287206>")
                        .setDescription("> **" + target.getAsMention() + "님의 포인트를 초기화하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "순위" -> {
                List<User> topUsers = DatabaseManager.getUserService().getTopUsersByPoints(10);
                StringBuilder rankMessage = new StringBuilder();
                AtomicInteger processedUsers = new AtomicInteger(0);

                for (User user : topUsers) {
                    event.getJDA().retrieveUserById(user.discordId()).queue(retrievedUser -> {
                        if (retrievedUser != null) {
                            rankMessage.append("> ")
                                    .append("**")
                                    .append(processedUsers.incrementAndGet())
                                    .append(".")
                                    .append(" <@" + user.discordId() + "> ")
                                    .append(": ")
                                    .append(user.point())
                                    .append(" 포인트**\n");

                            if (processedUsers.get() == topUsers.size()) {
                                MessageEmbed topEmbed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR)
                                        .setTitle("<a:loading:1141623256558866482> 순위 | 포인트 <a:loading:1141623256558866482>")
                                        .setDescription(rankMessage)
                                        .build();
                                event.replyEmbeds(topEmbed).queue();
                            }
                        }
                    });
                }
            }

            default -> {
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1141623256558866482> 오류 | 포인트 <a:loading:1141623256558866482>")
                        .setDescription("> **알 수 없는 명령입니다.**")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }
        }
    }

    @SuppressWarnings("all")
    private void handleCheckCommand(SlashCommandInteractionEvent event) {
        long targetId;
        if (event.getOption("유저") != null) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event.getChannel());
                return;
            }

            targetId = event.getOption("유저").getAsUser().getIdLong();
        } else {
            targetId = event.getUser().getIdLong();
        }

        String userAvatarCheck = event.getJDA().retrieveUserById(targetId).complete().getAvatarUrl();
        int currentPoint = DatabaseManager.getUserService().getPoint(targetId);
        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1141623256558866482> 확인 | 포인트 <a:loading:1141623256558866482>")
                .setDescription("> **<@" + targetId + ">님의 현재 포인트: " + currentPoint + "**")
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