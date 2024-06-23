package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;

@BotCommand(
        command = "티켓생성",
        description = "티켓 임베드를 생성합니다.",
        usage = "?티켓생성"
)
public class CreateTicketCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getMessage());
            return;
        }

        event.getMessage().delete().queue();

        StringSelectMenu stringSelectMenu = StringSelectMenu.create("ticket-select-category")
                .setPlaceholder("원하시는 상담 카테고리를 선택해주세요.")
                .addOptions(SelectOption.of("일반 문의", "general")
                        .withDescription("서비스 및 주문제작")
                        .withEmoji(Emoji.fromUnicode("ℹ️")))

                .addOptions(SelectOption.of("결제 문의", "payment")
                        .withDescription("결제, 환불, 청구")
                        .withEmoji(Emoji.fromUnicode("💳")))

                .addOptions(SelectOption.of("서비스 제한 문의", "punishment")
                        .withDescription("계정 및 서비스의 제한")
                        .withEmoji(Emoji.fromUnicode("📎")))
                .build();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<:ticket:1039499247172718602> 고객센터 문의 | 스탈리 <:ticket:1039499247172718602>")
                .setDescription("""
                        > **스탈리 고객센터에 오신 것을 환영합니다.**
                        > **여기에서는 귀하의 문의사항이나 건의사항을 기다리고 있습니다.**
                        
                        ─────────────────────────────────────────────────
                        > **`\uD83D\uDC5F` 모든 문의는 신속하고 친절하게 처리될 것입니다.**
                        > **`\uD83E\uDDE8` 직원에게 폭언이나 성희롱을 하실 경우, 법적 처벌을 받을 수 있습니다.**
                        > **`\uD83D\uDC9D` 스탈리에서 행복한 시간을 보내시길 바랍니다.**
                        """
                )
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                .build();

        event.getChannel().sendMessageEmbeds(embed).addActionRow(stringSelectMenu).queue();
    }
}