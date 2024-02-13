package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.*;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.entity.payment.impl.BankTransferPayment;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;
import kr.starly.discordbot.entity.payment.impl.CulturelandPayment;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.entity.product.impl.CustomPriceProduct;
import kr.starly.discordbot.entity.product.impl.OutSourcingProduct;
import kr.starly.discordbot.entity.product.impl.PremiumPluginProduct;
import kr.starly.discordbot.entity.rank.perk.impl.CashbackPerk;
import kr.starly.discordbot.enums.*;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.*;
import kr.starly.discordbot.util.RankUtil;
import kr.starly.discordbot.util.external.TossPaymentsUtil;
import kr.starly.discordbot.util.messaging.AuditLogger;
import kr.starly.discordbot.util.messaging.PaymentLogger;
import kr.starly.discordbot.util.security.AESUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;


@BotEvent
public class BuyListener extends ListenerAdapter { // 코드 꼬라지..

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");
    private final String VERIFIED_ROLE_ID = configProvider.getString("VERIFIED_ROLE_ID");

    private final Map<Long, Product> productMap = new HashMap<>();
    private final Map<Long, Coupon> couponMap = new HashMap<>();
    private final Map<Long, Integer> pointMap = new HashMap<>();

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final String ID_PREFIX = "payment-";
    private final int POINT_USE_MINIMUM = 1000;
    private final int POINT_USE_UNIT = 100;

    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        List<SelectOption> selectedOptions = event.getSelectedOptions();

        String componentId = event.getComponentId();
        if (componentId.equals(ID_PREFIX + "payment-method")) {
            // 변수 선언
            boolean isPure = true;

            // 무결성 검증1
            Product product = productMap.get(userId);
            if (product == null) isPure = false;
            else if (product.getPrice() == 0) isPure = false;

            if (!isPure) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("""
                                > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                
                                ─────────────────────────────────────────────────
                                > **오류 코드: {MAL1}**
                                """
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                stopSession(userId);
                return;
            }

            // 무결성 검증2
            Coupon coupon = couponMap.get(userId);
            if (coupon != null) {
                Discount discount = coupon.getDiscount();
                if (discount.getType() == DiscountType.FIXED
                        && discount.getValue() > product.getPrice()) {
                    isPure = false;
                } else {
                    if (!coupon.isUsable(userId, product)) isPure = false;
                }
            }

            if (!isPure) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("""
                                > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                
                                ─────────────────────────────────────────────────
                                > **오류 코드: {MAL2}**
                                """
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                stopSession(userId);
                return;
            }

            // 무결성 검증3
            int price = product.getPrice();
            if (coupon != null) price = coupon.getDiscount().computeFinalPrice(price);

            int point = pointMap.get(userId);
            if (point < 0) isPure = false;
            else if (point != 0) {
                if (point % POINT_USE_UNIT != 0) isPure = false;
                else if (point < POINT_USE_MINIMUM) isPure = false;
                else if (point > price) isPure = false;
            }

            if (!isPure) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("""
                                > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                
                                ─────────────────────────────────────────────────
                                > **오류 코드: {MAL3}**
                                """
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                stopSession(userId);
                return;
            }

            // 결제 처리
            String paymentMethodStr = selectedOptions.get(0).getValue();
            PaymentMethod paymentMethod = PaymentMethod.valueOf(paymentMethodStr.toUpperCase());

            switch (paymentMethod) {
                case CREDIT_CARD -> {
                    PaymentService paymentService = DatabaseManager.getPaymentService();
                    List<Payment> recentPayments = paymentService.getDataByUserId(userId).stream()
                            .filter(Payment::isAccepted)
                            .filter(payment -> payment.getMethod() == PaymentMethod.CREDIT_CARD)
                            .toList();

                    CreditCardPayment recentPayment = recentPayments.isEmpty() ? null : recentPayments.get(recentPayments.size() - 1).asCreditCard();

                    TextInput cardNumber = TextInput.create("card-number", "카드번호", TextInputStyle.SHORT)
                            .setMinLength(10)
                            .setMaxLength(20)
                            .setPlaceholder("카드번호를 입력해주세요.")
                            .setValue(recentPayment == null ? null : recentPayment.getCardNumber())
                            .setRequired(true)
                            .build();
                    TextInput cardExpirationMonth = TextInput.create("card-expiration-month", "유효기간 (월)", TextInputStyle.SHORT)
                            .setMinLength(1)
                            .setMaxLength(2)
                            .setPlaceholder("카드 유효기간 (월)을 입력해주세요.")
                            .setValue(recentPayment == null ? null : recentPayment.getCardExpirationMonth())
                            .setRequired(true)
                            .build();
                    TextInput cardExpirationYear = TextInput.create("card-expiration-year", "유효기간 (년)", TextInputStyle.SHORT)
                            .setMinLength(2)
                            .setMaxLength(2)
                            .setPlaceholder("카드 유효기간 (년)을 입력해주세요.")
                            .setValue(recentPayment == null ? null : recentPayment.getCardExpirationYear())
                            .setRequired(true)
                            .build();
                    TextInput customerBirthdate = TextInput.create("customer-birthdate", "생년월일", TextInputStyle.SHORT)
                            .setMinLength(6)
                            .setMaxLength(13)
                            .setPlaceholder("생년월일을 입력해주세요. (6자)")
                            .setValue(recentPayment == null ? null : recentPayment.getCustomerBirthdate())
                            .setRequired(true)
                            .build();
                    TextInput customerEmail = TextInput.create("customer-email", "이메일", TextInputStyle.SHORT)
                            .setMinLength(5)
                            .setMaxLength(50)
                            .setPlaceholder("이메일을 입력해주세요.")
                            .setValue(recentPayment == null ? null : recentPayment.getCustomerEmail())
                            .setRequired(false)
                            .build();
                    Modal modal = Modal.create(ID_PREFIX + "credit-card", "카드 결제" + (recentPayment == null ? "" : " (자동 입력)"))
                            .addActionRow(cardNumber)
                            .addActionRow(cardExpirationMonth)
                            .addActionRow(cardExpirationYear)
                            .addActionRow(customerBirthdate)
                            .addActionRow(customerEmail)
                            .build();
                    event.replyModal(modal).queue();
                }

                case BANK_TRANSFER -> {
                    TextInput depositor = TextInput.create("depositor", "입금자명", TextInputStyle.SHORT)
                            .setMinLength(2)
                            .setMaxLength(20)
                            .setPlaceholder("입금자명을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    Modal modal = Modal.create(ID_PREFIX + "bank-transfer", "계좌이체")
                            .addActionRow(depositor)
                            .build();
                    event.replyModal(modal).queue();
                }

                case CULTURELAND -> {
                    TextInput pinNumber = TextInput.create("pin-number", "핀 번호", TextInputStyle.SHORT)
                            .setMinLength(14)
                            .setMaxLength(16)
                            .setPlaceholder("상품권의 핀 번호를 입력해주세요.")
                            .setRequired(true)
                            .build();
                    Modal modal = Modal.create(ID_PREFIX + "cultureland", "문화상품권")
                            .addActionRow(pinNumber)
                            .build();
                    event.replyModal(modal).queue();
                }
            }

            event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
        }
    }

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String componentId = event.getComponentId();

        if (componentId.startsWith(ID_PREFIX + "start-")) {
            // 인증여부 검증
            UserService userService = DatabaseManager.getUserService();
            User user = userService.getDataByDiscordId(userId);
            if (user == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("""
                                > **상품을 구매하기 전 유저 인증을 마쳐야 합니다.**
                                > **인증을 마치신 후 다시 시도해 주세요.**
                                """)
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                return;
            }

            Guild guild = DiscordBotManager.getInstance().getGuild();
            Role verifiedRole = guild.getRoleById(VERIFIED_ROLE_ID);
            if (!event.getMember().getRoles().contains(verifiedRole)) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("""
                                > **상품을 구매하기 전 유저 인증을 마쳐야 합니다.**
                                > **인증을 마치신 후 다시 시도해 주세요.**
                                """)
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                return;
            }

            String productData = componentId.substring((ID_PREFIX + "start-").length());
            String[] productArgs = productData.split("§");

            Product product;
            if (productArgs[0].equals("CUSTOM!!!!PRICE")) {
                String orderName = productArgs[1];
                int orderPrice = Integer.parseInt(productArgs[2]);

                String summary = "%s (기타ㅡ%,d원)".formatted(orderName, orderPrice);
                product = new CustomPriceProduct(orderName, orderPrice, summary);
            } else if (productArgs[0].equals("OUT!!!!SOURCING")) {
                String productName = productArgs[1];
                int productPrice = Integer.parseInt(productArgs[2]);

                String summary = "%s (외주ㅡ%,d원)".formatted(productName, productPrice);
                product = new OutSourcingProduct(productName, productPrice, summary);
            } else {
                PluginService pluginService = DatabaseManager.getPluginService();
                Plugin plugin = pluginService.getDataByENName(productData);
                if (plugin.getPrice() == 0) return;

                JDA jda = DiscordBotManager.getInstance().getJda();
                Role buyerRole = jda.getRoleById(plugin.getBuyerRole());
                if (event.getMember().getRoles().contains(buyerRole)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **해당 플러그인을 이미 구매하셨습니다.**
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();
                    return;
                }

                String summary = plugin.getKRName() + " (유료)";
                product = new PremiumPluginProduct(plugin, summary);
            }

            productMap.put(userId, product);
            couponMap.remove(userId);
            pointMap.remove(userId);

            Button withCouponBtn = Button.primary(ID_PREFIX + "coupon-yes", "예");
            Button withoutCouponBtn = Button.secondary(ID_PREFIX + "coupon-no", "아니오");

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **쿠폰을 사용하시겠습니까?**
                            > **최초 구매자는 STARLY1 쿠폰을 사용하실 수 있습니다.**
                                                                
                            """
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).addActionRow(withCouponBtn, withoutCouponBtn, CANCEL_BUTTON).setEphemeral(true).queue();
        } else if (componentId.startsWith(ID_PREFIX + "accept-")) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            PaymentService paymentService = DatabaseManager.getPaymentService();
            UUID paymentId = UUID.fromString(
                    componentId
                            .substring((ID_PREFIX + "accept-").length())
                            .replace("_", "-")
            );
            Payment payment = paymentService.getDataByPaymentId(paymentId);

            if (payment == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("> **데이터가 변조되었습니다.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
                return;
            } else if (payment.getApprovedAt() != null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("> **이미 승인이 완료되었습니다.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
                return;
            }

            InteractionHook callback = event.deferReply().complete();

            // Payment 객체: update & query
            payment.updateAccepted(true);
            payment.updateApprovedAt(new Date());

            paymentService.saveData(payment);

            // 금액 계산
            int finalPrice = payment.getFinalPrice();

            // 구매 로그
            Product product = payment.getProduct();
            int usedPoint = payment.getUsedPoint();
            CouponState usedCoupon = payment.getUsedCoupon();

            PaymentLogger.info(
                    new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **결제가 완료되었습니다.**
                                                                
                                    ─────────────────────────────────────────────────
                                    > **결제 번호: %s**
                                    > **결제 금액: %d**
                                    > **실제 결제 금액: %d**
                                    > **결제 수단: %s**
                                    > **승인 시각: %s**
                                    > **결제자: <@%d>**
                                    > **구매 정보: %s**
                                    > **사용된 포인트: %d**
                                    > **사용된 쿠폰: %s**
                                    > **생성된 티켓: %s**
                                    """
                                    .formatted(
                                            payment.getPaymentId().toString(),
                                            payment.getProduct().getPrice(),
                                            finalPrice,
                                            payment.getMethod().getKRName(),
                                            DATE_FORMAT.format(payment.getApprovedAt()),
                                            payment.getRequestedBy(),
                                            product.getSummary(),
                                            usedPoint,
                                            usedCoupon != null ? usedCoupon.getCode() : "없음",
                                            event.getChannel().getAsMention()
                                    )
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
            );

            // 구매 처리
            affectPayment(payment);

            // 메시지 전송
            net.dv8tion.jda.api.entities.User requestedBy = event.getJDA().getUserById(payment.getRequestedBy());

            MessageEmbed embed1 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                    .setDescription("> **승인 결과: <a:success:1168266537262657626>**")
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            callback.editOriginalEmbeds(embed1).queue();

            MessageEmbed embed2 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                    .setDescription("""
                            승인이 완료되었습니다.
                                                        
                            ─────────────────────────────────────────────────
                            > **결제자: %s**
                            > **승인 결과: <a:success:1168266537262657626>**
                            """.formatted("<@" + payment.getRequestedBy() + ">")
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();

            requestedBy
                    .openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed2))
                    .queue(null, (ignored) -> {
                        MessageEmbed embed3 = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 오류 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **DM으로 메시지를 전송하지 못했습니다.**
                                        
                                        ─────────────────────────────────────────────────
                                        > **유저: %s**
                                        """.formatted("<@" + payment.getRequestedBy() + ">")
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();

                        event.getChannel().sendMessageEmbeds(embed3).queue();
                    });

            MessageEmbed receipt = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 영수증 | 스탈리 <a:loading:1168266572847128709>")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            requestedBy
                    .openPrivateChannel().complete()
                    .sendMessage("""
                            ```
                            주소: 경기도 남양주시 다산동 4002-1
                            대표: 호예준
                            사업자 번호: 행정 처리중
                            이메일: hyjcompany30@naver.com
                            홈페이지: https://starly.kr/discord
                            ======================================
                                                            
                            주문번호: %s
                                                            
                            --------------------------------------
                            상품명: %s
                            정가: %,d₩
                            --------------------------------------
                                         판매총액:      %,d₩
                                         -------------------------
                                         사용 포인트:    %,d
                                         잔여 포인트:    %,d
                            --------------------------------------
                                                                
                            디스코드 ID: %d
                            디스코드 닉네임: %s
                                
                            --------------------------------------
                                     
                            %s
                                                            
                            --------------------------------------
                                                            
                            좋은 하루 되세요!
                            언제나 고객님을 위해 최선을 다하겠습니다.
                            ```
                            """.formatted(
                            payment.getPaymentId(),
                            product.getName(),
                            product.getPrice(),
                            product.getPrice(),
                            payment.getUsedPoint(),
                            DatabaseManager.getUserService().getPoint(userId),
                            payment.getRequestedBy(),
                            requestedBy.getEffectiveName(),
                            payment.getMethod() == PaymentMethod.BANK_TRANSFER ? """
                                    **************************************
                                      **** 계좌이체 매출전표(고객용) ****
                                    **************************************
                                    입금자명: %s
                                    승인일시: %s
                                    """.formatted(
                                        payment.asBankTransfer().getDepositor(),
                                        DATE_FORMAT.format(payment.getApprovedAt())
                                    ) : (
                                            payment.getMethod() == PaymentMethod.NONE ? """
                                            **************************************
                                              *** 쿠폰,포인트 매출전표 (고객용) ***
                                            **************************************
                                            승인일시: %s
                                            """.formatted(
                                                DATE_FORMAT.format(payment.getApprovedAt())
                                            ) : """
                                            **************************************
                                              *** 문화상품권 매출전표 (고객용) ***
                                            **************************************
                                            핀 번호: %s
                                            승인일시: %s
                                            """
                                            .formatted(
                                                payment.asCultureland().getPinNumber(),
                                                DATE_FORMAT.format(payment.getApprovedAt())
                                            )
                                    )
                    ))
                    .setEmbeds(receipt)
                    .queue(null, (ignored) -> {
                        MessageEmbed embed4 = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 오류 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **DM으로 영수증을 전송하지 못했습니다.**
                                        
                                        ─────────────────────────────────────────────────
                                        > **유저: %s**
                                        """.formatted("<@" + payment.getRequestedBy() + ">")
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.getChannel().sendMessageEmbeds(embed4).queue();
                    });

            // 컴포넌트 전체 비활성화
            event.getMessage().editMessageComponents(
                    event.getMessage()
                            .getComponents().stream()
                            .map(LayoutComponent::asDisabled)
                            .toList()
            ).queue();
        } else if (componentId.startsWith(ID_PREFIX + "refuse-")) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            PaymentService paymentService = DatabaseManager.getPaymentService();
            UUID paymentId = UUID.fromString(
                    componentId
                            .substring((ID_PREFIX + "refuse-").length())
                            .replace("_", "-")
            );
            Payment payment = paymentService.getDataByPaymentId(paymentId);

            if (payment == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("> **데이터가 변조되었습니다. (거래를 취소합니다.)**"
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
                return;
            } else if (payment.getApprovedAt() != null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                        .setDescription("> **이미 승인이 완료되었습니다.**"
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
                return;
            }

            InteractionHook callback = event.deferReply().complete();

            // Payment 객체: update & query
            payment.updateAccepted(false);
            payment.updateApprovedAt(new Date());

            paymentService.saveData(payment);
            // 메시지 전송
            MessageEmbed embed1 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:success:1168266537262657626> 오류 | 결제 <a:success:1168266537262657626>")
                    .setDescription("> **승인 결과: <a:cross:1058939340505497650>**")
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            callback.editOriginalEmbeds(embed1).queue();

            MessageEmbed embed2 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:success:1168266537262657626> 오류 | 결제 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **승인 결과: <a:cross:1058939340505497650>**
                            > **결제자: %s**
                            > **결제 번호: %s**
                                                        
                            """.formatted("<@" + payment.getRequestedBy() + ">", payment.getPaymentId().toString())
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.getJDA().getUserById(payment.getRequestedBy())
                    .openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(embed2))
                    .queue(null, (ignored) -> {
                        MessageEmbed embed3 = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 오류 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **DM으로 메시지를 전송하지 못했습니다.**
                                        
                                        ─────────────────────────────────────────────────
                                        > **유저: %s**
                                        """.formatted("<@" + payment.getRequestedBy() + ">")
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.getChannel().sendMessageEmbeds(embed3).queue();
                    });

            // 컴포넌트 전체 비활성화
            event.getMessage().editMessageComponents(
                    event.getMessage()
                            .getComponents().stream()
                            .map(LayoutComponent::asDisabled)
                            .toList()
            ).queue();
        } else {
            switch (componentId) {
                case ID_PREFIX + "cancel" -> {
                    if (!productMap.containsKey(userId) && !couponMap.containsKey(userId) && !pointMap.containsKey(userId))
                        return;

                    stopSession(userId);

                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 취소 | 결제 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **결제를 취소하였습니다.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed3).setEphemeral(true).queue();
                }

                case ID_PREFIX + "point-yes" -> {
                    if (!productMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("> **진행 중인 거래가 존재하지 않습니다.**")
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).setEphemeral(true).queue();

                        stopSession(userId);
                        return;
                    } else if (pointMap.containsKey(userId)) return;

                    TextInput amountInput = TextInput.create("amount", "사용액", TextInputStyle.SHORT)
                            .setMinLength(4)
                            .setPlaceholder("사용할 포인트의 금액을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    Modal modal = Modal.create(ID_PREFIX + "point", "포인트 할인")
                            .addActionRow(amountInput)
                            .build();
                    event.replyModal(modal).queue();
                }

                case ID_PREFIX + "point-no" -> {
                    if (!productMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **진행 중인 거래가 존재하지 않습니다.**
                                        > **처음부터 다시 시도해 주세요.**
                                                                            
                                        """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed)
                                .setEphemeral(true)
                                .queue();

                        stopSession(userId);
                        return;
                    } else if (pointMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                                                                                                   
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed)
                                .setEphemeral(true)
                                .queue();

                        stopSession(userId);
                        return;
                    }

                    pointMap.put(userId, 0);

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **포인트를 사용하지 않고 결제를 진행합니다.**
                                    > **결제 수단을 선택해 주세요.**
                                                                    
                                          """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(createPaymentMethodSelectMenu(
                                    false /* productMap.get(userId).getType() != ProductType.PREMIUM_RESOURCE */
                            ))
                            .addActionRow(CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                }

                case ID_PREFIX + "coupon-yes" -> {
                    if (!productMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **진행 중인 거래가 존재하지 않습니다.**
                                        > **처음부터 다시 시도해 주세요.**
                                                                                                                      
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).setEphemeral(true).queue();

                        stopSession(userId);
                        return;
                    } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                                                                                                   
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).setEphemeral(true).queue();

                        stopSession(userId);
                        return;
                    }

                    TextInput couponInput = TextInput.create("code", "쿠폰", TextInputStyle.SHORT)
                            .setPlaceholder("쿠폰 코드를 입력해주세요.")
                            .setRequired(true)
                            .build();
                    Modal modal = Modal.create(ID_PREFIX + "coupon", "쿠폰 할인")
                            .addActionRow(couponInput)
                            .build();
                    event.replyModal(modal).queue();
                }

                case ID_PREFIX + "coupon-no" -> {
                    if (!productMap.containsKey(userId)) {

                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **진행 중인 거래가 존재하지 않습니다.**
                                        > **처음부터 다시 시도해 주세요.**
                                                                                                                      
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).setEphemeral(true).queue();

                        stopSession(userId);
                        return;
                    } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                                                                                                   
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).setEphemeral(true).queue();

                        stopSession(userId);
                        return;
                    }

                    UserService userService = DatabaseManager.getUserService();
                    User user = userService.getDataByDiscordId(userId);
                    int point = user.point();

                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");

                    if (point == 0) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **보유 중인 포인트가 없습니다..**
                                        > **포인트를 사용하지 않고 결제를 진행합니다.**
                                        > **계속하시겠습니까?**                                         
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).addActionRow(withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    } else if (point < POINT_USE_MINIMUM) {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR)
                                .setTitle("제목")
                                .setDescription("보유중인 포인트가 %d 이하이므로 쿠폰, 포인트를 모두 사용하지 않고 결제를 진행합니다.\n(최소 사용금액 미달로 인한 적용 불가능)\n\n계속하시겠습니까?".formatted(POINT_USE_MINIMUM))
                                .build();
                        event.replyEmbeds(embed)
                                .addActionRow(withoutPointBtn, CANCEL_BUTTON)
                                .setEphemeral(true)
                                .queue();
                    } else {
                        Button withPointBtn = Button.primary(ID_PREFIX + "point-yes", "예");
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **쿠폰을 사용하지 않고 결제를 진행합니다.**
                                        > **포인트를 사용하시겠습니까?**
                                                                          
                                              """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).addActionRow(withPointBtn, withoutPointBtn.withLabel("아니오"), CANCEL_BUTTON).setEphemeral(true).queue();
                    }
                }
            }
        }
    }

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String modalId = event.getModalId();
        switch (modalId) {
            case ID_PREFIX + "coupon" -> {
                long userId = event.getUser().getIdLong();

                if (!productMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **진행 중인 거래가 존재하지 않습니다.**
                                    > **처음부터 다시 시도해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                                                                                               
                                          """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                }


                CouponService couponService = DatabaseManager.getCouponService();
                String couponCode = event.getValue("code").getAsString();
                Coupon coupon = couponService.getData(couponCode);
                if (coupon == null) {
                    Button retryBtn = Button.primary(ID_PREFIX + "coupon-yes", "다시 입력하기");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **존재하지 않는 쿠폰입니다.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }

                Product product = productMap.get(userId);
                if (!coupon.isUsable(userId, product)) {
                    Button retryBtn = Button.primary(ID_PREFIX + "coupon-yes", "다시 입력하기");
                    Button withoutCouponBtn = Button.secondary(ID_PREFIX + "coupon-no", "쿠폰 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **쿠폰을 사용할 수 없습니다.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutCouponBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }

                couponMap.put(userId, coupon);

                int finalPrice = coupon.getDiscount().computeFinalPrice(product.getPrice());
                if (finalPrice < 0) {
                    couponMap.remove(userId);

                    Button retryBtn = Button.primary(ID_PREFIX + "coupon-yes", "다시 입력하기");
                    Button withoutCouponBtn = Button.secondary(ID_PREFIX + "coupon-no", "쿠폰 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **쿠폰을 사용할 수 없습니다.**
                                    > **(할인 적용 금액 < 0)**
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutCouponBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                } else if (finalPrice == 0) {
                    int usedPoint = pointMap.get(userId);
                    Coupon usedCoupon = couponMap.get(userId);

                    PaymentService paymentService = DatabaseManager.getPaymentService();
                    Payment payment = new Payment(
                            UUID.randomUUID(), userId, PaymentMethod.NONE,
                            usedPoint, usedCoupon,
                            product
                    );
                    paymentService.saveData(payment);

                    TextChannel ticketChannel = createTicketChannel(payment);

                    String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");
                    Button approveBtn = Button.primary(ID_PREFIX + "accept-" + paymentIdForId, "수락");
                    Button rejectBtn = Button.danger(ID_PREFIX + "refuse-" + paymentIdForId, "거절");

                    MessageEmbed embed1 = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **결제가 요청되었습니다.**
                                    > **승인하시겠습니까?**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    ticketChannel.sendMessageEmbeds(embed1).setActionRow(approveBtn, rejectBtn).queue();

                    MessageEmbed embed2 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **쿠폰(%s)을 적용하였습니다.**
                                    > **결제할 금액이 남아있지 않습니다.**
                                                                        
                                    """.formatted(coupon.getCode())
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                            .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                    "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                            .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                            .build();
                    event.replyEmbeds(embed2, embed3).setEphemeral(true).queue();
                } else {
                    UserService userService = DatabaseManager.getUserService();
                    User user = userService.getDataByDiscordId(userId);

                    if (user.point() > 0) {
                        Button withPointBtn = Button.primary(ID_PREFIX + "point-yes", "예");
                        Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "아니오");

                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR)
                                .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                    > **쿠폰(%s)을 적용하였습니다.**
                                    > **포인트를 사용하시겠습니까?**
                                                                        
                                    """.formatted(coupon.getCode())
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).addActionRow(withPointBtn, withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    } else {
                        Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");

                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR)
                                .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                                .setDescription("""
                                    > **쿠폰(%s)을 적용하였습니다.**
                                    > **적용할 포인트가 없습니다. 진행 하시겠습니까?**
                                                                        
                                    """.formatted(coupon.getCode())
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                                .build();
                        event.replyEmbeds(embed).addActionRow(withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    }
                }
            }

            case ID_PREFIX + "point" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                Product product = productMap.get(userId);
                Coupon usedCoupon = couponMap.get(userId);

                // 진행도 검증
                if (!productMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **진행 중인 거래가 존재하지 않습니다.**
                                    > **처음부터 다시 시도해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                } else if (pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **데이터가 변조되었습니다. (거래를 취소합니다.)**
                                                                                                               
                                          """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                }

                // 입력값 추출
                int amount;
                try {
                    String amountStr = event.getValue("amount").getAsString();
                    amount = Integer.parseInt(amountStr);
                } catch (NumberFormatException ignored) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **숫자만 입력할 수 있습니다.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }

                // 값 검증1
                if (amount % POINT_USE_UNIT != 0) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **포인트는 %d 단위로 사용할 수 있습니다.**
                                                                        
                                    """.formatted(POINT_USE_UNIT)
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }
                if (amount < POINT_USE_MINIMUM) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **포인트는 %d 이상 사용할 수 있습니다.**
                                                                        
                                    """.formatted(POINT_USE_MINIMUM)
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }

                // 값 검증2
                int price = product.getPrice();
                if (usedCoupon != null) {
                    price = usedCoupon.getDiscount().computeFinalPrice(amount);
                }

                if (amount > price) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **포인트는 결제 금액 이하로 사용할 수 있습니다.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON).setEphemeral(true).queue();
                    return;
                }

                // 값 검증3
                int finalPrice = price - amount;
                if (finalPrice == 0) {
                    PaymentService paymentService = DatabaseManager.getPaymentService();
                    Payment payment = new Payment(
                            UUID.randomUUID(), userId, PaymentMethod.NONE,
                            amount, usedCoupon,
                            product
                    );
                    paymentService.saveData(payment);

                    TextChannel ticketChannel = createTicketChannel(payment);

                    String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");
                    Button approveBtn = Button.primary(ID_PREFIX + "accept-" + paymentIdForId, "수락");
                    Button rejectBtn = Button.danger(ID_PREFIX + "refuse-" + paymentIdForId, "거절");

                    MessageEmbed embed1 = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **결제가 요청되었습니다.**
                                    > **승인하시겠습니까?**
                                                                        
                                          """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    ticketChannel.sendMessageEmbeds(embed1)
                            .setActionRow(approveBtn, rejectBtn)
                            .queue();

                    MessageEmbed embed2 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **포인트(%s원)을 적용하였습니다.**
                                    > **결제할 금액이 남아있지 않습니다.**
                                                                        
                                    """.formatted(amount)
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                            .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                    "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                            .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                            .build();
                    event.replyEmbeds(embed2, embed3)
                            .setEphemeral(true)
                            .queue();

                    return;
                }

                pointMap.put(userId, amount);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **포인트(%s원)을 적용하였습니다.**
                                > **결제 수단을 선택해 주세요.**
                                                                    
                                """.formatted(amount)
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(createPaymentMethodSelectMenu(
                                false /* productMap.get(userId).getType() != ProductType.PREMIUM_RESOURCE */
                        ))
                        .addActionRow(CANCEL_BUTTON)
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "credit-card" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!productMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **진행 중인 거래가 존재하지 않습니다.**
                                    > **처음부터 다시 시도해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String cardNumber = event.getValue("card-number").getAsString();
                String cardExpirationMonth = event.getValue("card-expiration-month").getAsString();
                String cardExpirationYear = event.getValue("card-expiration-year").getAsString();
                String customerBirthdate = event.getValue("customer-birthdate").getAsString();
                String customerEmail = event.getValue("customer-email") != null ? event.getValue("customer-email").getAsString() : null;

                // SecureKey, SecureIV 생성
                SecretKey key = AESUtil.generateKey(128);
                IvParameterSpec iv = AESUtil.generateIv();

                Product product = productMap.get(userId);
                int usedPoint = pointMap.get(userId);
                Coupon usedCoupon = couponMap.get(userId);

                CreditCardPayment payment;
                try {
                    payment = new CreditCardPayment(
                            product, userId,
                            cardNumber, cardExpirationYear, cardExpirationMonth, 0,
                            customerBirthdate, customerEmail, event.getUser().getEffectiveName(),
                            usedPoint, usedCoupon,
                            key, iv
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **결제 정보 생성 도중 오류가 발생하였습니다.**
                                                                                                               
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    PaymentLogger.warning(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                    .setDescription("""
                                            > **결제 정보 생성 도중 오류가 발생하였습니다.**
                                            > **결제자: %s**
                                                                                        
                                            """.formatted(event.getUser().getAsMention())
                                    )
                                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    );
                    stopSession(userId);
                    return;
                }

                // 결제 요청
                try {
                    TossPaymentsUtil.request(payment);
                } catch (Exception ex) {
//                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **결제 요청 도중 오류가 발생하였습니다.**
                                                                                                               
                                    """)
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    PaymentLogger.error(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                    .setDescription("""
                                            > **결제 요청 도중 오류가 발생하였습니다.**
                                            
                                            > **결제 번호: %s**
                                            > **결제자: %s**
                                            > **오류 내용: %s**
                                            
                                            """.formatted(payment.getPaymentId().toString(), event.getUser().getAsMention(), ex.getMessage())
                                    )
                                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    );

                    payment.updateAccepted(true);
                    payment.updateApprovedAt(null);
                    payment.updateResponse(null);

                    stopSession(userId);
                    return;
                }

                // DB 기록
                try {
                    PaymentService paymentService = DatabaseManager.getPaymentService();
                    paymentService.saveData(payment);
                } catch (Exception ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **결제 정보 저장 도중 오류가 발생하였습니다.**
                                    > **카드사에 비용이 청구되었을 수 있으니, 고객센터를 통하여 관리자에게 문의해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    PaymentLogger.error(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                    .setDescription("""
                                            > **결제 정보 저장 도중 오류가 발생하였습니다.**
                                            
                                            > **결제 번호: %s**
                                            > **결제자: %s**
                                                                                
                                            """.formatted(payment.getPaymentId().toString(), event.getUser().getAsMention())
                                    )
                                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    );

                    stopSession(userId);
                    return;
                }

                if (!payment.isAccepted()) return;

                try {
                    affectPayment(payment);
                } catch (Exception ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **상품 지급 도중 오류가 발생하였습니다.**
                                    > **카드사에 비용이 청구되었을 수 있으니, 고객센터를 통하여 관리자에게 문의해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    PaymentLogger.error(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                                    .setDescription("""
                                            > **상품 지급 도중 오류가 발생하였습니다.**
                                            
                                            > **결제 번호: %s**
                                            > **결제자: %s**
                                                                                
                                            """.formatted(payment.getPaymentId().toString(), event.getUser().getAsMention())
                                    )
                                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    );

                    stopSession(userId);
                    return;
                }

                int finalPrice = payment.getFinalPrice();
                PaymentLogger.info(
                        new EmbedBuilder()
                                .setColor(EMBED_COLOR_SUCCESS)
                                .setTitle("<a:success:1168266537262657626> 성공 | 결제 <a:success:1168266537262657626>")
                                .setDescription("""
                                    > **결제가 완료되었습니다.**
                                                                
                                    ─────────────────────────────────────────────────
                                    > **결제 번호: %s**
                                    > **결제 금액: %d**
                                    > **실제 결제 금액: %d**
                                    > **결제 수단: %s**
                                    > **승인 시각: %s**
                                    > **결제자: %s**
                                    > **구매 정보: %s**
                                    > **사용된 포인트: %d**
                                    > **사용된 쿠폰: %s**
                                    > **생성된 티켓: 없음**
                                    """
                                        .formatted(
                                                payment.getPaymentId().toString(),
                                                payment.getProduct().getPrice(),
                                                finalPrice,
                                                payment.getMethod().getKRName(),
                                                DATE_FORMAT.format(payment.getApprovedAt()),
                                                event.getUser().getAsMention(),
                                                product.getSummary(),
                                                usedPoint,
                                                usedCoupon != null ? usedCoupon.getCode() : "없음"
                                        )
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                );

                MessageEmbed receipt = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 영수증 | 스탈리 <a:loading:1168266572847128709>")
                        .setDescription("[매출전표](" + payment.getReceiptUrl() + ")")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.getJDA().getUserById(payment.getRequestedBy())
                        .openPrivateChannel().complete()
                        .sendMessage("""
                                ```
                                주소: 경기도 남양주시 다산동 4002-1
                                대표: 호예준
                                사업자 번호: 행정 처리중
                                이메일: hyjcompany30@naver.com
                                홈페이지: https://starly.kr/
                                ======================================
                                                                
                                주문번호: %s
                                                                
                                --------------------------------------
                                상품명: %s
                                정가: %,d₩
                                --------------------------------------
                                             판매총액:      %,d₩
                                             -------------------------
                                             사용 포인트:    %,d
                                             잔여 포인트:    %,d
                                --------------------------------------
                                                                
                                디스코드 ID: %d
                                디스코드 닉네임: %s
                                
                                --------------------------------------
                                         
                                **************************************
                                  **** 신용카드 매출전표(고객용) ****
                                **************************************
                                카드번호: %s
                                할부: %d개월
                                                                
                                --------------------------------------
                                                                
                                좋은 하루 되세요!
                                언제나 고객님을 위해 최선을 다하겠습니다.
                                ```
                                """.formatted(
                                payment.getPaymentId(),
                                product.getName(),
                                product.getPrice(),
                                product.getPrice(),
                                payment.getUsedPoint(),
                                DatabaseManager.getUserService().getPoint(userId),
                                payment.getRequestedBy(),
                                event.getUser().getEffectiveName(),
                                payment.getMaskedCardNumber(),
                                payment.getCardInstallmentPlan()
                        ))
                        .setEmbeds(receipt)
                        .queue(null, (ignored) -> {
                            AuditLogger.warning(new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 결제 <a:loading:1168266572847128709>")
                                    .setDescription("""
                                        > **DM으로 영수증을 전송하지 못했습니다.**
                                        > **%s**
                                                                            
                                        """.formatted("<@" + payment.getRequestedBy() + ">")
                                    )
                                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png"));
                        });

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 결제가 완료되었습니다! <a:success:1168266537262657626>")
                        .setDescription("> **🥳 축하드려요! 결제가 성공적으로 완료되었습니다!**\n" +
                                "> **또한, [여기를 클릭](" + payment.getReceiptUrl() + ")하면 영수증을 확인하실 수 있어요.**\n" +
                                "> **정말 감사합니다! 🙏**\n\u1CBB")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                stopSession(userId);
            }

            case ID_PREFIX + "bank-transfer" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!productMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **진행 중인 거래가 존재하지 않습니다.**
                                    > **처음부터 다시 시도해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String depositor = event.getValue("depositor").getAsString();

                Product product = productMap.get(userId);
                int usedPoint = pointMap.get(userId);
                Coupon usedCoupon = couponMap.get(userId);

                BankTransferPayment payment = new BankTransferPayment(
                        product, userId,
                        depositor,
                        usedPoint, usedCoupon
                );

                PaymentService paymentService = DatabaseManager.getPaymentService();
                paymentService.saveData(payment);

                // 티켓 생성
                TextChannel ticketChannel = createTicketChannel(payment);

                String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");

                Button approveBtn = Button.primary(ID_PREFIX + "accept-" + paymentIdForId, "수락");
                Button rejectBtn = Button.danger(ID_PREFIX + "refuse-" + paymentIdForId, "거절");
                MessageEmbed embed1 = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                        .setDescription("""
                                > **계좌이체 결제가 요청되었습니다.**
                                > **승인하시겠습니까?**
                                                                    
                                > **입금자명: %s**
                                                                  
                                """.formatted(depositor)
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                ticketChannel.sendMessageEmbeds(embed1)
                        .setActionRow(approveBtn, rejectBtn)
                        .queue();

                MessageEmbed embed2 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                        .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                        .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                        .build();
                event.replyEmbeds(embed2).setEphemeral(true).queue();

                // 세션 데이터 삭제
                stopSession(userId);
            }

            case ID_PREFIX + "cultureland" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!productMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:cross:1058939340505497650> 오류 | 결제 <a:cross:1058939340505497650>")
                            .setDescription("""
                                    > **진행 중인 거래가 존재하지 않습니다.**
                                    > **처음부터 다시 시도해 주세요.**
                                                                        
                                    """
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                            .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String pinNumber = event.getValue("pin-number").getAsString();

                Product product = productMap.get(userId);
                int usedPoint = pointMap.get(userId);
                Coupon usedCoupon = couponMap.get(userId);

                CulturelandPayment payment = new CulturelandPayment(
                        product, userId,
                        pinNumber,
                        usedPoint, usedCoupon
                );

                PaymentService paymentService = DatabaseManager.getPaymentService();
                paymentService.saveData(payment);

                // 티켓 생성
                TextChannel ticketChannel = createTicketChannel(payment);

                String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");

                Button approveBtn = Button.primary(ID_PREFIX + "accept-" + paymentIdForId, "수락");
                Button rejectBtn = Button.danger(ID_PREFIX + "refuse-" + paymentIdForId, "거절");
                MessageEmbed embed1 = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 대기 | 결제 <a:loading:1168266572847128709>")
                        .setDescription("""
                                > **문화상품권 결제가 요청되었습니다.**
                                > **승인하시겠습니까?**
                                                                    
                                > **핀번호: %s**
                                                                  
                                """.formatted(pinNumber)
                        )
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                ticketChannel.sendMessageEmbeds(embed1).setActionRow(approveBtn, rejectBtn).queue();

                MessageEmbed embed2 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                        .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                        .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                        .build();
                event.replyEmbeds(embed2).setEphemeral(true).queue();

                // 세션 데이터 삭제
                stopSession(userId);
            }
        }
    }

    // UTILITY
    private void stopSession(long userId) {
        productMap.remove(userId);
        pointMap.remove(userId);
        couponMap.remove(userId);
    }

    private SelectMenu createPaymentMethodSelectMenu(boolean onlyCreditCard) {
        return onlyCreditCard ?
                StringSelectMenu.create(ID_PREFIX + "payment-method")
                        .setPlaceholder("결제수단을 선택해주세요.")
//                        .addOption("카드", "credit_card", "카드로 결제합니다.", Emoji.fromUnicode("💳"))
                        .build() :
                StringSelectMenu.create(ID_PREFIX + "payment-method")
                        .setPlaceholder("결제수단을 선택해주세요.")
//                        .addOption("카드", "credit_card", "카드로 결제합니다.", Emoji.fromUnicode("💳"))
                        .addOption("계좌이체", "bank_transfer", "계좌이체로 결제합니다.", Emoji.fromUnicode("💰"))
                        .addOption("문화상품권", "cultureland", "문화상품권로 결제합니다.", Emoji.fromUnicode("🪙"))
                        .build();
    }

    private TextChannel createTicketChannel(Payment payment) {
        long userId = payment.getRequestedBy();
        JDA jda = DiscordBotManager.getInstance().getJda();
        TicketService ticketService = DatabaseManager.getTicketService();

        String userName = jda.getUserById(userId).getEffectiveName();
        int usedPoint = payment.getUsedPoint();
        Coupon usedCoupon = payment.getUsedCoupon();
        String dateStr = DATE_FORMAT.format(new Date());
        int price = payment.getFinalPrice();

        String title = "자동결제 승인요청 (" + payment.getMethod().getKRName() + ")";
        String description = """
                > 결제 ID
                > %s
                                
                > 결제수단
                > %s
                                
                > 결제금액
                > 실결제액: %,d원, 쿠폰: %s, 포인트: %,d원
                                
                > 결제자
                > %s (%d)
                                
                > 결제일
                > %s
                                
                > 승인상태
                > %s
                """.formatted(
                payment.getPaymentId().toString(),
                payment.getMethod().getKRName(),
                price,
                usedCoupon != null ? usedCoupon.getDiscount().toString() : "해당없음",
                usedPoint,
                userName,
                userId,
                dateStr,
                payment.isAccepted() ? "완료 (승인)" : (payment.getApprovedAt() != null ? "완료 (거절)" : "대기")
        );

        long ticketIndex = ticketService.getLastIndex() + 1;
        TicketType ticketType = TicketType.PAYMENT;

        Category category = jda.getCategoryById(TICKET_CATEGORY_ID);
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
                title,
                description
        );

        return ticketChannel;
    }

    private void affectPayment(Payment payment) {
        // 변수 선언
        long userId = payment.getRequestedBy();
        int usedPoint = payment.getUsedPoint();
        CouponState usedCoupon = payment.getUsedCoupon();
        Product product = payment.getProduct();

        // DB 기록 (포인트 차감 및 쿠폰 사용 처리)
        UserService userService = DatabaseManager.getUserService();
        userService.removePoint(userId, usedPoint);

        if (usedCoupon != null) {
            CouponRedeemService couponRedeemService = DatabaseManager.getCouponRedeemService();
            couponRedeemService.saveData(
                    UUID.randomUUID(),
                    usedCoupon, new CouponState(usedCoupon),
                    userId, new Date()
            );
        }

        // 역할 지급
        Guild guild = DiscordBotManager.getInstance().getGuild();
        Role buyerRole = guild.getRoleById(configProvider.getString("BUYER_ROLE_ID"));
        guild.addRoleToMember(UserSnowflake.fromId(userId), buyerRole).queue();

        if (product.getType() == ProductType.PREMIUM_RESOURCE) {
            Plugin plugin = product.asPremiumPlugin().getPlugin();
            Role pluginBuyerRole = guild.getRoleById(plugin.getBuyerRole());

            guild.addRoleToMember(UserSnowflake.fromId(userId), pluginBuyerRole).queue();
        }

        // 랭크 검색
        User user = userService.getDataByDiscordId(userId);
        List<Rank> userRanks = new ArrayList<>(user.rank());

        userRanks.sort(Comparator.comparingInt(Rank::getOrdinal));
        Rank highestRank = userRanks.get(userRanks.size() - 1);

        // 랭크 특권
        if (highestRank.hasPerk(RankPerkType.CASHBACK)) {
            CashbackPerk cashbackPerk = (CashbackPerk) highestRank.getPerk(RankPerkType.CASHBACK);
            int cashbackAmount = payment.getFinalPrice() / 100 * cashbackPerk.getPercentage();
            userService.addPoint(userId, cashbackAmount);

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 캐시백 | 결제 <a:loading:1168266572847128709>")
                    .setDescription("""
                                    > **랭크 특권으로 %d원을 캐시백 받았습니다.**
                                                                        
                                    """.formatted(cashbackAmount)
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyBot/StarlyBot_YELLOW.png")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();

            JDA jda = DiscordBotManager.getInstance().getJda();
            jda.getUserById(userId).openPrivateChannel().complete().sendMessageEmbeds(embed).queue();
        }

        // 랭크 지급
        PaymentService paymentService = DatabaseManager.getPaymentService();
        Rank rank2 = RankRepository.getInstance().getRank(2);
        Rank rank3 = RankRepository.getInstance().getRank(3);
        Rank rank4 = RankRepository.getInstance().getRank(4);
        Rank rank5 = RankRepository.getInstance().getRank(5);

        long totalPrice = paymentService.getTotalPaidPrice(userId);
        if (totalPrice >= 500000 && !userRanks.contains(rank3)) {
            RankUtil.giveRank(userId, rank3);
        }
        if (totalPrice >= 1000000 && !userRanks.contains(rank4)) {
            RankUtil.giveRank(userId, rank4);
        }
        if (totalPrice >= 3000000 && !userRanks.contains(rank5)) {
            RankUtil.giveRank(userId, rank5);
        }

        RankUtil.giveRank(userId, rank2);
    }
}