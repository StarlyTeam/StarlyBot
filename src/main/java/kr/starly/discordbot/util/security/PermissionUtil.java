package kr.starly.discordbot.util.security;

import kr.starly.discordbot.configuration.ConfigProvider;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;

import java.awt.Color;

public class PermissionUtil {

    private PermissionUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();

    public static boolean hasPermission(Member member, Permission permission) {
        if (member == null) return false;

        return member.hasPermission(permission);
    }

    public static void sendPermissionError(MessageChannel channel) {
        channel.sendMessageEmbeds(createEmbed()).queue();
    }

    public static void sendPermissionError(IReplyCallback callback) {
        callback.replyEmbeds(createEmbed()).queue();
    }

    public static void sendPermissionError(Message message) {
        message.replyEmbeds(createEmbed()).queue();
    }

    private static MessageEmbed createEmbed() {
        return new EmbedBuilder()
                .setColor(Color.decode(configProvider.getString("EMBED_COLOR_ERROR")))
                .setTitle("<a:loading:1168266572847128709> 오류 | 사용권한 <a:loading:1168266572847128709>")
                .setDescription("> **이 기능을 사용할 권한이 없습니다.**")
                .build();
    }
}