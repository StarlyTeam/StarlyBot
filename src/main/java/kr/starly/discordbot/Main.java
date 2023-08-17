package kr.starly.discordbot;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.util.DiscordBotUtil;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        ConfigManager.getInstance().loadConfig();
        DiscordBotManager botManager = DiscordBotManager.getInstance();
        botManager.startBot();

        DiscordBotUtil.waitForBotToLoad(botManager);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String command = scanner.nextLine();

            if ("stop".equals(command.toLowerCase())) {
                System.out.println("Stopping the bot...");
                botManager.stopBot();
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
