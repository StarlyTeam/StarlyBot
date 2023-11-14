package kr.starly.discordbot.listener.pluginmanager;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;

@BotEvent
public class ListInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String PLUGIN_MANAGEMENT_CHANNEL_ID = configProvider.getString("PLUGIN_MANAGEMENT_CHANNEL_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;

        String componentId = event.getComponentId();
        if (componentId.equals("plugin-management-action")) {
            String selectedOption = event.getValues().get(0);

            if (selectedOption.equals("plugin-list")) {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }

                PluginService pluginService = DatabaseManager.getPluginService();
                List<Plugin> plugins = pluginService.getAllData();

                StringBuilder description = new StringBuilder();
                for (Plugin plugin : plugins) {
                    description
                            .append("> **")
                            .append(plugin.getENName())
                            .append(" (" + plugin.getKRName() + ")")
                            .append("**\n");
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 목록 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            %s
                            ─────────────────────────────────────────────────"""
                                .formatted(description.toString())
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                event.editSelectMenu(event.getSelectMenu()).queue();
            }
        }
    }
}