
package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.UserService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Logger;

public class AuthHandler implements HttpHandler {

    private final UserService userService = DatabaseManager.getUserService();
    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final String AUTH_ROLE = configProvider.getString("AUTH_ROLE");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String[] pathSegments = exchange.getRequestURI().getPath().split("/");
        long userId = Long.parseLong(pathSegments[2]);
        String token = pathSegments[3];

        if (!authService.validateToken(userId, token)) {
            sendErrorResponse(exchange, "Invalid or expired token.");
            return;
        }

        String userIp = exchange.getRemoteAddress().getAddress().getHostAddress();

        Guild guild = DiscordBotManager.getInstance().getJda().getGuilds().get(0);
        Member member = guild.getMemberById(userId);
        if (member == null) {
            sendErrorResponse(exchange, "User not found.");
            return;
        }

        Role authorizedRole = guild.getRoleById(AUTH_ROLE);
        if (authorizedRole == null) {
            System.out.println("Role with ID " + AUTH_ROLE + " not found in server " + guild.getName());
            sendErrorResponse(exchange, "Authorized role not found in the server.");
            return;
        }

        if (member.getRoles().contains(authorizedRole)) {
            sendErrorResponse(exchange, "The user already has that role.");
        } else {
            guild.addRoleToMember(member, authorizedRole).queue();

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1141625729386287206> 성공 | 인증 완료 <a:success:1141625729386287206>")
                    .setDescription("> **🎉 축하합니다! 인증이 성공적으로 완료되었습니다.**\n"
                            + "> **커뮤니티의 모든 기능을 마음껏 즐기세요! 🥳**\n"
                            + "> **즐거운 시간 보내세요! \uD83C\uDF88**\n\u1CBB")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .setFooter("스탈리 커뮤니티에서 발송된 메시지입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .build();

            User user = member.getUser();
            user.openPrivateChannel()
                    .flatMap(channel -> channel.sendMessageEmbeds(messageEmbed))
                    .queue(null, throwable ->
                            LOGGER.warning("해당 유저에게 DM을 보낼 수 없습니다: " + throwable.getMessage()));

            if (userService.getDataByDiscordId(userId) == null) {
                userService.saveData(userId, userIp, new Date(), 0);
                LOGGER.info("유저 인증을 하였으므로 데이터를 추가했습니다: " + userId);
            } else {
                LOGGER.warning("이미 데이터베이스에 존재하는 유저입니다: " + userId);
            }

            String response = "Successfully authenticated and role assigned!";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

            if (authService.validateToken(userId, token)) {
                authService.removeTokenForUser(userId);
            }
        }
    }

    private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        exchange.sendResponseHeaders(400, errorMessage.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorMessage.getBytes());
        }
    }
}