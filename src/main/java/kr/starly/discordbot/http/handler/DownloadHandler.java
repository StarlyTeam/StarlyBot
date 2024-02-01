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
import net.lingala.zip4j.ZipFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
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

        File tempFolder = new File("download/" + token);
        tempFolder.mkdirs();

        File metaFolder = new File(tempFolder, "META-INF");
        metaFolder.mkdirs();

        File metadataFile = new File(metaFolder, "metadata");
        metadataFile.createNewFile();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(metadataFile))) {
            String encodedString = Base64.getEncoder().encodeToString("""
                    UserId: %s
                    
                    DownloadToken: %s
                    DownloadIP: %s
                    DownloadTime: %s
                    """.formatted(
                    download.getUserId(),
                    download.getToken(),
                    exchange.getRequestHeaders().get("x-forwarded-for").get(0),
                    download.getUsedAt()
            ).getBytes(StandardCharsets.UTF_8));

            bw.write(encodedString);
        }

        File sourceFile = Files.copy(pluginFile.getFile().toPath(), new File(tempFolder, plugin.getENName() + ".jar").toPath()).toFile();
        try (ZipFile zipFile = new ZipFile(sourceFile)) {
            zipFile.addFolder(metaFolder);
            zipFile.addFile(new File("download/이용안내.txt"));
        } catch (IOException ex) {
            ex.printStackTrace();
            sendResponse(exchange, 500, ex.getMessage());

            download.updateIsSuccess(false);
            downloadService.saveData(download);
            return;
        }

        File outputFile = new File(tempFolder, "output.zip");
        try (ZipFile zipFile = new ZipFile(outputFile)) {
            zipFile.addFile(new File("download/이용안내.txt"));
        } catch (IOException ex) {
            ex.printStackTrace();
            sendResponse(exchange, 500, ex.getMessage());

            download.updateIsSuccess(false);
            downloadService.saveData(download);
            return;
        }

        try {
            String fileName = "%s [%s, %s].zip".formatted(plugin.getENName(), pluginFile.getMcVersion(), pluginFile.getVersion());
            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(outputFile));
            byte[] bytes = bis.readAllBytes();
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();

            download.updateIsSuccess(true);
        } catch (IOException ex) {
            ex.printStackTrace();
            sendResponse(exchange, 500, ex.getMessage());

            download.updateIsSuccess(false);
            downloadService.saveData(download);
            return;
        }

        download.updateUsedAt(new Date());
        downloadService.saveData(download);

        AuditLogger.info(new EmbedBuilder()
                .setTitle("<a:success:1168266537262657626> 성공 | 플러그인 다운로드 <a:success:1168266537262657626>")
                .setDescription("""
                                > **%s님이 %s을(를) 다운로드하였습니다.**

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
                                plugin.getENName() + "(" + plugin.getKRName() + ")",
                                pluginFile.getMcVersion(),
                                pluginFile.getVersion(),
                                "<@" + download.getUserId() + ">",
                                exchange.getRequestHeaders().get("x-forwarded-for").get(0),
                                download.getToken()
                        )
                )
        );
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