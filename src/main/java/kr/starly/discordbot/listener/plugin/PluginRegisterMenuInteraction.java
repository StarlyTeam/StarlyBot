package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class PluginRegisterMenuInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!"plugin-management-register".equals(event.getComponentId())) return;

        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String selectedValue = event.getValues().get(0);

        switch (selectedValue) {
            case "free" -> {
                TextInput pluginNameEnglish = TextInput.create("plugin-register-free-plugin-name-english", "플러그인 이름 (영문)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 영문으로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(25)
                        .setRequired(true)
                        .build();
                TextInput pluginNameKorean = TextInput.create("plugin-register-free-plugin-name-korean", "플러그인 이름 (한글)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 한으로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(25)
                        .setRequired(true)
                        .build();
                TextInput pluginWikiLink = TextInput.create("plugin-register-free-plugin-wiki-link", "위키 링크", TextInputStyle.SHORT)
                        .setPlaceholder("위키 링크를 전체 주소로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(true)
                        .build();
                TextInput pluginVideoLink = TextInput.create("plugin-register-free-plugin-video-link", "영상 링크 (선택)", TextInputStyle.SHORT)
                        .setPlaceholder("영상 링크를 전체 주소로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(false)
                        .build();
                TextInput dependency = TextInput.create("plugin-register-free-dependency", "의존성 (선택)", TextInputStyle.SHORT)
                        .setPlaceholder("의존성 플러그인을 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(false)
                        .build();

                Modal modal = Modal.create("plugin-register-free", "무료 플러그인 등록")
                        .addActionRow(pluginNameEnglish)
                        .addActionRow(pluginNameKorean)
                        .addActionRow(pluginWikiLink)
                        .addActionRow(pluginVideoLink)
                        .addActionRow(dependency)
                        .build();

                event.replyModal(modal).queue();
            }

            case "premium" -> {
                MessageEmbed premiumEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1141623256558866482>")
                        .setDescription("> **유료 플러그인을 선택하셨습니다!** \n" +
                                "> **해당 플러그인의 정보와 가격을 입력해주세요.** \n\n" +
                                "─────────────────────────────────────────────────"
                        )
                        .setThumbnail("https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://media.discordapp.net/attachments/1038091650674724954/1148008081784066108/YELLOWBACKGROUND.png?width=597&height=597")
                        .build();
                event.replyEmbeds(premiumEmbed).setEphemeral(true).queue();
            }
        }
    }
}