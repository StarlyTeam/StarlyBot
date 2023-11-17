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
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@BotSlashCommand(
        command = "환불",
        description = "결제건을 환불처리합니다.",
        optionName = {"거래id"},
        optionType = {OptionType.STRING},
        optionDescription = {"환불처리할 거래의 ID를 입력해 주세요."},
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
            event.replyEmbeds(
                    new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("제목")
                            .setDescription("해당하는 거래를 찾을 수 없습니다.")
                            .build()
            ).queue();
            return;
        }

        if (payment.isRefunded()) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("제목")
                    .setDescription("이미 환불처리된 거래건을 다시 환불할 수 없습니다.")
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
                        .setTitle("제목")
                        .setDescription("환불처리중 오류가 발생했습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                PaymentLogger.error(
                        new EmbedBuilder()
                                .setTitle("환불처리중 오류가 발생했습니다.")
                                .setDescription("결제번호: " + payment.getPaymentId() + "\n" + ex.getMessage())
                );
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
            } if (totalPrice < 1000000 && userRanks.contains(rank4)) {
                RankUtil.takeRank(userId, rank4);
            } if (totalPrice < 500000 && userRanks.contains(rank3)) {
                RankUtil.takeRank(userId, rank3);
            } if (totalPrice < 0 && userRanks.contains(rank2)) {
                RankUtil.takeRank(userId, rank2);
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("제목")
                    .setDescription("환불처리가 완료되었습니다.")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        } else {
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
            Modal modal = Modal.create("refund-" + paymentId, "환불계좌 입력")
                    .addActionRow(holder)
                    .addActionRow(number)
                    .addActionRow(bank)
                    .build();
            event.replyModal(modal).queue();
        }

        payment.updateRefundedAt(new Date());
    }
} // TODO: 메시지 작업, 테스트