package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class RoleChecker {

    private RoleChecker() {}

    private final static ConfigProvider configProvider = ConfigProvider.getInstance();
    private final static String ADMIN_ROLE = configProvider.getString("ADMIN_ROLE");
    private final static String GUILD_ID = configProvider.getString("GUILD_ID");

    public static boolean hasAdminRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getJda().getGuildById(GUILD_ID);
        Role adminRole = guild.getRoleById(ADMIN_ROLE);

        return hasRole(member, adminRole);
    }

    public static boolean hasRole(Member member, Role role) {
        List<Role> memberRoles = member.getRoles();
        return memberRoles.contains(role);
    }
}