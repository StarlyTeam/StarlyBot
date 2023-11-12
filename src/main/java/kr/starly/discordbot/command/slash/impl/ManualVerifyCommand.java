package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.service.VerifyService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
        Member target = event.getOption("유저").getAsMember();
        if (target == null) {
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("해당 유저는 서버에 존재하지 않습니다.")
                            .build()
            ).queue();
            return;
        }

        UserService userService = DatabaseManager.getUserService();
        if (userService.getDataByDiscordId(target.getIdLong()) != null) {
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("해당 유저는 이미 인증된 유저입니다.")
                            .build()
            ).queue();
            return;
        }

        userService.saveData(
                target.getIdLong(),
                "-",
                new Date(),
                0,
                new ArrayList<>()
        );

        VerifyService verifyService = DatabaseManager.getVerifyService();
        Verify verify = new Verify(
                "-",
                target.getIdLong(),
                "-",
                false,
                new Date()
        );
        verifyService.saveData(verify);

        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("제목")
                        .setDescription("해당 유저의 인증이 완료되었습니다.")
                        .build()
        ).queue();
    }
} // TODO: 메시지 작업, 테스트