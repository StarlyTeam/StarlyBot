package kr.starly.discordbot.http;

import com.sun.net.httpserver.HttpServer;
import kr.starly.discordbot.http.handler.AuthHandler;
import kr.starly.discordbot.http.handler.DownloadHandler;
import kr.starly.discordbot.http.handler.ShortenLinkHandler;
import kr.starly.discordbot.http.handler.api.APIv1Handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;

public class WebServer {

    private HttpServer server;

    private final Logger LOGGER = Logger.getLogger(getClass().getName());

    public void start(int port) throws IOException {
        if (isPortAvailable(port)) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/v1/", new APIv1Handler());
            server.createContext("/download/", new DownloadHandler());
            server.createContext("/auth/", new AuthHandler());
            server.createContext("/", new ShortenLinkHandler());
            server.setExecutor(null);
            server.start();

            LOGGER.info("HTTP 서버를 실행했습니다. (포트: " + port + ")");
        } else {
            LOGGER.severe("HTTP 서버를 실행하지 못했습니다. (포트: " + port + " 사용 중)");
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}