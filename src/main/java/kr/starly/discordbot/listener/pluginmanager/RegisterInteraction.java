package kr.starly.discordbot.listener.pluginmanager;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.enums.RegisterStatus;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginFileService;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.util.PermissionUtil;
import kr.starly.discordbot.util.PluginForumUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
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
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@BotEvent
public class RegisterInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String PLUGIN_MANAGEMENT_CHANNEL_ID = configProvider.getString("PLUGIN_MANAGEMENT_CHANNEL_ID");
    private final String RELEASE_NOTICE_CHANNEL_ID = configProvider.getString("RELEASE_NOTICE_CHANNEL_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    private final Map<Long, RegisterStatus> registerStatusMap = new HashMap<>();
    private final Map<Long, Plugin> sessionDataMap = new HashMap<>();

    private final String ID_PREFIX = "plugin-register-";
    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");

    // CHAT
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        if (!event.getChannel().getId().equals(PLUGIN_MANAGEMENT_CHANNEL_ID)) return;
        if (event.getAuthor().isBot()) return;

        long userId = event.getAuthor().getIdLong();
        if (!sessionDataMap.containsKey(userId)) {
            event.getMessage().delete().queueAfter(5, TimeUnit.SECONDS);
            return;
        }

        String messageContent = event.getMessage().getContentRaw();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        switch (registerStatusMap.get(userId)) {
            case MODAL_SUBMITTED -> {
                // 의존성 설정

                List<String> dependency = Stream.of(messageContent.split(",")).map(String::trim).toList();
                Plugin plugin = sessionDataMap.get(userId);
                Plugin newPlugin = new Plugin(
                        plugin.ENName(),
                        plugin.KRName(),
                        plugin.emoji(),
                        plugin.wikiUrl(),
                        plugin.iconUrl(),
                        plugin.videoUrl(),
                        plugin.gifUrl(),
                        dependency,
                        plugin.manager(),
                        plugin.buyerRole(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);
                registerStatusMap.put(userId, RegisterStatus.DEPENDENCY_ENTERED);

                event.getMessage().reply("의존성을 `" + String.join(", ", dependency) + "` (으)로 설정했습니다.\n아래에 이모지를 입력해 주세요. (백틱 사이에 넣어주세요.)")
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }

            case DEPENDENCY_ENTERED -> {
                // 이모지 설정

                Emoji emoji;
                try {
                    emoji = Emoji.fromFormatted(messageContent.replace("`", ""));
                } catch (IllegalArgumentException ignored) {
                    event.getMessage().reply("올바른 이모지를 입력해 주세요.")
                            .addActionRow(CANCEL_BUTTON)
                            .queue();
                    return;
                }

                if (emoji.getType() != Emoji.Type.UNICODE) {
                    event.getMessage().reply("유니코드 이모지로만 설정하실 수 있습니다. 다시 시도해 주세요.")
                            .addActionRow(CANCEL_BUTTON)
                            .queue();
                    return;
                }

                Plugin plugin = sessionDataMap.get(userId);
                Plugin newPlugin = new Plugin(
                        plugin.ENName(),
                        plugin.KRName(),
                        messageContent.replace("`", ""),
                        plugin.wikiUrl(),
                        plugin.iconUrl(),
                        plugin.videoUrl(),
                        plugin.gifUrl(),
                        plugin.dependency(),
                        plugin.manager(),
                        plugin.buyerRole(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);
                registerStatusMap.put(userId, RegisterStatus.EMOJI_ENTERED);

                SelectMenu managerSelectMenu = EntitySelectMenu.create(ID_PREFIX + "manager", EntitySelectMenu.SelectTarget.USER)
                        .setPlaceholder("담당자를 선택해 주세요.")
                        .setRequiredRange(1, 10)
                        .build();
                event.getMessage().reply("이모지를 `\\" + ((UnicodeEmoji) emoji).getAsCodepoints() + "` (으)로 설정했습니다.\n아래에서 담당자를 선택해 주세요.")
                        .addActionRow(managerSelectMenu)
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }

            case BUYER_ROLE_SELECTED -> {
                // 아이콘 이미지 업로드

                String iconUrl = messageContent;

                Plugin plugin = sessionDataMap.get(userId);
                Plugin newPlugin = new Plugin(
                        plugin.ENName(),
                        plugin.KRName(),
                        plugin.emoji(),
                        plugin.wikiUrl(),
                        iconUrl,
                        plugin.videoUrl(),
                        plugin.gifUrl(),
                        plugin.dependency(),
                        plugin.manager(),
                        plugin.buyerRole(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);
                registerStatusMap.put(userId, RegisterStatus.ICON_UPLOADED);

                event.getMessage().reply("아이콘 이미지를 설정했습니다.\n아래에 GIF 이미지 URL을 입력해 주세요. (Cloudflare Images)")
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }

            case ICON_UPLOADED -> {
                // GIF 이미지 업로드

                String imageUrl = messageContent;

                Plugin plugin = sessionDataMap.get(userId);
                Plugin newPlugin = new Plugin(
                        plugin.ENName(),
                        plugin.KRName(),
                        plugin.emoji(),
                        plugin.wikiUrl(),
                        plugin.iconUrl(),
                        plugin.videoUrl(),
                        imageUrl,
                        plugin.dependency(),
                        plugin.manager(),
                        plugin.buyerRole(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);
                registerStatusMap.put(userId, RegisterStatus.GIF_UPLOADED);

                event.getMessage().reply("""
                                GIF 이미지를 설정했습니다. 아래에 플러그인 파일을 첨부해 주세요.
                                
                                (파일명: <서버버전>-<플러그인버전>.jar/zip)
                                예) `v1_12-1.0.jar`, `v1_17-1.1.zip`
                                """)
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }

            case GIF_UPLOADED -> {
                // 플러그인 파일 업로드

                if (attachments.isEmpty()) {
                    event.getMessage().reply("플러그인 파일을 첨부해 주세요.").queue();
                    return;
                }

                Plugin plugin = sessionDataMap.get(userId);

                PluginFileService pluginFileService = DatabaseManager.getPluginFileService();

                int successCount = 0;
                for (int i = 0; i < attachments.size(); i++) {
                    Message.Attachment attachment = attachments.get(i);
                    String fileName = attachment.getFileName();
                    if (!List.of("jar", "zip").contains(attachment.getFileExtension())) {
                        event.getMessage().reply("플러그인 파일은 .jar 또는 .zip 형식의 파일만 업로드 가능합니다. [" + fileName + "]").queue();
                        continue;
                    }

                    String[] fileNameSplit = fileName.replace("." + attachment.getFileExtension(), "").split("-");
                    if (fileNameSplit.length != 2) {
                        event.getMessage().reply("플러그인 파일명이 올바르지 않습니다. 파일명을 확인해 주세요. [" + fileName + "]").queue();
                        continue;
                    }

                    String mcVersion = fileNameSplit[0];
                    String version = fileNameSplit[1];

                    File pluginFile = new File("\\plugin\\%s\\%s.%s".formatted(plugin.ENName(), mcVersion + "-" + version, attachment.getFileExtension()));
                    File pluginDir = pluginFile.getParentFile();

                    if (!pluginDir.exists()) pluginDir.mkdirs();
                    attachment.getProxy().downloadToFile(pluginFile);

                    pluginFileService.saveData(pluginFile, plugin, MCVersion.valueOf(mcVersion), version);
                    successCount++;
                }

                if (successCount == 0) {
                    event.getMessage().reply("플러그인 파일을 모두 업로드하지 못했습니다. 다시 시도해 주세요.")
                            .addActionRow(CANCEL_BUTTON)
                            .queue();
                    return;
                }

                int failCount = attachments.size() - successCount;
                if (failCount == 0) {
                    event.getMessage().reply("플러그인 파일을 모두 업로드했습니다.").queue();
                } else {
                    event.getMessage().reply(failCount + "개의 파일을 제외한 " + successCount + "개의 파일을 업로드했습니다.").queue();
                }

                PluginService pluginService = DatabaseManager.getPluginService();
                pluginService.pluginRepository().put(plugin);

                sessionDataMap.remove(userId);
                registerStatusMap.remove(userId);

                clearChannel();
                event.getMessage().reply("플러그인이 등록되었습니다. 채널이 청소됩니다.").queue();


                PluginForumUtil.createPluginChannel(plugin);
                sendReleaseAnnouncement(plugin);
            }
        }
    }

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        List<SelectOption> selectedOptions = event.getSelectedOptions();

        String componentId = event.getComponentId();
        switch (componentId) {
            case "plugin-management-action" -> {
                if (selectedOptions.get(0).getValue().equals("plugin-register")) {
                    MessageEmbed registerEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("<a:loading:1141623256558866482> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1141623256558866482>")
                            .setDescription("""
                                    > **플러그인의 유형을 선택해 주세요.**\s
                                    > **무료 또는 유료 중 원하는 타입을 선택하세요.**\s

                                    ─────────────────────────────────────────────────"""
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
                TextInput price = TextInput.create("price", "가격 (선택)", TextInputStyle.SHORT)
                        .setPlaceholder("가격을 입력해 주세요.")
                        .setMinLength(0)
                        .setMaxLength(10)
                        .setRequired(false)
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
                registerStatusMap.put(userId, RegisterStatus.STARTED);

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();
            }
        }
    }

    @Override
    public void onEntitySelectInteraction(@NotNull EntitySelectInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String componentId = event.getComponentId();
        switch (componentId) {
            case ID_PREFIX + "manager" -> {
                long userId = event.getUser().getIdLong();

                List<User> users = event.getMentions().getUsers();
                Plugin plugin = sessionDataMap.get(userId);
                Plugin newPlugin = new Plugin(
                        plugin.ENName(),
                        plugin.KRName(),
                        plugin.emoji(),
                        plugin.wikiUrl(),
                        plugin.iconUrl(),
                        plugin.videoUrl(),
                        plugin.gifUrl(),
                        plugin.dependency(),
                        users.stream().map(User::getIdLong).toList(),
                        plugin.buyerRole(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);

                event.editSelectMenu(event.getSelectMenu().asDisabled()).queue();

                String managerMention = users.stream().map(User::getAsMention).collect(Collectors.joining(", "));
                if (plugin.price() != 0) {
                    EntitySelectMenu roleSelectMenu = EntitySelectMenu.create(ID_PREFIX + "buyerrole", EntitySelectMenu.SelectTarget.ROLE).build();
                    event.getMessage().reply("담당자를 " + managerMention + "님으로 설정했습니다.\n아래에서 구매자 역할을 선택해 주세요.")
                            .addActionRow(roleSelectMenu)
                            .addActionRow(CANCEL_BUTTON)
                            .queue();

                    registerStatusMap.put(userId, RegisterStatus.MANAGER_SELECTED);
                } else {
                    event.getMessage().reply("담당자를 " + managerMention + "님으로 설정했습니다.\n아래에 아이콘 URL을 전송해 주세요. (jpg, png)")
                            .addActionRow(CANCEL_BUTTON)
                            .queue();

                    registerStatusMap.put(userId, RegisterStatus.BUYER_ROLE_SELECTED);
                }
            }

            case ID_PREFIX + "buyerrole" -> {
                long userId = event.getUser().getIdLong();

                Role role = event.getMentions().getRoles().get(0);
                Plugin plugin = sessionDataMap.get(userId);
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
                        role.getIdLong(),
                        plugin.threadId(),
                        plugin.version(),
                        plugin.price()
                );
                sessionDataMap.put(userId, newPlugin);
                registerStatusMap.put(userId, RegisterStatus.BUYER_ROLE_SELECTED);

                event.reply("구매자역할을 " + role.getAsMention() + "(으)로 설정했습니다.").queue();
                event.getChannel().sendMessage("아래에 아이콘을 업로드하거나 URL을 전송해 주세요. (jpg, png)")
                        .addActionRow(CANCEL_BUTTON)
                        .queue();
            }
        }
    }

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String buttonId = event.getComponentId();
        if (buttonId.equals((ID_PREFIX + "cancel"))) {
            long userId = event.getUser().getIdLong();
            sessionDataMap.remove(userId);
            registerStatusMap.remove(userId);

            event.editButton(event.getButton().asDisabled()).queue();

            clearChannel();
            event.getMessage().reply("플러그인이 등록을 취소했습니다. 채널이 청소됩니다.").queue();
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
        if (modalId.equals(ID_PREFIX + "modal-free") || modalId.equals(ID_PREFIX + "modal-premium")) {
            String ENName = event.getValue("name-en").getAsString();
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
            registerStatusMap.put(userId, RegisterStatus.MODAL_SUBMITTED);

            event.reply("입력하신 내용입니다 : %s, %s, %s, %s, %d\n의존성을 입력해 주세요.\n예) StarlyCore, ProtocolLib".formatted(ENName, KRName, wikiUrl, videoUrl, price))
                    .addActionRow(CANCEL_BUTTON)
                    .queue();
        }
    }

    // UTILITY
    private void sendReleaseAnnouncement(Plugin plugin) {
        String formattedPluginName = plugin.ENName() + "(" + plugin.KRName() + ")";

        MessageEmbed noticeEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:success:1141625729386287206> " + formattedPluginName + " | 신규 플러그인 <a:success:1141625729386287206>")
                .setDescription("""
                        > **`\uD83C\uDF89` %s 플러그인이 출시되었습니다. `\uD83C\uDF89`**
                        > **`\uD83C\uDF1F` 많은 관심 부탁드립니다. `\uD83C\uDF1F`**
                        """.formatted(formattedPluginName))
                .setImage(plugin.gifUrl())
                .setThumbnail(plugin.iconUrl())
                .build();

        TextChannel channel = DiscordBotManager.getInstance().getJda().getTextChannelById(RELEASE_NOTICE_CHANNEL_ID);
        channel.sendMessageEmbeds(noticeEmbed).queue();
        channel.sendMessage("> @everyone").queue();
    }

    private void clearChannel() {
        TextChannel channel = DiscordBotManager.getInstance().getJda().getTextChannelById(PLUGIN_MANAGEMENT_CHANNEL_ID);
        channel.getHistoryFromBeginning(100).queueAfter(5, TimeUnit.SECONDS, history -> {
            List<Message> messages = new ArrayList<>(history.getRetrievedHistory());
            messages.remove(messages.size() - 1);

            channel.purgeMessages(messages);
        });
    }
} // TODO : 메시지 디자인