package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.TicketService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class TicketRequestButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CHANNEL_ID = configProvider.getString("TICKET_CHANNEL_ID");
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (!textChannel.getId().equals(TICKET_CHANNEL_ID)) return;

        long userId = event.getUser().getIdLong();
        String buttonId = event.getComponentId();

        Ticket ticket = ticketService.findByDiscordId(userId);
        TextChannel ticketChannel = event.getJDA().getTextChannelById(ticket.channelId());
        if (ticket != null && ticketChannel == null) {
            ticketService.recordTicket(
                    new Ticket(ticket.openBy(), event.getUser().getIdLong(), ticket.channelId(), ticket.ticketType(), ticket.index())
            );
        }

        TicketType ticketTypeFormat = TicketType.valueOf(buttonId.replace("button-", "").replace("-", "_").toUpperCase());
        addUserTicketStatus(userId, ticketTypeFormat);

        TicketType ticketType = TicketType.getUserTicketTypeMap().get(userId);
        Modal modal = generateModalForType(ticketType);
        event.getInteraction().replyModal(modal).queue();
    }

    private void addUserTicketStatus(long userId, TicketType ticketType) {
        if (!TicketType.getUserTicketTypeMap().containsKey(userId)) {
            TicketType.getUserTicketTypeMap().put(userId, ticketType);
            return;
        }

        TicketType.getUserTicketTypeMap().replace(userId, ticketType);
    }

    private Modal generateModalForType(TicketType ticketType) {
        Modal modal = null;

        switch (ticketType) {
            case GENERAL -> {
                TextInput normalTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput normalDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-general", "일반 문의")
                        .addComponents(
                                ActionRow.of(normalTitle),
                                ActionRow.of(normalDescription)
                        ).build();
            }

            case QUESTION -> {
                TextInput questionTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("질문의 제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput questionType = TextInput.create("type", "사유", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문의 사유를 입력해 주시기 바랍니다.\n(예: 제품/서비스)")
                        .setMaxLength(10)
                        .build();

                TextInput questionDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문의 내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(880)
                        .build();

                modal = Modal.create("modal-question", "질문 하기")
                        .addComponents(
                                ActionRow.of(questionTitle),
                                ActionRow.of(questionType),
                                ActionRow.of(questionDescription)
                        )
                        .build();
            }

            case CONSULTING -> {
                TextInput consultingTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("상담의 제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput consultingDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상담의 내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(950)
                        .build();

                TextInput consultingCall = TextInput.create("call", "통화여부", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("통화를 원하시면 '네'라고 입력해 주시기 바랍니다.")
                        .setMaxLength(1)
                        .setRequired(false)
                        .build();

                modal = Modal.create("modal-consulting", "상담 하기")
                        .addComponents(
                                ActionRow.of(consultingTitle),
                                ActionRow.of(consultingCall),
                                ActionRow.of(consultingDescription)
                        ).build();
            }

            case PAYMENT -> {
                TextInput purchaseInquiryTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput purchaseInquiryType = TextInput.create("type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("결제 관련 상담 사유를 입력해 주시기 바랍니다.\n(예: 결제, 환불, 청구)")
                        .setMaxLength(2)

                        .build();
                TextInput purchaseInquiryDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 적으세요.")
                        .setMaxLength(750)
                        .build();

                modal = Modal.create("modal-payment", "결제 문의")
                        .addComponents(
                                ActionRow.of(purchaseInquiryTitle),
                                ActionRow.of(purchaseInquiryType),
                                ActionRow.of(purchaseInquiryDescription)
                        )
                        .build();
            }
            case PUNISHMENT -> {
                TextInput useRestrictionTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput useRestrictionDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("정확한 사건 경위를 입력해 주시기 바랍니다.")
                        .setMaxLength(890)
                        .build();

                modal = Modal.create("modal-punishment", "처벌 문의")
                        .addComponents(
                                ActionRow.of(useRestrictionTitle),
                                ActionRow.of(useRestrictionDescription)
                        )
                        .build();
            }

            case OTHER_ERROR -> {
                TextInput bugReportTicketTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput bugReportTicketTag = TextInput.create("tag", "태그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("태그를 입력해 주시기 바랍니다.\n(예: 봇/시스템)")
                        .setMaxLength(10)
                        .build();

                TextInput bugReportTicketDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-other-error", "기타 버그 문의")
                        .addComponents(
                                ActionRow.of(bugReportTicketTitle),
                                ActionRow.of(bugReportTicketTag),
                                ActionRow.of(bugReportTicketDescription))
                        .build();
            }

            case PLUGIN_ERROR -> {
                TextInput version = TextInput.create("mcVersion", "버킷 버전", TextInputStyle.SHORT)
                        .setPlaceholder("버킷 버전을 입력해 주시기 바랍니다. (예: 1.12.2/1.19.4)")
                        .setMaxLength(6)
                        .build();

                TextInput log = TextInput.create("log", "버킷 로그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("버킷 로그를 첨부해 주시기 바랍니다.")
                        .setMaxLength(4000)
                        .setRequired(false)
                        .build();

                TextInput bukkit = TextInput.create("type", "버킷 종류", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("사용중인 버킷 이름을 입력해 주시기 바랍니다.\n(예: spigot/paper/purpur)")
                        .setMaxLength(10)
                        .setRequired(false)
                        .build();

                TextInput description = TextInput.create("description", "상황", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-plugin-error", "플러그인 버그 문의")
                        .addComponents(
                                ActionRow.of(version),
                                ActionRow.of(bukkit),
                                ActionRow.of(log),
                                ActionRow.of(description))
                        .build();
            }

            case OTHER -> {
                TextInput etcTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();
                TextInput etcDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("본문을 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();
                modal = Modal.create("modal-other", "기타 문의")
                        .addComponents(
                                ActionRow.of(etcTitle),
                                ActionRow.of(etcDescription)
                        ).build();
            }
        }
        return modal;
    }

    private boolean isExistUserTicket(JDA jda, long channelId) {
        return jda.getTextChannelById(channelId) != null;
    }
}