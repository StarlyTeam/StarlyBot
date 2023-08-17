package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.util.List;

@BotCommand(
        command = "청소",
        description = "<개수>만큼 메시지를 청소(삭제)합니다.",
        usage = "?청소 <개수>"
)
public class CleanUpCommand extends DiscordCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!checkAdminRole(event)) return;

        event.getMessage().delete().queue();

        String[] args = event.getMessage().getContentRaw().split("\\s", 2);
        if (args.length < 2) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                    .setDescription("**청소할 메시지의 개수를 입력해주세요. 예) ?청소 10**");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return;
        }

        try {
            int count = Integer.parseInt(args[1]);
            if (count < 1 || count > 99) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                        .setDescription("**메시지는 1개에서 99개까지만 삭제할 수 있습니다.**");

                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            } else {
                deleteMessages(event.getChannel(), count);
            }
        } catch (NumberFormatException e) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                    .setDescription("**유효한 숫자를 입력해주세요. 예) ?청소 10**");

            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        }
    }

    private void deleteMessages(MessageChannel channel, int count) {
        channel.getHistoryBefore(channel.getLatestMessageId(), count)
                .queue(history -> {
                    List<Message> messages = history.getRetrievedHistory();
                    messages.forEach(message -> channel.deleteMessageById(message.getId()).queue());
                });
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                .setTitle("<a:success:1141625729386287206> 성공 | 채팅청소 <a:success:1141625729386287206>")
                .setDescription("**" + count + "개의 메시지를 청소했습니다.**");

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
