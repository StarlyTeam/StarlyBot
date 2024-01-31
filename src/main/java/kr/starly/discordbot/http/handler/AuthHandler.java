package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.RankRepository;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.service.VerifyService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthHandler implements HttpHandler {

    private final UserService userService = DatabaseManager.getUserService();

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
            sendResponse(exchange, 400, "존재하지 않거나, 만료된 토큰입니다.");
            return;
        }

        String userIp = exchange.getRequestHeaders().get("x-forwarded-for").get(0);

        Guild guild = DiscordBotManager.getInstance().getJda().getGuilds().get(0);
        Member member = guild.getMemberById(userId);
        if (member == null) {
            sendResponse(exchange, 400, "유저를 찾을 수 없습니다.");
            return;
        }

        Role authorizedRole = guild.getRoleById(VERIFIED_ROLE_ID);
        if (authorizedRole == null) {
            sendResponse(exchange, 500, "인증역할을 찾을 수 없습니다.");
            return;
        }

        VerifyService verifyService = DatabaseManager.getVerifyService();
        List<Verify> verifies = verifyService.getDataByUserIp(userIp);
        if (verifies.stream()
                .anyMatch(verify ->
                        !verify.getUserIp().equals(userIp)
                                && guild.getMemberById(verify.getUserId()) != null
                )
        ) {
            sendResponse(exchange, 403, "이미 해당 IP 주소에서 인증된 다른 유저가 있습니다.");

            AuditLogger.warning(new EmbedBuilder()
                    .setTitle("<a:cross:1058939340505497650> 오류 | 유저인증 <a:cross:1058939340505497650>")
                    .setDescription("""
                            > **이미 해당 IP 주소에서 인증된 다른 유저가 있습니다.**
                            
                            ─────────────────────────────────────────────────
                            > **유저: %s**
                            > **아이피: %s**
                            """
                            .formatted(member.getAsMention() + " (" + member.getEffectiveName() + ")", userIp)
                    )
            );
            return;
        }

        BlacklistService blacklistService = DatabaseManager.getBlacklistService();
        if (blacklistService.getDataByUserId(userId) != null
            || blacklistService.getDataByIpAddress(userIp) != null) {
            sendResponse(exchange, 403, "당신은 블랙리스트에 등록되어 있습니다.");

            AuditLogger.warning(new EmbedBuilder()
                    .setTitle("<a:cross:1058939340505497650> 오류 | 유저인증 <a:cross:1058939340505497650>")
                    .setDescription("""
                            > **블랙리스트에 등록된 유저가 인증을 시도했습니다.**
                            
                            ─────────────────────────────────────────────────
                            > **유저: %s**
                            > **아이피: %s**
                            """
                            .formatted(member.getAsMention() + " (" + member.getEffectiveName() + ")", userIp)
                    )
            );
            return;
        }

        guild.addRoleToMember(member, authorizedRole)
                .queue(null, (ignored) -> {});

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 인증 완료 <a:success:1168266537262657626>")
                .setDescription("""
                        > **🎉 축하합니다! 인증이 성공적으로 완료되었습니다.**
                        > **이제 커뮤니티의 모든 기능을 마음껏 즐기실 수 있습니다! 🥳**
                        > **즐거운 시간 보내세요! \uD83C\uDF88**
                        \u1CBB""")
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                .setFooter("스탈리에서 발송된 메시지입니다.", "https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                .build();

        AtomicBoolean isDMSent = new AtomicBoolean(true);
        member.getUser()
                .openPrivateChannel().complete()
                .sendMessageEmbeds(embed)
                .queue(null, throwable -> {
                            AuditLogger.warning(new EmbedBuilder()
                                    .setTitle("<a:success:1168266537262657626> 경고 | 유저 인증 <a:success:1168266537262657626>")
                                    .setDescription("""
                                            > **DM을 전송하지 못했습니다.**
                                            
                                            ─────────────────────────────────────────────────
                                            > **유저: %s**
                                            """
                                            .formatted(member.getAsMention() + " (" + member.getEffectiveName() + ")")
                                    )
                            );

                            isDMSent.set(false);
                        }
                );

        if (userService.getDataByDiscordId(userId) == null) {
            RankRepository rankRepository = RankRepository.getInstance();
            Rank rank1 = rankRepository.getRank(1);

            userService.saveData(new User(userId, userIp, new Date(), 0, new ArrayList<>(List.of(rank1))));
            AuditLogger.info(new EmbedBuilder()
                    .setTitle("<a:success:1168266537262657626> 성공 | 유저 인증 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **유저 인증에 성공했습니다.**
                            
                            ─────────────────────────────────────────────────
                            > **유저: %s**
                            > **아이피: %s**
                            """
                            .formatted(member.getAsMention() + " (" + member.getEffectiveName() + ")", userIp)
                    )
            );
        } else {
            AuditLogger.warning(
                    new EmbedBuilder()
                            .setTitle("<a:loading:1168266572847128709> 경고 | 유저 인증 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **데이터가 존재하는 유저가 인증을 시도했습니다.**
                                    > **다만, 인증은 처리되었습니다.**
                                                                            
                                    > **유저: %s**
                                    > **아이피: %s**
                                                                            
                                    ─────────────────────────────────────────────────
                                    """
                                    .formatted(member.getAsMention() + " (" + member.getEffectiveName() + ")", userIp)
                            )
            );
        }

        sendResponse(exchange, 200, "성공적으로 인증을 마쳤습니다.");

        if (authService.validateToken(userId, token)) {
            authService.removeTokenForUser(userId);
        }

        verifyService.saveData(new Verify(token, userId, userIp, isDMSent.get(), new Date()));
    }

    private void sendResponse(HttpExchange exchange, int rCode, String rBody) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(rCode, rBody.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(rBody.getBytes(StandardCharsets.UTF_8));
        }

        exchange.close();
    }
}