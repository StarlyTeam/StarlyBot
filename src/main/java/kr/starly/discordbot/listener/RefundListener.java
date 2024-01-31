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
import kr.starly.discordbot.util.security.PermissionUtil;
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
import java.util.List;
import java.util.*;

@BotEvent
public class RefundListener extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final String ID_PREFIX = "refund-";

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (!componentId.startsWith(ID_PREFIX)) return;

        if (componentId.startsWith(ID_PREFIX + "start-")) {
            String paymentIdFromId = componentId.substring((ID_PREFIX + "start-").length());

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

            Modal modal = Modal.create(ID_PREFIX + paymentIdFromId, "환불계좌 입력")
                    .addActionRow(holder)
                    .addActionRow(number)
                    .addActionRow(bank)
                    .build();
            event.replyModal(modal).queue();
            return;
        }

        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String paymentId;
        boolean isAccepted;
        if (componentId.startsWith(ID_PREFIX + "accept-")) {
            paymentId = componentId.substring((ID_PREFIX + "accept-").length());
            isAccepted = true;
        } else if (componentId.startsWith(ID_PREFIX + "refuse-")) {
            paymentId = componentId.substring((ID_PREFIX + "refuse-").length());
            isAccepted = false;
        } else return;

        PaymentService paymentService = DatabaseManager.getPaymentService();
        Payment payment = paymentService.getDataByPaymentId(paymentId.replace("_", "-"));

        if (isAccepted) {
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
            }
            if (totalPrice < 1000000 && userRanks.contains(rank4)) {
                RankUtil.takeRank(userId, rank4);
            }
            if (totalPrice < 500000 && userRanks.contains(rank3)) {
                RankUtil.takeRank(userId, rank3);
            }
            if (totalPrice < 0 && userRanks.contains(rank2)) {
                RankUtil.takeRank(userId, rank2);
            }
        }

        MessageEmbed embed1 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 환불 <a:success:1168266537262657626>")
                .setDescription("""
                        > **환불처리 승인이 완료되었습니다. 🥳**
                        > **승인 결과: %s**          
                        """.formatted(isAccepted ? "<a:success:1168266537262657626>" : "<a:cross:1058939340505497650>")
                )
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                .build();
        event.replyEmbeds(embed1)
                .queue();

        PaymentLogger.info(new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 환불 <a:success:1168266537262657626>")
                .setDescription("""
                                > **환불처리 승인이 완료되었습니다. 🥳**
                                > **승인 결과: %s**
                                > **결제 번호: %s**
                                                            
                                """.formatted(
                                isAccepted ? "<a:success:1168266537262657626>" : "<a:cross:1058939340505497650>",
                                payment.getPaymentId().toString()
                        )
                )
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
        );

        MessageEmbed embed2 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 환불 <a:success:1168266537262657626>")
                .setDescription("""
                        > **환불처리 승인이 완료되었습니다. 🥳**
                        > **승인 결과: %s**
                        > **결제 번호: %s**
                                                    
                        """.formatted(
                        isAccepted ? "<a:success:1168266537262657626>" : "<a:cross:1058939340505497650>",
                        payment.getPaymentId().toString())
                )
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                .build();

        event.getJDA().getUserById(payment.getRequestedBy())
                .openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed2))
                .queue(null, (ignored) -> {
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 환불 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **DM으로 메시지를 전송하지 못했습니다.**
                                    
                                    ─────────────────────────────────────────────────
                                    > **유저: %s**
                                    """.formatted("<@" + payment.getRequestedBy() + ">")
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();

                    event.getChannel().sendMessageEmbeds(embed3).queue();
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
                payment.getProduct().getSummary(),
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
                .setTitle("<a:loading:1168266572847128709> 대기 | 환불 <a:loading:1168266572847128709>")
                .setDescription("""
                        > **환불을 요청하였습니다. 승인하시겠습니까?**
                                                            
                        """
                )
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
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
                .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                .build();
        event.replyEmbeds(embed2).setEphemeral(true).queue();
    }
}