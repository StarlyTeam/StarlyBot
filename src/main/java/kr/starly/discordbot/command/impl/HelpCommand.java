package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.util.AdminRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.reflections.Reflections;

import java.awt.*;
import java.util.Set;

@BotCommand(
        command = "도움말",
        description = "물음표 명령어 사용법을 출력합니다.",
        usage = "?도움말"
)
public class HelpCommand implements DiscordCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void execute(MessageReceivedEvent event) {
        boolean hasAdminRole = AdminRoleChecker.hasAdminRole(event.getMember());
        if (!hasAdminRole) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 권한 없음 <a:loading:1141623256558866482>")
                    .setDescription("**이 명령어를 사용할 권한이 없습니다.**");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return;
        }


        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR))
                .setTitle("<a:loading:1141623256558866482> 도움말 | 물음표 명령어 <a:loading:1141623256558866482>");

        String packageName = "kr.starly.discordbot.command.impl";
        Reflections reflections = new Reflections(packageName);
        Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(BotCommand.class);

        embed.addBlankField(false);

        annotated.forEach(clazz -> {
            BotCommand annotation = clazz.getAnnotation(BotCommand.class);
            String description = annotation.description();
            String usage = annotation.usage();

            embed.addField(usage, description, false);
            embed.addBlankField(false);
        });

        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
}
