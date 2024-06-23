package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@BotSlashCommand(
        command = "경고",
        description = "유저의 경고를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "추가",
                        description = "경고를 추가합니다.",
                        optionName = {"유저", "경고", "사유"},
                        optionType = {OptionType.USER, OptionType.INTEGER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "경고를 입력하세요.", "사유를 입력해주세요."},
                        optionRequired = {true, true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "제거",
                        description = "경고를 제거합니다.",
                        optionName = {"유저", "경고", "사유"},
                        optionType = {OptionType.USER, OptionType.INTEGER, OptionType.STRING},
                        optionDescription = {"유저를 선택하세요.", "경고를 입력하세요.", "사유를 입력해주세요."},
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
    private final String WARN_CHANNEL_ID = configProvider.getString("WARN_CHANNEL_ID");

    private final WarnService warnService = DatabaseManager.getWarnService();

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

        switch (subCommand) {
            case "추가" -> {
                User target = event.getOption("유저").getAsUser();
                String reason = event.getOption("사유").getAsString();
                int amount = event.getOption("경고").getAsInt();

                MessageEmbed embed1 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:warn:1168266548541145298> 경고 알림 <a:warn:1168266548541145298>")
                        .setDescription("""
                                        > **%s님에게 경고 %d회가 추가되었습니다.**
                                        > **사유: %s**
                                        """
                                .formatted(target.getAsMention(), amount, reason)
                        )
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.getJDA().getTextChannelById(WARN_CHANNEL_ID).sendMessageEmbeds(embed1).queue();
                target
                        .openPrivateChannel().complete()
                        .sendMessageEmbeds(embed1)
                        .queue(null, (ignored) -> {});

                Warn warn = new Warn(target.getIdLong(), event.getUser().getIdLong(), reason, amount, new Date());
                warnService.saveData(warn);

                // 자동차단
                MessageEmbed embed2 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 추가 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + " 님에게 " + amount + "경고를 추가 하였습니다.**\n" +
                                "> 사유 : " + reason)
                        .setThumbnail(target.getAvatarUrl())
                        .build();

                if (warnService.getTotalWarn(target.getIdLong()) >= 3) {
                    Guild guild = DiscordBotManager.getInstance().getGuild();
                    guild.kick(target)
                            .reason("경고 3회 이상 누적")
                            .queueAfter(5, TimeUnit.SECONDS);

                    BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                    blacklistService.saveData(target.getIdLong(), null, 0, "경고 3회 이상 누적");

                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("<a:success:1168266537262657626> 차단 완료 | 경고 <a:success:1168266537262657626>")
                            .setDescription("> **" + target.getAsMention() + " 님을 성공적으로 차단처리 하였습니다.**\n" +
                                    "> 사유 : 경고 3회 이상 누적")
                            .setThumbnail(target.getAvatarUrl())
                            .build();
                    event.replyEmbeds(embed2, embed3).queue();
                } else {
                    event.replyEmbeds(embed2).queue();
                }
            }

            case "제거" -> {
                User target = event.getOption("유저").getAsUser();
                String reason = event.getOption("사유").getAsString();
                int amount = event.getOption("경고").getAsInt();

                Warn warn = new Warn(target.getIdLong(), event.getUser().getIdLong(), reason, amount * -1, new Date());
                warnService.saveData(warn);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:success:1168266537262657626> 제거 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + ">님의 경고를" + amount + " 제거하였습니다.** \n" +
                                "사유 > `" + reason + "`")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
                target
                        .openPrivateChannel().complete()
                        .sendMessageEmbeds(embed)
                        .queue(null, (ignored) -> {});
            }

            case "설정" -> {
                User target = event.getOption("유저").getAsUser();
                String reason = event.getOption("사유").getAsString();
                int amount = event.getOption("경고").getAsInt();

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:success:1168266537262657626> 설정 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님의 경고를 " + amount + "로 설정 되었습니다.** \n" +
                                "사유 > " + reason)
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
                target
                        .openPrivateChannel().complete()
                        .sendMessageEmbeds(embed)
                        .queue(null, (ignored) -> {});
            }

            case "초기화" -> {
                User target = event.getOption("유저").getAsUser();

                warnService.deleteDataByDiscordId(target.getIdLong());

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 초기화 완료 | 경고 <a:success:1168266537262657626>")
                        .setDescription("> **" + target.getAsMention() + "님의 경고를 초기화 하였습니다.**")
                        .setThumbnail(target.getAvatarUrl())
                        .build();
                event.replyEmbeds(embed).queue();
                target
                        .openPrivateChannel().complete()
                        .sendMessageEmbeds(embed)
                        .queue(null, (ignored) -> {});
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

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 확인 | 경고 <a:loading:1168266572847128709>")
                .setDescription("> **<@" + targetId + ">님의 현재 경고: " + warnService.getTotalWarn(targetId) + "**")
                .setThumbnail(userAvatarCheck)
                .build();
        event.replyEmbeds(embed).queue();
    }
}