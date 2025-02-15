package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.service.VerifyService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@BotSlashCommand(
        command = "인증내역",
        description = "인증 내역을 확인합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"인증 내역을 확인할 유저를 선택해주세요."},
        optionRequired = {true}
)
public class VerifyHistoryCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        User target = event.getOption("유저").getAsUser();

        VerifyService verifyService = DatabaseManager.getVerifyService();
        List<Verify> verifies = verifyService.getDataByUserId(target.getIdLong());

        StringBuilder list = new StringBuilder();
        if (verifies.isEmpty()) {
            list.append("없음\n");
        } else {
            list.append("| 유저 | 일시 | IP | 토큰 | DM |\n");

            for (Verify verify : verifies) {
                list
                        .append("| %s | %s | %s | %s | %s |".formatted(
                                verify.getUserId(),
                                DATE_FORMAT.format(verify.getVerifiedAt()),
                                verify.getUserIp(),
                                verify.getToken(),
                                verify.isDMSent() ? "전송 성공" : "전송 실패"
                        ))
                        .append("\n");
            }
        }

        event.replyEmbeds(new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 목록 | 인증 내역 <a:loading:1168266572847128709>")
                .setDescription("""
                            ```
                            %s```
                            """.formatted(list.toString()))
                .build()).queue();
    }
}