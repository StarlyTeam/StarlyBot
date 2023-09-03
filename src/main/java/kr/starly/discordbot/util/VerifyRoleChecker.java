package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class VerifyRoleChecker {

    private VerifyRoleChecker() {}

    private final static ConfigProvider configProvider = ConfigProvider.getInstance();
    private final static String AUTH_ROLE = configProvider.getString("AUTH_ROLE");
    private final static String GUILD_ID = configProvider.getString("GUILD_ID");

    public static boolean hasVerifyRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getJda().getGuildById(GUILD_ID);
        Role role = guild.getRoleById(AUTH_ROLE);

        return member.getRoles().contains(role);
    }
}