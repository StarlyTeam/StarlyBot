package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.RankUtil;
import kr.starly.discordbot.util.messaging.PaymentLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

@BotEvent
public class RefundListener extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String ID_PREFIX = "refund-";

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith(ID_PREFIX)) return;


        String paymentId;
        Boolean isAccepted = null;
        if (componentId.startsWith(ID_PREFIX + "accept-")) {
            paymentId = componentId.substring((ID_PREFIX + "accept-").length());
            isAccepted = true;
        } else if (componentId.startsWith(ID_PREFIX + "refuse-")) {
            paymentId = componentId.substring((ID_PREFIX + "refuse-").length());
            isAccepted = false;
        } else if (componentId.startsWith(ID_PREFIX + "start-")) {
            paymentId = componentId.substring((ID_PREFIX + "start-").length());
        } else return;

        PaymentService paymentService = DatabaseManager.getPaymentService();
        Payment payment = paymentService.getDataByPaymentId(paymentId.replace("_", "-"));

        if (isAccepted == null) {
            TextInput holder = TextInput.create("holder", "예금주명", TextInputStyle.SHORT)
                    .setPlaceholder("환불계좌의 예금주명을 입력해 주세요.")
                    .setRequired(true)
                    .build();
            TextInput number = TextInput.create("number", "계좌번호", TextInputStyle.SHORT)
                    .setPlaceholder("환불계좌의 계좌번호를 입력해 주세요.")
                    .setRequired(true)
                    .build();
            TextInput bank = TextInput.create("bank", "계좌은행", TextInputStyle.SHORT)
                    .setPlaceholder("환불계좌의 은행을 입력해 주세요.")
                    .setRequired(true)
                    .build();

            String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");
            Modal modal = Modal.create(ID_PREFIX + paymentIdForId, "환불계좌 입력")
                    .addActionRow(holder)
                    .addActionRow(number)
                    .addActionRow(bank)
                    .build();
            event.replyModal(modal).queue();
            return;
        } else if (isAccepted) {
            payment.updateRefundedAt(new Date());
            paymentService.saveData(payment);


            UserService userService = DatabaseManager.getUserService();
            long userId = event.getUser().getIdLong();
            User user = userService.getDataByDiscordId(userId);
            List<Rank> userRanks = new ArrayList<>(user.rank());

            Rank rank2 = RankRepository.getInstance().getRank(2);
            Rank rank3 = RankRepository.getInstance().getRank(3);
            Rank rank4 = RankRepository.getInstance().getRank(4);
            Rank rank5 = RankRepository.getInstance().getRank(5);

            long totalPrice = paymentService.getTotalPaidPrice(userId);
            if (totalPrice < 3000000 && userRanks.contains(rank5)) {
                RankUtil.takeRank(userId, rank5);
            } if (totalPrice < 1000000 && userRanks.contains(rank4)) {
                RankUtil.takeRank(userId, rank4);
            } if (totalPrice < 500000 && userRanks.contains(rank3)) {
                RankUtil.takeRank(userId, rank3);
            } if (totalPrice < 0 && userRanks.contains(rank2)) {
                RankUtil.takeRank(userId, rank2);
            }
        }

        MessageEmbed embed1 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("환불처리 승인이 완료되었습니다.")
                .setDescription("> 승인 결과\n> " + (isAccepted ? "수락" : "거절"))
                .build();
        event.replyEmbeds(embed1)
                .queue();

        PaymentLogger.info(new EmbedBuilder()
                .setTitle("환불처리가 승인되었습니다.")
                .setDescription("> 결제번호: " + payment.getPaymentId() + "\n\n> 승인 결과\n> " + (isAccepted ? "수락" : "거절"))
        );

        MessageEmbed embed2 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("환불요청이 승인되었습니다.")
                .setDescription("<@" + payment.getRequestedBy() + ">님이 요청하신 결제(" + payment.getPaymentId() + ")가 " + (isAccepted ? "수락" : "거절") + "되었습니다.")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                .build();
        event.getJDA().getUserById(payment.getRequestedBy())
                .openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed2))
                .queue(null, (err) -> {
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("DM 안내 메시지를 전송하지 못했습니다.")
                            .setDescription("> <@" + payment.getRequestedBy() + ">")
                            .build();
                    event.getChannel().sendMessageEmbeds(embed3)
                            .queue();
                });
    }

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        if (!modalId.startsWith(ID_PREFIX)) return;

        PaymentService paymentService = DatabaseManager.getPaymentService();
        String paymentId = modalId.substring(ID_PREFIX.length())
                .replace("_", "-");
        Payment payment = paymentService.getDataByPaymentId(paymentId);

        String holder = event.getValue("holder").getAsString();
        String number = event.getValue("number").getAsString();
        String bank = event.getValue("bank").getAsString();

        long userId = event.getUser().getIdLong();
        TicketService ticketService = DatabaseManager.getTicketService();

        JDA jda = DiscordBotManager.getInstance().getJda();
        Category category = jda.getCategoryById(TICKET_CATEGORY_ID);

        long ticketIndex = ticketService.getLastIndex() + 1;
        String userName = jda.getUserById(userId).getEffectiveName();
        TicketType ticketType = TicketType.PAYMENT;
        TextChannel ticketChannel = category.createTextChannel(ticketIndex + "-" + userName + "-" + ticketType.getName())
                .addMemberPermissionOverride(userId, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .complete();

        ticketService.recordTicket(new Ticket(
                userId,
                0,
                ticketChannel.getIdLong(),
                ticketType,
                ticketIndex
        ));

        net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);

        TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
        ticketUserDataRepository.registerUser(userId, user);

        Map<Long, TicketType> userTicketTypeMap = TicketType.getUserTicketTypeMap();
        userTicketTypeMap.put(userId, ticketType);

        TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();
        ticketModalDataRepository.registerModalData(
                ticketChannel.getIdLong(),
                payment.getProduct().getNote(),
                "환불처리 요청",
                """
                > 결제 ID
                > %s
                
                > 결제수단
                > %s
                
                > 환불금액
                > %,d원
                
                > 환불계좌 예금주명
                > %s
                
                > 환불계좌 번호
                > %s
                
                > 환불계좌 은행
                > %s
                """.formatted(
                        payment.getPaymentId().toString(),
                        payment.getMethod().getKRName(),
                        payment.getFinalPrice(),
                        holder,
                        number,
                        bank
                )
        );

        String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");
        Button approveBtn = Button.primary(ID_PREFIX + "accept-" + paymentIdForId, "수락");
        Button rejectBtn = Button.danger(ID_PREFIX + "refuse-" + paymentIdForId, "거절");
        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("환불이 요청되었습니다.")
                .setDescription("승인하시겠습니까?")
                .build();
        ticketChannel.sendMessageEmbeds(embed)
                .setActionRow(approveBtn, rejectBtn)
                .queue();


        MessageEmbed embed2 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                .setDescription("""
                            > **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!**
                            > **%s 곧 답변 드리겠습니다. 감사합니다! 🙏**
                            """
                        .formatted(ticketChannel.getAsMention())
                )
                .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                .build();
        event.replyEmbeds(embed2).setEphemeral(true).queue();
    }
} // TODO: 메시지 작업, 테스트