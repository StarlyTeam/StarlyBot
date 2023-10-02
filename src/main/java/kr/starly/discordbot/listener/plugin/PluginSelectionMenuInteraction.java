package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class PluginSelectionMenuInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (!"plugin-management".equals(event.getComponentId())) return;

        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String selectedValue = event.getValues().get(0);

        switch (selectedValue) {
            case "plugin-register" -> {
                MessageEmbed registerEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1141623256558866482>")
                        .setDescription("> **플러그인의 유형을 선택해주세요.** \n" +
                                "> **무료 또는 유료 중 원하는 타입을 선택하세요.** \n\n" +
                                "─────────────────────────────────────────────────"
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();

                StringSelectMenu registerSelectMenu = StringSelectMenu.create("plugin-management-register")
                        .setPlaceholder("플러그인의 유형을 선택해주세요.")
                        .addOption("무료", "free", "무료로 사용할 수 있는 플러그인입니다.", Emoji.fromUnicode("🆓"))
                        .addOption("유료", "premium", "구매가 필요한 유료 플러그인입니다.", Emoji.fromUnicode("💰"))
                        .build();

                event.replyEmbeds(registerEmbed).addActionRow(registerSelectMenu).queue();
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
        event.editSelectMenu(event.getSelectMenu()).queue();
    }
}