package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PluginForumUtil {

    private PluginForumUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String FREE_PLUGIN_FORUM_ID = configProvider.getString("FREE_PLUGIN_FORUM_ID");
    private static final String PREMIUM_PLUGIN_FORUM_ID = configProvider.getString("PREMIUM_PLUGIN_FORUM_ID");
    private static final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    public static void createPluginChannel(Plugin plugin) {
        String postName = plugin.getEmoji() + "┃" + plugin.getKRName();
        if (postName.length() > 100) {
            postName = postName.substring(0, 100);
        }

        boolean isPremium = plugin.getPrice() != 0;
        JDA jda = DiscordBotManager.getInstance().getJda();
        ForumChannel forumChannel = jda.getForumChannelById(isPremium ? PREMIUM_PLUGIN_FORUM_ID : FREE_PLUGIN_FORUM_ID);;

        forumChannel.createForumPost(postName, createMessageData(plugin)).queue(post -> {
            long threadId = post.getThreadChannel().getIdLong();
            plugin.updateThreadId(threadId);
            PluginService pluginService = DatabaseManager.getPluginService();
            pluginService.saveData(plugin);
        });
    }

    public static void updatePluginChannel(Plugin plugin) {
        JDA jda = DiscordBotManager.getInstance().getJda();

        ThreadChannel threadChannel = jda.getThreadChannelById(plugin.getThreadId());
        if (threadChannel != null) threadChannel.delete().queue();

        createPluginChannel(plugin);
    }

    private static MessageCreateData createMessageData(Plugin plugin) {
        try {
            MessageCreateBuilder messageCreateBuilder = MessageCreateBuilder.from(
                    MessageCreateData.fromEmbeds(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR)
                                    .setTitle("%s %s(%s) %s".formatted(plugin.getEmoji(), plugin.getKRName(), plugin.getENName(), plugin.getEmoji()))
                                    .setThumbnail(plugin.getIconUrl())
                                    .setImage(plugin.getGifUrl())
//                                    .addField("> **지원 버전**", "> **" + plugin.getSupportedVersionsRange() + "**", true)
//                                    .addField("> **의존성**", "> **" + String.join(", ", plugin.getDependency() + "**"), true)
//                                    .addField("> **가격**", "> **" + NumberFormat.getCurrencyInstance(Locale.KOREA).format(plugin.getPrice() + "**"), true)
//                                    .addField("> **담당자**",  "> **" + plugin.getManager().stream().map(managerId -> "<@" + managerId + ">").collect(Collectors.joining(", ")) + "**", true)
                                    .build()
                    )
                    // TODO 테스트해야하는 코드
            );

            FileUpload iconFile = FileUpload.fromData(new URL(plugin.getIconUrl()).openStream(), "icon.png");
            messageCreateBuilder.addFiles(iconFile);

            // 버튼 (구매, 다운로드, 위키, 영상)
            List<Button> components = new ArrayList<>();
            if (plugin.getPrice() != 0) {
                Button buyBtn = Button.primary("payment-start-" + plugin.getENName(), "구매");
                components.add(buyBtn);
            }

            Button editBtn = Button.secondary("pluginaction-download-" + plugin.getENName(), "다운로드");
            components.add(editBtn);

            if (plugin.getWikiUrl() != null) {
                Button wikiUrl = Button.link(plugin.getWikiUrl(), "위키");
                components.add(wikiUrl);
            } if (plugin.getVideoUrl() != null) {
                Button videoUrl = Button.link(plugin.getVideoUrl(), "영상");
                components.add(videoUrl);
            }

            return messageCreateBuilder
                    .addActionRow(components)
                    .build();
        } catch (IOException ex) {
            ex.printStackTrace();

            return null;
        }
    }
} // TODO : 메시지 디자인