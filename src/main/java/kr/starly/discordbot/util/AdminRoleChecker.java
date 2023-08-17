package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class AdminRoleChecker {

    private final static ConfigManager configManager = ConfigManager.getInstance();
    private final static String ADMIN_ROLE = configManager.getString("ADMIN_ROLE");

    public static boolean hasAdminRole(Member member) {
        for (Role role : member.getRoles()) {
            if (role.getId().equals(ADMIN_ROLE)) {
                return true;
            }
        }
        return false;
    }
}
