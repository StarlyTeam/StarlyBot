package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.*;

@BotSlashCommand(
        command = "유저정보",
        description = "유저정보를 조회합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"정보를 조회할 유저를 입력하세요."},
        optionRequired = {true}
)
public class UserInfoCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        Guild guild = DiscordBotManager.getInstance().getGuild();

        net.dv8tion.jda.api.entities.User discordUser = event.getOption("유저").getAsUser();

        UserService userService = DatabaseManager.getUserService();
        User user = userService.getDataByDiscordId(discordUser.getIdLong());

        WarnService warnService = DatabaseManager.getWarnService();
        PaymentService paymentService = DatabaseManager.getPaymentService();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 유저정보 <a:success:1168266537262657626>")
                .setDescription("""
                        > **유저: %s**
                        > **밴 여부: %s**
                        
                        > **참가일: %s**
                        > **인증일: %s**
                        > **경고: %d개**
                        > **IP: %s**
                        
                        > **포인트: %,d**
                        > **총 구매액: %,d원**
                        """.formatted(
                        discordUser.getAsMention(),
                        guild.retrieveBan(discordUser).mapToResult().complete().isSuccess() ? "O" : "X",
                        TimeFormat.DATE_TIME_SHORT.now().toString(),
                        TimeFormat.DATE_TIME_SHORT.atInstant(user.verifiedAt().toInstant()).toString(),
                        warnService.getTotalWarn(discordUser.getIdLong()),
                        user.ip(),
                        user.point(),
                        paymentService.getTotalPaidPrice(discordUser.getIdLong())
                ))
                .build();
        event.replyEmbeds(embed).queue();
    }
}