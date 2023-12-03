package kr.starly.discordbot.util.security;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class RoleChecker {

    private RoleChecker() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String ADMIN_ROLE = configProvider.getString("ADMIN_ROLE");
    private static final String VERIFIED_ROLE_ID = configProvider.getString("VERIFIED_ROLE_ID");

    public static boolean hasAdminRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getGuild();
        Role adminRole = guild.getRoleById(ADMIN_ROLE);

        return hasRole(member, adminRole);
    }

    public static boolean hasVerifiedRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getGuild();
        Role role = guild.getRoleById(VERIFIED_ROLE_ID);

        return hasRole(member, role);
    }

    public static boolean hasRole(Member member, Role role) {
        List<Role> memberRoles = member.getRoles();
        return memberRoles.contains(role);
    }
}