package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.service.ShortenLinkService;

import java.io.IOException;

public class ShortenLinkHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equals("GET")) return;

        String[] params = exchange.getRequestURI().toString().substring(1).split("/");
        if (params.length != 1) return;

        ShortenLinkService shortenLinkService = DatabaseManager.getShortenLinkService();
        String shortenUrl = params[0];

        ShortenLink shortenLink = shortenLinkService.getDataByShortenUrl(shortenUrl);
        if (shortenLink == null) return;

        try {
            String originUrl = shortenLink.originUrl();
            exchange.getResponseHeaders().set("Location", originUrl);
            exchange.sendResponseHeaders(302, 0);
            exchange.getResponseBody().close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
