package kr.starly.discordbot.listener.pluginaction;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.enums.MCVersion;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.DownloadTokenRepository;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.service.PluginFileService;
import kr.starly.discordbot.service.PluginService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.TokenUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
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

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

@BotEvent
public class DownloadListener extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");

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
                        .setTitle("제목")
                        .setDescription("플러그인을 구매하셔야 다운로드 하실 수 있습니다.")
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
                        .setTitle("제목")
                        .setDescription("지원되지 않는 버전입니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 토큰 생성 및 등록
            DownloadTokenRepository tokenRepository = DownloadTokenRepository.getInstance();
            String token = TokenUtil.generateToken();
            tokenRepository.put(token, pluginFile);

            // 메시지 전송
            String url = WEB_ADDRESS + "download/" + token;

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("제목")
                    .setDescription("다운로드 링크를 생성했습니다.")
                    .build();
            Button button = Button.link(url, "다운로드");
            event.replyEmbeds(embed)
                    .addActionRow(button)
                    .setEphemeral(true)
                    .queue();
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
                        .setTitle("인증을 마치지 않았습니다.")
                        .setDescription("상품을 구매하기 위해서는 인증을 하셔야 합니다.\n" +
                                "인증을 마치신 후 다시 시도해주세요.")
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
                        .setTitle("제목")
                        .setDescription("블랙리스트에 등록되어 있으셔서 다운로드 불가합니다. ㅋㅎㅋㅎ")
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
                        .setTitle("제목")
                        .setDescription("플러그인을 구매하셔야 다운로드 하실 수 있습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
                return;
            }

            // 플러그인 파일 검색
            PluginFileService pluginFileService = DatabaseManager.getPluginFileService();
            List<PluginFile> pluginFiles = pluginFileService.getData(ENName, plugin.getVersion());
            pluginFiles.sort(Comparator.comparing(PluginFile::getMcVersion));

            // 메시지 전송
            StringSelectMenu.Builder selectOptionBuilder = StringSelectMenu.create(ID_PREFIX + "version-" + plugin.getENName());
            pluginFiles.forEach(pluginFile -> selectOptionBuilder.addOptions(getSelectOption(pluginFile.getMcVersion())));

            MessageEmbed embed = new EmbedBuilder()
                    .setTitle("제목")
                    .setDescription("버전을 선택해주세요")
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
            case v1_20 -> selectOption.withDescription("1.20 ~ 1.20.1 버전을 지원합니다.");
        };
    }

    private boolean canDownload(Member member, Plugin plugin) {
        if (plugin.getPrice() != 0) {
            JDA jda = DiscordBotManager.getInstance().getJda();
            Role buyerRole = jda.getRoleById(plugin.getBuyerRole());

            return !member.getRoles().contains(buyerRole)
                    && !PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR);
        } else return true;
    }
} // TODO : 메시지 디자인