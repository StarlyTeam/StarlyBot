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
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        OptionMapping user = event.getOption("유저");
        OptionMapping paymentId = event.getOption("거래id");
        if (user != null && paymentId != null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("거래내역")
                    .setDescription("조회 조건은 하나만 선택해주세요.")
                    .build();
            event.replyEmbeds(embed).queue();
        } else if (user != null) {
            PaymentService paymentService = DatabaseManager.getPaymentService();
            List<Payment> payments = paymentService.getDataByUserId(user.getAsUser().getIdLong());

            StringBuilder list = new StringBuilder();
            for (Payment payment : payments) {
                list
                        .append(payment.getPaymentId())
                        .append("\n");
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("제목")
                    .setDescription("```\n" + list + "```")
                    .build();
            event.replyEmbeds(embed).queue();
        } else if (paymentId != null) {
            PaymentService paymentService = DatabaseManager.getPaymentService();
            Payment payment = paymentService.getDataByPaymentId(paymentId.getAsString());

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
                        payment.getPaymentId(),
                        payment.getProduct().getNote(),
                        payment.getMethod().getKRName(),
                        payment.getUsedCoupon() == null ? "없음" : payment.getUsedCoupon().getCode() + "(" + payment.getUsedCoupon().getDiscount() + ")",
                        payment.getUsedPoint(),
                        payment.getFinalPrice(),
                        "<@" + payment.getRequestedBy() + ">",
                        DATE_FORMAT.format(payment.getApprovedAt()),
                        payment.isAccepted() ? "승인됨" : "거절됨"
            ));

            switch (payment.getMethod()) {
                case CREDIT_CARD -> {
                    CreditCardPayment payment1 = (CreditCardPayment) payment;

                    info.append("""
                            
                            
                            **‼️ 민감한 고객 정보가 복호화 되어있습니다. ‼️**
                            **‼️ 정보 조회시 각별히 주의하시기 바랍니다. ‼️**
                            
                            > 결제 Key
                            > %s
                            
                            > 카드번호 (Masked-Raw)
                            > %s ||%s||
                            
                            > 카드 유효기간 월-일
                            > ||%s-%s||
                            
                            > 카드 할부기간
                            > ||%d개월||
                            
                            > 고객 생년월일
                            > ||%s||
                            
                            > 고객 이메일
                            > ||%s||
                            
                            > 고객 이름
                            > ||%s||
                            
                            > 영수증
                            > ||%s||
                            """.formatted(
                            payment1.getPaymentKey(),
                            payment1.getCardNumber(),
                            payment1.getMaskedCardNumber(),
                            payment1.getCardExpirationMonth(),
                            payment1.getCardExpirationYear(),
                            payment1.getCardInstallmentPlan(),
                            payment1.getCustomerBirthdate(),
                            payment1.getCustomerEmail(),
                            payment1.getCustomerName(),
                            payment1.getReceiptUrl()
                    ));
                }

                case BANK_TRANSFER -> {
                    BankTransferPayment payment1 = (BankTransferPayment) payment;

                    info.append("""
                            
                            
                            > 입금자명
                            > %s
                            """.formatted(
                            payment1.getDepositor()
                    ));
                }

                case CULTURELAND -> {
                    CulturelandPayment payment1 = (CulturelandPayment) payment;

                    info.append("""
                            
                            
                            > 문화상품권 핀번호
                            > %s
                            """.formatted(
                            payment1.getPinNumber()
                    ));
                }
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("제목")
                    .setDescription("```\n" + info + "\n```")
                    .build();
            event.replyEmbeds(embed).queue();
        } else {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("거래내역")
                    .setDescription("조회 조건을 설정해주세요.")
                    .build();
            event.replyEmbeds(embed).queue();
        }
    }
} // TODO: 테스트