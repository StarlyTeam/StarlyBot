package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.service.*;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@BotSlashCommand(
        command = "개인정보삭제",
        description = "개인정보를 삭제합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"개인정보를 삭제할 유저를 선택합니다."},
        optionRequired = {true}
)
public class ClearPersonalInfoCommand implements DiscordSlashCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
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

        event.reply("성공적으로 개인정보를 삭제했습니다. (블랙리스트, 경고 제외)").queue();
    }
}