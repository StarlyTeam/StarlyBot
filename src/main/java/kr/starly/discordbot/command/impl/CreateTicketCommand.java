package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.*;

@BotCommand(
        command = "티켓생성",
        description = "티켓 임베드를 생성합니다.",
        usage = "?티켓생성"
)
public class CreateTicketCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed reportEmbed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("플러그인 문의")
                .setDescription("아래 버튼을 눌러 플러그인 문의 사항을 작성 하실 수 있습니다!")
                .build();

        StringSelectMenu stringSelectMenu = StringSelectMenu.create("ticket-selectMenu")
                .addOptions(SelectOption.of("구매문의", "ticket-purchase")
                        .withDescription("구매관련")
                        .withEmoji(Emoji.fromUnicode("\uD83C\uDFAB"))
                )
                .addOptions(SelectOption.of("버그제보", "report-bug")
                        .withDescription("버그관련")
                        .withEmoji(Emoji.fromUnicode("\uD83C\uDF9F\uFE0F"))
                )
                .addOptions(SelectOption.of("일반문의", "ticket-default")
                        .withDescription("기타관련")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCF0")))
                .build();

        event.getChannel().sendMessageEmbeds(reportEmbed).addActionRow(stringSelectMenu).queue();
    }
}