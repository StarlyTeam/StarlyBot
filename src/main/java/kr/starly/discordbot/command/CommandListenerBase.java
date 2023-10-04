package kr.starly.discordbot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandListenerBase extends ListenerAdapter {

    private final Map<String, DiscordCommand> commands = new HashMap<>();

    public CommandListenerBase() {
        registerCommands();
    }

    private void registerCommands() {
        Set<Class<? extends DiscordCommand>> commandClasses = getCommandClasses();
        commandClasses.forEach(commandClass -> {
            BotCommand annotation = commandClass.getAnnotation(BotCommand.class);
            if (annotation != null) {
                try {
                    DiscordCommand commandInstance = commandClass.getDeclaredConstructor().newInstance();
                    commands.put(annotation.command(), commandInstance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Set<Class<? extends DiscordCommand>> getCommandClasses() {
        String packageName = "kr.starly.discordbot.command.impl";

        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotCommand.class);

        return annotated.stream()
                .filter(DiscordCommand.class::isAssignableFrom)
                .map(clazz -> (Class<? extends DiscordCommand>) clazz)
                .collect(Collectors.toSet());
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentDisplay();
        if (!message.startsWith("?")) return;

        String[] args = message.substring(1).split(" ", 2);
        String command = args[0].toLowerCase();
        DiscordCommand commandExecutor = commands.get(command);

        if (commandExecutor != null) {
            commandExecutor.execute(event);
        }
    }
}