package kr.starly.discordbot.listener.verify;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.security.RoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@BotEvent
public class VerifyButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        switch (buttonId) {
            case "successVerify" -> {
                if (RoleChecker.hasVerifiedRole(event.getMember())) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:warn:1168266548541145298> 오류 | 이미 인증된 유저입니다. <a:warn:1168266548541145298>")
                            .setDescription("""
                                    > **당신은 이미 인증된 유저예요! \uD83C\uDF89**
                                    > **추가적으로 인증하지 않아도 됩니다! \uD83E\uDD73**
                                    
                                    """)
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                            .setFooter("이미 인증이 완료된 계정입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                            .build();
                    event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
                    return;
                }

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
            // 안녕하세요, <@255311287704223745\u003e님\n\n<:termsofuse:1168335473152892948>  결제 도와드리겠습니다.\n아래 계좌로 36,900원 입금바랍니다 🙂\n\n> 계좌번호: 3333275249398\n\u003e 은행: 카카오뱅크\n\u003e 예금주명: 양대영\n\u003e \n\u003e 입금 금액: 36,900원

            case "helpVerify" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<:notice:1168265600301277284> 유저인증 | 인증이 안되시나요? <:notice:1168265600301277284>")
                        .setDescription("""
                                > **인증이 안 될 경우 아래 항목을 확인해보세요.**
                                > **문제가 계속되면 관리자에게 DM으로 문의해 주세요.**
                                
                                ─────────────────────────────────────────────────
                                > **`1️⃣` 계정 상태 (인증된 계정, 차단된 계정 등)를 확인하세요.**
                                > **`2️⃣` 인터넷 연결이 원활한지 확인하세요.**
                                > **`3️⃣` 웹 브라우저의 캐시와 쿠키를 지우고 다시 시도해보세요.**
                                > **`4️⃣` 사용 중인 브라우저 혹은 앱이 최신 버전인지 확인하세요.**
                                > **`5️⃣` 방화벽이나 보안 프로그램이 인증을 차단하고 있지 않은지 확인하세요.**
                                > **`6️⃣` 자신의 디스코드 계정에 전화번호가 추가되어있는지 확인하세요.**
                                > **`7️⃣` 30일 이내에 생성된 계정인지 확인하세요.**
                                > **`8️⃣` 디스코드 DM(Direct Message) 수신을 차단하였는지 확인하세요.**
                                > **`9️⃣` 블랙리스트 유저일 경우 인증이 되지 않을 수 있습니다.**
                                """)
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .setFooter("도움이 필요하시면 언제든지 관리자에게 문의하세요.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .build();
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }

            case "termsOfService" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 유저인증 | 이용약관 <a:loading:1168266572847128709>")
                        .setDescription("> **이용약관은 <#1168253041812701398> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .setFooter("이용약관을 준수하지 않을 경우 서비스 이용이 제한될 수 있습니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .build();
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }

            case "serverRule" -> {
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 유저인증 | 서버규칙 <a:loading:1168266572847128709>")
                        .setDescription("> **서버규칙은 <#1038741748941340732> 채널에서 확인하실 수 있으며, 클릭하면 해당 채널로 이동합니다.**\n\u1CBB")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .setFooter("규칙을 위반할 경우 제재가 이루어질 수 있습니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                        .build();
                event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            }
        }
    }
}