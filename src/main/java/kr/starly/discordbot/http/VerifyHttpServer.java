package kr.starly.discordbot.http;

import com.sun.net.httpserver.HttpServer;
import kr.starly.discordbot.http.handler.AuthHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class VerifyHttpServer {

    private HttpServer server;

    public void start(int port) throws IOException {
        if (isPortAvailable(port)) {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/auth/", new AuthHandler());
            server.setExecutor(null);
            server.start();
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