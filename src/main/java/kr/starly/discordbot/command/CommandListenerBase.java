package kr.starly.discordbot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandListenerBase extends ListenerAdapter {

    private final Map<String, DiscordExecutor> commands = new HashMap<>();

    public CommandListenerBase() {
        registerCommands();
    }

    private void registerCommands() {
        Set<Class<? extends DiscordExecutor>> commandClasses = getCommandClasses();
        commandClasses.forEach(commandClass -> {
            BotCommand annotation = commandClass.getAnnotation(BotCommand.class);
            if (annotation != null) {
                try {
                    DiscordExecutor commandInstance = commandClass.getDeclaredConstructor().newInstance();
                    commands.put(annotation.command(), commandInstance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Set<Class<? extends DiscordExecutor>> getCommandClasses() {
        String packageName = "kr.starly.discordbot.command.impl";

        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotCommand.class);

        return annotated.stream()
                .filter(DiscordExecutor.class::isAssignableFrom)
                .map(clazz -> (Class<? extends DiscordExecutor>) clazz)
                .collect(Collectors.toSet());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay();

        if (!message.startsWith("?")) return;

        String[] split = message.substring(1).split(" ", 2);
        String command = split[0].toLowerCase();
        DiscordExecutor commandExecutor = commands.get(command);

        if (commandExecutor != null) {
            commandExecutor.execute(event);
        }
    }
}