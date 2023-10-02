package kr.starly.discordbot;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.http.WebServer;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.util.DiscordBotUtil;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final DiscordBotManager botManager = DiscordBotManager.getInstance();

    private static final WebServer webServer = new WebServer();

    public static void main(String[] args) {
        startDiscordBot();
        startHttpServer();
        handleCommands();
    }

    private static void startDiscordBot() {
        botManager.startBot();
        DiscordBotUtil.waitForBotToLoad(botManager);
    }

    private static void startHttpServer() {
        int WEB_PORT = configProvider.getInt("WEB_PORT");

        try {
            webServer.start(WEB_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not start HTTP Server due to: " + e.getMessage());
        }
    }

    private static void handleCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String command = scanner.nextLine();

            if ("stop".equals(command.toLowerCase())) {
                stopServices(scanner);
                break;
            }

            if ("reload".equals(command.toLowerCase()) || "restart".equals(command.toLowerCase())) {
                restartBot();
            }
        }
    }

    private static void stopServices(Scanner scanner) {
        System.out.println("Stopping the bot...");
        botManager.stopBot();
        webServer.stop();
        scanner.close();
    }

    private static void restartBot() {
        System.out.println("Restarting the bot...");
        botManager.stopBot();
        DiscordBotUtil.reloadBotAfterDelay(botManager, 5000);
    }
}