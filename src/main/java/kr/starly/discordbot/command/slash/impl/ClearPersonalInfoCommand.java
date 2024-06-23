package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.service.*;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;

@BotSlashCommand(
        command = "개인정보삭제",
        description = "개인정보를 삭제합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"개인정보를 삭제할 유저를 선택합니다."},
        optionRequired = {true}
)
public class ClearPersonalInfoCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        Member target = event.getOption("유저").getAsMember();

        CouponRedeemService redeemService = DatabaseManager.getCouponRedeemService();
        redeemService.deleteData(target.getIdLong());

        DownloadService downloadService = DatabaseManager.getDownloadService();
        downloadService.deleteData(target.getIdLong());

        PaymentService paymentService = DatabaseManager.getPaymentService();
        paymentService.deleteData(target.getIdLong());

        TicketService ticketService = DatabaseManager.getTicketService();
        ticketService.deleteDataByDiscordId(target.getIdLong());

        UserService userService = DatabaseManager.getUserService();
        userService.deleteDataByDiscordId(target.getIdLong());

        VerifyService verifyService = DatabaseManager.getVerifyService();
        verifyService.deleteDataByUserId(target.getIdLong());

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 개인정보삭제 <a:success:1168266537262657626>")
                .setDescription("""
                        > **성공적으로 `쿠폰 사용내역`, `다운로드 내역`, `결제 내역`, `티켓 내역`, `유저 정보`, `인증 내역`을 삭제했습니다.**
                        > **단, 이용약관에 의거하여 `티켓 내화내역`, `처벌 내역`, `블랙리스트` 등은 삭제되지 않음을 유념하시기 바랍니다.**
                        """)
                .build();
        event.replyEmbeds(embed).queue();
    }
}