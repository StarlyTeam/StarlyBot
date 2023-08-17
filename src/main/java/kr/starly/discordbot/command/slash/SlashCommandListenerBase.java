package kr.starly.discordbot.command.slash;


import lombok.Getter;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.util.*;
import java.util.stream.Collectors;

public class SlashCommandListenerBase extends ListenerAdapter {

    private final Map<String, DiscordSlashCommand> commandActions = new HashMap<>();

    @Getter
    private List<CommandData> commands = new ArrayList<>();


    private HashMap<String, OptionType> options = new HashMap<>();


    public SlashCommandListenerBase() {
        registerCommands();
    }

    private void registerCommands() {
        Set<Class<? extends DiscordSlashCommand>> commandClasses = getCommandClasses();
        commandClasses.forEach(commandClass -> {
            BotSlashCommand annotation = commandClass.getAnnotation(BotSlashCommand.class);
            if (annotation != null) {
                try {
                    DiscordSlashCommand commandInstance = commandClass.getDeclaredConstructor().newInstance();
                    List<String> newNames = new ArrayList<>();

                    SlashCommandData commandData = Commands.slash(annotation.command(), annotation.description());

                    for (String name : annotation.names()) {
                        newNames.add(name);
                        int index = newNames.indexOf(name);

                        OptionType optionType = annotation.optionType()[index];
                        String description = annotation.optionDescription()[index];
                        options.put(name, optionType);
                        commandData.addOption(optionType, name, description);
                    }

                    commands.add(commandData);
                    commandActions.put(annotation.command(), commandInstance);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private Set<Class<? extends DiscordSlashCommand>> getCommandClasses() {
        String packageName = "kr.starly.discordbot.command.slash.impl";

        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotSlashCommand.class);

        return annotated.stream()
                .filter(DiscordSlashCommand.class::isAssignableFrom)
                .map(clazz -> (Class<? extends DiscordSlashCommand>) clazz)
                .collect(Collectors.toSet());
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        String command = event.getInteraction().getCommandString();

        command = command.split(" ")[0];
        command = command.replace("/", "");

        if (event.getGuild().getId().equals("1141562591940968481")) {
            DiscordSlashCommand discordSlashCommand = commandActions.get(command);

            if (discordSlashCommand != null) {
                discordSlashCommand.execute(event);
            }
        }
    }
}
