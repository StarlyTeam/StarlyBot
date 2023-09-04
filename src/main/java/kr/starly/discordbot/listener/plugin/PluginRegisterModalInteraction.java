package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.entity.PluginInfoDTO;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.PluginDataRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@BotEvent
public class PluginRegisterModalInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        long userId = event.getUser().getIdLong();
        String modalId = event.getModalId();

        switch (modalId) {
            case "plugin-register-free-step1" -> {
                PluginInfoDTO pluginInfo = new PluginInfoDTO();
                pluginInfo.setPluginNameEnglish(event.getValues().get(0).getAsString());
                pluginInfo.setPluginNameKorean(event.getValues().get(1).getAsString());
                pluginInfo.setPluginWikiLink(event.getValues().get(2).getAsString());
                pluginInfo.setPluginVideoLink(event.getValues().get(3).getAsString());
                List<String> dependencies = List.of(event.getValues().get(4).getAsString().split(","))
                        .stream()
                        .map(String::trim)
                        .collect(Collectors.toList());
                pluginInfo.setDependency(dependencies);

                PluginDataRepository dataRepo = PluginDataRepository.getInstance();
                dataRepo.setData(userId, pluginInfo);

                MessageEmbed pluginRegisterEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:loading:1141623256558866482> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1141623256558866482>")
                        .setDescription(generateDescriptionFromDTO(pluginInfo))
                        .setThumbnail("https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                        .build();
                event.replyEmbeds(pluginRegisterEmbed).queue();
            }
        }
    }

    private String generateDescriptionFromDTO(PluginInfoDTO dto) {
        StringBuilder description = new StringBuilder();

        description.append("> **`\uD83D\uDCCC` 담당자 정보를 입력해주세요. `\uD83D\uDCCC`** \n");
        description.append("> **`\uD83D\uDCCC` 예: `담당자: @홍길동, @이순신` `\uD83D\uDCCC`** \n\n").append("─────────────────────────────────────────────────\n");
        description.append("> **`\uD83C\uDD70\uFE0F` | 플러그인 이름 (영어): `").append(dto.getPluginNameEnglish()).append("`**\n");
        description.append("> **`\uD83C\uDD71\uFE0F` | 플러그인 이름 (한국어): `").append(dto.getPluginNameKorean()).append("`**\n");
        description.append("> **`\uD83D\uDCD6` | 플러그인 위키 링크: `").append(dto.getPluginWikiLink()).append("`**\n");

        if (dto.getPluginVideoLink() == null || dto.getPluginVideoLink().isEmpty()) {
            description.append("> **`\uD83C\uDFA5` |  플러그인 영상 링크: `없음`**\n");
        } else {
            description.append("> **`\uD83C\uDFA5` |  플러그인 영상 링크: `").append(dto.getPluginVideoLink()).append("`**\n");
        }

        if (dto.getDependency() == null || dto.getDependency().isEmpty()) {
            description.append("> **`\uD83D\uDD17` | 의존성: `없음`**\n\n");
        } else {
            String dependencies = String.join(", ", dto.getDependency());
            description.append("> **`\uD83D\uDD17` | 의존성: `").append(dependencies).append("`**\n\n");
        }

        description.append("─────────────────────────────────────────────────");

        return description.toString();
    }
}