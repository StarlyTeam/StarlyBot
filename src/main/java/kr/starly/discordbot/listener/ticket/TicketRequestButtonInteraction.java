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

@BotEvent
public class TicketRequestButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CHANNEL_ID = configProvider.getString("TICKET_CHANNEL_ID");

    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (!textChannel.getId().equals(TICKET_CHANNEL_ID)) return;

        long userId = event.getUser().getIdLong();
        String buttonId = event.getComponentId();

        Ticket Ticket = ticketService.findByDiscordId(userId);

        if (Ticket != null && isExistUserTicket(event.getJDA(), Ticket.channelId())) {
            TextChannel ticketChannel = event.getJDA().getTextChannelById(Ticket.channelId());
            String message = ticketChannel != null ? "이미 티켓이 있습니다! " + ticketChannel.getAsMention() : "관리자가 티켓을 닫기 전, 채널을 삭제해버렸습니다. 관리자에게 문의 해 주세요.";
            event.reply(message).setEphemeral(true).queue();
            return;
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
                TextInput normalTitle = TextInput.create("text-input-normal-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput normalDescription = TextInput.create("text-input-normal-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-general-ticket", "일반 문의")
                        .addComponents(
                                ActionRow.of(normalTitle),
                                ActionRow.of(normalDescription)
                        ).build();
            }

            case QUESTION -> {
                TextInput questionTitle = TextInput.create("text-question-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("질문의 제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput questionType = TextInput.create("text-question-type", "사유", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문의 사유를 입력해 주시기 바랍니다.\n(예: 제품/서비스)")
                        .setMaxLength(10)
                        .build();

                TextInput questionDescription = TextInput.create("text-question-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문의 내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(880)
                        .build();

                modal = Modal.create("modal-question-ticket", "질문 하기")
                        .addComponents(
                                ActionRow.of(questionTitle),
                                ActionRow.of(questionType),
                                ActionRow.of(questionDescription)
                        )
                        .build();
            }

            case CONSULTING -> {
                TextInput consultingTitle = TextInput.create("text-consulting-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("상담의 제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput consultingDescription = TextInput.create("text-consulting-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상담의 내용을 입력해 주시기 바랍니다.")
                        .setMaxLength(950)
                        .build();

                TextInput consultingCall = TextInput.create("text-consulting-call", "통화여부", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("통화를 원하시면 '네'라고 입력해 주시기 바랍니다.")
                        .setMaxLength(1)
                        .setRequired(false)
                        .build();

                modal = Modal.create("modal-consulting-ticket", "상담 하기")
                        .addComponents(
                                ActionRow.of(consultingTitle),
                                ActionRow.of(consultingCall),
                                ActionRow.of(consultingDescription)
                        ).build();
            }

            case PAYMENT -> {
                TextInput purchaseInquiryTitle = TextInput.create("text-payment-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput purchaseInquiryType = TextInput.create("text-payment-type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("결제 관련 상담 사유를 입력해 주시기 바랍니다.\n(예: 결제, 환불, 청구)")
                        .setMaxLength(2)

                        .build();
                TextInput purchaseInquiryDescription = TextInput.create("text-payment-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 적으세요.")
                        .setMaxLength(750)
                        .build();

                modal = Modal.create("modal-payment-ticket", "결제 문의")
                        .addComponents(
                                ActionRow.of(purchaseInquiryTitle),
                                ActionRow.of(purchaseInquiryType),
                                ActionRow.of(purchaseInquiryDescription)
                        )
                        .build();
            }
            case PUNISHMENT -> {
                TextInput useRestrictionTitle = TextInput.create("text-punishment-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput useRestrictionType = TextInput.create("text-punishment-type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("사용 정지가 된 곳을 입력해 주시기 바랍니다.\n(예: 다운로드/경고)")
                        .setMaxLength(10)
                        .build();

                TextInput useRestrictionDescription = TextInput.create("text-punishment-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("정확한 사건 경위를 입력해 주시기 바랍니다.")
                        .setMaxLength(890)
                        .build();

                modal = Modal.create("modal-punishment-ticket", "처벌 문의")
                        .addComponents(
                                ActionRow.of(useRestrictionTitle),
                                ActionRow.of(useRestrictionType),
                                ActionRow.of(useRestrictionDescription)
                        )
                        .build();
            }

            case OTHER_ERROR -> {
                TextInput bugReportTicketTitle = TextInput.create("text-bug-report-other-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput bugReportTicketTag = TextInput.create("text-bug-report-other-tag", "태그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("태그를 입력해 주시기 바랍니다.\n(예: 봇/시스템)")
                        .setMaxLength(10)
                        .build();

                TextInput bugReportTicketDescription = TextInput.create("text-bug-report-other-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-bug-report-other-ticket", "기타 버그 문의")
                        .addComponents(
                                ActionRow.of(bugReportTicketTitle),
                                ActionRow.of(bugReportTicketTag),
                                ActionRow.of(bugReportTicketDescription))
                        .build();
            }

            case PLUGIN_ERROR -> {
                TextInput version = TextInput.create("text-error-mcVersion", "버킷 버전", TextInputStyle.SHORT)
                        .setPlaceholder("버킷 버전을 입력해 주시기 바랍니다. (예: 1.12.2/1.19.4)")
                        .setMaxLength(6)
                        .build();

                TextInput log = TextInput.create("text-error-log", "버킷 로그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("버킷 로그를 첨부해 주시기 바랍니다.")
                        .setMaxLength(4000)
                        .setRequired(false)
                        .build();

                TextInput bukkit = TextInput.create("text-error-type", "버킷 종류", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("사용중인 버킷 이름을 입력해 주시기 바랍니다.\n(예: spigot/paper/purpur)")
                        .setMaxLength(10)
                        .setRequired(false)
                        .build();

                TextInput description = TextInput.create("text-error-description", "상황", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-error-ticket", "플러그인 버그 문의")
                        .addComponents(
                                ActionRow.of(version),
                                ActionRow.of(bukkit),
                                ActionRow.of(log),
                                ActionRow.of(description))
                        .build();
            }

            case OTHER -> {
                TextInput etcTitle = TextInput.create("text-other-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();
                TextInput etcDescription = TextInput.create("text-other-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("본문을 입력해 주시기 바랍니다.")
                        .setMaxLength(900)
                        .build();
                modal = Modal.create("modal-other-ticket", "기타 문의")
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