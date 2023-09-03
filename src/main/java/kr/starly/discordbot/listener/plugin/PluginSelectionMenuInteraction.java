package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.listener.BotEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

@BotEvent
public class PluginSelectionMenuInteraction extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!"plugin-management".equals(event.getComponentId())) return;

        String selectedValue = event.getValues().get(0);

        switch (selectedValue) {
            case "plugin-register" -> {
                // TODO 플러그인 등록 관련 처리
                TextInput pluginNameEnglish = TextInput.create("plugin-name-english","플러그인 이름 (영문)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 영문으로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(30)
                        .setRequired(true)
                        .build();
                TextInput pluginNameKorean = TextInput.create("plugin-name-korean","플러그인 이름 (한글)", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 이름을 한글로 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(30)
                        .setRequired(true)
                        .build();
                TextInput pluginVersion = TextInput.create("plugin-version","플러그인 버전", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 버전을 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(15)
                        .setRequired(true)
                        .build();
                TextInput pluginWikiLink = TextInput.create("plugin-wiki","플러그인 위키 링크", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 위키 링크를 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(true)
                        .build();
                TextInput pluginVideoLink = TextInput.create("plugin-video","플러그인 영상 링크", TextInputStyle.SHORT)
                        .setPlaceholder("플러그인 영상 링크를 입력해주세요.")
                        .setMinLength(1)
                        .setMaxLength(150)
                        .setRequired(true)
                        .build();

                Modal pluginRegisterModal = Modal.create("plugin-reigster-modal", "플러그인 등록")
                        .addActionRow(pluginNameEnglish)
                        .addActionRow(pluginNameKorean)
                        .addActionRow(pluginVersion)
                        .addActionRow(pluginWikiLink)
                        .addActionRow(pluginVideoLink)
                        .build();

                event.replyModal(pluginRegisterModal).queue();
            }
            case "plugin-delete" -> {
                // TODO 플러그인 삭제 관련 처리
                event.reply("플러그인 삭제 시스템 구현중..").setEphemeral(true).queue();
            }
            case "plugin-edit" -> {
                // TODO 플러그인 수정 관련 처리
                event.reply("플러그인 수정 시스템 구현중..").setEphemeral(true).queue();
            }
            case "plugin-list" -> {
                // TODO 플러그인 목록 조회 관련 처리
                event.reply("플러그인 목록 조회 시스템 구현중..").setEphemeral(true).queue();
            }
            case "plugin-info" -> {
                // TODO 플러그인 정보 조회 관련 처리
                event.reply("플러그인 정보 조회 시스템 구현중..").setEphemeral(true).queue();
            }
            default -> {
                // TODO 예상치 못한 선택 값에 대한 처리
            }
        }
    }
}