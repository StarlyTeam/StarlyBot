package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Download;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.service.DownloadService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class DownloadHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) return;

        String[] params = exchange.getRequestURI().getPath().split("/");
        if (params.length != 3) return;
        String token = params[2];

        DownloadService downloadService = DatabaseManager.getDownloadService();
        Download download = downloadService.getDataByToken(token);
        if (download == null || download.isUsed() || download.isExpired()) {
            sendResponse(exchange, 404, "존재하지 않거나, 만료된 토큰입니다.");
            return;
        }

        PluginFile pluginFile = download.getPluginFile();
        Plugin plugin = pluginFile.getPlugin();

        String originalFileName = pluginFile.getFile().getName();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".") + 1);
        String fileName = "%s [%s, %s].%s".formatted(plugin.getENName(), pluginFile.getMcVersion(), pluginFile.getVersion(), fileExtension);

        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pluginFile.getFile()));
            byte[] bytes = bis.readAllBytes();

            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();

            download.updateIsSuccess(true);
        } catch (IOException ex) {
            ex.printStackTrace();
            sendResponse(exchange, 500, ex.getMessage());

            download.updateIsSuccess(false);
        }

        download.updateIsUsed(true);
        download.updateUsedAt(new Date());
        downloadService.saveData(download);

        AuditLogger.info(new EmbedBuilder()
                .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 다운로드 <a:success:1168266537262657626>")
                .setDescription("""
                                > **%s님이 %s을(를) 다운로드하였습니다.**
                                > **성공 여부: %s**

                                ─────────────────────────────────────────────────
                                > **플러그인: %s**
                                > **버전: %s**
                                > **파일: %s**
                                > **유저: %s**
                                > **아이피: %s**
                                > **토큰: %s**
                                """.formatted(
                                "<@" + download.getUserId() + ">",
                                plugin.getENName() + "(" + plugin.getKRName() + ")",
                                download.isSuccess() ? "<a:success:1168266537262657626>" : "<a:cross:1058939340505497650>",
                                plugin.getENName() + "(" + plugin.getKRName() + ")",
                                pluginFile.getMcVersion(),
                                pluginFile.getVersion(),
                                "<@" + download.getUserId() + ">",
                                exchange.getRemoteAddress().getAddress().getHostAddress(),
                                download.getToken()
                        )
                )
        );
    }

    private void sendResponse(HttpExchange exchange, int rCode, String rBody) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(rCode, rBody.getBytes(StandardCharsets.UTF_8).length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(rBody.getBytes());
        }

        exchange.close();
    }
}