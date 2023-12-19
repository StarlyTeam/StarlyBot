package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Download;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.*;
import kr.starly.discordbot.util.TokenUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Date;
import java.util.List;

@BotEvent
public class DownloadListener extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");
    private final String VERIFIED_ROLE_ID = configProvider.getString("VERIFIED_ROLE_ID");

    private final String ID_PREFIX = "pluginaction-download-";

    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith((ID_PREFIX + "version-"))) {
            // 플러그인 검색
            PluginService pluginService = DatabaseManager.getPluginService();
            String ENName = componentId.substring((ID_PREFIX + "version-").length());
            Plugin plugin = pluginService.getDataByENName(ENName);

            // 플러그인 존재여부 검증
            if (plugin == null) return;

            // 구매여부 검증
            if (!canDownload(event.getMember(), plugin)) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **플러그인 구매 후에 다운로드가 가능합니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 플러그인 파일 검색
            PluginFileService pluginFileService = DatabaseManager.getPluginFileService();
            MCVersion mcVersion = MCVersion.valueOf(event.getSelectedOptions().get(0).getValue());
            PluginFile pluginFile = pluginFileService.getData(ENName, mcVersion, plugin.getVersion());

            // 버전 지원여부 검증
            if (pluginFile == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **지원되지 않는 마인크래프트 버전입니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 토큰 생성 및 등록
            String token = TokenUtil.generateToken();
            Download download = new Download(
                    token,
                    pluginFile,
                    event.getUser().getIdLong(),
                    null,
                    false,
                    null,
                    false,
                    new Date(),
                    null,
                    new Date(new Date().getTime() + 1000 * 60 * 30)
            );

            DownloadService downloadService = DatabaseManager.getDownloadService();
            downloadService.saveData(download);

            // 메시지 전송
            String url = WEB_ADDRESS + "download/" + token;

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:loading:1168266572847128709> 다운로드 | 플러그인 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **다운로드 링크가 생성되었습니다.**
                            > **아래의 다운로드 버튼을 클릭하여 파일을 받으세요.**
                            
                            ─────────────────────────────────────────────────
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("항상 라이선스를 준수해 주셔서 감사합니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            Button button = Button.link(url, "다운로드");
            event.replyEmbeds(embed)
                    .addActionRow(button)
                    .setEphemeral(true)
                    .queue();

            event.editSelectMenu(event.getSelectMenu()).queue();
        }
    }

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String componentId = event.getComponentId();
        if (componentId.startsWith((ID_PREFIX))) {
            // 인증여부 검증
            UserService userService = DatabaseManager.getUserService();
            User user = userService.getDataByDiscordId(event.getUser().getIdLong());
            if (user == null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **다운로드를 위해 인증 절차가 필요합니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                return;
            }

            Guild guild = DiscordBotManager.getInstance().getGuild();
            Role verifiedRole = guild.getRoleById(VERIFIED_ROLE_ID);
            if (!event.getMember().getRoles().contains(verifiedRole)) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **다운로드를 위해 인증 절차가 필요합니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();

                return;
            }

            // 블랙리스트 여부 검증
            BlacklistService blacklistService = DatabaseManager.getBlacklistService();
            if (blacklistService.getDataByUserId(event.getUser().getIdLong()) != null) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **블랙리스트에 등록된 상태로 다운로드를 진행할 수 없습니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 플러그인 검색
            PluginService pluginService = DatabaseManager.getPluginService();
            String ENName = componentId.substring(ID_PREFIX.length());
            Plugin plugin = pluginService.getDataByENName(ENName);

            // 구매여부 검증
            if (!canDownload(event.getMember(), plugin)) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 플러그인 <a:loading:1168266572847128709>")
                        .setDescription("""
                            > **플러그인을 구매하신 후에 다운로드가 가능합니다.**
                            
                            ─────────────────────────────────────────────────
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 플러그인 파일 검색
            List<MCVersion> availableVersions = plugin.getSupportedVersions();

            // 메시지 전송
            StringSelectMenu.Builder selectOptionBuilder = StringSelectMenu.create(ID_PREFIX + "version-" + plugin.getENName());
            availableVersions.forEach(availableVersion -> {
                selectOptionBuilder.addOptions(getSelectOption(availableVersion));
            });

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 다운로드 | 플러그인 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **사용하실 마인크래프트 버전을 선택해 주세요.**
                            
                            ─────────────────────────────────────────────────
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .setFooter("항상 라이선스를 준수해 주셔서 감사합니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                    .build();
            event.replyEmbeds(embed)
                    .addActionRow(selectOptionBuilder.build())
                    .setEphemeral(true)
                    .queue();
        }
    }

    // UTILITY
    private SelectOption getSelectOption(MCVersion mcVersion) {
        SelectOption selectOption = SelectOption.of(mcVersion.name(), mcVersion.name());

        return switch (mcVersion) {
            case v1_12 -> selectOption.withDescription("1.12 ~ 1.12.2 버전을 지원합니다.");
            case v1_13 -> selectOption.withDescription("1.13 ~ 1.13.2 버전을 지원합니다.");
            case v1_14 -> selectOption.withDescription("1.14 ~ 1.14.4 버전을 지원합니다.");
            case v1_15 -> selectOption.withDescription("1.15 ~ 1.15.2 버전을 지원합니다.");
            case v1_16 -> selectOption.withDescription("1.16 ~ 1.16.5 버전은 지원합니다.");
            case v1_17 -> selectOption.withDescription("1.17 ~ 1.17.1 버전을 지원합니다.");
            case v1_18 -> selectOption.withDescription("1.18 ~ 1.18.2 버전을 지원합니다.");
            case v1_19 -> selectOption.withDescription("1.19 ~ 1.19.4 버전을 지원합니다.");
            case v1_20 -> selectOption.withDescription("1.20 ~ 1.20.4 버전을 지원합니다.");
        };
    }

    private boolean canDownload(Member member, Plugin plugin) {
        if (plugin.getPrice() != 0) {
            JDA jda = DiscordBotManager.getInstance().getJda();
            Role buyerRole = jda.getRoleById(plugin.getBuyerRole());

            return member.getRoles().contains(buyerRole)
                    || PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR);
        } else return true;
    }
}