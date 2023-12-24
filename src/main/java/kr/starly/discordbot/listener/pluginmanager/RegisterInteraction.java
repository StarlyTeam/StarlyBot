package kr.starly.discordbot.listener.pluginmanager;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.enums.RegisterStatus;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.PluginFileUtil;
import kr.starly.discordbot.util.PluginForumUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@BotEvent
public class RegisterInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String PLUGIN_MANAGEMENT_CHANNEL_ID = configProvider.getString("PLUGIN_MANAGEMENT_CHANNEL_ID");
    private final String RELEASE_NOTICE_CHANNEL_ID = configProvider.getString("RELEASE_NOTICE_CHANNEL_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final Map<Long, Plugin> sessionDataMap = new HashMap<>();
    private final Map<Long, RegisterStatus> sessionStatusMap = new HashMap<>();

    private final String ID_PREFIX = "plugin-register-";
    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");

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
        if (!(sessionDataMap.containsKey(userId) && sessionStatusMap.containsKey(userId))) {
            try {
                event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {}
            return;
        }

        String messageContent = event.getMessage().getContentRaw();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        switch (sessionStatusMap.get(userId)) {
            case SELECT_DEPENDENCY -> {
                List<String> dependency = messageContent.equals("X") ? new ArrayList<>() : Stream.of(messageContent.split(",")).map(String::trim).toList();
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateDependency(dependency);

                sessionDataMap.put(userId, plugin);
                sessionStatusMap.put(userId, RegisterStatus.SUBMIT_EMOJI);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 이모지 | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **[ 등록 정보 ]**
                                > **의존성 플러그인: %s**
                                                                
                                > **아래에 이모지를 입력해 주세요. (백틱 사이에 넣어주세요)**
                                > **예) `\uD83C\uDF20`**
                                
                                ─────────────────────────────────────────────────
                                """
                                .formatted(String.join(", ", dependency))
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();

                event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
            }

            case SUBMIT_EMOJI -> {
                Emoji emoji;
                try {
                    emoji = Emoji.fromFormatted(messageContent.replace("`", ""));
                } catch (IllegalArgumentException ignored) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 등록 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **올바른 이모지를 입력해 주세요.**
                                    
                                    ─────────────────────────────────────────────────
                                    """)
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
                    return;
                }

                if (emoji.getType() != Emoji.Type.UNICODE) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 등록 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **유니코드 이모지로만 설정하실 수 있습니다. (다시 시도해 주세요.)**
                                    
                                    ─────────────────────────────────────────────────
                                    """)
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
                    return;
                }

                String emojiStr = messageContent.replace("`", "");
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateEmoji(emojiStr);

                sessionDataMap.put(userId, plugin);
                sessionStatusMap.put(userId, RegisterStatus.SELECT_MANAGER);

                SelectMenu managerSelectMenu = EntitySelectMenu.create(ID_PREFIX + "manager", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder("담당자를 선택해 주세요.")
                        .setRequiredRange(1, 10)
                        .build();

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 담당자 | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **[ 등록 정보 ]**
                                > **이모지: %s**
                                > **아래에 플러그인 담당자를 선택해 주세요.**
                                
                                ─────────────────────────────────────────────────
                                """
                                .formatted("`\\" + ((UnicodeEmoji) emoji).getAsCodepoints() + "`")
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.getMessage().replyEmbeds(embed)
                        .addActionRow(managerSelectMenu)
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }

            case UPLOAD_ICON_IMAGE -> {
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateIconUrl(messageContent);

                sessionDataMap.put(userId, plugin);
                sessionStatusMap.put(userId, RegisterStatus.UPLOAD_GIF_IMAGE);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> gif | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **아이콘 이미지 설정을 완료하였습니다.**
                                > **아래에 .gif 이미지 URL을 입력해 주세요. (Cloudflare Images)**
                                
                                ─────────────────────────────────────────────────
                                """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
            }

            case UPLOAD_GIF_IMAGE -> {
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateGifUrl(messageContent);

                sessionDataMap.put(userId, plugin);
                sessionStatusMap.put(userId, RegisterStatus.UPLOAD_PLUGIN_FILE);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 파일 | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription("""
                                > **.gif 이미지를 설정을 완료하였습니다.**
                                > **아래에 플러그인 파일을 첨부해 주세요.**
                                > **예) `v1_12-1.0.jar`, `v1_17-1.1.zip`**
                                
                                ─────────────────────────────────────────────────
                                """)
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
            }

            case UPLOAD_PLUGIN_FILE -> {
                if (attachments.isEmpty()) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 등록 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **플러그인 파일을 첨부해 주세요.**
                                    
                                    ─────────────────────────────────────────────────
                                    """)
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.getMessage().replyEmbeds(embed).queue();
                    return;
                }

                Plugin plugin = sessionDataMap.get(userId);

                List<String> errors = PluginFileUtil.uploadPluginFile(plugin, attachments);
                int successCount = attachments.size() - errors.size();
                int failCount = errors.size();

                if (successCount == 0) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 등록 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **플러그인 파일을 모두 업로드하지 못했습니다. (다시 시도해 주세요.)**
                                    > **%s**
                                    
                                    ─────────────────────────────────────────────────
                                    """
                                    .formatted(String.join("\n", errors))
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();

                    event.getMessage().replyEmbeds(embed).addActionRow(CANCEL_BUTTON).queue();
                    return;
                } else if (failCount == 0) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 파일 | 플러그인 등록 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **플러그인 파일을 모두 업로드했습니다.**
                                    
                                    ─────────────────────────────────────────────────
                                    """
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.getMessage().replyEmbeds(embed).queue();
                } else {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 업로드 | 플러그인 등록 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **%d개의 파일을 제외한 %d개의 파일을 업로드하였습니다.**
                                    > **%s**
                                    
                                    "─────────────────────────────────────────────────"
                                    """
                                    .formatted(
                                            failCount,
                                            successCount,
                                            String.join("\n", errors)
                                    )
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();
                    event.getMessage().replyEmbeds(embed).queue();
                }


                PluginService pluginService = DatabaseManager.getPluginService();
                pluginService.saveData(plugin);

                sessionDataMap.remove(userId);
                sessionStatusMap.remove(userId);

                clearChannel();
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription("""
                                        > **플러그인이 등록되었습니다.**
                                        > **채널이 5초후에 자동으로 청소됩니다.**

                                        ─────────────────────────────────────────────────
                                    """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.getMessage().replyEmbeds(embed).queue();

                PluginForumUtil.createPluginChannel(plugin);
                sendReleaseAnnouncement(plugin);
            }
        }
    }

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        List<SelectOption> selectedOptions = event.getSelectedOptions();

        String componentId = event.getComponentId();
        if (componentId.equals("plugin-management-action")) {
            if (selectedOptions.get(0).getValue().equals("plugin-register")) {
                MessageEmbed registerEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **플러그인의 유형을 선택해 주세요.**\s
                                    > **무료 또는 유료 중 원하는 타입을 선택하세요.**\s

                                    ─────────────────────────────────────────────────
                                    """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();

                StringSelectMenu registerSelectMenu = StringSelectMenu.create(ID_PREFIX + "type")
                        .setPlaceholder("플러그인의 유형을 선택해 주세요.")
                        .addOption("무료", "free", "무료로 사용할 수 있는 플러그인입니다.", Emoji.fromUnicode("🆓"))
                        .addOption("유료", "premium", "구매가 필요한 유료 플러그인입니다.", Emoji.fromUnicode("💰"))
                        .build();
                event.replyEmbeds(registerEmbed)
                        .addActionRow(registerSelectMenu)
                        .addActionRow(CANCEL_BUTTON)
                        .queue();

                event.editSelectMenu(event.getSelectMenu()).queue();
            }
        }

        if (!event.getComponentId().startsWith(ID_PREFIX)) return;
        switch (componentId) {
            case ID_PREFIX + "type" -> {
                boolean isPremium = selectedOptions.get(0).getValue().equals("premium");
                TextInput ENName = TextInput.create("name-en", "플러그인 이름 (영문)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 영문으로 입력해 주세요.")
                        .setMinLength(1)
                        .setMaxLength(25)
                        .setRequired(true)
                        .build();
                TextInput KRName = TextInput.create("name-kr", "플러그인 이름 (한글)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 한글로 입력해 주세요.")
                        .setMinLength(1)
                        .setMaxLength(25)
                        .setRequired(true)
                        .build();
                TextInput wikiUrl = TextInput.create("wiki-url", "위키 링크", TextInputStyle.SHORT)
                        .setPlaceholder("위키 링크를 전체 주소로 입력해 주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(true)
                        .build();
                TextInput videoUrl = TextInput.create("video-url", "영상 링크 (선택)", TextInputStyle.SHORT)
                        .setPlaceholder("영상 링크를 전체 주소로 입력해 주세요.")
                        .setMinLength(0)
                        .setMaxLength(150)
                        .setRequired(false)
                        .build();
                TextInput price = TextInput.create("price", "가격 (필수)", TextInputStyle.SHORT)
                        .setPlaceholder("가격을 입력해 주세요.")
                        .setMinLength(0)
                        .setMaxLength(10)
                        .setRequired(true)
                        .build();

                Modal modal;
                if (isPremium) {
                    modal = Modal.create(ID_PREFIX + "modal-premium",
                                    "유료 플러그인 등록 - 단계 1")
                            .addActionRow(ENName)
                            .addActionRow(KRName)
                            .addActionRow(wikiUrl)
                            .addActionRow(videoUrl)
                            .addActionRow(price)
                            .build();
                } else {
                    modal = Modal.create(ID_PREFIX + "modal-free",
                                    "무료 플러그인 등록 - 단계 1")
                            .addActionRow(ENName)
                            .addActionRow(KRName)
                            .addActionRow(wikiUrl)
                            .addActionRow(videoUrl)
                            .build();
                }

                event.replyModal(modal).queue();

                long userId = event.getUser().getIdLong();
                sessionStatusMap.put(userId, RegisterStatus.STARTED);

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (!event.getComponentId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String componentId = event.getComponentId();
        switch (componentId) {
            case ID_PREFIX + "manager" -> {
                long userId = event.getUser().getIdLong();
                if (sessionStatusMap.get(userId) != RegisterStatus.SELECT_MANAGER) return;

                List<User> mentionedUsers = event.getMentions().getUsers();
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateManager(mentionedUsers.stream().map(User::getIdLong).toList());

                sessionDataMap.put(userId, plugin);

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();

                String managerMention = mentionedUsers.stream().map(User::getAsMention).collect(Collectors.joining(", "));
                if (plugin.getPrice() != 0) {
                    EntitySelectMenu roleSelectMenu = EntitySelectMenu.create(ID_PREFIX + "buyerrole", EntitySelectMenu.SelectTarget.ROLE).build();
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 역할 | 플러그인 등록 <a:success:1168266537262657626>")
                            .setDescription(
                                    "> **[ 등록 정보 ]**\n" +
                                    "> **담당자: " + managerMention + "**\n\n" +
                                    "> **아래에 구매자 역할을 선택해 주세요.**\n\n" +
                                    "─────────────────────────────────────────────────"
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();

                    event.getMessage().replyEmbeds(embed)
                            .addActionRow(roleSelectMenu)
                            .addActionRow(CANCEL_BUTTON)
                            .queue();

                    sessionStatusMap.put(userId, RegisterStatus.SELECT_BUYER_ROLE);
                } else {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 아이콘 | 플러그인 등록 <a:success:1168266537262657626>")
                            .setDescription(
                                    "> **[ 등록 정보 ]**\n" +
                                    "> **담당자: " + managerMention + "**\n\n" +
                                    "> **아래에 아이콘 URL을 전송해 주세요. (Cloudflare Images)**\n\n" +
                                    "─────────────────────────────────────────────────"
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();

                    event.getMessage().replyEmbeds(embed)
                            .addActionRow(CANCEL_BUTTON)
                            .queue();

                    sessionStatusMap.put(userId, RegisterStatus.UPLOAD_ICON_IMAGE);
                }
            }

            case ID_PREFIX + "buyerrole" -> {
                long userId = event.getUser().getIdLong();
                if (sessionStatusMap.get(userId) != RegisterStatus.SELECT_BUYER_ROLE) return;

                Role role = event.getMentions().getRoles().get(0);
                Plugin plugin = sessionDataMap.get(userId);
                plugin.updateBuyerRole(role.getIdLong());

                sessionDataMap.put(userId, plugin);
                sessionStatusMap.put(userId, RegisterStatus.UPLOAD_ICON_IMAGE);

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 아이콘 | 플러그인 등록 <a:success:1168266537262657626>")
                        .setDescription(
                                "> **[ 등록 정보 ]**\n" +
                                "> **구매자 역할: " + role.getAsMention() + "**\n\n" +
                                "> **아래에 아이콘 이미지 URL을 전송해 주세요. (Cloudflare Images)**\n\n" +
                                "─────────────────────────────────────────────────"
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.getMessage()
                        .replyEmbeds(embed)
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
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
        if (buttonId.equals((ID_PREFIX + "cancel"))) {
            event.editButton(event.getButton().asDisabled()).queue();

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 등록 취소 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **플러그인 등록을 취소하였습니다.**
                            > **채널이 5초후에 자동으로 청소됩니다.**
                            
                            ─────────────────────────────────────────────────"""
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            event.getMessage().replyEmbeds(embed).queue();

            cancelProcess(event.getUser().getIdLong());
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
        if (modalId.equals(ID_PREFIX + "modal-free") || modalId.equals(ID_PREFIX + "modal-premium")) {
            String ENName = event.getValue("name-en").getAsString();
            if (!ENName.matches("[a-zA-Z0-9-_]+$")) {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 등록 <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **영문 이름은 영문자와 숫자, 특수문자(-, _)만 입력할 수 있습니다.**
                                    > **플러그인 등록을 취소합니다. (채널이 5초뒤 청소됩니다.)**
                                    
                                    ─────────────────────────────────────────────────
                                    """)
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();

                event.replyEmbeds(messageEmbed).queue();
                cancelProcess(event.getUser().getIdLong());
                return;
            }

            String KRName = event.getValue("name-kr").getAsString();
            String wikiUrl = event.getValue("wiki-url").getAsString();
            String videoUrl = event.getValue("video-url").getAsString();

            int price;
            try {
                String priceStr = event.getValue("price").getAsString();
                price = Integer.parseInt(priceStr);
            } catch (NumberFormatException ignored) {
                return;
            } catch (NullPointerException ignored) {
                price = 0;
            }

            long userId = event.getUser().getIdLong();
            Plugin plugin = new Plugin(
                    ENName,
                    KRName,
                    null,
                    wikiUrl,
                    null,
                    videoUrl.isEmpty() ? null : videoUrl,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "1.0",
                    price
            );
            sessionDataMap.put(userId, plugin);
            sessionStatusMap.put(userId, RegisterStatus.SELECT_DEPENDENCY);

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 1단계 | 플러그인 등록 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **[ 등록 정보 ]**
                            > **플러그인 이름(영문): %s**
                            > **플러그인 이름(한글): %s**
                            > **위키 링크: %s**
                            > **영상 링크: %s**
                            > **가격: %s**
                            
                            > **아래에 의존성 플러그인을 입력해 주세요.**
                            > **예) StarlyCore, Vault**
                            
                            ─────────────────────────────────────────────────
                            """
                            .formatted(
                                    ENName,
                                    KRName,
                                    wikiUrl,
                                    videoUrl,
                                    NumberFormat.getCurrencyInstance(Locale.KOREA).format(price)
                            )
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();

            event.replyEmbeds(messageEmbed).addActionRow(CANCEL_BUTTON).queue();
        }
    }

    // UTILITY
    private void sendReleaseAnnouncement(Plugin plugin) {
        String formattedPluginName = plugin.getENName() + "(" + plugin.getKRName() + ")";

        MessageEmbed noticeEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:success:1168266537262657626> " + formattedPluginName + " | 신규 플러그인 <a:success:1168266537262657626>")
                .setDescription("""
                        > **`\uD83C\uDF89` %s 플러그인이 출시되었습니다. `\uD83C\uDF89`**
                        > **`\uD83C\uDF1F` 많은 관심 부탁드립니다. `\uD83C\uDF1F`**
                        """.formatted(formattedPluginName))
                .setImage(plugin.getGifUrl())
                .setThumbnail(plugin.getIconUrl())
                .build();

        TextChannel channel = DiscordBotManager.getInstance().getJda().getTextChannelById(RELEASE_NOTICE_CHANNEL_ID);
        channel.sendMessageEmbeds(noticeEmbed).queue();
//        channel.sendMessage("> @everyone").queue();
    }

    private void cancelProcess(long userId) {
        sessionDataMap.remove(userId);
        sessionStatusMap.remove(userId);

        clearChannel();
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