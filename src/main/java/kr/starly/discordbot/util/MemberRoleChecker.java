package kr.starly.discordbot.util;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class MemberRoleChecker {

    private MemberRoleChecker() {}

    public static boolean hasRole(Member member, Role role) {
        List<Role> memberRoles = member.getRoles();
        return memberRoles.contains(role);
    }
}