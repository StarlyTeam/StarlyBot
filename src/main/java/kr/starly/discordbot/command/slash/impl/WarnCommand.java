package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.security.PermissionUtil;
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
                        optionDescription = {"유저를 선택하세요.", "설정할 경고를 입력하세요.", "사유를 입력하세요."},
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
                        optionDescription = {"유저를 선택하세요.", "사유를 입력하세요."},
                        optionRequired = {true, true}
                )
        }
)
public class WarnCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final WarnService warnService = DatabaseManager.getWarnService();

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
            case "추가" -> {
                User userForAdd = event.getOption("유저").getAsUser();
                String userAvatarForAdd = event.getOption("유저").getAsUser().getAvatarUrl();
                String reason = event.getOption("사유").getAsString();
                int warnToAdd = getSafeIntFromOption(event.getOption("경고"));

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 추가 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + userForAdd.getAsMention() + " 님에게 " + warnToAdd + "경고를 추가 하였습니다.** \n" +
                                "> 사유 : " + reason)
                        .setThumbnail(userAvatarForAdd)
                        .build();
                event.replyEmbeds(messageEmbed).queue();

                long manager = event.getUser().getIdLong();

                Warn warn = new Warn(userForAdd.getIdLong(), manager, reason, warnToAdd, new Date(System.currentTimeMillis()));
                warnService.saveData(warn);

                try {
                    userForAdd.openPrivateChannel().queue(
                            privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue()
                    );
                } catch (UnsupportedOperationException ignored) {}
            }

            case "제거" -> {
                User userForRemove = event.getOption("유저").getAsUser();
                String userAvatarForRemove = event.getOption("유저").getAsUser().getAvatarUrl();
                int removeAmount = getSafeIntFromOption(event.getOption("경고"));
                String reason = event.getOption("사유").getAsString();

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:success:1168266537262657626> 제거 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + userForRemove.getAsMention() + ">님의 경고를" + removeAmount + " 제거하였습니다.** \n" +
                                "사유 > `" + reason + "`")
                        .setThumbnail(userAvatarForRemove)
                        .build();

                Warn warn = new Warn(userForRemove.getIdLong(), event.getUser().getIdLong(), reason, removeAmount * -1, new Date(System.currentTimeMillis()));
                warnService.saveData(warn);

                event.replyEmbeds(messageEmbed).queue();

                try {
                    userForRemove.openPrivateChannel().queue(
                            privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue()
                    );
                } catch (UnsupportedOperationException ignored) {}
            }

            case "설정" -> {
                User userForRemove = event.getOption("유저").getAsUser();

                String userAvatarForSet = event.getOption("유저").getAsUser().getAvatarUrl();
                int warnToSet = getSafeIntFromOption(event.getOption("경고"));
                String reason = event.getOption("사유").getAsString();

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:success:1168266537262657626> 설정 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + userForRemove.getAsMention() + "님의 경고를 " + warnToSet + "로 설정 되었습니다.** \n" +
                                "사유 > " + reason)
                        .setThumbnail(userAvatarForSet)
                        .build();
                event.replyEmbeds(messageEmbed).queue();

                try {
                    userForRemove.openPrivateChannel().queue(
                            privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue()
                    );
                } catch (UnsupportedOperationException ignored) {}
            }

            case "초기화" -> {
                User userForRemove = event.getOption("유저").getAsUser();
                String userAvatarReset = event.getOption("유저").getAsUser().getAvatarUrl();

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 초기화 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + userForRemove.getAsMention() + "님의 경고를 초기화 하였습니다.**")
                        .setThumbnail(userAvatarReset)
                        .build();


                event.replyEmbeds(messageEmbed).queue();

                try {
                    userForRemove.openPrivateChannel().queue(
                            privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).queue()
                    );
                } catch (UnsupportedOperationException ignored) {}
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

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 확인 | 경고 <a:loading:1168266572847128709>")
                .setDescription("> **<@" + targetId + ">님의 현재 경고: " + warnService.getTotalWarn(targetId) + "**")
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