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
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<:tos:1168335473152892948> 사업자 정보 안내 | 스탈리 <:ticket:1168335473152892948>")
                .setDescription("> **스탈리 스토어를 방문해 주셔서 감사합니다.** \n" +
                        "> **저희는 투명한 운영을 위해 사업자 등록 정보를 공개합니다.** \n\n" +
                        "─────────────────────────────────────────────────\n" +
                        "> <a:loading:1168266572847128709> **<< 사업자 정보 >>** <a:loading:1168266572847128709> \n" +
                        "> **`❇\uFE0F` | 사업자 등록 번호: 210-36-72319**\n" +
                        "> **`\uD83D\uDCD2` | 사업자 명: 양대영**\n" +
                        "> **`♻\uFE0F` | 등록된 주소: 경상남도 통영시 광도면 신죽xx길 xxxxxx xxxxxx동 xxxxxx호 (주영 더 팰리스 xx차 아파트)**\n" +
                        "> **`\uD83D\uDCE9` | 연락 가능한 이메일: yangdaeyeong0808@gmail.com**\n\n" +
                        "> **위 정보들은 고객님들께서 더 안심하고 서비스를 이용하실 수 있도록 제공됩니다.**\n" +
                        "> **추가 문의 사항이 있으시면 언제든지 연락 주시기 바랍니다.**\n\n" +
                        "> **고객님의 신뢰를 최우선으로 생각하는 스탈리가 되겠습니다.**")
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .setFooter("정식 사업자 등록을 완료한 스탈리에서 안전하고 신뢰할 수 있는 서비스를 경험하세요.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                .build();

        Button tosButton = Button.link("https://www.ftc.go.kr/bizCommPop.do?wrkr_no=2103672319", "사업자등록증 보기").withEmoji(Emoji.fromFormatted("<:tos:1168335473152892948>"));

        event.getChannel().sendMessageEmbeds(messageEmbed).addActionRow(tosButton).queue();
    }
}