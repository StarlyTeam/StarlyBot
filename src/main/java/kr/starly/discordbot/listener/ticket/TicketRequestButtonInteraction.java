package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.TicketUserDataRepository;
import kr.starly.discordbot.service.TicketInfoService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;
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

    private final TicketInfoService ticketInfoService = DatabaseConfig.getTicketInfoService();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getChannel().asTextChannel().getId().equals(TICKET_CHANNEL_ID)) return;

        long userId = event.getUser().getIdLong();
        String buttonId = event.getButton().getId();

        TicketInfo ticketInfo = ticketInfoService.findByDiscordId(userId);

        if (ticketInfo != null && isExistUserTicket(event.getJDA(), ticketInfo.channelId())) {
            TextChannel textChannel = event.getJDA().getTextChannelById(ticketInfo.channelId());
            String message = textChannel != null ? "이미 티켓이 있습니다! " + textChannel.getAsMention() : "관리자가 티켓을 닫기 전, 채널을 삭제해버렸습니다. 관리자에게 문의 해주세요.";
            event.reply(message).setEphemeral(true).queue();
            return;
        }

        TicketType ticketStatusFormat = TicketType.valueOf(buttonId.replace("button-", "").replace("-", "_").toUpperCase());

        addUserTicketStatus(userId, ticketStatusFormat);

        TicketType ticketStatus = TicketType.getUserTicketStatusMap().get(userId);

        Modal modal = generateModalForType(ticketStatus);
        event.getInteraction().replyModal(modal).queue();
    }

    private void addUserTicketStatus(long userId, TicketType ticketStatus) {
        if (!TicketType.getUserTicketStatusMap().containsKey(userId)) {
            TicketType.getUserTicketStatusMap().put(userId, ticketStatus);
            return;
        }
        TicketType.getUserTicketStatusMap().replace(userId, ticketStatus);
    }

    private Modal generateModalForType(TicketType ticketStatus) {
        Modal modal = null;

        switch (ticketStatus) {
            case NORMAL_TICKET -> {
                TextInput normalTitle = TextInput.create("text-input-normal-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주세요.")
                        .setMaxLength(50)
                        .build();

                TextInput normalDescription = TextInput.create("text-input-normal-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문 내용을 입력해 주세요.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-normal-ticket", "일반 문의")
                        .addComponents(
                                ActionRow.of(normalTitle),
                                ActionRow.of(normalDescription)
                        ).build();
            }

            case QUESTION_TICKET -> {
                TextInput questionTitle = TextInput.create("text-question-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("질문 제목을 입력해 주세요.")
                        .setMaxLength(50)
                        .build();

                TextInput questionType = TextInput.create("text-question-type", "사유", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문 사유를 입력해 주세요. \n" +
                                "제품/서비스 기타 등등 입력")
                        .setMaxLength(10)
                        .build();

                TextInput questionDescription = TextInput.create("text-question-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("질문 내용을 입력해 주세요.")
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

            case CONSULTING_TICKET -> {
                TextInput consultingTitle = TextInput.create("text-consulting-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("상담 제목을 입력하여 주세요")
                        .setMaxLength(50)
                        .build();

                TextInput consultingDescription = TextInput.create("text-consulting-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상담 내용을 입력하여 주세요")
                        .setMaxLength(950)
                        .build();

                TextInput consultingCall = TextInput.create("text-consulting-call", "통화여부", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상담원과 통화로 진행을 원하시면 (네) 를 입력하여 주세요. \n" +
                                "생략시 채팅으로 상담 진행 됩니다. ")
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

            case PURCHASE_INQUIRY_TICKET -> {
                TextInput purchaseInquiryTitle = TextInput.create("text-purchase-inquiry-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주세요")
                        .setMaxLength(50)
                        .build();

                TextInput purchaseInquiryType = TextInput.create("text-purchase-inquiry-type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("결제에 관해 상담하실 사유를 적어 주세요 \n" +
                                "예시) 결제, 환불, 청구 기타 등등 \n" +
                                "만약 사유가 공백일 경우 장난 티켓으로 간주되며 처벌을 받습니다.")
                        .setMaxLength(2)

                        .build();
                TextInput purchaseInquiryDescription = TextInput.create("text-purchase-inquiry-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 적어주세요.")
                        .setMaxLength(750)
                        .build();

                modal = Modal.create("modal-purchase-inquiry-ticket", "결제 문의")
                        .addComponents(
                                ActionRow.of(purchaseInquiryTitle),
                                ActionRow.of(purchaseInquiryType),
                                ActionRow.of(purchaseInquiryDescription)
                        )
                        .build();
            }
            case USE_RESTRICTION_TICKET -> {
                TextInput useRestrictionTitle = TextInput.create("text-use-restriction-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주세요.")
                        .setMaxLength(50)
                        .build();

                TextInput useRestrictionType = TextInput.create("text-use-restriction-type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("사용정지가 된 곳을 입력하여 주세요. \n" +
                                "예시) 플러그인 라이센스, 플러그인 다운로드,경고 이의제기")
                        .setMaxLength(10)
                        .build();

                TextInput useRestrictionDescription = TextInput.create("text-use-restriction-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("정확한 사건 경위를 적어 주세요.")
                        .setMaxLength(890)
                        .build();

                modal = Modal.create("modal-use-restriction-ticket", "이용 제한 문의")
                        .addComponents(
                                ActionRow.of(useRestrictionTitle),
                                ActionRow.of(useRestrictionType),
                                ActionRow.of(useRestrictionDescription)
                        )
                        .build();
            }

            case BUG_REPORT_ETC_TICKET -> {
                TextInput bugReportTicketTitle = TextInput.create("text-bug-report-etc-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주세요")
                        .setMaxLength(50)
                        .build();

                TextInput bugReportTicketTag = TextInput.create("text-bug-report-etc-tag", "태그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("태그를 입력해 주세요 \n" +
                                "예시) 웹 관련 버그, 디스코드 봇 관련 버그 등등")
                        .setMaxLength(10)
                        .build();

                TextInput bugReportTicketDescription = TextInput.create("text-bug-report-etc-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 적어 주세요.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-bug-report-etc-ticket", "기타 버그 문의")
                        .addComponents(
                                ActionRow.of(bugReportTicketTitle),
                                ActionRow.of(bugReportTicketTag),
                                ActionRow.of(bugReportTicketDescription))
                        .build();
            }

            case BUG_REPORT_BUKKIT_TICKET -> {
                TextInput version = TextInput.create("text-bug-report-bukkit-version", "버킷 버젼", TextInputStyle.SHORT)
                        .setPlaceholder("버킷 버젼을 입력해 주세요. (1.12.2 ~ 1.19.2) 중 입력")
                        .setMaxLength(6)
                        .build();

                TextInput log = TextInput.create("text-bug-report-bukkit-log", "버킷 로그", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("버킷 로그를 첨부해 주세요.")
                        .setMaxLength(4000)
                        .setRequired(false)
                        .build();

                TextInput bukkit = TextInput.create("text-bug-report-bukkit-type", "버킷 종류", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("사용하시는 버킷 이름을 입력해 주세요. \n" +
                                "(기본값: spigot)")
                        .setMaxLength(10)
                        .setRequired(false)
                        .build();

                TextInput description = TextInput.create("text-bug-report-bukkit-description", "상황", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("상황 설명을 구체적으로 적어주세요.")
                        .setMaxLength(900)
                        .build();

                modal = Modal.create("modal-bug-report-bukkit-ticket", "플러그인 버그 문의")
                        .addComponents(
                                ActionRow.of(version),
                                ActionRow.of(bukkit),
                                ActionRow.of(log),
                                ActionRow.of(description))
                        .build();
            }

            case ETC_TICKET -> {
                TextInput etcTitle = TextInput.create("text-etc-title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해 주세요.")
                        .setMaxLength(50)
                        .build();
                TextInput etcDescription = TextInput.create("text-etc-description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("본문을 입력해 주세요.")
                        .setMaxLength(900)
                        .build();
                modal = Modal.create("modal-etc-ticket", "기타 문의")
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