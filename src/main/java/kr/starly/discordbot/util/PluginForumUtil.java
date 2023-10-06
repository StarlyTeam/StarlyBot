package kr.starly.discordbot.util;

import ch.qos.logback.core.encoder.JsonEscapeUtil;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumPost;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;
import org.yaml.snakeyaml.external.com.google.gdata.util.common.base.UnicodeEscaper;

import java.awt.*;
import java.io.IOException;
import java.net.URL;

public class PluginForumUtil {

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String FREE_PLUGIN_FORUM_ID = configProvider.getString("FREE_PLUGIN_FORUM_ID");
    private static final String PREMIUM_PLUGIN_FORUM_ID = configProvider.getString("PREMIUM_PLUGIN_FORUM_ID");
    private static final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    private PluginForumUtil() {}

    public static void createPluginChannel(Plugin plugin) {
        String postName = plugin.emoji() + "┃" + plugin.KRName();
        if (postName.length() > 100) {
            postName = postName.substring(0, 100);
        }

        boolean isPremium = plugin.price() != 0;

        JDA jda = DiscordBotManager.getInstance().getJda();
        ForumChannel channel = jda.getForumChannelById(isPremium ? PREMIUM_PLUGIN_FORUM_ID : FREE_PLUGIN_FORUM_ID);

        channel.createForumPost(postName, createMessageData(plugin)).queue(post -> {
            long threadId = post.getThreadChannel().getIdLong();

            PluginService pluginService = DatabaseManager.getPluginService();
            Plugin newPlugin = new Plugin(
                    plugin.ENName(),
                    plugin.KRName(),
                    plugin.emoji(),
                    plugin.wikiUrl(),
                    plugin.iconUrl(),
                    plugin.videoUrl(),
                    plugin.gifUrl(),
                    plugin.dependency(),
                    plugin.manager(),
                    plugin.buyerRole(),
                    threadId,
                    plugin.version(),
                    plugin.price()
            );
            pluginService.pluginRepository().put(newPlugin);
        });
    }

    public static void updatePluginChannel(Plugin plugin) {
        String postName = plugin.emoji() + "┃" + plugin.KRName();
        if (postName.length() > 100) {
            postName = postName.substring(0, 100);
        }

        JDA jda = DiscordBotManager.getInstance().getJda();
        ForumPost post = (ForumPost) jda.getThreadChannelById(plugin.threadId());

        post.getMessage().editMessage(MessageEditData.fromCreateData(createMessageData(plugin))).queue();
        post.getThreadChannel().getManager().setName(postName).queue();
    }

    private static MessageCreateData createMessageData(Plugin plugin) {
        try {
            MessageCreateData messageCreateData = MessageCreateData.fromEmbeds(
                    new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("제목")
                            .setDescription("내용")
                            .build()
            );
            return MessageCreateBuilder.from(messageCreateData)
                    .addFiles(FileUpload.fromData(new URL(plugin.iconUrl()).openStream(), "icon.png"))
                    .build();
        } catch (IOException ex) {
            ex.printStackTrace();

            return null;
        }
    }
} // TODO : 메시지 디자인