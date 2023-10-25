package kr.starly.discordbot.listener.pluginaction;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Discount;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.service.CouponRedeemService;
import kr.starly.discordbot.service.CouponService;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.enums.PaymentMethod;
import kr.starly.discordbot.entity.payment.impl.BankTransferPayment;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;
import kr.starly.discordbot.entity.payment.impl.CulturelandPayment;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.entity.product.impl.PremiumPluginProduct;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.enums.RankPerkType;
import kr.starly.discordbot.entity.perk.impl.CashbackPerk;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.util.RankUtil;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.TokenUtil;
import kr.starly.discordbot.util.external.TossPaymentsUtil;
import kr.starly.discordbot.util.messaging.PaymentLogger;
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
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

import static java.lang.String.format;

@BotEvent
public class BuyListener extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final Map<Long, Plugin> pluginMap = new HashMap<>();
    private final Map<Long, Coupon> couponMap = new HashMap<>();
    private final Map<Long, Integer> pointMap = new HashMap<>();

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("H:mm:ss a (yyyy-MM-dd)");
    private final String ID_PREFIX = "pluginaction-buy-";
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
            Plugin plugin = pluginMap.get(userId);
            if (plugin == null) isPure = false;
            else if (plugin.getPrice() == 0) isPure = false;

            if (!isPure) {
                event.reply("데이터가 변조되었습니다. {MAL1}\n거래를 취소합니다.")
                        .setEphemeral(true)
                        .queue();

                stopProcess(userId);
                return;
            }

            // 무결성 검증2
            Coupon coupon = couponMap.get(userId);
            if (coupon != null) {
                Discount discount = coupon.getDiscount();
                if (discount.getType() == DiscountType.FIXED
                        && discount.getValue() > plugin.getPrice()) {
                    isPure = false;
                } else {
                    Product product = new PremiumPluginProduct(plugin, event.getUser().getEffectiveName() + " | " + plugin.getKRName());
                    if (!coupon.getRequirements().stream()
                            .allMatch(requirement -> requirement.isSatisfied(userId, coupon, product))) isPure = false;
                }
            }

            if (!isPure) {
                event.reply("데이터가 변조되었습니다. {MAL2}\n거래를 취소합니다.")
                        .setEphemeral(true)
                        .queue();

                stopProcess(userId);
                return;
            }

            // 무결성 검증3
            int price = plugin.getPrice();
            if (coupon != null) price = coupon.getDiscount().computeFinalPrice(price);

            int point = pointMap.get(userId);
            if (point < 0) isPure = false;
            else if (point != 0) {
                if (point % POINT_USE_UNIT != 0) isPure = false;
                else if (point < POINT_USE_MINIMUM) isPure = false;
                else if (point > price) isPure = false;
            }

            if (!isPure) {
                event.reply("데이터가 변조되었습니다. {MAL3}\n거래를 취소합니다.")
                        .setEphemeral(true)
                        .queue();

                stopProcess(userId);
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
                            .setMaxLength(6)
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
                    Modal modal = Modal.create(ID_PREFIX + "bank-transfer", "무통장 입금")
                            .addActionRow(depositor)
                            .build();
                    event.replyModal(modal).queue();
                }

                case CULTURELAND -> {
                    TextInput pinNumber = TextInput.create("pin-number", "핀번호", TextInputStyle.SHORT)
                            .setMinLength(14)
                            .setMaxLength(16)
                            .setPlaceholder("상품권의 핀번호를 입력해주세요.")
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
            UserService userService = DatabaseManager.getUserService();
            User user = userService.getDataByDiscordId(userId);
            if (user == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("인증을 마치지 않았습니다.")
                        .setDescription("상품을 구매하기 위해서는 인증을 하셔야 합니다.\n" +
                                "인증을 마치신 후 다시 시도해주세요.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                return;
            }

            PluginService pluginService = DatabaseManager.getPluginService();
            String ENName = componentId.substring((ID_PREFIX + "start-").length());
            Plugin plugin = pluginService.getDataByENName(ENName);
            if (plugin.getPrice() == 0) return;

            // 보유 여부 확인
            JDA jda = DiscordBotManager.getInstance().getJda();
            Role buyerRole = jda.getRoleById(plugin.getBuyerRole());
            if (event.getMember().getRoles().contains(buyerRole)) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("제목")
                        .setDescription("해당 플러그인을 이미 구매하셨습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            Button withCouponBtn = Button.primary(ID_PREFIX + "coupon-yes", "예");
            Button withoutCouponBtn = Button.secondary(ID_PREFIX + "coupon-no", "아니오");
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("제목")
                    .setDescription("쿠폰을 사용하시겠습니까?")
                    .build();
            event.replyEmbeds(embed)
                    .addActionRow(withCouponBtn, withoutCouponBtn, CANCEL_BUTTON)
                    .setEphemeral(true)
                    .queue();

            pluginMap.put(userId, plugin);
            couponMap.remove(userId);
            pointMap.remove(userId);
        } else if (componentId.startsWith(ID_PREFIX + "accept-")) {
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
                        .setTitle("제목")
                        .setDescription("데이터가 변조되었습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            } if (payment.getApprovedAt() != null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("제목")
                        .setDescription("이미 승인 완료되었습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // Payment 객체: update & query
            payment.updateAccepted(true);
            payment.updateApprovedAt(new Date());

            paymentService.saveData(payment);

            // 금액 계산
            int finalPrice = payment.getFinalPrice();

            // 구매 로그
            PremiumPluginProduct product = payment.getProduct().asPremiumPlugin();
            Plugin plugin = product.getPlugin();
            int usedPoint = payment.getUsedPoint();
            CouponState usedCoupon = payment.getUsedCoupon();
            PaymentLogger.info(new EmbedBuilder()
                    .setTitle("결제가 완료되었습니다.")
                    .setDescription("결제번호: " + payment.getPaymentId() + "\n" +
                            "결제금액: " + payment.getProduct().getPrice() + "원\n" +
                            "실결제금액: " + finalPrice + "원\n" +
                            "결제수단: " + payment.getMethod().getKRName() + "\n" +
                            "승인시각: " + DATE_FORMAT.format(payment.getApprovedAt()) + "\n" +
                            "결제자: " + event.getUser().getAsMention() + "\n" +
                            "구매한 플러그인: " + plugin.getENName() + "\n" +
                            "사용된 포인트: " + usedPoint + "\n" +
                            "사용된 쿠폰: " + (usedCoupon != null ? usedCoupon.getCode() : "없음") + "\n" +
                            "생성된 티켓: " + event.getChannel().getAsMention())
            );

            // 구매 처리
            affectPayment(payment);

            // 메시지 전송
            MessageEmbed embed1 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("결제 승인이 완료되었습니다.")
                    .setDescription("> 승인 결과\n> 수락")
                    .build();
            event.replyEmbeds(embed1)
                    .queue();

            MessageEmbed embed2 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("결제가 수락되었습니다.")
                    .setDescription("<@" + payment.getRequestedBy() + ">님이 요청하신 결제(" + payment.getPaymentId() + ")가 수락되었습니다.")
                    .setFooter("스탈리 커뮤니티에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
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

            // 컴포넌트 전체 비활성화
            event.getMessage().editMessageComponents(
                    event.getMessage()
                            .getComponents().stream()
                            .map(LayoutComponent::asDisabled)
                            .toList()
            ).queue();
        } else if (componentId.startsWith(ID_PREFIX + "refuse-")) {
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
                        .setTitle("제목")
                        .setDescription("데이터가 변조되었습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            } if (payment.getApprovedAt() != null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("제목")
                        .setDescription("이미 승인 완료되었습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // Payment 객체: update & query
            payment.updateAccepted(false);
            payment.updateApprovedAt(new Date());

            paymentService.saveData(payment);

            // 메시지 전송
            MessageEmbed embed1 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("결제 승인이 완료되었습니다.")
                    .setDescription("> 승인 결과\n> 거절")
                    .build();
            event.replyEmbeds(embed1)
                    .queue();

            MessageEmbed embed2 = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("결제가 거절되었습니다.")
                    .setDescription("<@" + payment.getRequestedBy() + ">님이 요청하신 결제(" + payment.getPaymentId() + ")가 거절되었습니다.")
                    .setFooter("스탈리 커뮤니티에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
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

            // 컴포넌트 전체 비활성화
            event.getMessage().editMessageComponents(
                    event.getMessage()
                            .getComponents().stream()
                            .map(LayoutComponent::asDisabled)
                            .toList()
            ).queue();
        }

        switch (componentId) {
            case ID_PREFIX + "cancel" -> {
                if (!pluginMap.containsKey(userId) && !couponMap.containsKey(userId) && !pointMap.containsKey(userId)) return;

                stopProcess(userId);

                event.reply("결제를 취소했습니다.")
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "point-yes" -> {
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
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
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("데이터가 변조되었습니다.\n거래를 취소합니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                }

                pointMap.put(userId, 0);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("제목")
                        .setDescription("포인트를 사용하지 않고 결제를 진행합니다.\n결제수단을 선택해주세요.")
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(createPaymentMethodSelectMenu())
                        .addActionRow(CANCEL_BUTTON)
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "coupon-yes" -> {
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("데이터가 변조되었습니다.\n거래를 취소합니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
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
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("데이터가 변조되었습니다.\n거래를 취소합니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                }

                UserService userService = DatabaseManager.getUserService();
                User user = userService.getDataByDiscordId(userId);
                int point = user.point();

                Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");

                if (point == 0) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("제목")
                            .setDescription("보유중인 포인트가 없어 쿠폰, 포인트를 모두 사용하지 않고 결제를 진행합니다.\n\n계속하시겠습니까?")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
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
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("제목")
                            .setDescription("쿠폰을 사용하지 않고 결제를 진행합니다.\n포인트를 사용하시겠습니까?")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(withPointBtn, withoutPointBtn.withLabel("아니오"), CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
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
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (couponMap.containsKey(userId) || pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("데이터가 변조되었습니다.\n거래를 취소합니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                }

                // 입력값 추출
                String code = event.getValue("code").getAsString();

                // 값 검증
                CouponService couponService = DatabaseManager.getCouponService();
                Coupon coupon = couponService.getData(code);
                if (coupon == null) {
                    Button retryBtn = Button.primary(ID_PREFIX + "coupon-yes", "다시 입력하기");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("존재하지 않는 쿠폰입니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                Plugin plugin = pluginMap.get(userId);
                Product product = new PremiumPluginProduct(plugin, event.getUser().getEffectiveName() + "님, " + plugin.getKRName() + " 구매");
                if (!coupon.getRequirements().stream()
                        .allMatch(requirement -> requirement.isSatisfied(userId, coupon, product))) {
                    Button retryBtn = Button.primary(ID_PREFIX + "coupon-yes", "다시 입력하기");
                    Button withoutCouponBtn = Button.secondary(ID_PREFIX + "coupon-no", "쿠폰 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("쿠폰을 사용할 수 없습니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutCouponBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
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
                            .setTitle("제목")
                            .setDescription("쿠폰을 사용할 수 없습니다.\n(할인 적용 금액 < 0)")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutCouponBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                } else if (finalPrice == 0) {
                    // Payment 객체 생성
                    int usedPoint = pointMap.get(userId);
                    Coupon usedCoupon = couponMap.get(userId);
                    Payment payment = new Payment(
                            UUID.randomUUID(), userId, PaymentMethod.NONE,
                            usedPoint, usedCoupon,
                            product
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
                            .setTitle("결제가 요청되었습니다.")
                            .setDescription("승인하시겠습니까?")
                            .build();
                    ticketChannel.sendMessageEmbeds(embed1)
                            .setActionRow(approveBtn, rejectBtn)
                            .queue();

                    MessageEmbed embed2 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("제목")
                            .setDescription("쿠폰(" + coupon.getCode() + ")을 적용하였습니다.\n결제할 금액이 남아있지 않습니다.")
                            .build();
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1141625729386287206> 티켓 생성 완료! <a:success:1141625729386287206>")
                            .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                    "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                            .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                            .build();
                    event.replyEmbeds(embed2, embed3)
                            .setEphemeral(true)
                            .queue();
                } else {
                    Button withPointBtn = Button.primary(ID_PREFIX + "point-yes", "예");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "아니오");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("제목")
                            .setDescription("쿠폰(" + coupon.getCode() + ")을 적용하였습니다.\n포인트를 사용하시겠습니까?")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(withPointBtn, withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                }
            }

            case ID_PREFIX + "point" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (pointMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("데이터가 변조되었습니다.\n거래를 취소합니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
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
                            .setTitle("제목")
                            .setDescription("숫자만 입력할 수 있습니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                // 값 검증1
                if (amount % POINT_USE_UNIT != 0) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("포인트는 %d 단위로 사용할 수 있습니다.".formatted(POINT_USE_UNIT))
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                    return;
                } if (amount < POINT_USE_MINIMUM) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("포인트는 %d 이상 사용할 수 있습니다.".formatted(POINT_USE_MINIMUM))
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                // 값 검증2
                int price = pluginMap.get(userId).getPrice();

                Coupon usedCoupon = couponMap.get(userId);
                if (usedCoupon != null) {
                    price = usedCoupon.getDiscount().computeFinalPrice(amount);
                }

                if (amount > price) {
                    Button retryBtn = Button.primary(ID_PREFIX + "point-yes", "다시 입력하기");
                    Button withoutPointBtn = Button.secondary(ID_PREFIX + "point-no", "포인트 없이 진행");
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("포인트는 결제 금액 이하로 사용할 수 있습니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .addActionRow(retryBtn, withoutPointBtn, CANCEL_BUTTON)
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                // 값 검증3
                int finalPrice = price - amount;
                if (finalPrice == 0) {
                    // Payment 객체 생성
                    Plugin plugin = pluginMap.get(userId);
                    String note = event.getUser().getEffectiveName() + " | 신용카드 [" + plugin.getKRName() + "]";
                    Product product = new PremiumPluginProduct(
                            plugin,
                            note
                    );

                    int usedPoint = pointMap.get(userId);
                    Payment payment = new Payment(
                            UUID.randomUUID(), userId, PaymentMethod.NONE,
                            usedPoint, usedCoupon,
                            product
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
                            .setTitle("결제가 요청되었습니다.")
                            .setDescription("승인하시겠습니까?")
                            .build();
                    ticketChannel.sendMessageEmbeds(embed1)
                            .setActionRow(approveBtn, rejectBtn)
                            .queue();

                    MessageEmbed embed2 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("제목")
                            .setDescription("포인트(\" + amount + \"원)를 적용하였습니다.\n결제할 금액이 남아있지 않습니다.")
                            .build();
                    MessageEmbed embed3 = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1141625729386287206> 티켓 생성 완료! <a:success:1141625729386287206>")
                            .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                    "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                            .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                            .build();
                    event.replyEmbeds(embed2, embed3)
                            .setEphemeral(true)
                            .queue();
                }

                // 데이터 저장
                pointMap.put(userId, amount);

                // 메시지 전송
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("제목")
                        .setDescription("포인트(" + amount + "원)를 적용하였습니다.\n결제수단을 선택해주세요.")
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(createPaymentMethodSelectMenu())
                        .addActionRow(CANCEL_BUTTON)
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "credit-card" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String cardNumber = event.getValue("card-number").getAsString();
                String cardExpirationMonth = event.getValue("card-expiration-month").getAsString();
                String cardExpirationYear = event.getValue("card-expiration-year").getAsString();
                String customerBirthdate = event.getValue("customer-birthdate").getAsString();
                String customerEmail = event.getValue("customer-email") != null ? event.getValue("customer-email").getAsString() : null;

                // SecureSalt 생성
                String secureSalt, key;
                boolean isKeyUsable;
                do {
                    secureSalt = new String(TokenUtil.generateBytes());
                    key = new String(
                            (secureSalt + userId).getBytes(),
                            0,
                            32
                    );

                    isKeyUsable = key.getBytes().length == 32
                            && new String(key.getBytes(), 0, 15).getBytes().length == 16;
                } while (!isKeyUsable);

                // Payment 객체 생성
                Plugin plugin = pluginMap.get(userId);
                String note = event.getUser().getEffectiveName() + " | 신용카드 [" + plugin.getKRName() + "]";
                Product product = new PremiumPluginProduct(
                        plugin,
                        note
                );

                int usedPoint = pointMap.get(userId);
                Coupon usedCoupon = couponMap.get(userId);
                CreditCardPayment payment;
                try {
                    payment = new CreditCardPayment(
                            product, userId,
                            cardNumber, cardExpirationYear, cardExpirationMonth, 0,
                            customerBirthdate, customerEmail, event.getUser().getEffectiveName(),
                            usedPoint, usedCoupon,
                            secureSalt
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("결제 정보 생성중 오류가 발생했습니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    PaymentLogger.warning(new EmbedBuilder()
                            .setTitle("결제 정보 생성중 오류가 발생했습니다.")
                            .setDescription(ex.getMessage()));

                    stopProcess(userId);
                    return;
                }

                // 결제 요청
                try {
                    TossPaymentsUtil.request(payment);
                } catch (IOException | IllegalStateException | InterruptedException | ParseException ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("결제 요청 중 오류가 발생했습니다.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    PaymentLogger.warning(new EmbedBuilder()
                            .setTitle("결제 요청 중 오류가 발생했습니다.")
                            .setDescription("결제번호: " + payment.getPaymentId() + "\n" +
                                    "결제금액: " + payment.getProduct().getPrice() + "원\n" +
                                    "결제수단: 신용카드\n" +
                                    "승인시각: " + DATE_FORMAT.format(payment.getApprovedAt()) + "\n" +
                                    "결제자: " + event.getUser().getAsMention() + "\n" +
                                    "구매한 플러그인: " + plugin.getENName() + "\n" +
                                    "사용된 포인트: " + usedPoint + "\n" +
                                    "사용된 쿠폰: " + (usedCoupon != null ? usedCoupon.getCode() : "없음") + "\n" +
                                    "생성된 티켓: 없음"));

                    stopProcess(userId);
                    payment.updateAccepted(false);
                }

                // DB 기록
                try {
                    PaymentService paymentService = DatabaseManager.getPaymentService();
                    paymentService.saveData(payment);
                } catch (Exception ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("결제 정보 저장 중 오류가 발생했습니다.\n\n카드사에 비용이 청구되었을 수 있으니,\n티켓을 통하여 관리자에게 문의해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    PaymentLogger.error(new EmbedBuilder()
                            .setTitle("결제 정보 저장 중 오류가 발생했습니다.")
                            .setDescription("결제번호: " + payment.getPaymentId() + "\n" +
                                    "결제금액: " + payment.getProduct().getPrice() + "원\n" +
                                    "결제수단: 신용카드\n" +
                                    "승인시각: " + DATE_FORMAT.format(payment.getApprovedAt()) + "\n" +
                                    "결제자: " + event.getUser().getAsMention() + "\n" +
                                    "구매한 플러그인: " + plugin.getENName() + "\n" +
                                    "사용된 포인트: " + usedPoint + "\n" +
                                    "사용된 쿠폰: " + (usedCoupon != null ? usedCoupon.getCode() : "없음") + "\n" +
                                    "생성된 티켓: 없음"));

                    stopProcess(userId);
                    return;
                }

                if (!payment.isAccepted()) return;

                // 금액 계산
                int finalPrice = payment.getFinalPrice();

                // 결제 로그
                PaymentLogger.info(new EmbedBuilder()
                        .setTitle("결제가 완료되었습니다.")
                        .setDescription("결제번호: " + payment.getPaymentId() + "\n" +
                                "결제금액: " + payment.getProduct().getPrice() + "원\n" +
                                "실결제금액: " + finalPrice + "원\n" +
                                "결제수단: 신용카드\n" +
                                "승인시각: " + DATE_FORMAT.format(payment.getApprovedAt()) + "\n" +
                                "결제자: " + event.getUser().getAsMention() + "\n" +
                                "구매한 플러그인: " + plugin.getENName() + "\n" +
                                "사용된 포인트: " + usedPoint + "\n" +
                                "사용된 쿠폰: " + (usedCoupon != null ? usedCoupon.getCode() : "없음") + "\n" +
                                "생성된 티켓: 없음")
                );

                affectPayment(payment);

                // 다운로드 안내 전송
                String threadMention = "<#" + plugin.getThreadId() + ">";

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 결제가 완료되었습니다! <a:success:1141625729386287206>")
                        .setDescription("> **🥳 축하드려요! 결제가 성공적으로 완료되었습니다!**\n" +
                                "> **또한, [여기를 클릭](" + payment.getReceiptUrl() + ")하여 영수증을 확인하실 수 있어요. 🧾**\n" +
                                "> **" + threadMention + " 에서 다운로드 받으실 수 있습니다. 감사합니다! 🙏**\n\u1CBB")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                // 세션 데이터 삭제
                stopProcess(userId);
            }

            case ID_PREFIX + "bank-transfer" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String depositor = event.getValue("depositor").getAsString();

                // Payment 객체 생성
                Plugin plugin = pluginMap.get(userId);
                String note = event.getUser().getEffectiveName() + " | 계좌이체 [" + plugin.getKRName() + "]";
                Product product = new PremiumPluginProduct(
                        plugin,
                        note
                );

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
                        .setTitle("계좌이체 결제가 요청되었습니다.")
                        .setDescription("입금자명: " + depositor + "\n\n" +
                                "승인하시겠습니까?")
                        .build();
                ticketChannel.sendMessageEmbeds(embed1)
                        .setActionRow(approveBtn, rejectBtn)
                        .queue();

                MessageEmbed embed2 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 티켓 생성 완료! <a:success:1141625729386287206>")
                        .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .build();
                event.replyEmbeds(embed2)
                        .setEphemeral(true)
                        .queue();

                // 세션 데이터 삭제
                stopProcess(userId);
            }

            case ID_PREFIX + "cultureland" -> {
                // 변수 선언
                long userId = event.getUser().getIdLong();

                // 진행도 검증
                if (!pluginMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("진행중인 거래가 존재하지 않습니다.\n처음부터 다시 시도해주세요.")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    stopProcess(userId);
                    return;
                } else if (!pointMap.containsKey(userId)) return;

                // 입력값 추출
                String pinNumber = event.getValue("pin-number").getAsString();

                // Payment 객체 생성
                Plugin plugin = pluginMap.get(userId);
                String note = event.getUser().getEffectiveName() + " | 문화상품권 [" + plugin.getKRName() + "]";
                Product product = new PremiumPluginProduct(
                        plugin,
                        note
                );

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
                        .setTitle("문화상품권 결제가 요청되었습니다.")
                        .setDescription("핀번호: " + pinNumber + "\n\n" +
                                "승인하시겠습니까?")
                        .build();
                ticketChannel.sendMessageEmbeds(embed1)
                        .setActionRow(approveBtn, rejectBtn)
                        .queue();

                MessageEmbed embed2 = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 티켓 생성 완료! <a:success:1141625729386287206>")
                        .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                                "> **" + ticketChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .build();
                event.replyEmbeds(embed2)
                        .setEphemeral(true)
                        .queue();

                // 세션 데이터 삭제
                stopProcess(userId);
            }
        }
    }

    // UTILITY
    private void stopProcess(long userId) {
        pluginMap.remove(userId);
        pointMap.remove(userId);
        couponMap.remove(userId);
    }

    private SelectMenu createPaymentMethodSelectMenu() {
        return StringSelectMenu.create(ID_PREFIX + "payment-method")
                .setPlaceholder("결제수단을 선택해주세요.")
                .addOption("카드", "credit_card", "카드로 결제합니다.", Emoji.fromUnicode("💳"))
                .addOption("계좌이체", "bank_transfer", "계좌이체로 결제합니다.", Emoji.fromUnicode("💰"))
                .addOption("문화상품권", "cultureland", "문화상품권로 결제합니다.", Emoji.fromUnicode("🪙"))
                .build();
    }

    private TextChannel createTicketChannel(Payment payment) {
        // 변수 선언
        long userId = payment.getRequestedBy();
        TicketService ticketService = DatabaseManager.getTicketService();

        // 카테고리 로딩
        JDA jda = DiscordBotManager.getInstance().getJda();
        Category category = jda.getCategoryById(TICKET_CATEGORY_ID);

        // 채널 생성
        long ticketIndex = ticketService.getLastIndex() + 1;
        String userName = jda.getUserById(userId).getEffectiveName();
        TicketType ticketType = TicketType.PAYMENT;
        TextChannel textChannel = category.createTextChannel(ticketIndex + "-" + userName + "-" + ticketType.getName())
                .addMemberPermissionOverride(userId, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .complete();

        // Ticket Data 등록
        ticketService.recordTicket(new Ticket(
                userId,
                0,
                textChannel.getIdLong(),
                ticketType,
                ticketIndex
        ));

        // Ticket User Data 등록
        net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);

        TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
        ticketUserDataRepository.registerUser(userId, user);

        // Ticket Type Data 등록
        Map<Long, TicketType> userTicketTypeMap = TicketType.getUserTicketTypeMap();
        userTicketTypeMap.put(userId, ticketType);

        // Modal Data 등록
        int usedPoint = payment.getUsedPoint();
        Coupon usedCoupon = payment.getUsedCoupon();
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        int price = payment.getProduct().getPrice();
        if (usedCoupon != null) price = usedCoupon.getDiscount().computeFinalPrice(price);
        price = price - usedPoint;

        TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();
        ticketModalDataRepository.registerModalData(
                textChannel.getIdLong(),
                payment.getProduct().getNote(),
                "자동결제 승인요청 (" + payment.getMethod().getKRName() + ")",
                """
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
                )
        );

        return textChannel;
    }

    private void affectPayment(Payment payment) {
        // 변수 선언
        long userId = payment.getRequestedBy();
        int usedPoint = payment.getUsedPoint();
        CouponState usedCoupon = payment.getUsedCoupon();
        PremiumPluginProduct product = payment.getProduct().asPremiumPlugin();
        Plugin plugin = product.getPlugin();

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
        JDA jda = DiscordBotManager.getInstance().getJda();
        Guild guild = jda.getGuildById(configProvider.getString("GUILD_ID"));
        Role buyerRole = guild.getRoleById(configProvider.getString("BUYER_ROLE_ID"));
        Role pluginBuyerRole = guild.getRoleById(plugin.getBuyerRole());

        guild.addRoleToMember(UserSnowflake.fromId(userId), buyerRole).queue();
        guild.addRoleToMember(UserSnowflake.fromId(userId), pluginBuyerRole).queue();

        // 랭크 검색
        User user = userService.getDataByDiscordId(userId);
        List<Rank> userRanks = new ArrayList<>(user.rank());

        userRanks.sort(Comparator.comparingInt(Rank::getOrdinal));
        Rank highestRank = userRanks.get(userRanks.size() - 1);

        // 랭크 특권
        if (highestRank.hasPerk(RankPerkType.CASHBACK)) {
            CashbackPerk cashbackPerk = (CashbackPerk) highestRank.getPerk(RankPerkType.CASHBACK);
            int cashbackAmount = product.getPrice() / 100 * cashbackPerk.getPercentage();
            userService.addPoint(userId, cashbackAmount);

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("제목")
                    .setDescription(format("랭크 특권으로 %,d원을 캐시백 받았습니다.", cashbackAmount))
                    .build();
            jda.getUserById(userId).openPrivateChannel().complete().sendMessageEmbeds(embed).queue();
        }

        // 랭크 지급
        PaymentService paymentService = DatabaseManager.getPaymentService();
        long totalPrice = paymentService.getTotalPaidPrice(userId);

        if (totalPrice >= 500000 && !userRanks.contains(RankRepository.getInstance().getRank(3))) {
            Rank rank3 = RankRepository.getInstance().getRank(3);
            RankUtil.giveRank(userId, rank3);
        } if (totalPrice >= 1000000 && !userRanks.contains(RankRepository.getInstance().getRank(4))) {
            Rank rank4 = RankRepository.getInstance().getRank(4);
            RankUtil.giveRank(userId, rank4);
        } if (totalPrice >= 3000000 && !userRanks.contains(RankRepository.getInstance().getRank(5))) {
            Rank rank5 = RankRepository.getInstance().getRank(5);
            RankUtil.giveRank(userId, rank5);
        }
    }
} // TODO : 메시지 디자인 & 코드 청소 (디자인 후)