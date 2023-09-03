package kr.starly.discordbot.listener.verify;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.VerifyRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class VerifyButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");
    private final String EMBED_COLOR_ERROR = configProvider.getString("EMBED_COLOR_ERROR");

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();

        switch (buttonId) {
            case "successVerify" -> {
                if (VerifyRoleChecker.hasVerifyRole(event.getMember())) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(Color.decode(EMBED_COLOR_ERROR))
                            .setTitle("<a:warn:1141721287526465656> 오류 | 이미 인증된 유저입니다. <a:warn:1141721287526465656>")
                            .setDescription("> **당신은 이미 인증된 유저이에요! \uD83C\uDF89**\n"
                                    + "> **추가적으로 인증하지 않아도 됩니다! \uD83E\uDD73**\n\u1CBB")
                            .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .setFooter("이미 인증이 완료된 계정입니다.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                            .build();
                    event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
                    return;
                }

                TextInput verifyMessage = TextInput.create("verify-tos","이용약관에 동의하시면, '네'라고 입력 후 전송을 눌러주세요.", TextInputStyle.SHORT)
                        .setPlaceholder("네")
                        .setMinLength(1)
                        .setMaxLength(1)
                        .setRequired(true)
                        .build();

                Modal ticketModal = Modal.create("verify-modal", "약관 확인 및 동의")
                        .addActionRow(verifyMessage)
                        .build();

                event.replyModal(ticketModal).queue();
            }

            case "helpVerify" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<:notice:1141720944935719002> 유저인증 | 인증이 안되시나요? <:notice:1141720944935719002>")
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
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }

            case "termsOfService" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 유저인증 | 이용약관 <a:loading:1141623256558866482>")
                        .setDescription("> **이용약관은 <#1141984482619035698> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .setFooter("이용약관을 준수하지 않을 경우 서비스 이용이 제한될 수 있습니다.", "https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .build();
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }

            case "serverRule" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 유저인증 | 서버규칙 <a:loading:1141623256558866482>")
                        .setDescription("> **서버규칙은 <#1141982220219846686> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .setFooter("규칙을 위반할 경우 제재가 이루어질 수 있습니다.", "https://media.discordapp.net/attachments/1141992315406270484/1142168135768744077/KakaoTalk_20230726_065722121_01.png?width=671&height=671")
                        .build();
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }
        }
    }
}