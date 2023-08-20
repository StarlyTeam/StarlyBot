package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class VerifyRoleChecker {

    private VerifyRoleChecker() {}

    private final static ConfigManager configManager = ConfigManager.getInstance();
    private final static String AUTHORIZED_ROLE_ID = configManager.getString("AUTHORIZED_ROLE_ID");
    private final static String GUILD_ID = configManager.getString("GUILD_ID");

    public static boolean hasVerifyRole(Member member) {
        Guild guild = DiscordBotManager.getInstance().getJda().getGuildById(GUILD_ID);
        Role role = guild.getRoleById(AUTHORIZED_ROLE_ID);

        return member.getRoles().contains(role);
    }
}