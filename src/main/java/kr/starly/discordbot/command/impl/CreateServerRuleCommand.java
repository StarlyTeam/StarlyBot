package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;

@BotCommand(
        command = "서버규칙생성",
        description = "서버규칙 임베드를 생성합니다.",
        usage = "?서버규칙생성"
)
public class CreateServerRuleCommand implements DiscordCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));

    @Override
    public void execute(MessageReceivedEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<:tos:1168335473152892948> 서버규칙 | 스탈리 <:tos:1168335473152892948>")
                .setDescription("""
                        > **아래는 커뮤니티의 질서를 위한 몇 가지 중요한 규칙입니다**
                        
                        ─────────────────────────────────────────────────
                        > <a:warn:1168266548541145298> **<< 디스코드 서버 규칙 >> <a:warn:1168266548541145298>**
                        > **1. 모든 회원은 서로에 대한 예의를 지켜야 합니다.**
                        > **2. 스팸, 뒷메 행위나 불필요한 메시지는 자제해 주시기 바랍니다.**
                        > **3. 정확하고 신뢰할 수 있는 정보만을 공유해 주세요.**
                        > **4. 친목과 타커뮤니티의 언급은 자제해 주세요.**
                        > **5. 질문포럼에 올린 포스트는 삭제하지마세요.**
                        > **6. 각 채널이 가진 고유한 목적을 존중하여 활동해 주세요.**
                        
                        ─────────────────────────────────────────────────
                        > <a:warn:1168266548541145298> **<<  규칙 위반 시 >>** <a:warn:1168266548541145298>
                        > **1. 초기 위반 시에는 경고로 조치하며, 반복적인 규칙 위반은 커뮤니티에서의 참여 제한으로 이어질 수 있습니다.**
                        > **2. 규칙을 존중하지 않고 커뮤니티를 떠나기로 결정하신 경우, 회원님의 계정은 차단 조치됩니다.**
                        > **3. 커뮤니티를 해치는 의도적인 도배, 비방 또는 기타 테러 행위에 대해서는 즉시 추방 및 차단으로 대응합니다.**
                        > **상기 규칙은 기본적인 것들로, 추가적인 상황은 운영진의 재량으로 처리될 수 있습니다. 규칙에 대해 의견이 있으시면 언제든지 <#1039505496496930866>를 통해 문의해주세요.**
                        """
                )
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .setFooter("서버 규칙을 준수하지 않을 경우, 규정에 따른 조치가 취해질 수 있음을 알려드립니다", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .build();

        event.getChannel().sendMessageEmbeds(messageEmbed).queue();
    }
}