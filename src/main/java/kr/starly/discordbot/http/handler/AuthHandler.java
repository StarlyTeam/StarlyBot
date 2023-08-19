
package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.UserInfoService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.logging.Logger;

public class AuthHandler implements HttpHandler {

    private final UserInfoService userInfoService = DatabaseConfig.getUserInfoService();
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String AUTHORIZED_ROLE_ID = configManager.getString("AUTHORIZED_ROLE_ID");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] pathSegments = exchange.getRequestURI().getPath().split("/");
        String discordUserId = pathSegments[2];
        String token = pathSegments[3];

        if (!authService.validateToken(discordUserId, token)) {
            sendErrorResponse(exchange, "Invalid or expired token.");
            return;
        }

        String userIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        Guild guild = DiscordBotManager.getInstance().getJda().getGuilds().get(0);
        Member member = guild.getMemberById(discordUserId);
        User user = member.getUser();

        if (member != null) {
            Role authorizedRole = guild.getRoleById(AUTHORIZED_ROLE_ID);

            if (authorizedRole != null) {
                if (member.getRoles().contains(authorizedRole)) {
                    sendErrorResponse(exchange, "The user already has that role.");
                } else {
                    guild.addRoleToMember(member, authorizedRole).queue();

                    MessageEmbed embedBuilder = new EmbedBuilder()
                            .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                            .setTitle("<a:success:1141625729386287206> 성공 | 인증 완료 <a:success:1141625729386287206>")
                            .setDescription("> **🎉 축하합니다! 인증이 성공적으로 완료되었습니다.**\n"
                                    + "> **커뮤니티의 모든 기능을 마음껏 즐기세요! 🥳**\n"
                                    + "> **즐거운 시간 보내세요! \uD83C\uDF88**\n\u1CBB")
                            .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .setFooter("스탈리 커뮤니티에서 발송된 메시지입니다.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .build();

                    user.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(embedBuilder))
                            .queue(null, throwable ->
                                LOGGER.severe("해당 유저에게 DM을 보낼 수 없습니다: " + throwable.getMessage()));

                    if (userInfoService.getUserInfo(discordUserId) == null) {
                        LocalDateTime verifyDate = LocalDateTime.now();
                        userInfoService.recordUserInfo(discordUserId, userIp, verifyDate);
                        LOGGER.severe("유저 인증을 하였으므로 데이터를 추가했습니다: " + discordUserId);
                    } else {
                        LOGGER.severe("이미 데이터베이스에 존재하는 유저입니다: " + discordUserId);
                    }

                    String response = "Successfully authenticated and role assigned!";
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();

                    if (authService.validateToken(discordUserId, token)) {
                        authService.removeTokenForUser(discordUserId);
                    }
                }
            } else {
                System.out.println("Role with ID " + AUTHORIZED_ROLE_ID + " not found in server " + guild.getName());
                sendErrorResponse(exchange, "Authorized role not found in the server.");
            }
        } else {
            sendErrorResponse(exchange, "User not found.");
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        exchange.sendResponseHeaders(400, errorMessage.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorMessage.getBytes());
        }
    }
}