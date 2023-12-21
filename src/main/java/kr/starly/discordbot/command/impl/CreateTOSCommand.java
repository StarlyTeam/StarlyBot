package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@BotCommand(
        command = "이용약관생성",
        description = "이용약관 임베드를 생성합니다.",
        usage = "?이용약관생성"
)
public class CreateTOSCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getMessage());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<:tos:1168335473152892948> 약관 | 스탈리 <:ticket:1168335473152892948>")
                .setDescription("""
                        > **스탈리 스토어에 오신 것을 환영합니다.**
                        > **아래의 버튼을 클릭하여 약관의 세부 내용을 확인하실 수 있습니다.**
                        
                        ─────────────────────────────────────────────────
                        """
                )
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .setFooter("이용약관을 준수하지 않을 경우 처벌 대상이 될 수 있습니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .build();

        Button tosButton = Button.link("https://file.starly.kr/terms/", "바로가기").withEmoji(Emoji.fromFormatted("<:download:1168339542588268624>"));

        event.getChannel().sendMessageEmbeds(embed).addActionRow(tosButton).queue();
    }
}