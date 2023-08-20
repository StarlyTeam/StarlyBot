package kr.starly.discordbot.http;

import com.sun.net.httpserver.HttpServer;
import kr.starly.discordbot.http.handler.AuthHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class VerifyHttpServer {

    private HttpServer server;

    public void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/auth/", new AuthHandler());
        server.setExecutor(null);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}