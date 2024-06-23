package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.Color;
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
            PermissionUtil.sendPermissionError(event);
            return;
        }

        MessageEmbed embed;

        switch (subCommand) {
            case "지급" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToAdd = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().addPoint(target.getIdLong(), pointToAdd);
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 지급 완료 | 포인트 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님에게 " + pointToAdd + "포인트를 지급하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "제거" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToRemove = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().removePoint(target.getIdLong(), pointToRemove);
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:success:1168266537262657626> 제거 완료 | 포인트 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님의 " + pointToRemove + "포인트를 제거하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "설정" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                int pointToSet = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseManager.getUserService().setPoint(target.getIdLong(), pointToSet);
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:success:1168266537262657626> 설정 완료 | 포인트 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님의 포인트를 " + pointToSet + "로 설정되었습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "초기화" -> {
                net.dv8tion.jda.api.entities.User target = event.getOption("유저").getAsUser();
                DatabaseManager.getUserService().setPoint(target.getIdLong(), 0);
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 초기화 완료 | 포인트 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님의 포인트를 초기화하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
            }

            default -> {
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 포인트 <a:loading:1168266572847128709>")
                        .setDescription("> **알 수 없는 명령입니다.**")
                        .build();
                event.replyEmbeds(embed).queue();
            }
        }
    }

    @SuppressWarnings("all")
    private void handleCheckCommand(SlashCommandInteractionEvent event) {
        long targetId;
        if (event.getOption("유저") != null) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            targetId = event.getOption("유저").getAsUser().getIdLong();
        } else {
            targetId = event.getUser().getIdLong();
        }

        String userAvatarCheck = event.getJDA().retrieveUserById(targetId).complete().getAvatarUrl();
        int currentPoint = DatabaseManager.getUserService().getPoint(targetId);
        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 확인 | 포인트 <a:loading:1168266572847128709>")
                .setDescription("> **<@" + targetId + ">님의 현재 포인트: " + currentPoint + "**")
                .setThumbnail(userAvatarCheck)
                .build();
        event.replyEmbeds(embed).queue();
    }

    private int getSafeIntFromOption(OptionMapping option) {
        long value = option.getAsLong();
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return 0;
        }
        return (int) value;
    }
}