package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Download;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.service.DownloadService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

@BotSlashCommand(
        command = "다운로드내역",
        description = "다운로드 내역을 확인합니다.",
        optionName = {"유저", "토큰"},
        optionDescription = {"다운로드 내역을 확인할 유저를 선택합니다.", "다운로드 내역을 확인할 토큰을 선택합니다."},
        optionType = {OptionType.USER, OptionType.STRING},
        optionRequired = {false, false}
)
public class DownloadHistoryCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    private final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        OptionMapping user = event.getOption("유저");
        OptionMapping token = event.getOption("토큰");

        DownloadService downloadService = DatabaseManager.getDownloadService();
        List<Download> downloads;
        if (user != null) {
            downloads = downloadService.getDataByUserId(user.getAsUser().getIdLong());
        } else if (token != null) {
            downloads = List.of(downloadService.getDataByToken(token.getAsString()));
        } else {
            downloads = downloadService.getAllData();
        }
        downloads.subList(Math.max(0, downloads.size() - 50), downloads.size());

        StringBuilder list = new StringBuilder();
        if (downloads.isEmpty()) {
            list
                    .append("없음")
                    .append("\n");
        } else {
            list.append("| 유저 | 일시 | 플러그인 | 버전1 | 버전2 |");

            for (Download download : downloads) {
                PluginFile pluginFile = download.getPluginFile();

                list
                        .append("| %s | %s | %s | %s | %s |".formatted(
                                download.getUserId(),
                                DATE_FORMAT.format(download.getUsedAt()),
                                pluginFile.getPlugin().getENName(),
                                pluginFile.getVersion(),
                                pluginFile.getMcVersion()
                        ))
                        .append("\n");
            }
        }

        event.replyEmbeds(
                new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle(" <a:loading:1168266572847128709> 목록 | 다운로드 내역 <a:loading:1168266572847128709>")
                        .setDescription("""
                                ```
                                %s```
                                """.formatted(list.toString()))
                        .build()
        ).queue();
    }
}