package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.service.UserInfoService;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.logging.Logger;

@BotEvent
public class GuildJoinListener extends ListenerAdapter {

    private final UserInfoService userInfoService;

    public GuildJoinListener() {
        this.userInfoService = DatabaseConfig.getUserInfoService();
    }

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        String discordId = event.getUser().getId();
        String ip = "";
        LocalDateTime joinDate = LocalDateTime.now();

        if (userInfoService.getUserInfo(discordId) == null) {
            userInfoService.recordUserInfo(discordId, ip, joinDate);
            LOGGER.severe("서버에 처음 접속하였으므로 데이터를 추가했습니다: " + discordId);
        } else {
            LOGGER.severe("이미 데이터베이스에 존재하는 유저입니다: " + discordId);
        }
    }
}
