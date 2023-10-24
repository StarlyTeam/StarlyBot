package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.PluginFile;
import kr.starly.discordbot.repository.impl.DownloadTokenRepository;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DownloadHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) {
        if (!exchange.getRequestMethod().equals("GET")) return;

        String[] params = exchange.getRequestURI().getPath().split("/");
        if (params.length != 3) return;
        String token = params[2];

        DownloadTokenRepository tokenRepository = DownloadTokenRepository.getInstance();
        PluginFile pluginFile = tokenRepository.remove(token);
        if (pluginFile == null) {
            try {
                String response = "존재하지 않거나, 만료된 토큰입니다.";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }

        Plugin plugin = pluginFile.getPlugin();
        String fileName = pluginFile.getFile().getName();
        String newFileName = plugin.getENName() + " [" + pluginFile.getMcVersion() + ", " + pluginFile.getVersion() + "]." + fileName.substring(fileName.lastIndexOf(".") + 1);

        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pluginFile.getFile()));
            byte[] bytes = bis.readAllBytes();

            exchange.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + newFileName + "\"");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException ex) {
            ex.printStackTrace();

            try {
                exchange.sendResponseHeaders(500, 0);
                exchange.getResponseBody().close();
            } catch (IOException ex1) {
                ex1.printStackTrace();
            }
        }
    }
}