package kr.starly.discordbot;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.http.VerifyHttpServer;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.util.DiscordBotUtil;

import java.io.IOException;
import java.util.Scanner;

public class Main {

    private static final ConfigManager configManager = ConfigManager.getInstance();
    private static int AUTH_PORT;

    public static void main(String[] args) {
        configManager.loadConfig();
        DiscordBotManager botManager = DiscordBotManager.getInstance();
        botManager.startBot();

        VerifyHttpServer httpServer = new VerifyHttpServer();
        AUTH_PORT = configManager.getInt("AUTH_PORT");
        try {

            httpServer.start(AUTH_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not start HTTP Server");
        }

        DiscordBotUtil.waitForBotToLoad(botManager);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String command = scanner.nextLine();

            if ("stop".equals(command.toLowerCase())) {
                System.out.println("Stopping the bot...");
                botManager.stopBot();
                httpServer.stop();
                scanner.close();
                break;
            }

            if ("reload".equals(command.toLowerCase()) || "restart".equals(command.toLowerCase())) {
                System.out.println("Restarting the bot...");
                botManager.stopBot();
                DiscordBotUtil.reloadBotAfterDelay(botManager, 5000);
            }
        }
    }
}