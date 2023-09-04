package kr.starly.discordbot.listener.plugin;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.entity.PluginInfoDTO;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.PluginDataRepository;
import kr.starly.discordbot.repository.impl.MongoPluginInfoRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@BotEvent
public class PluginManagerChatInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String GUILD_ID = configProvider.getString("CHANNEL_ID_PLUGIN_MANAGEMENT");
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    private final MongoPluginInfoRepository mongoRepo;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

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
                        .setThumbnail("https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
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

            String pluginInfoDescription = pluginInfoDescription(existingData);

            MessageEmbed successEmbed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                    .setTitle("<a:success:1141625729386287206> 플러그인 관리 | 스탈리 (관리자 전용) <a:success:1141625729386287206>")
                    .setDescription("> **`\uD83C\uDD99` GIF 파일이 성공적으로 업로드되었습니다. `\uD83C\uDD99`** \n" +
                            "> **`\uD83D\uDCE5` 데이터가 저장되었습니다. `\uD83D\uDCE5`**\n\n" +
                            "> **`🚫` 20초 뒤에 모든 메시지가 삭제됩니다. `🚫`** \n\n" +
                            "─────────────────────────────────────────────────\n" +
                            pluginInfoDescription +
                            "─────────────────────────────────────────────────"
                    )
                    .setThumbnail("https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                    .build();

            event.getChannel().sendMessageEmbeds(successEmbed).queue(response -> {
                scheduler.schedule(() -> event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                    messages.remove(messages.size() - 1);
                    event.getChannel().purgeMessages(messages);
                }), 20, TimeUnit.SECONDS);
            });
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
}
