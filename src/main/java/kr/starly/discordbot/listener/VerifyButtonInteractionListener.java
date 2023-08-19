package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.util.VerifyRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class VerifyButtonInteractionListener extends ListenerAdapter {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");
    private final String AUTH_LINK = configManager.getString("AUTH_LINK");
    private final int AUTH_PORT = configManager.getInt("AUTH_PORT");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();

        switch (buttonId) {
            case "successVerify" -> {
                if (VerifyRoleChecker.hasVerifyRole(event.getMember())) {
                    MessageEmbed embedBuilder = new EmbedBuilder()
                            .setColor(Color.decode(EMBED_COLOR_ERROR))
                            .setTitle("<a:warn:1141721287526465656> 이미 인증된 유저입니다. | 오류 <a:warn:1141721287526465656>")
                            .setDescription("> **당신은 이미 인증된 유저이에요! \uD83C\uDF89**\n"
                                    + "> **추가적으로 인증하지 않아도 됩니다! \uD83E\uDD73**\n\u1CBB")
                            .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .setFooter("이미 인증이 완료된 계정입니다.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .build();
                    event.replyEmbeds(embedBuilder).setEphemeral(true).queue();
                    return;
                }

                String discordId = event.getMember().getId();
                String token = authService.generateToken(discordId);
                String authLink = AUTH_LINK + ":" + AUTH_PORT + "/auth/" + discordId + "/" + token;

                MessageEmbed embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:success:1141625729386287206> 인증 단계를 완료해주세요! | 인증하기 <a:success:1141625729386287206>")
                        .setDescription("> **커뮤니티를 막힘 없이 이용하려면, 인증을 완료해주세요! 😊**\n"
                                + "> **[여기를 클릭](" + authLink + ")하여 인증을 완료해 주세요.**\n"
                                + "> **🕒 주의: 30분 후에 링크가 만료됩니다. 빨리 인증해주세요!**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                        .setFooter("참고: DM을 허용해야 인증 성공 메시지를 받을 수 있습니다.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                        .build();
                event.replyEmbeds(embedBuilder).setEphemeral(true).queue();
            }

            case "helpVerify" -> {
                MessageEmbed embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<:notice:1141720944935719002> 인증이 안되시나요? | 도움말 <:notice:1141720944935719002>")
                        .setDescription("> **인증이 안 될 경우 아래 항목을 확인해보세요.\n" +
                                "> **문제가 계속되면 관리자에게 DM으로 문의해주세요.\n" +
                                "\n" +
                                "───────────────────────────────────────────────── \n" +
                                "> **`1️⃣` 계정 상태 (인증된 계정, 차단된 계정 등)를 확인하세요.**\n" +
                                "> **`2️⃣` 인터넷 연결이 원활한지 확인하세요.**\n" +
                                "> **`3️⃣` 웹 브라우저의 캐시와 쿠키를 지우고 다시 시도해보세요.**\n" +
                                "> **`4️⃣` 사용 중인 브라우저 혹은 앱이 최신 버전인지 확인하세요.**\n" +
                                "> **`5️⃣` 방화벽이나 보안 프로그램이 인증을 차단하고 있지 않은지 확인하세요.**\n" +
                                "> **`6️⃣` 자신의 디스코드 계정에 전화번호가 추가되어있는지 확인하세요.**\n" +
                                "> **`7️⃣` 30일 이내에 생성된 계정인지 확인하세요.**\n" +
                                "> **`8️⃣` 디스코드 DM(Direct Message) 수신을 차단하였는지 확인하세요.**\n" +
                                "> **`9️⃣` 블랙리스트 유저일 경우 인증이 되지 않을 수 있습니다.**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                        .setFooter("도움이 필요하시면 언제든지 관리자에게 문의하세요.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                        .build();
                event.replyEmbeds(embedBuilder).setEphemeral(true).queue();
            }

            case "termsOfService" -> {
                MessageEmbed embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 이용약관 | 필독사항 <a:loading:1141623256558866482>")
                        .setDescription("> **이용약관은 <#1141984482619035698> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .setFooter("이용약관을 준수하지 않을 경우 서비스 이용이 제한될 수 있습니다.", "https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .build();
                event.replyEmbeds(embedBuilder).setEphemeral(true).queue();
            }

            case "serverRule" -> {
                MessageEmbed embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 서버규칙 | 필독사항 <a:loading:1141623256558866482>")
                        .setDescription("> **서버규칙은 <#1141982220219846686> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .setFooter("규칙을 위반할 경우 제재가 이루어질 수 있습니다.", "https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .build();
                event.replyEmbeds(embedBuilder).setEphemeral(true).queue();
            }
        }
    }
}
