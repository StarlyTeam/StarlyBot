package kr.starly.discordbot.command.slash;

import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

public abstract class DiscordSlashCommand implements DiscordSlashExecutor {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    public abstract void execute(SlashCommandInteractionEvent event);

    protected boolean checkAdminRole(SlashCommandInteractionEvent event) {

        if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 권한 없음 <a:loading:1141623256558866482>")
                    .setDescription("**이 명령어를 사용할 권한이 없습니다.**");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return false;
        }
        return true;
    }
}
