package kr.starly.discordbot.command.slash;

import kr.starly.discordbot.configuration.ConfigManager;
import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

public class SlashCommandListenerBase extends ListenerAdapter {

    @Getter
    private List<CommandData> commands = new ArrayList<>();
    private final Map<String, DiscordSlashExecutor> commandActions = new HashMap<>();

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String GUILD_ID = configManager.getString("GUILD_ID");

    public SlashCommandListenerBase() {
        registerCommands();
    }

    private void registerCommands() {
        Set<Class<? extends DiscordSlashExecutor>> commandClasses = getCommandClasses();
        commandClasses.forEach(commandClass -> {
            BotSlashCommand annotation = commandClass.getAnnotation(BotSlashCommand.class);
            if (annotation != null) {
                try {
                    DiscordSlashExecutor commandInstance = commandClass.getDeclaredConstructor().newInstance();

                    SlashCommandData commandData = Commands.slash(annotation.command(), annotation.description());

                    for (BotSlashCommand.SubCommand subCommand : annotation.subcommands()) {
                        SubcommandData subCommandData = new SubcommandData(subCommand.name(), subCommand.description());
                        if (subCommand.names().length > 0 &&
                                subCommand.names().length == subCommand.optionType().length &&
                                subCommand.names().length == subCommand.optionDescription().length) {
                            for (int i = 0; i < subCommand.names().length; i++) {
                                subCommandData.addOption(subCommand.optionType()[i], subCommand.names()[i], subCommand.optionDescription()[i], subCommand.required()[i]);  // required 추가
                            }
                        }
                        commandData.addSubcommands(subCommandData);
                    }

                    if (annotation.names().length > 0 &&
                            annotation.names().length == annotation.optionType().length &&
                            annotation.names().length == annotation.optionDescription().length) {
                        for (int i = 0; i < annotation.names().length; i++) {
                            commandData.addOption(annotation.optionType()[i], annotation.names()[i], annotation.optionDescription()[i]);
                        }
                    }

                    commands.add(commandData);
                    commandActions.put(annotation.command(), commandInstance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Set<Class<? extends DiscordSlashExecutor>> getCommandClasses() {
        String packageName = "kr.starly.discordbot.command.slash.impl";

        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotSlashCommand.class);

        return annotated.stream()
                .filter(DiscordSlashExecutor.class::isAssignableFrom)
                .map(clazz -> (Class<? extends DiscordSlashExecutor>) clazz)
                .collect(Collectors.toSet());
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String command = event.getInteraction().getCommandString();

        command = command.split(" ")[0];
        command = command.replace("/", "");

        if (event.getGuild().getId().equals(GUILD_ID)) {
            DiscordSlashExecutor discordSlashExecutor = commandActions.get(command);

            if (discordSlashExecutor != null) {
                discordSlashExecutor.execute(event);
            }
        }
    }
}