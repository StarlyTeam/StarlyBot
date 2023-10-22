package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.concurrent.TimeUnit;

@BotCommand(
        command = "청소",
        description = "<개수>만큼 메시지를 청소(삭제)합니다.",
        usage = "?청소 <개수>"
)
public class CleanUpCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String[] args = event.getMessage().getContentRaw().split("\\s", 2);
        if (args.length < 2) {
            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                    .setDescription("**청소할 메시지의 개수를 입력해 주세요. 예) ?청소 10**")
                    .build();

            event.getChannel().sendMessageEmbeds(messageEmbed).queue();
            return;
        }

        try {
            int count = Integer.parseInt(args[1]);
            if (count < 1 || count > 99) {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                        .setDescription("**메시지는 1개에서 99개까지만 삭제할 수 있습니다.**")
                        .build();

                event.getChannel().sendMessageEmbeds(messageEmbed).queue();
            } else {
                deleteMessages(event.getChannel(), count);
            }
        } catch (NumberFormatException e) {
            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                    .setDescription("**유효한 숫자를 입력해 주세요. 예) ?청소 10**")
                    .build();

            event.getChannel().sendMessageEmbeds(messageEmbed).queue();
        }
    }

    private void deleteMessages(MessageChannel channel, int count) {
        try {
            channel.getIterableHistory()
                    .takeAsync(count + 1)
                    .thenAccept(channel::purgeMessages);
        } catch (Exception ignored) {}

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1141625729386287206> 성공 | 채팅청소 <a:success:1141625729386287206>")
                .setDescription("**" + count + "개의 메시지를 청소했습니다.**")
                .setFooter("이 메시지는 5초후에 자동으로 삭제됩니다.")
                .build();
        channel.sendMessageEmbeds(messageEmbed).queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
    }
}