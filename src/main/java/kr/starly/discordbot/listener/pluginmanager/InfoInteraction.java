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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.stream.Collectors;

@BotEvent
public class InfoInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String PLUGIN_MANAGEMENT_CHANNEL_ID = configProvider.getString("PLUGIN_MANAGEMENT_CHANNEL_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final String ID_PREFIX = "plugin-info-";

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;

        String componentId = event.getComponentId();
        if (componentId.equals("plugin-management-action")) {
            String selectedOption = event.getValues().get(0);

            if (selectedOption.equals("plugin-info")) {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event.getChannel());
                    return;
                }

                TextInput name = TextInput.create("name-en", "플러그인 이름 (영어)", TextInputStyle.SHORT)
                        .setPlaceholder("조회할 플러그인의 영어 이름을 입력해주세요.")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create(ID_PREFIX + "modal", "플러그인 정보")
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
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!event.getModalId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String modalId = event.getModalId();
        if (modalId.equals(ID_PREFIX + "modal")) {
            String ENName = event.getInteraction().getValue("name-en").getAsString();

            PluginService pluginService = DatabaseManager.getPluginService();
            if (pluginService.getDataByENName(ENName) == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 미존재 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **존재하지 않는 플러그인입니다.**
                            
                            ─────────────────────────────────────────────────"""
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            Plugin plugin = pluginService.getDataByENName(ENName);
            StringBuilder info = new StringBuilder()
                    .append("플러그인 이름(영문): %s \n".formatted(plugin.getENName()))
                    .append("플러그인 이름(한글): %s \n".formatted(plugin.getKRName()))
                    .append("플러그인 버전: %s \n".formatted(plugin.getVersion()))
                    .append("위키 링크: [바로가기](%s) \n".formatted(plugin.getWikiUrl()))
                    .append("영상 링크: [바로가기](%s) \n".formatted(plugin.getVideoUrl()))
                    .append("담당자: %s \n".formatted(plugin.getManager().stream().map(managerId -> "<@" + managerId + ">").collect(Collectors.joining(", "))))
                    .append("의존성: %s \n".formatted(String.join(", ", plugin.getDependency())))
                    .append("이모지: `%s` \n".formatted(plugin.getEmoji()))
                    .append("아이콘: [바로가기](%s) \n".formatted(plugin.getIconUrl()))
                    .append(".gif: [바로가기](%s) \n".formatted(plugin.getGifUrl()))
                    .append("가격: %d \n".formatted(plugin.getPrice()))
                    .append("구매자 역할: <@&%d> \n".formatted(plugin.getBuyerRole()))
                    .append("포스트: <#%s> \n".formatted(plugin.getThreadId()));

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 정보 | 플러그인 <a:loading:1168266572847128709>")
                    .setDescription("""
                            %s
                            ─────────────────────────────────────────────────"""
                            .formatted(info.toString())
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            event.replyEmbeds(embed)
                    .setEphemeral(true)
                    .queue();
        }
    }
}