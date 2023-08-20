package kr.starly.discordbot.command;

import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public abstract class DiscordCommand implements DiscordExecutor {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    public abstract void execute(MessageReceivedEvent event);

    protected boolean checkAdminPermission(MessageReceivedEvent event) {
        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 권한 없음 <a:loading:1141623256558866482>")
                    .setDescription("**이 명령어를 사용할 권한이 없습니다.**")
                    .build();

            event.getMessage().replyEmbeds(messageEmbed).queue();
            return false;
        }
        return true;
    }
}