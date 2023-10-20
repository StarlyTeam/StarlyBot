package kr.starly.discordbot.listener.pluginmanager;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.PluginFileService;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.util.List;

@BotEvent
public class DeleteInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final String ID_PREFIX = "plugin-delete-";

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
            if (selectedOptions.get(0).getValue().equals("plugin-delete")) {
                TextInput name = TextInput.create("name-en", "플러그인 이름 (영어)", TextInputStyle.SHORT)
                        .setPlaceholder("삭제할 플러그인의 영어 이름을 입력해주세요.")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create(ID_PREFIX + "modal", "플러그인 삭제")
                        .addActionRow(name)
                        .build();
                event.replyModal(modal).queue();

                event.editSelectMenu(event.getSelectMenu()).queue();
            }
        }
    }

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String modalId = event.getModalId();
        if (modalId.equals(ID_PREFIX + "modal")) {
            String ENName = event.getInteraction().getValue("name-en").getAsString();

            PluginService pluginService = DatabaseManager.getPluginService();
            Plugin plugin = pluginService.getDataByENName(ENName);
            if (plugin == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("플러그인 삭제")
                        .setDescription("존재하지 않는 플러그인입니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            pluginService.deleteDataByENName(ENName);

            PluginFileService pluginFileService = DatabaseManager.getPluginFileService();
            List<PluginFile> pluginFiles = pluginFileService.getData(plugin.getENName());
            for (PluginFile pluginFile : pluginFiles) {
                pluginFileService.deleteData(plugin.getENName(), pluginFile.getMcVersion(), pluginFile.getVersion());
            }

            File pluginFolder = new File("plugin\\" + plugin.getENName() + "\\");
            if (pluginFolder.exists()) {
                File[] pluginFilesInFolder = pluginFolder.listFiles();
                if (pluginFilesInFolder != null) {
                    for (File pluginFile : pluginFilesInFolder) {
                        pluginFile.delete();
                    }
                }
                pluginFolder.delete();
            }

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("플러그인 삭제")
                    .setDescription("플러그인을 삭제했습니다.")
                    .build();
            event.replyEmbeds(embed)
                    .setEphemeral(true)
                    .queue();
        }
    }
} // TODO : 테스트 & 메시지 디자인