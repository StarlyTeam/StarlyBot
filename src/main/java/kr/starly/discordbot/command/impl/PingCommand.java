package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

@BotCommand(command = "ping")
public class PingCommand implements DiscordCommand {

    @Override
    public void execute(MessageReceivedEvent event) {
        event.getChannel().sendMessage("pong!").queue();
    }
}
