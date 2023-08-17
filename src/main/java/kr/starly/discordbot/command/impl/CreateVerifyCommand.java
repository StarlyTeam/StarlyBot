package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.util.AdminRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@BotCommand(
        command = "인증생성",
        description = "인증 임베드를 생성합니다.",
        usage = "?인증생성"
)
public class CreateVerifyCommand extends DiscordCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!checkAdminRole(event)) return;

        event.getMessage().delete().queue();

        EmbedBuilder verifyEmbed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("테스트 제목")
                .setDescription("테스트 설명");

        Button verify = Button.success("successVerify", "인증하기");
        Button help = Button.danger("helpVerify", "인증이 안되시나요?");
        event.getChannel().sendMessageEmbeds(verifyEmbed.build()).addActionRow(verify, help).queue();
    }
}