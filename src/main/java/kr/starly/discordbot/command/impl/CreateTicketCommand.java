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
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete().queue();

        StringSelectMenu stringSelectMenu = StringSelectMenu.create("ticket-select-category")
                .setPlaceholder("상담 카테고리를 선택하여 주세요.")
                .addOptions(SelectOption.of("일반", "normal-ticket")
                        .withDescription("서비스 전반에 관한 기본적인 문의를 보내주세요.")
                        .withEmoji(Emoji.fromUnicode("ℹ\uFE0F")))

                .addOptions(SelectOption.of("질문", "question-ticket")
                        .withDescription("제품/서비스에 대한 일반적인 궁금증을 문의하세요.")
                        .withEmoji(Emoji.fromUnicode("❓")))

                .addOptions(SelectOption.of("상담", "consulting-ticket")
                        .withDescription("개인화된 조언이나 지원에 관한 문의입니다.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDDE3\uFE0F")))

                .addOptions(SelectOption.of("결제", "purchase-inquiry-ticket")
                        .withDescription("결제/환불/청구와 관련된 문의를 해주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCB3")))

                .addOptions(SelectOption.of("이용제한", "use-restriction-ticket")
                        .withDescription("계정/서비스의 이용제한에 대한 문의입니다.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDCCE")))

                .addOptions(SelectOption.of("버그", "bug-report-ticket")
                        .withDescription("시스템의 오류나 버그 관련 문의를 해주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDC1E")))

                .addOptions(SelectOption.of("기타", "etc-ticket")
                        .withDescription("기타 분류되지 않은 모든 문의를 보내주세요.")
                        .withEmoji(Emoji.fromUnicode("\uD83D\uDEAB")))
                .build();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                .setTitle("고객센터 주의사항")
                .setDescription("직원에게 폭언, 성희롱을 할 시 법적 조치를 받으실 수 있습니다.")
                .build();

        event.getChannel().sendMessageEmbeds(embed).addActionRow(stringSelectMenu).queue();
    }
}