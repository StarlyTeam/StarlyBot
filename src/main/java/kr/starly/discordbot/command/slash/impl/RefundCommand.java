package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;
import kr.starly.discordbot.enums.PaymentMethod;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.RankUtil;
import kr.starly.discordbot.util.external.TossPaymentsUtil;
import kr.starly.discordbot.util.messaging.PaymentLogger;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@BotSlashCommand(
        command = "환불",
        description = "결제건을 환불처리합니다.",
        optionName = {"거래id"},
        optionType = {OptionType.STRING},
        optionDescription = {"환불처리를 할 거래의 ID를 입력해 주세요."},
        optionRequired = {true}
)
public class RefundCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String paymentId = event.getOption("거래id").getAsString();

        PaymentService paymentService = DatabaseManager.getPaymentService();
        Payment payment = paymentService.getDataByPaymentId(paymentId);
        if (payment == null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:cross:1058939340505497650> 오류 | 환불 <a:cross:1058939340505497650>")
                    .setDescription("> **해당하는 거래를 찾을 수 없습니다.**")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        }

        if (payment.isRefunded()) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:cross:1058939340505497650> 오류 | 환불 <a:cross:1058939340505497650>")
                    .setDescription("> **이미 처리가 완료된 거래건입니다.**")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        }

        if (payment.getMethod() == PaymentMethod.CREDIT_CARD) {
            CreditCardPayment payment1 = (CreditCardPayment) payment;

            try {
                TossPaymentsUtil.refund(payment1, "기타");
            } catch (Exception ex) {
                ex.printStackTrace();

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:cross:1058939340505497650> 오류 | 환불 <a:cross:1058939340505497650>")
                        .setDescription("> **환불처리 중 오류가 발생하였습니다.*")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                PaymentLogger.error(
                        new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:cross:1058939340505497650> 오류 | 환불 <a:cross:1058939340505497650>")
                                .setDescription("""
                                        > **환불처리 중 오류가 발생하였습니다.**
                                        
                                        > **결제 번호: %s**
                                        > **오류: %s**
                                        """.formatted(payment.getPaymentId().toString(), ex.getMessage())
                                )
                                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                                .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                );
                return;
            }

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

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 환불 <a:success:1168266537262657626>")
                    .setDescription("> **환불 처리가 완료되었습니다.**"
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        } else {
            String paymentIdForId = payment.getPaymentId().toString().replace("-", "_");
            Button button = Button.danger("refund-start-" + paymentIdForId, "입력하기");

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 입력 | 환불 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **환불처리를 받기 위해 환불 계좌를 입력해 주세요.**
                                                                
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("스탈리에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();
            event.replyEmbeds(embed)
                    .addActionRow(button)
                    .queue();
        }

        payment.updateRefundedAt(new Date());
    }
}