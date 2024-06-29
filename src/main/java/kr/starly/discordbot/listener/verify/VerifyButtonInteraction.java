package kr.starly.discordbot.listener.verify;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.UserService;
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
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        switch (buttonId) {
            case "successVerify" -> {
                TextInput verifyMessage = TextInput.create("verify-tos", "이용약관 및 개인정보처리방침에 동의하시면, '네'라고 입력 해주세요.", TextInputStyle.SHORT)
                        .setPlaceholder("네")
                        .setMinLength(1)
                        .setMaxLength(2)
                        .setRequired(true)
                        .build();

                Modal ticketModal = Modal.create("verify-modal", "약관 확인 및 동의")
                        .addActionRow(verifyMessage)
                        .build();

                event.replyModal(ticketModal).queue();
            }

            case "helpVerify" -> {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<:notice:1168265600301277284> 유저인증 | 인증이 안되시나요? <:notice:1168265600301277284>")
                        .setDescription("""
                                > **인증이 안 될 경우 아래 항목을 확인해보세요.**
                                > **문제가 계속되면 관리자에게 DM으로 문의해주세요.**
                                
                                ─────────────────────────────────────────────────
                                > **`1️⃣` 처벌내역이 존재하지 않은지 확인하세요.**
                                > **`2️⃣` 인터넷 연결이 원활한지 확인하세요.**
                                > **`3️⃣` 웹 브라우저의 캐시와 쿠키를 삭제하세요.**
                                > **`4️⃣` 사용 중인 브라우저 혹은 앱이 최신 버전인지 확인하세요.**
                                > **`5️⃣` 방화벽이나 보안 프로그램이 인증을 차단하고 있지 않은지 확인하세요.**
                                > **`6️⃣` 자신의 디스코드 계정에 전화번호가 인증 되어있는지 확인하세요.**
                                > **`7️⃣` 30일 이내에 생성된 계정은 아닌지 확인하세요.**
                                > **`8️⃣` 디스코드 DM 수신을 차단하지는 않았는지 확인하세요.**
                                """)
                        .setThumbnail("https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                        .setFooter("도움이 필요하시면 언제든지 관리자에게 문의하세요.", "https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
            }
        }
    }
}