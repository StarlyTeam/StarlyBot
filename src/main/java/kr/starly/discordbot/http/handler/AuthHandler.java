
package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AuthHandler implements HttpHandler {

    private final UserService userService = DatabaseManager.getUserService();
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final String VERIFIED_ROLE_ID = configProvider.getString("VERIFIED_ROLE_ID");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] pathSegments = exchange.getRequestURI().getPath().split("/");
        long userId = Long.parseLong(pathSegments[2]);
        String token = pathSegments[3];

        if (!authService.validateToken(userId, token)) {
            sendResponse(exchange, 404, "존재하지 않거나, 만료된 토큰입니다.");
            return;
        }

        String userIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        Guild guild = DiscordBotManager.getInstance().getJda().getGuilds().get(0);
        Member member = guild.getMemberById(userId);
        if (member == null) {
            sendResponse(exchange, 500, "유저를 찾을 수 없습니다.");
            return;
        }

        Role authorizedRole = guild.getRoleById(VERIFIED_ROLE_ID);
        if (authorizedRole == null) {
            sendResponse(exchange, 500, "인증역할을 찾을 수 없습니다.");
            return;
        }

        if (member.getRoles().contains(authorizedRole)) {
            sendResponse(exchange, 403, "당신은 이미 인증을 마쳤습니다.");
            return;
        }

        BlacklistService blacklistService = DatabaseManager.getBlacklistService();
        if (blacklistService.getDataByUserId(userId) != null) {
            sendResponse(exchange, 403, "당신은 블랙리스트에 등록되어 있습니다.");

            AuditLogger.warning(new EmbedBuilder()
                    .setTitle("블랙리스트에 등록된 유저가 인증을 시도했습니다.")
                    .setDescription("유저: " + member.getAsMention() + " (" + member.getEffectiveName() + ")\n"
                            + "IP 주소: " + userIp)
            );
            return;
        } else if (blacklistService.getDataByIpAddress(userIp) != null) {
            sendResponse(exchange, 403, "당신의 IP 주소는 블랙리스트에 등록되어 있습니다.");

            AuditLogger.warning(new EmbedBuilder()
                    .setTitle("블랙리스트에 등록된 IP 주소에서 인증을 시도했습니다.")
                    .setDescription("유저: " + member.getAsMention() + " (" + member.getEffectiveName() + ")\n"
                            + "IP 주소: " + userIp)
            );
            return;
        }

        guild.addRoleToMember(member, authorizedRole).queue();

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 인증 완료 <a:success:1168266537262657626>")
                .setDescription("""
                        > **🎉 축하합니다! 인증이 성공적으로 완료되었습니다.**
                        > **이제 커뮤니티의 모든 기능을 마음껏 즐기실 수 있습니다! 🥳**
                        > **즐거운 시간 보내세요! \uD83C\uDF88**
                        \u1CBB""")
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                .setFooter("스탈리 커뮤니티에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                .build();

        User user = member.getUser();
        user.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(messageEmbed))
                .queue(null, throwable -> {
                            LOGGER.warning("해당 유저 (" + userId + ")에게 DM을 전송할 수 없습니다: " + throwable.getMessage());

                            AuditLogger.warning(new EmbedBuilder()
                                    .setTitle("인증을 마쳤으나, 해당 유저에게 DM을 전송하지 못했습니다.")
                                    .setDescription("유저: " + member.getAsMention() + " (" + member.getEffectiveName() + ")\n"
                                            + "IP 주소: " + userIp)
                            );
                        }
                );

        if (userService.getDataByDiscordId(userId) == null) {
            RankRepository rankRepository = RankRepository.getInstance();
            Rank rank1 = rankRepository.getRank(1);

            userService.saveData(userId, userIp, new Date(), 0, new ArrayList<>(List.of(rank1)));
            LOGGER.info("유저 인증을 하였으므로 데이터를 추가했습니다: " + userId);

            AuditLogger.info(new EmbedBuilder()
                    .setTitle("유저가 인증을 마쳤습니다.")
                    .setDescription("유저: " + member.getAsMention() + " (" + member.getEffectiveName() + ")\n"
                            + "IP 주소: " + userIp)
            );
        } else {
            LOGGER.warning("이미 데이터베이스에 존재하는 유저입니다: " + userId);
        }

        String response = "성공적으로 인증을 마쳤습니다.";
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();

        if (authService.validateToken(userId, token)) {
            authService.removeTokenForUser(userId);
        }
    }

    private void sendResponse(HttpExchange exchange, int rCode, String rBody) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(rCode, rBody.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(rBody.getBytes());
        }
    }
}