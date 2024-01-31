package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.entity.payment.impl.BankTransferPayment;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;
import kr.starly.discordbot.entity.payment.impl.CulturelandPayment;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@BotSlashCommand(
        command = "거래내역",
        description = "거래 내역을 확인합니다.",
        optionName = {"유저", "거래id"},
        optionType = {OptionType.USER, OptionType.STRING},
        optionDescription = {"거래 내역을 확인할 유저를 선택해주세요.", "거래 내역을 확인할 거래ID를 입력해주세요."},
        optionRequired = {false, false}

)
public class PaymentHistoryCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        OptionMapping userMapping = event.getOption("유저");
        OptionMapping paymentIdMapping = event.getOption("거래id");
        if (userMapping != null && paymentIdMapping != null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 거래내역 <a:loading:1168266572847128709>")
                    .setDescription("> **조회 조건은 하나만 선택해 주세요.**")
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).queue();
        } if (paymentIdMapping != null) {
            PaymentService paymentService = DatabaseManager.getPaymentService();
            Payment rawPayment = paymentService.getDataByPaymentId(paymentIdMapping.getAsString());
            if (rawPayment == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 거래내역 <a:loading:1168266572847128709>")
                        .setDescription("> **거래 ID를 다시 확인해 주세요.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).queue();
                return;
            } else if (rawPayment.getRequestedBy() != event.getUser().getIdLong()
                    && !PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            StringBuilder info = new StringBuilder();
            info.append("""
                    > ID
                    > %s
                    
                    > 상품
                    > %s
                   
                    > 수단
                    > %s
                    
                    > 사용한 쿠폰
                    > %s
                    
                    > 사용한 포인트
                    > %,d
                   
                    > 실결제액
                    > %,d원
                   
                    > 고객
                    > %s
                   
                    > 일시
                    > %s
                   
                    > 승인 상태
                    > %s
                    """.formatted(
                        rawPayment.getPaymentId(),
                        rawPayment.getProduct().getSummary(),
                        rawPayment.getMethod().getKRName(),
                        rawPayment.getUsedCoupon() == null ? "없음" : rawPayment.getUsedCoupon().getCode() + "(" + rawPayment.getUsedCoupon().getDiscount() + ")",
                        rawPayment.getUsedPoint(),
                        rawPayment.getFinalPrice(),
                        "<@" + rawPayment.getRequestedBy() + ">",
                        rawPayment.getApprovedAt() == null ? "-" : DATE_FORMAT.format(rawPayment.getApprovedAt()),
                        rawPayment.isAccepted() ? "승인됨" : "거절됨"
            ));

            switch (rawPayment.getMethod()) {
                case CREDIT_CARD -> {
                    CreditCardPayment payment = (CreditCardPayment) rawPayment;

                    info.append("""
                            
                            
                            **‼️ 민감한 정보가 복호화 되어있습니다. ‼️**
                            **‼️ 정보 조회시 각별히 주의하시기 바랍니다. ‼️**
                            
                            > 결제 Key
                            > %s
                            
                            > 카드번호
                            > %s
                            
                            > 카드 유효기간
                            > %s/%s
                            
                            > 카드 할부기간
                            > %d개월
                            
                            > 고객 생년월일
                            > %s
                            
                            > 고객 이메일
                            > %s
                            
                            > 고객 이름
                            > %s
                            
                            > 영수증
                            > [바로가기](%s)
                            """.formatted(
                            payment.getPaymentKey(),
                            payment.getMaskedCardNumber(),
                            payment.getCardExpirationMonth(),
                            payment.getCardExpirationYear(),
                            payment.getCardInstallmentPlan(),
                            payment.getCustomerBirthdate(),
                            payment.getCustomerEmail(),
                            payment.getCustomerName(),
                            payment.getReceiptUrl()
                    ));
                }

                case BANK_TRANSFER -> {
                    BankTransferPayment payment = (BankTransferPayment) rawPayment;

                    info.append("""
                            
                            
                            > 입금자명
                            > %s
                            """.formatted(
                            payment.getDepositor()
                    ));
                }

                case CULTURELAND -> {
                    CulturelandPayment payment = (CulturelandPayment) rawPayment;

                    info.append("""
                            
                            
                            > 문화상품권 핀번호
                            > %s
                            """.formatted(
                            payment.getPinNumber()
                    ));
                }
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 정보 | 거래내역 <a:loading:1168266572847128709>")
                    .setDescription(info)
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).setEphemeral(true).queue();
        } else {
            User target;
            if (userMapping == null) {
                target = event.getUser();
            } else if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            } else {
                target = userMapping.getAsUser();
            }

            PaymentService paymentService = DatabaseManager.getPaymentService();
            List<Payment> payments = paymentService.getDataByUserId(target.getIdLong());

            StringBuilder list = new StringBuilder();
            for (Payment payment : payments) {
                list
                        .append(payment.getPaymentId())
                        .append("\n");
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 목록 | 거래내역 <a:loading:1168266572847128709>")
                    .setDescription(payments.isEmpty() ? "없음\n" : list)
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).queue();
        }
    }
}