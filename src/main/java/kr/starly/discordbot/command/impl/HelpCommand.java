package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigManager;
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
public class HelpCommand extends DiscordCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!checkAdminPermission(event)) return;

        event.getMessage().delete();

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