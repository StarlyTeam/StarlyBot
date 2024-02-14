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
        command = "사업자등록생성",
        description = "사업자등록 임베드를 생성합니다.",
        usage = "?사업자등록생성"
)
public class CreateBRCommand implements DiscordCommand {

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
                .setTitle("<:tos:1168335473152892948> 사업자 정보 안내 | 스탈리 <:ticket:1168335473152892948>")
                .setDescription("""
                        > **스탈리 스토어를 방문해 주셔서 감사합니다.**
                        > **저희는 투명한 운영을 위해 사업자 등록 정보를 공개합니다.**
                                                
                        ─────────────────────────────────────────────────
                        > <a:loading:1168266572847128709> **<< 사업자 정보 >>** <a:loading:1168266572847128709>
                        > **`❇️` | 등록 번호: 362-43-01146**
                        > **`📒` | 대표자명: 호예준**
                        > **`♻️` | 등록 주소지: 경기도 남양주시 다산동 4002-1**
                        > **`📩` | 이메일: hyjcompany30@naver.com**
                        
                        > **추가 문의 사항이 있으시면 언제든지 연락 주시기 바랍니다.**
                        > **고객님의 신뢰를 최우선으로 생각하는 스탈리가 되겠습니다.**
                        """
                )
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("정식 사업자 등록을 완료한 스탈리에서 안전하고 신뢰할 수 있는 서비스를 경험하세요.", "https://file.starly.kr/images/Logo/Starly/white.png")
                .build();

        Button tosButton = Button.link("https://www.ftc.go.kr/bizCommPop.do?wrkr_no=0000000000", "사업자등록 확인").withEmoji(Emoji.fromFormatted("<:tos:1168335473152892948>"));

        event.getChannel().sendMessageEmbeds(embed).addActionRow(tosButton).queue();
    }
}