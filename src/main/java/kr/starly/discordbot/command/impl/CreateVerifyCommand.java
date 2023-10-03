package kr.starly.discordbot.command.impl;

import kr.starly.discordbot.command.BotCommand;
import kr.starly.discordbot.command.DiscordCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

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
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        event.getMessage().delete().queue();

        MessageEmbed verifyEmbed = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("<a:success:1141625729386287206> 유저인증 | 스탈리 <a:success:1141625729386287206>")
                .setDescription("> **스탈리 커뮤니티에 오신걸 환영합니다.** \n" +
                        "> **서버에 입장하기 전 아래 `인증하기` 버튼을 클릭해주세요.** \n\n" +
                        "─────────────────────────────────────────────────"
                )
                .addField("> <:notice:1141720944935719002> 필독 사항",
                        "**> 이용약관을 필독해주세요.**\n" +
                                "**> 서버규칙을 필독해주세요.**\n" +
                                "**> 불법적인 서버에 활동 중이라면 지금 당장 퇴장해주세요.**\n" +
                                "**> 자신의 디스코드 닉네임 또는 아이디가 부적절한지 확인해주세요. **\u1CBB", true)
                .addField("> <a:loading:1141623256558866482> 인증 오류",
                        "**> 자신의 디스코드 계정에 전화번호가 추가되어있는지 확인해보세요.**\n" +
                                "**> 30일 이내에 생성된 계정인지 확인해보세요.**\n" +
                                "**> 블랙리스트 유저**", true)
                .addField("> <a:warn:1141721287526465656> 주의 사항",
                        "**> 3일 이내에서 서버를 퇴장할 경우 다운로드 금지가 됩니다.**\n" +
                                "**> 부계정 사용을 금지합니다. **\n", true)
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                .setFooter("인증을 완료할 경우 위 내용을 모두 동의한 것으로 간주됩니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                .build();

        Button verify = Button.success("successVerify", "인증하기");
        Button help = Button.danger("helpVerify", "인증이 안되시나요?");
        Button termsOfService = Button.primary("termsOfService", "이용약관");
        Button serverRule = Button.primary("serverRule", "서버규칙");
        event.getChannel().sendMessageEmbeds(verifyEmbed).addActionRow(verify, help, termsOfService, serverRule).queue();
    }
}