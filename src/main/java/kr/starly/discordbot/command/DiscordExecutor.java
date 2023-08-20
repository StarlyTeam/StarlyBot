package kr.starly.discordbot.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface DiscordExecutor {

    void execute(MessageReceivedEvent event);
}