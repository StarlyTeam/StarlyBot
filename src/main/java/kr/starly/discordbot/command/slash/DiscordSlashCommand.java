package kr.starly.discordbot.command.slash;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface DiscordSlashCommand {

    void execute(SlashCommandInteractionEvent event);
}
