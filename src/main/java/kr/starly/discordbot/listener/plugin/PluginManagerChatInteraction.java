package kr.starly.discordbot.listener.plugin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.entity.PluginInfoDTO;
import kr.starly.discordbot.enums.UploadStatus;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.PluginDataRepository;
import kr.starly.discordbot.repository.impl.MongoPluginInfoRepository;
import kr.starly.discordbot.util.TokenUtil;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@BotEvent
public class PluginManagerChatInteraction extends ListenerAdapter {

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String GUILD_ID = configProvider.getString("CHANNEL_ID_PLUGIN_MANAGEMENT");
    private static final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    private final MongoPluginInfoRepository mongoRepo;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Getter
    private static final Map<Long, String> userTokens = new HashMap<>();
    @Getter
    private static MessageReceivedEvent lastEvent;

    public PluginManagerChatInteraction() {
        String DB_CONNECTION_STRING = configProvider.getString("DB_HOST");
        String DB_NAME = configProvider.getString("DB_DATABASE");
        String DB_COLLECTION_PLUGIN = configProvider.getString("DB_COLLECTION_PLUGIN");

        MongoClient mongoClient = MongoClients.create(DB_CONNECTION_STRING);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        MongoCollection<Document> collection = database.getCollection(DB_COLLECTION_PLUGIN);

        mongoRepo = new MongoPluginInfoRepository(collection);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getChannel().getId().equals(GUILD_ID)) return;

        if (!event.getAuthor().isBot()) {
            event.getMessage().delete().queue();
            this.lastEvent = event;
        }

        long userId = event.getAuthor().getIdLong();
        String messageContent = event.getMessage().getContentRaw();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();

        if (messageContent.startsWith("담당자:")) {
            PluginDataRepository dataRepo = PluginDataRepository.getInstance();
            PluginInfoDTO existingData = dataRepo.getData(userId);
            if (existingData != null) {
                List<String> managers = Arrays.stream(messageContent.substring("담당자:".length()).trim().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                existingData.setManagers(managers);

                MessageEmbed gifRequestEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:loading:1141623256558866482> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1141623256558866482>")
                        .setDescription("> **`📌` 이제 .gif 파일을 업로드하거나 링크를 제공해주세요.** \n\n" +
                                "─────────────────────────────────────────────────")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();

                event.getChannel().sendMessageEmbeds(gifRequestEmbed).queue();
            }
        } else if (!attachments.isEmpty() && attachments.get(0).isImage() && attachments.get(0).getFileExtension().equalsIgnoreCase("gif")) {
            handleGifUpload(event, attachments.get(0).getUrl());
        } else if (messageContent.contains(".gif")) {
            handleGifUpload(event, extractGifUrlFromMessage(messageContent));
        }
    }

    private String extractGifUrlFromMessage(String messageContent) {
        Pattern pattern = Pattern.compile("https?://[^\\s]+\\.gif");
        Matcher matcher = pattern.matcher(messageContent);

        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private void handleGifUpload(MessageReceivedEvent event, String gifUrl) {
        long userId = event.getAuthor().getIdLong();
        PluginDataRepository dataRepo = PluginDataRepository.getInstance();
        PluginInfoDTO existingData = dataRepo.getData(userId);
        if (existingData != null) {
            existingData.setGifLink(gifUrl);

            mongoRepo.save(existingData);

            String token = TokenUtil.generateNewToken();
            userTokens.put(userId, token);

            String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");
            int SERVER_PORT = configProvider.getInt("PLUGIN_PORT");

            String uploadLink = "http://" + WEB_ADDRESS + ":" + SERVER_PORT + "/upload/" + userId + "?token=" + token;

            String pluginInfoDescription = pluginInfoDescription(existingData);
            System.out.println("HashMap put: " + userId);

            MessageEmbed successEmbed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                    .setTitle("<a:success:1141625729386287206> 플러그인 관리 | 스탈리 (관리자 전용) <a:success:1141625729386287206>")
                    .setDescription("> **`\uD83C\uDD99` GIF 파일이 성공적으로 업로드되었습니다. `\uD83C\uDD99`** \n" +
                            "> **`\uD83D\uDCE5` 데이터가 저장되었습니다. `\uD83D\uDCE5`**\n\n" +
                            "> **🔗 [여기를 클릭하여 플러그인 파일을 업로드하세요](" + uploadLink + ")**\n\n" +
                            "─────────────────────────────────────────────────\n" +
                            pluginInfoDescription +
                            "─────────────────────────────────────────────────"
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();

            UploadStatus.getUserUploadStatus().put(userId, UploadStatus.GIF_UPLOADED);
            event.getChannel().sendMessageEmbeds(successEmbed).queue();
        }
    }

    private String pluginInfoDescription(PluginInfoDTO dto) {
        StringBuilder description = new StringBuilder();

        description.append("> **`\uD83C\uDD70\uFE0F` | 플러그인 이름 (영어): `").append(dto.getPluginNameEnglish()).append("`**\n");
        description.append("> **`\uD83C\uDD71\uFE0F` | 플러그인 이름 (한국어): `").append(dto.getPluginNameKorean()).append("`**\n");
        description.append("> **`\uD83D\uDCD6` | 플러그인 위키 링크: `").append(dto.getPluginWikiLink()).append("`**\n");

        if (dto.getPluginVideoLink() == null || dto.getPluginVideoLink().isEmpty()) {
            description.append("> **`\uD83C\uDFA5` | 플러그인 영상 링크: `없음`**\n");
        } else {
            description.append("> **`\uD83C\uDFA5` | 플러그인 영상 링크: `").append(dto.getPluginVideoLink()).append("`**\n");
        }

        if (dto.getDependency() == null || dto.getDependency().isEmpty()) {
            description.append("> **`\uD83D\uDD17` | 의존성: `없음`**\n\n");
        } else {
            String dependencies = String.join(", ", dto.getDependency());
            description.append("> **`\uD83D\uDD17` | 의존성: `").append(dependencies).append("`**\n\n");
        }

        description.append("> **`\uD83D\uDCCC` 담당자: ").append(String.join(", ", dto.getManagers())).append("**\n");
        description.append("> **🎥 | GIF 링크: `").append(dto.getGifLink()).append("`**\n");

        return description.toString();
    }

    public static void createPluginChannel() {
        Guild guild = lastEvent.getGuild();
        long userId = lastEvent.getAuthor().getIdLong();

        PluginDataRepository dataRepo = PluginDataRepository.getInstance();
        PluginInfoDTO existingData = dataRepo.getData(userId);

        String channelName = "\uD83D\uDCE6┃" + existingData.getPluginNameKorean();
        if (channelName.length() > 100) {
            channelName = channelName.substring(0, 100);
        }

        String FREE_PLUGIN_FORUM = configProvider.getString("FREE_PLUGIN_FORUM");

        guild.getCategoryById(FREE_PLUGIN_FORUM).createForumChannel(channelName).queue();

    }

    public static void releaseNotice() {
        long userId = lastEvent.getAuthor().getIdLong();
        PluginDataRepository dataRepo = PluginDataRepository.getInstance();
        PluginInfoDTO existingData = dataRepo.getData(userId);

        String pluginFullName = existingData.getPluginNameEnglish() + "(" + existingData.getPluginNameKorean() + ")";

        MessageEmbed noticeEmbed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("<a:success:1141625729386287206> " + pluginFullName + " | 신규 플러그인 <a:success:1141625729386287206>")
                .setDescription("> **`\uD83C\uDF89`" + pluginFullName + " 플러그인이 출시되었습니다. `\uD83C\uDF89`** \n" +
                        "> **`\uD83C\uDF1F` 많은 관심 부탁드립니다. `\uD83C\uDF1F`**\n\n" +
                        "─────────────────────────────────────────────────"
                )
                .setImage(existingData.getGifLink())
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .build();


        String RELEASE_NOTICE_CHANNEL = configProvider.getString("RELEASE_NOTICE_CHANNEL");
        TextChannel textChannel = DiscordBotManager.getInstance().getJda().getTextChannelById(RELEASE_NOTICE_CHANNEL);
        textChannel.sendMessageEmbeds(noticeEmbed).queue();
        textChannel.sendMessage("> @everyone").queue();
    }

    public static void checkAndDeleteMessages(TextChannel channel, long userId) {
        if (getUserUploadStatus(userId) == UploadStatus.FILE_UPLOADED) {
            scheduler.schedule(() -> channel.getHistory().retrievePast(100).queue(messages -> {
                messages.remove(messages.size() - 1);
                channel.purgeMessages(messages);
            }), 15, TimeUnit.SECONDS);
        }
    }

    public static void setUserUploadStatus(long userId, UploadStatus status) {
        UploadStatus.getUserUploadStatus().put(userId, status);
    }

    private static UploadStatus getUserUploadStatus(long userId) {
        return UploadStatus.getUserUploadStatus().getOrDefault(userId, UploadStatus.NONE);
    }
}
