package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.service.ShortenLinkService;
import kr.starly.discordbot.service.VerifyService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.IOException;

public class ShortenLinkHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equals("GET")) return;

        String[] params = exchange.getRequestURI().getPath().split("/");
        if (params.length != 2) return;
        String shortenCode = params[1];

        ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
        ShortenLink shortenLink = shortenLinkService.getDataByShortenUrl(shortenCode);
        if (shortenLink == null) return;

        String userIp = exchange.getRequestHeaders().get("x-real-ip").get(0);

        try {
            String originUrl = shortenLink.originUrl();
            exchange.getResponseHeaders().set("Location", originUrl);
            exchange.sendResponseHeaders(302, 0);
            exchange.getResponseBody().close();
        } catch (IOException ex) {
            ex.printStackTrace();

            AuditLogger.error(new EmbedBuilder()
                    .setTitle("<a:cross:1058939340505497650> 실패 | 단축링크 <a:cross:1058939340505497650>")
                    .setDescription("""
                            > **원본링크로 이동하는데 실패했습니다.**
                            
                            > **단축링크: %s**
                            > **아이피: %s**
                            
                            ─────────────────────────────────────────────────
                            """
                            .formatted(shortenCode, userIp)
                    )
            );
            return;
        }

        VerifyService verifyService = DatabaseManager.getVerifyService();
        Verify verify = verifyService.getDataByUserIp(userIp).get(0);

        AuditLogger.info(new EmbedBuilder()
                .setTitle("<a:success:1168266537262657626> 성공 | 단축링크 <a:success:1168266537262657626>")
                .setDescription("""
                            > **원본링크로 이동하는데 성공했습니다.**
                            
                            ─────────────────────────────────────────────────
                            > **단축링크: %s**
                            > **추정되는 유저: %s**
                            > **아이피: %s**
                            """
                        .formatted(shortenCode, verify == null ? "없음" : "<@" + verify.getUserId() + ">", userIp)
                )
        );
    }
}