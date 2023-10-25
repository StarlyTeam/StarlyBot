package kr.starly.discordbot.util.security;

import kr.starly.discordbot.configuration.ConfigProvider;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.Color;

public class PermissionUtil {

    private PermissionUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();

    public static boolean hasPermission(Member member, Permission permission) {
        if (member == null) return false;

        return member.hasPermission(permission);
    }

    public static void sendPermissionError(MessageChannel channel) {
        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(Color.decode(configProvider.getString("EMBED_COLOR_ERROR")))
                .setTitle("<a:loading:1141623256558866482> 오류 | 권한 없음 <a:loading:1141623256558866482>")
                .setDescription("**이 명령어를 사용할 권한이 없습니다.**")
                .build();

        channel.sendMessageEmbeds(messageEmbed).queue();
    }
}