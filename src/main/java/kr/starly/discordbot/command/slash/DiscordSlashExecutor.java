package kr.starly.discordbot.command.slash;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface DiscordSlashExecutor {

    void execute(SlashCommandInteractionEvent event);
}