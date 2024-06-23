package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.service.VerifyService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

@BotSlashCommand(
        command = "수동인증",
        description = "수동으로 인증을 진행합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"인증할 유저를 선택해주세요."},
        optionRequired = {true}
)
public class ManualVerifyCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        Member target = event.getOption("유저").getAsMember();
        if (target == null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 수동인증 <a:loading:1168266572847128709>")
                    .setDescription("> **해당 유저는 서버에 존재하지 않습니다.**")
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        }

        UserService userService = DatabaseManager.getUserService();
        if (userService.getDataByDiscordId(target.getIdLong()) != null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 수동인증 <a:loading:1168266572847128709>")
                    .setDescription("> **해당 유저는 이미 인증된 유저입니다.**")
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        }

        userService.saveData(new User(
                target.getIdLong(), "-", new Date(), 0, new ArrayList<>()
        ));

        VerifyService verifyService = DatabaseManager.getVerifyService();
        Verify verify = new Verify(
                "-",
                target.getIdLong(),
                "-",
                false,
                new Date()
        );
        verifyService.saveData(verify);

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 수동인증 <a:success:1168266537262657626>")
                .setDescription("> **해당 유저의 인증이 완료되었습니다.**")
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/Starly/white.png")
                .build();
        event.replyEmbeds(embed).queue();
    }
}