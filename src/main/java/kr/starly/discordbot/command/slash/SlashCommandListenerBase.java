package kr.starly.discordbot.command.slash;

import kr.starly.discordbot.configuration.ConfigProvider;
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
    private final Map<String, DiscordSlashCommand> commandActions = new HashMap<>();

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String GUILD_ID = configProvider.getString("GUILD_ID");

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

                    SlashCommandData commandData = Commands.slash(annotation.command(), annotation.description());

                    for (BotSlashCommand.SubCommand subCommand : annotation.subcommands()) {
                        SubcommandData subCommandData = new SubcommandData(subCommand.name(), subCommand.description());
                        if (subCommand.optionName().length > 0 &&
                                subCommand.optionName().length == subCommand.optionType().length &&
                                subCommand.optionName().length == subCommand.optionDescription().length) {
                            for (int i = 0; i < subCommand.optionName().length; i++) {
                                subCommandData.addOption(subCommand.optionType()[i], subCommand.optionName()[i], subCommand.optionDescription()[i], subCommand.optionRequired()[i]);
                            }
                        }
                        commandData.addSubcommands(subCommandData);
                    }

                    if (annotation.optionName().length > 0 &&
                            annotation.optionName().length == annotation.optionType().length &&
                            annotation.optionName().length == annotation.optionDescription().length) {
                        for (int i = 0; i < annotation.optionName().length; i++) {
                            commandData.addOption(annotation.optionType()[i], annotation.optionName()[i], annotation.optionDescription()[i], annotation.optionRequired()[i]);
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

    private Set<Class<? extends DiscordSlashCommand>> getCommandClasses() {
        String packageName = "kr.starly.discordbot";

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

        if (event.getGuild().getId().equals(GUILD_ID)) {
            DiscordSlashCommand discordSlashCommand = commandActions.get(command);

            if (discordSlashCommand != null) {
                discordSlashCommand.execute(event);
            }
        }
    }
}