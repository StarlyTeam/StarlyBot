package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.awt.Color;

@BotCommand(
        command = "플러그인관리생성",
        description = "플러그인 관리 임베드를 생성합니다.",
        usage = "?플러그인관리생성"
)
public class CreatePluginManagementCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getMessage());
            return;
        }

        MessageEmbed verifyEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 플러그인 관리 | 스탈리 (관리자 전용) <a:loading:1168266572847128709>")
                .setDescription("""
                        > **플러그인 관리 메뉴입니다.**\s
                        > **아래 선택 메뉴에서 원하는 작업을 선택하세요.**\s

                        ─────────────────────────────────────────────────
                        """
                )
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .setFooter("이 기능은 관리자 전용입니다.", "https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .build();

        StringSelectMenu pluginSelectionMenu = StringSelectMenu.create("plugin-management-action")
                .setPlaceholder("원하시는 작업을 선택해 주세요.")
                .addOption("플러그인 등록", "plugin-register", "플러그인을 등록합니다.", Emoji.fromUnicode("➕"))
                .addOption("플러그인 삭제", "plugin-delete", "플러그인을 삭제합니다.", Emoji.fromUnicode("❌"))
                .addOption("플러그인 수정", "plugin-edit", "플러그인 정보를 수정합니다.", Emoji.fromUnicode("✏"))
                .addOption("플러그인 목록", "plugin-list", "등록된 플러그인 목록을 확인합니다.", Emoji.fromUnicode("📋"))
                .addOption("플러그인 정보", "plugin-info", "특정 플러그인의 정보를 확인합니다.", Emoji.fromUnicode("🔍"))
                .build();

        event.getChannel().sendMessageEmbeds(verifyEmbed).addActionRow(pluginSelectionMenu).queue();
    }
}