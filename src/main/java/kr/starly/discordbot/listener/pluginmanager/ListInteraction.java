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
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.List;

@BotEvent
public class ListInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        List<SelectOption> selectedOptions = event.getSelectedOptions();

        String componentId = event.getComponentId();
        if (componentId.equals("plugin-management-action")) {
            if (selectedOptions.get(0).getValue().equals("plugin-list")) {
                PluginService pluginService = DatabaseManager.getPluginService();
                List<Plugin> plugins = pluginService.getAllData();

                StringBuilder description = new StringBuilder();
                for (Plugin plugin : plugins) {
                    description
                            .append(plugin.getENName())
                            .append(" (" + plugin.getKRName() + ")")
                            .append("\n");
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("플러그인 목록")
                        .setDescription(description.toString())
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                event.editSelectMenu(event.getSelectMenu()).queue();
            }
        }
    }
} // TODO : 테스트 & 메시지 디자인   + 디스코드의 글자 수 제한은 어떻게 할 것인가?