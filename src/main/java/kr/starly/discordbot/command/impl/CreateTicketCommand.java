package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@BotCommand(
        command = "티켓",
        description = "티켓 임베드를 생성합니다.",
        usage = "?티켓생성"
)
public class CreateTicketCommand extends DiscordCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");


    public void execute(MessageReceivedEvent event) {
        if (!checkAdminPermission(event)) return;

        event.getMessage().delete().queue();

        EmbedBuilder reportEmbed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("플러그인 문의")
                .setDescription("아래 버튼을 눌러 플러그인 문의 사항을 작성 하실 수 있습니다!");

        Button reportButton = Button.danger("ticket-report", "버그제보");

        Button buyButton = Button.success("ticket-buy", "구매 문의");

        Button defaultButton = Button.primary("ticket-default", "구매 문의");

        event.getChannel().sendMessageEmbeds(reportEmbed.build()).addActionRow(reportButton, buyButton, defaultButton).queue();
    }
}