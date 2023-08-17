package kr.starly.discordbot.command;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.util.AdminRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

public abstract class DiscordCommand implements DiscordExecutor {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    public abstract void execute(MessageReceivedEvent event);

    protected boolean checkAdminRole(MessageReceivedEvent event) {
        boolean hasAdminRole = AdminRoleChecker.hasAdminRole(event.getMember());

        if (!hasAdminRole) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 권한 없음 <a:loading:1141623256558866482>")
                    .setDescription("**이 명령어를 사용할 권한이 없습니다.**");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        }

        return hasAdminRole;
    }
}
