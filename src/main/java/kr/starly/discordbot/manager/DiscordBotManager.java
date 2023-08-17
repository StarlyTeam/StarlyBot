package kr.starly.discordbot.manager;

import kr.starly.discordbot.configuration.ConfigManager;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.logging.Logger;

public class DiscordBotManager {

    private static DiscordBotManager instance;

    public static DiscordBotManager getInstance() {
        if (instance == null) instance = new DiscordBotManager();
        return instance;
    }

    private DiscordBotManager() {}

    private static final Logger LOGGER = Logger.getLogger(DiscordBotManager.class.getName());
    private final ConfigManager configManager = ConfigManager.getInstance();

    @Getter
    private JDA jda;
    private boolean isBotFullyLoaded = false;

    public void startBot() {
        String BOT_TOKEN = configManager.getString("BOT_TOKEN");

        try {
            JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN);
            configureBot(builder);
            jda = builder.build();
        } catch (Exception e) {
            LOGGER.severe("봇을 실행하는 도중에 오류가 발생하였습니다." + e.getMessage());
        }
    }

    public void stopBot() {
        System.out.println("Shutting down JDA...");
        jda.shutdown();
        System.out.println("JDA has been shut down.");
    }

    public boolean isBotFullyLoaded() {
        return isBotFullyLoaded;
    }

    private void configureBot(JDABuilder builder) {
        ConfigManager configManager = ConfigManager.getInstance();
        String activityName = configManager.getString("ACTIVITY_NAME");
        String activityType = configManager.getString("ACTIVITY_TYPE");
        String status = configManager.getString("STATUS");

        try {
            Activity.ActivityType type = Activity.ActivityType.valueOf(activityType.toUpperCase());
            Activity activity = Activity.of(type, activityName);
            builder.setActivity(activity);
        } catch (IllegalArgumentException e) {
            System.out.println("존재하지 않는 Activity 타입입니다.");
        }

        enableAllIntents(builder);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.MEMBER_OVERRIDES)
                .setEnableShutdownHook(false)
                .setStatus(OnlineStatus.valueOf(status));

        builder.addEventListeners(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                isBotFullyLoaded = true;
                System.out.println("The bot is fully loaded and ready!");
            }
        });
    }

    private void enableAllIntents(JDABuilder builder) {
        for (GatewayIntent intent : GatewayIntent.values()) {
            builder.enableIntents(intent);
        }
    }
}
