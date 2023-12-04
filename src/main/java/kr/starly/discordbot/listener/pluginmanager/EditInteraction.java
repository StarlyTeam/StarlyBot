package kr.starly.discordbot.listener.pluginmanager;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginFileService;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.PluginFileUtil;
import kr.starly.discordbot.util.PluginForumUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@BotEvent
public class EditInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String PLUGIN_MANAGEMENT_CHANNEL_ID = configProvider.getString("PLUGIN_MANAGEMENT_CHANNEL_ID");
    private final String UPDATE_ANNOUNCEMENT_CHANNEL_ID = configProvider.getString("UPDATE_ANNOUNCEMENT_CHANNEL_ID");
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    private final Map<Long, String> sessionMap = new HashMap<>();
    private final List<Long> uploadSession = new ArrayList<>();

    private final String ID_PREFIX = "plugin-edit-";

    // CHAT
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (event.getAuthor().isBot()) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getMessage());
            return;
        }

        long userId = event.getAuthor().getIdLong();
        if (!(sessionMap.containsKey(userId) && uploadSession.contains(userId))) {
            try {
                event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            return;
        }

        PluginService pluginService = DatabaseManager.getPluginService();
        Plugin plugin = pluginService.getDataByENName(sessionMap.get(userId));

        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (attachments.isEmpty()) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **플러그인 파일을 첨부해 주세요.**
                                                        
                            ─────────────────────────────────────────────────"""
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();

            event.getChannel().sendMessageEmbeds(embed).queue();
            return;
        }

        List<String> errors = PluginFileUtil.uploadPluginFile(plugin, attachments);
        int successCount = attachments.size() - errors.size();
        int failCount = errors.size();

        if (successCount == 0) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **플러그인 파일을 모두 업로드하지 못했습니다.**
                            > **다시 시도해 주세요.**
                                                        
                            ─────────────────────────────────────────────────"""
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();

            event.getMessage().replyEmbeds(embed).queue();
            return;
        } else if (failCount == 0) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 관리 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **플러그인 파일을 모두 업로드하였습니다. (채널이 5초뒤 청소됩니다.)**
                                                        
                            ─────────────────────────────────────────────────"""
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            event.getMessage().replyEmbeds(embed).queue();
        } else {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 관리 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **%d개의 파일을 제외한 %d개의 파일을 업로드하였습니다.**
                            > **%s**
                                                        
                            ─────────────────────────────────────────────────"""
                            .formatted(
                                    failCount,
                                    successCount,
                                    String.join("\n", errors)
                            )
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            event.getMessage().replyEmbeds(embed).queue();
        }

        sessionMap.remove(userId);
        uploadSession.remove(userId);
    }

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String selectedOption = event.getValues().get(0);

        String componentId = event.getComponentId();
        if (componentId.equals("plugin-management-action")) {
            if (selectedOption.equals("plugin-edit")) {
                TextInput name = TextInput.create("name-en", "플러그인 이름 (영어)", TextInputStyle.SHORT)
                        .setPlaceholder("수정할 플러그인의 영어 이름을 입력해 주세요.")
                        .setRequired(true)
                        .build();

                Modal modal = Modal.create(ID_PREFIX + "modal", "플러그인 수정")
                        .addActionRow(name)
                        .build();
                event.replyModal(modal).queue();

                event.editSelectMenu(event.getSelectMenu()).queue();
            }
        }

        if (!event.getComponentId().startsWith(ID_PREFIX)) return;
        switch (componentId) {
            case ID_PREFIX + "file" -> {
                switch (selectedOption) {
                    case "list" -> {
                        long userId = event.getUser().getIdLong();
                        if (!sessionMap.containsKey(userId)) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                    .setDescription("""
                                            > **요청이 거부되었습니다.**
                                                                        
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

                        PluginFileService pluginFileService = DatabaseManager.getPluginFileService();
                        List<PluginFile> pluginFiles = pluginFileService.getData(sessionMap.get(userId));

                        StringBuilder description = new StringBuilder();
                        for (PluginFile pluginFile : pluginFiles) {
                            description
                                    .append("> **")
                                    .append(pluginFile.getMcVersion())
                                    .append(" - ")
                                    .append(pluginFile.getVersion())
                                    .append("**\n");
                        }

                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR)
                                .setTitle("<a:loading:1168266572847128709> 파일 목록 | 플러그인 관리 <a:loading:1168266572847128709>")
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

                        sessionMap.remove(userId);

                        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
                    }

                    case "upload" -> {
                        long userId = event.getUser().getIdLong();
                        if (!sessionMap.containsKey(userId)) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                    .setDescription("""
                                            > **요청이 거부되었습니다.**
                                                                        
                                            ─────────────────────────────────────────────────"""
                                    )
                                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                    .build();
                            event.replyEmbeds(embed)
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        } else if (uploadSession.contains(userId)) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                    .setDescription("""
                                            > **이미 파일 업로드가 진행중입니다.**
                                                                        
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

                        uploadSession.add(userId);

                        PluginService pluginService = DatabaseManager.getPluginService();
                        Plugin plugin = pluginService.getDataByENName(sessionMap.get(userId));

                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR)
                                .setTitle("<a:loading:1168266572847128709> 파일 업로드 | 플러그인 관리 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **아래에 플러그인 파일을 첨부해 주세요.**
                                                                    
                                        ─────────────────────────────────────────────────"""
                                )
                                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                .build();
                        event.replyEmbeds(embed).queue();

                        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
                    }

                    case "delete" -> {
                        long userId = event.getUser().getIdLong();
                        if (!sessionMap.containsKey(userId)) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                    .setDescription("""
                                            > **요청이 거부되었습니다.**
                                                                        
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

                        TextInput mcVersion = TextInput.create("mc-version", "마인크래프트 버전", TextInputStyle.SHORT)
                                .setPlaceholder("삭제할 플러그인 파일의 마인크래프트 버전을 입력해주세요.")
                                .setRequired(true)
                                .build();
                        TextInput version = TextInput.create("version", "버전", TextInputStyle.SHORT)
                                .setPlaceholder("삭제할 플러그인 파일의 플러그인 버전을 입력해주세요.")
                                .setRequired(true)
                                .build();

                        Modal modal = Modal.create(ID_PREFIX + "delete", "플러그인 파일 삭제")
                                .addActionRow(mcVersion)
                                .addActionRow(version)
                                .build();
                        event.replyModal(modal).queue();

                        event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
                    }
                }
            }

            case ID_PREFIX + "info" -> {
                List<TextInput> textInputs = new ArrayList<>();

                List<String> selectedValues = event.getValues();
                if (selectedValues.contains("name-en")) {
                    TextInput name = TextInput.create("name-en", "플러그인 이름 (영어)", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 영어 이름을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(name);
                }
                if (selectedValues.contains("name-kr")) {
                    TextInput name = TextInput.create("name-kr", "플러그인 이름 (한국어)", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 한국어 이름을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(name);
                }
                if (selectedValues.contains("emoji")) {
                    TextInput emoji = TextInput.create("emoji", "이모지", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 이모지를 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(emoji);
                }
                if (selectedValues.contains("wiki-url")) {
                    TextInput wikiUrl = TextInput.create("wiki-url", "위키 URL", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 위키 URL을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(wikiUrl);
                }
                if (selectedValues.contains("icon-url")) {
                    TextInput iconUrl = TextInput.create("icon-url", "아이콘 URL", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 아이콘 URL을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(iconUrl);
                }
                if (selectedValues.contains("video-url")) {
                    TextInput videoUrl = TextInput.create("video-url", "영상 URL", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 영상 URL을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(videoUrl);
                }
                if (selectedValues.contains("gif-url")) {
                    TextInput gifUrl = TextInput.create("gif-url", "GIF URL", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 GIF URL을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(gifUrl);
                }
                if (selectedValues.contains("dependency")) {
                    TextInput dependency = TextInput.create("dependency", "종속성", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 종속성을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(dependency);
                }
                if (selectedValues.contains("manager")) {
                    TextInput manager = TextInput.create("manager", "담당자", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 담당자를 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(manager);
                }
                if (selectedValues.contains("buyer-role")) {
                    TextInput buyerRole = TextInput.create("buyer-role", "구매자 역할", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 구매자 역할을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(buyerRole);
                }
                if (selectedValues.contains("version")) {
                    TextInput version = TextInput.create("version", "버전", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 버전을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(version);
                }
                if (selectedValues.contains("price")) {
                    TextInput price = TextInput.create("price", "가격", TextInputStyle.SHORT)
                            .setPlaceholder("수정할 플러그인의 가격을 입력해주세요.")
                            .setRequired(true)
                            .build();
                    textInputs.add(price);
                }

                Modal modal = Modal.create(ID_PREFIX + "info", "플러그인 정보 수정")
                        .addComponents(textInputs.stream().map(ActionRow::of).toList())
                        .build();
                event.replyModal(modal).queue();

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
            }
        }
    }

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!event.getModalId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String modalId = event.getModalId();
        switch (modalId) {
            case ID_PREFIX + "modal" -> {
                long userId = event.getUser().getIdLong();
                PluginService pluginService = DatabaseManager.getPluginService();

                String ENName = event.getValue("name-en").getAsString();
                if (pluginService.getDataByENName(ENName) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **해당 플러그인이 존재하지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                
                                    ─────────────────────────────────────────────────"""
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.replyEmbeds(embed).queue();

                    clearChannel();
                    return;
                }

                sessionMap.put(userId, ENName);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 수정 | 플러그인 관리 <a:loading:1168266572847128709>")
                        .setDescription("""
                                > **수정할 내용을 아래에서 선택해 주세요.**
                                
                                ─────────────────────────────────────────────────"""
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                Button fileButton = Button.primary(ID_PREFIX + "file", "파일 관리");
                Button infoButton = Button.primary(ID_PREFIX + "info", "정보 관리");

                event.replyEmbeds(embed)
                        .addActionRow(fileButton, infoButton)
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "delete" -> {
                long userId = event.getUser().getIdLong();
                if (!sessionMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **요청이 거부되었습니다.**
                                                                
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

                MCVersion mcVersion;
                try {
                    String mcVersionStr = "v" + event.getValue("mc-version").getAsString().replace(".", "_");
                    mcVersion = MCVersion.valueOf(mcVersionStr);
                } catch (IllegalArgumentException ignored) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **마인크래프트 버전이 올바르지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                
                                    ─────────────────────────────────────────────────"""
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    sessionMap.remove(userId);
                    clearChannel();
                    return;
                }

                String ENName = sessionMap.get(userId);
                String version = event.getValue("version").getAsString();

                PluginFileService pluginFileService = DatabaseManager.getPluginFileService();
                PluginFile pluginFile = pluginFileService.getData(ENName, mcVersion, version);
                if (pluginFile == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **해당 플러그인 파일이 존재하지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                
                                    ─────────────────────────────────────────────────"""
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    sessionMap.remove(userId);
                    clearChannel();
                    return;
                }

                if (pluginFile.getFile().exists()) pluginFile.getFile().delete();
                pluginFileService.deleteData(ENName, mcVersion, version);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 관리 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **플러그인 파일을 성공적으로 삭제하였습니다.**
                                                            
                                ─────────────────────────────────────────────────"""
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                sessionMap.remove(userId);
                clearChannel();
            }

            case ID_PREFIX + "info" -> {
                long userId = event.getUser().getIdLong();
                if (!sessionMap.containsKey(userId)) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **요청이 거부되었습니다.**
                                                                
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

                PluginService pluginService = DatabaseManager.getPluginService();
                Plugin plugin = pluginService.getDataByENName(sessionMap.get(userId));

                StringBuilder description = new StringBuilder();

                List<ModalMapping> mappings = event.getValues();
                for (ModalMapping mapping : mappings) {
                    String mappingValue = mapping.getAsString();

                    switch (mapping.getId()) {
                        case "name-en" -> {
                            if (pluginService.getDataByENName(mappingValue) != null) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **이미 존재하는 플러그인 이름입니다. (채널이 5초뒤 청소됩니다.)**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.replyEmbeds(embed).queue();

                                clearChannel();
                                continue;
                            }

                            plugin.updateENName(mappingValue);
                            description.append("> 플러그인 이름 (영어) | [" + mappingValue + "]\n");
                        }

                        case "name-kr" -> {
                            plugin.updateKRName(mappingValue);
                            description.append("> 플러그인 이름 (한국어) | [" + mappingValue + "]\n");
                        }

                        case "emoji" -> {
                            Emoji emoji;
                            try {
                                emoji = Emoji.fromFormatted(mappingValue);
                            } catch (IllegalArgumentException ignored) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **올바른 이미지를 입력해 주세요.**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.getMessage().replyEmbeds(embed).queue();
                                continue;
                            }

                            if (emoji.getType() != Emoji.Type.UNICODE) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **유니코드 이모지로만 설정하실 수 있습니다.**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.getMessage().replyEmbeds(embed).queue();
                                continue;
                            }

                            plugin.updateEmoji(mappingValue);
                            description.append("> 이모지 | [" + mappingValue + "]\n");
                        }

                        case "wiki-url" -> {
                            plugin.updateWikiUrl(mappingValue);
                            description.append("> 위키 URL | [" + mappingValue + "]\n");
                        }

                        case "icon-url" -> {
                            plugin.updateIconUrl(mappingValue);
                            description.append("> 아이콘 URL | [" + mappingValue + "]\n");
                        }

                        case "video-url" -> {
                            plugin.updateVideoUrl(mappingValue);
                            description.append("> 영상 URL | [" + mappingValue + "]\n");
                        }

                        case "gif-url" -> {
                            plugin.updateGifUrl(mappingValue);
                            description.append("> GIF URL | [" + mappingValue + "]\n");
                        }

                        case "dependency" -> {
                            List<String> dependency = mappingValue.equals("X") ? new ArrayList<>() : Stream.of(mappingValue.split(",")).map(String::trim).toList();

                            plugin.updateDependency(dependency);
                            description.append("> 종속성 | [" + mappingValue + "]\n");
                        }

                        case "manager" -> {
                            if (!mappingValue.matches("([0-9]+,?( +)?)+$")) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **담당자가 올바르지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.replyEmbeds(embed)
                                        .setEphemeral(true)
                                        .queue();

                                clearChannel();
                                continue;
                            }

                            List<Long> manager = Stream.of(mappingValue.split(","))
                                    .map(String::trim)
                                    .map(Long::parseLong)
                                    .toList();

                            plugin.updateManager(manager);
                            description.append("> 담당자 | [" + manager.stream()
                                    .map(id -> "<@" + id + ">")
                                    .collect(Collectors.joining(", "))
                                    + "]\n");
                        }

                        case "buyer-role" -> {
                            long roleId;
                            try {
                                roleId = Long.parseLong(mappingValue);
                            } catch (NumberFormatException ignored) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **역할이 올바르지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.replyEmbeds(embed)
                                        .setEphemeral(true)
                                        .queue();

                                clearChannel();
                                continue;
                            }

                            plugin.updateBuyerRole(roleId);
                            description.append("> 구매자 역할 | [<@&" + mappingValue + ">]\n");
                        }

                        case "version" -> {
                            plugin.updateVersion(mappingValue);
                            description.append("> 버전 | [" + mappingValue + "]\n");
                        }

                        case "price" -> {
                            int price;
                            try {
                                price = Integer.parseInt(mappingValue);
                            } catch (NumberFormatException ignored) {
                                MessageEmbed embed = new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 관리 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                                > **가격이 올바르지 않습니다. (채널이 5초뒤 청소됩니다.)**
                                                                            
                                                ─────────────────────────────────────────────────"""
                                        )
                                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                        .build();
                                event.replyEmbeds(embed)
                                        .setEphemeral(true)
                                        .queue();

                                clearChannel();
                                continue;
                            }

                            plugin.updatePrice(price);
                            description.append("> 가격 | [" + mappingValue + "]\n");
                        }
                    }

                    String originENName = sessionMap.get(userId);
                    if (!plugin.getENName().equals(originENName)) {
                        pluginService.deleteDataByENName(originENName);
                    }

                    pluginService.saveData(plugin);
//                    sendUpdateAnnouncement(plugin);
                    PluginForumUtil.updatePluginChannel(plugin);

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 관리 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **플러그인 설정을 완료하였습니다.**
                                    
                                    > **%s**
                                                                
                                    ─────────────────────────────────────────────────"""
                                    .formatted(description)
                            )
                            .setThumbnail(plugin.getIconUrl())
                            .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.replyEmbeds(embed)
                            .setEphemeral(true)
                            .queue();

                    sessionMap.remove(userId);
                }
            }
        }
    }


    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!event.getComponentId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String buttonId = event.getComponentId();
        switch (buttonId) {
            case ID_PREFIX + "file" -> {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 액션 | 플러그인 관리 <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **관리 액션을 아래에서 선택해 주세요.**
                                                                
                                    ─────────────────────────────────────────────────"""
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                SelectMenu actionSelectMenu = StringSelectMenu.create(ID_PREFIX + "file")
                        .addOption("파일 목록", "list", "파일 목록을 확인합니다.")
                        .addOption("파일 업로드", "upload", "파일을 업로드합니다.")
                        .addOption("파일 삭제", "delete", "파일을 삭제합니다.")
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(actionSelectMenu)
                        .setEphemeral(true)
                        .queue();

                event.editButton(event.getButton().asDisabled()).queue();
            }

            case ID_PREFIX + "info" -> {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 수정 | 플러그인 관리 <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **수정할 항목을 아래에서 선택해 주세요.**
                                                                
                                    ─────────────────────────────────────────────────"""
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                SelectMenu infoSelectMenu = StringSelectMenu.create(ID_PREFIX + "info")
                        .addOption("영어 이름", "name-en", "영어 이름을 수정합니다.")
                        .addOption("한국어 이름", "name-kr", "한국어 이름을 수정합니다.")
                        .addOption("이모지", "emoji", "이모지를 수정합니다.")
                        .addOption("위키 URL", "wiki-url", "위키 URL을 수정합니다.")
                        .addOption("아이콘 URL", "icon-url", "아이콘 URL을 수정합니다.")
                        .addOption("영상 URL", "video-url", "영상 URL을 수정합니다.")
                        .addOption("GIF URL", "gif-url", "GIF URL을 수정합니다.")
                        .addOption("종속성", "dependency", "종속성을 수정합니다.")
                        .addOption("담당자", "manager", "담당자를 수정합니다. (콤마로 구분, 사용자 ID 입력)")
                        .addOption("구매자 역할", "buyer-role", "구매자 역할을 수정합니다. (역할 ID 입력)")
                        .addOption("버전", "version", "버전을 수정합니다.")
                        .addOption("가격", "price", "가격을 수정합니다.")
                        .setRequiredRange(1, 5)
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(infoSelectMenu)
                        .setEphemeral(true)
                        .queue();

                event.editButton(event.getButton().asDisabled()).queue();
            }
        }
    }

    // UTILITY
    private void sendUpdateAnnouncement(Plugin plugin) {
        String formattedPluginName = plugin.getENName() + "(" + plugin.getKRName() + ")";

        // TODO 마지막 작업
        MessageEmbed noticeEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("제목")
                .setDescription("내용")
                .setImage(plugin.getGifUrl())
                .setThumbnail(plugin.getIconUrl())
                .build();

        TextChannel channel = DiscordBotManager.getInstance().getJda().getTextChannelById(UPDATE_ANNOUNCEMENT_CHANNEL_ID);
        channel.sendMessageEmbeds(noticeEmbed).queue();
    }

    private void clearChannel() {
        TextChannel channel = DiscordBotManager.getInstance().getJda().getTextChannelById(PLUGIN_MANAGEMENT_CHANNEL_ID);
        channel.getHistoryFromBeginning(100).queueAfter(5, TimeUnit.SECONDS, history -> {
            List<Message> messages = new ArrayList<>(history.getRetrievedHistory());
            messages.remove(messages.size() - 1);

            channel.purgeMessages(messages);
        });
    }
}