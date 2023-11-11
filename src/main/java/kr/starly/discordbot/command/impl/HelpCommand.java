package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.reflections.Reflections;

import java.awt.Color;
import java.util.Set;

@BotCommand(
        command = "도움말",
        description = "물음표 명령어 사용 방법을 출력합니다.",
        usage = "?도움말"
)
public class HelpCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete();

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:loading:1168266572847128709> 도움말 | 물음표 명령어 <a:loading:1168266572847128709>");

        String packageName = "kr.starly.discordbot";
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