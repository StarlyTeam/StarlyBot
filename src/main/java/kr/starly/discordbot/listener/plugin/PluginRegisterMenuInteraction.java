package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

@BotEvent
public class PluginRegisterMenuInteraction extends ListenerAdapter {

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!"plugin-management-register".equals(event.getComponentId())) return;

        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String selectedValue = event.getValues().get(0);

        switch (selectedValue) {
            case "free" -> showStep1Modal(event, false);
            case "premium" -> showStep1Modal(event, true);
        }
    }

    private void showStep1Modal(StringSelectInteractionEvent event, boolean isPremium) {
        TextInput pluginNameEnglish = TextInput.create("plugin-register-premium-plugin-name-english", "플러그인 이름 (영문)", TextInputStyle.SHORT)
                .setPlaceholder("플러그인 이름을 영문으로 입력해주세요.")
                .setMinLength(1)
                .setMaxLength(25)
                .setRequired(true)
                .build();
        TextInput pluginNameKorean = TextInput.create("plugin-register-premium-plugin-name-korean", "플러그인 이름 (한글)", TextInputStyle.SHORT)
                .setPlaceholder("플러그인 이름을 한으로 입력해주세요.")
                .setMinLength(1)
                .setMaxLength(25)
                .setRequired(true)
                .build();
        TextInput pluginWikiLink = TextInput.create("plugin-register-premium-plugin-wiki-link", "위키 링크", TextInputStyle.SHORT)
                .setPlaceholder("위키 링크를 전체 주소로 입력해주세요.")
                .setMinLength(1)
                .setMaxLength(150)
                .setRequired(true)
                .build();
        TextInput pluginVideoLink = TextInput.create("plugin-register-premium-plugin-video-link", "영상 링크 (선택)", TextInputStyle.SHORT)
                .setPlaceholder("영상 링크를 전체 주소로 입력해주세요.")
                .setMinLength(0)
                .setMaxLength(150)
                .setRequired(false)
                .build();
        TextInput dependency = TextInput.create("plugin-register-premium-dependency", "의존성 (선택) (예: StarlyCore, ProtocolLib)", TextInputStyle.SHORT)
                .setPlaceholder("의존성 플러그인을 입력해주세요.")
                .setMinLength(0)
                .setMaxLength(150)
                .setRequired(false)
                .build();

        Modal modal = Modal.create(isPremium ? "plugin-register-premium-step1" : "plugin-register-free-step1",
                        isPremium ? "유료 플러그인 등록 - 단계 1" : "무료 플러그인 등록 - 단계 1")
                .addActionRow(pluginNameEnglish)
                .addActionRow(pluginNameKorean)
                .addActionRow(pluginWikiLink)
                .addActionRow(pluginVideoLink)
                .addActionRow(dependency)
                .build();

        event.replyModal(modal).queue();
        event.editSelectMenu(event.getSelectMenu()).queue();
    }
}