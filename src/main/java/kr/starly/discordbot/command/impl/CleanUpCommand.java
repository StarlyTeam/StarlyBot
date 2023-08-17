package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@BotCommand(command = "청소")
public class CleanUpCommand implements DiscordCommand {

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getChannel().sendMessage("청소 안할거야 븅신아").queue();
    }
}
