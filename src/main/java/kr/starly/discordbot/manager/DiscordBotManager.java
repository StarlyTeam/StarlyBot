package kr.starly.discordbot.manager;

import kr.starly.discordbot.command.CommandListenerBase;
import kr.starly.discordbot.command.slash.SlashCommandListenerBase;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.listener.BotEvent;
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
import org.reflections.Reflections;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class DiscordBotManager {

    private static DiscordBotManager instance;

    public static DiscordBotManager getInstance() {
        if (instance == null) instance = new DiscordBotManager();
        return instance;
    }

    private DiscordBotManager() {}

    private static final Logger LOGGER = Logger.getLogger(DiscordBotManager.class.getName());

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String GUILD_ID = configProvider.getString("GUILD_ID");
    private final String BOT_TOKEN = configProvider.getString("BOT_TOKEN");

    @Getter
    private JDA jda;
    private boolean isBotFullyLoaded = false;

    private final SlashCommandListenerBase slashCommandListenerBase = new SlashCommandListenerBase();

    public void startBot() {
        try {
            JDABuilder builder = JDABuilder.createDefault(BOT_TOKEN);
            configureBot(builder);

            CommandListenerBase commandListener = new CommandListenerBase();
            builder.addEventListeners(commandListener);
            jda = builder.build();
            registerEventListeners();
        } catch (Exception e) {
            LOGGER.severe("봇을 실행하는 도중에 오류가 발생하였습니다." + e.getMessage());
        }
    }

    public void stopBot() {
        LOGGER.info("JDA를 종료합니다..");
        jda.shutdown();
        LOGGER.info("JDA가 종료되었습니다.");
    }

    public boolean isBotFullyLoaded() {
        return isBotFullyLoaded;
    }

    private void configureBot(JDABuilder builder) {
        ConfigProvider configProvider = ConfigProvider.getInstance();
        String activityName = configProvider.getString("ACTIVITY_NAME");
        String activityType = configProvider.getString("ACTIVITY_TYPE");
        String status = configProvider.getString("STATUS");

        try {
            Activity.ActivityType type = Activity.ActivityType.valueOf(activityType.toUpperCase());
            Activity activity = Activity.of(type, activityName);
            builder.setActivity(activity);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("존재하지 않는 Activity 타입입니다.");
        }

        builder.addEventListeners(slashCommandListenerBase);

        enableAllIntents(builder);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.MEMBER_OVERRIDES)
                .setEnableShutdownHook(false)
                .setStatus(OnlineStatus.valueOf(status));

        builder.addEventListeners(new ListenerAdapter() {
            @Override
            public void onReady(ReadyEvent event) {
                isBotFullyLoaded = true;
                LOGGER.info("봇을 성공적으로 실행하였습니다!");

                slashCommandListenerBase.getCommands().forEach(commandData -> {
                    jda.getGuildById(GUILD_ID).upsertCommand(commandData).queue();
                });
            }
        });
    }

    private void enableAllIntents(JDABuilder builder) {
        builder.enableIntents(List.of(GatewayIntent.values()));
    }

    private void registerEventListeners() {
        String packageName = "kr.starly.discordbot";
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotEvent.class);

        for (Class<?> clazz : annotated) {
            try {
                Object instance = clazz.getDeclaredConstructor().newInstance();
                if (instance instanceof ListenerAdapter) {
                    jda.addEventListener(instance);
                }
            } catch (Exception e) {
                LOGGER.severe("리스너 인스턴스를 생성할 수 없습니다: " + clazz.getName());
                e.printStackTrace();
            }
        }
    }
}