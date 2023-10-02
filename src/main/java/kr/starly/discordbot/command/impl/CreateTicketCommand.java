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

        StringSelectMenu stringSelectMenu = StringSelectMenu.create("ticket-select-category")
                .setPlaceholder("원하시는 상담 카테고리를 선택해 주세요.")
                .addOptions(SelectOption.of("일반 문의", "normal-ticket")
                        .withDescription("서비스에 대한 일반적인 문의사항을 전달해 주세요.")
                        .withEmoji(Emoji.fromUnicode("ℹ\uFE0F")))

                .addOptions(SelectOption.of("제품 질문", "question-ticket")
                        .withDescription("제품이나 서비스에 대한 궁금한 점을 알려주세요.")
                        .withEmoji(Emoji.fromUnicode("❓")))

                .addOptions(SelectOption.of("맞춤 상담", "consulting-ticket")
                        .withDescription("개인 맞춤형 조언이나 지원을 원하시면 선택해주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDDE3\uFE0F")))

                .addOptions(SelectOption.of("결제 문의", "purchase-inquiry-ticket")
                        .withDescription("결제, 환불, 청구 등에 대한 문의사항을 보내주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")))

                .addOptions(SelectOption.of("서비스 제한 문의", "use-restriction-ticket")
                        .withDescription("계정이나 서비스의 제한 관련 문의를 해주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCCE")))

                .addOptions(SelectOption.of("버그 보고", "bug-report-ticket")
                        .withDescription("시스템에 발견된 오류나 버그에 대해 알려주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDC1E")))

                .addOptions(SelectOption.of("기타 문의", "etc-ticket")
                        .withDescription("기타 분류에 속하지 않는 문의사항을 보내주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDEAB")))
                .build();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("<:ticket:1158139819650711582> 고객센터 문의 | 스탈리 <:ticket:1158139819650711582>")
                .setDescription("> **스탈리 고객센터에 오신 것을 환영합니다.** \n" +
                        "> **여기에서는 귀하의 문의사항이나 건의사항을 기다리고 있습니다.** \n\n" +
                        "─────────────────────────────────────────────────\n" +
                        "> **`\uD83D\uDC5F` 모든 문의는 신속하고 친절하게 처리될 것입니다.** \n" +
                        "> **`\uD83E\uDDE8` 직원에게 폭언이나 성희롱을 할 경우 법적 처벌을 받을 수 있습니다.** \n" +
                        "> **`\uD83D\uDC9D` 스탈리에서 행복한 시간을 보내시길 바랍니다.** \n\u1CBB\n")
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                .build();

        event.getChannel().sendMessageEmbeds(embed).addActionRow(stringSelectMenu).queue();
    }
}