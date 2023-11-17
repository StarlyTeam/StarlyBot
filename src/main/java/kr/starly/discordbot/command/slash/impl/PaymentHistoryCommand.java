package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.service.PaymentService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;

@BotSlashCommand(
        command = "거래내역",
        description = "거래내역을 확인합니다.",
        optionName = {"유저", "거래id"},
        optionType = {OptionType.USER, OptionType.STRING},
        optionDescription = {"거래내역을 확인할 유저를 선택해주세요.", "거래내역을 확인할 거래ID를 입력해주세요."},
        optionRequired = {false, false}

)
public class PaymentHistoryCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

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
            switch (payment.getMethod()) {
                case BANK_TRANSFER -> { // TODO: 메시지 작업 (게좌이체로 결제한 경우) (쿠폰, 포인트 사용했을 수도 있음)
                    info
                            .append("");
                }

                case CREDIT_CARD -> { // TODO: 메시지 작업 (신용카드로 결제한 경우) (쿠폰, 포인트 사용했을 수도 있음)
                    info
                            .append("");
                }

                case CULTURELAND -> { // TODO: 메시지 작업 (문화상품권으로 결제한 경우) (쿠폰, 포인트 사용했을 수도 있음)
                    info
                            .append("");
                }

                case NONE -> { // TODO: 메시지 작업 (포인트, 쿠폰으로만 결제한 경우)
                    info
                            .append("");
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