package kr.starly.discordbot;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.http.WebServer;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.RankRepository;

import java.io.IOException;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.logging.Logger;

public class Main {

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final DiscordBotManager botManager = DiscordBotManager.getInstance();

    private static final WebServer webServer = new WebServer();

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        startDiscordBot();
        startHttpServer();
        RankRepository.$initialize();

        handleCommands();
    }

    private static void startDiscordBot() {
        botManager.startBot();

        while(!botManager.isBotFullyLoaded()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static void startHttpServer() {
        int WEB_PORT = configProvider.getInt("WEB_PORT");

        try {
            webServer.start(WEB_PORT);
        } catch (IOException ex) {
            ex.printStackTrace();

            LOGGER.severe("HTTP 서버를 실행하지 못했습니다. (" + ex.getMessage() + ")");
        }
    }

    private static void handleCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("> ");
            System.out.flush();
            String command = scanner.nextLine();

            if ("stop".equalsIgnoreCase(command)) {
                stopServices(scanner);
                break;
            } else if ("help".equalsIgnoreCase(command)) {
                System.out.println("사용가능한 명령어 목록: stop, help");
            } else if (!command.isEmpty()) {
                System.out.println("알 수 없는 명령어입니다. 도움말을 확인하려면 'help' 명령어를 입력해보세요.");
            }
        }
    }

    private static void stopServices(Scanner scanner) {
        botManager.stopBot();
        webServer.stop();
        scanner.close();

        System.exit(0);
    }
}