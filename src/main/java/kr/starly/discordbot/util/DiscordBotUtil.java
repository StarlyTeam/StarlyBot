package kr.starly.discordbot.util;

import kr.starly.discordbot.manager.DiscordBotManager;

public class DiscordBotUtil {

    public static void waitForBotToLoad(DiscordBotManager botManager) {
        while (!botManager.isBotFullyLoaded()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void reloadBotAfterDelay(DiscordBotManager botManager, int delayMillis) {
        new Thread(() -> {
            try {
                Thread.sleep(1200);
                int delaySeconds = delayMillis / 1000;
                for (int i = delaySeconds; i > 0; i--) {
                    System.out.println(i + " second" + (i > 1 ? "s" : "") + " left");
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            botManager.startBot();
        }).start();
    }
}
