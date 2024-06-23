package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.Color;

@BotCommand(
        command = "인증생성",
        description = "인증 임베드를 생성합니다.",
        usage = "?인증생성"
)
public class CreateVerifyCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getMessage());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed verifyEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:success:1168266537262657626> 유저인증 | 스탈리 <a:success:1168266537262657626>")
                .setDescription("""
                        > **스탈리에 오신 것을 환영합니다.**
                        > **서버에 입장하기 전 아래 `인증하기` 버튼을 클릭해주세요.**

                        ─────────────────────────────────────────────────
                        """
                )
                .addField("> <:notice:1168265600301277284> 필독 사항",
                        """
                                **> 이용약관을 필독해주세요.**
                                **> 서버규칙을 필독해주세요.**
                                **> 불법적인 서버에서 활동 중이라면 지금 당장 퇴장해주세요.**
                                **> 디스코드 닉네임 또는 아이디를 적절하게 변경해주세요. **
                                **> 부계정을 이용하지 말아주세요.**""", true)
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                .setFooter("인증시 위 내용에 동의한 것으로 간주됩니다.", "https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                .build();

        Button verify = Button.success("successVerify", "인증하기");
        Button help = Button.danger("helpVerify", "인증이 안되시나요?");
        Button termsOfService = Button.link("https://discord.com/channels/1038714452352192553/1168253041812701398", "이용약관");
        Button serverRule = Button.link("https://discord.com/channels/1038714452352192553/1038741748941340732", "서버규칙");
        event.getChannel().sendMessageEmbeds(verifyEmbed).addActionRow(verify, help, termsOfService, serverRule).queue();
    }
}