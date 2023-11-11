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

import java.awt.Color;
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
                    .setTitle("<a:loading:1168266572847128709> 오류 | 잘못된 입력 <a:loading:1168266572847128709>")
                    .setDescription("> **청소할 메시지의 개수를 입력해 주세요. 예) ?청소 10**")
                    .build();

            event.getChannel().sendMessageEmbeds(messageEmbed).queue();
            return;
        }

        try {
            int count = Integer.parseInt(args[1]);
            if (count < 1 || count > 99) {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 잘못된 입력 <a:loading:1168266572847128709>")
                        .setDescription("**메시지는 1개에서 99개까지만 삭제할 수 있습니다.**")
                        .build();

                event.getChannel().sendMessageEmbeds(messageEmbed).queue();
            } else {
                deleteMessages(event.getChannel(), count);
            }
        } catch (NumberFormatException e) {
            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 잘못된 입력 <a:loading:1168266572847128709>")
                    .setDescription("> **유효한 숫자를 입력해 주세요. 예) ?청소 10**")
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
                .setTitle("<a:success:1168266537262657626> 성공 | 채팅청소 <a:success:1168266537262657626>")
                .setDescription("""
                        > **%d개의 메시지를 청소하였습니다.**
                        > **이 메시지는 5초 후에 자동으로 삭제됩니다.
                        """
                )
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .build();
        channel.sendMessageEmbeds(messageEmbed).queue(message -> message.delete().queueAfter(5, TimeUnit.SECONDS));
    }
}