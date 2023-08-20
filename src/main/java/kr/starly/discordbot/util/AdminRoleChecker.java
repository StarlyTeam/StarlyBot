package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class AdminRoleChecker {

    private AdminRoleChecker() {}

    private final static ConfigManager configManager = ConfigManager.getInstance();
    private final static String ADMIN_ROLE = configManager.getString("ADMIN_ROLE");
    private final static String GUILD_ID = configManager.getString("GUILD_ID");

    public static boolean hasAdminRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getJda().getGuildById(GUILD_ID);
        Role role = guild.getRoleById(ADMIN_ROLE);

        return member.getRoles().contains(role);
    }
}