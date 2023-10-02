package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.manager.DiscordBotManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AuditLogger {

    private final static ConfigProvider configProvider = ConfigProvider.getInstance();
    private final static String AUDIT_LOG_CHANNEL_ID = configProvider.getString("AUDIT_LOG_CHANNEL_ID");
    private final static Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final static Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final static Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("H:mm:ss a (yyyy-MM-dd)");

    private AuditLogger() {}

    public static void info(EmbedBuilder embedBuilder) {
        MessageEmbed embed = new EmbedBuilder(embedBuilder)
                .setFooter("일반 | " + DATE_FORMAT.format(new Date()))
                .setColor(EMBED_COLOR_SUCCESS)
                .build();

        sendToAuditLogChannel(embed);
    }

    public static void warning(EmbedBuilder embedBuilder) {
        MessageEmbed embed = new EmbedBuilder(embedBuilder)
                .setFooter("경고 | " + DATE_FORMAT.format(new Date()))
                .setColor(EMBED_COLOR)
                .build();

        sendToAuditLogChannel(embed);
    }

    public static void error(EmbedBuilder embedBuilder) {
        MessageEmbed embed = new EmbedBuilder(embedBuilder)
                .setFooter("오류 | " + DATE_FORMAT.format(new Date()))
                .setColor(EMBED_COLOR_ERROR)
                .build();

        sendToAuditLogChannel(embed);
    }

    private static void sendToAuditLogChannel(MessageEmbed embed) {
        TextChannel textChannel = DiscordBotManager.getInstance().getJda().getTextChannelById(AUDIT_LOG_CHANNEL_ID);
        textChannel.sendMessageEmbeds(embed).queue();
    }
}