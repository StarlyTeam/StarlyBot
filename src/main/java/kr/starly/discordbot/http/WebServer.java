package kr.starly.discordbot.http;

import com.sun.net.httpserver.HttpServer;
import kr.starly.discordbot.http.handler.AuthHandler;
import kr.starly.discordbot.http.handler.DownloadHandler;
import kr.starly.discordbot.http.handler.ShortenLinkHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class WebServer {

    private HttpServer server;

    public void start(int port) throws IOException {
        if (isPortAvailable(port)) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/download/", new DownloadHandler());
            server.createContext("/auth/", new AuthHandler());
            server.createContext("/", new ShortenLinkHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("HTTP server started on port: " + port);
        } else {
            System.out.println("Port " + port + " is already in use.");
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