package kr.starly.discordbot.listener.verify;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.listener.BotEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@BotEvent
public class VerifyModalInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getInteraction().getValue("verify-tos") == null) return;
        String response = event.getInteraction().getValue("verify-tos").getAsString();

        if ("네".equals(response)) {
            long discordId = event.getMember().getIdLong();
            String token = authService.generateToken(discordId);
            String authLink = WEB_ADDRESS + "auth/" + discordId + "/" + token;

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:success:1168266537262657626> 유저인증 | 인증 단계를 완료해 주세요! <a:success:1168266537262657626>")
                    .setDescription("""
                            > **커뮤니티를 막힘 없이 이용하려면, 인증을 완료해 주세요! 😊**
                            > **[여기를 클릭](%s)하여 인증을 완료해 주세요! 😊**
                            > **🕒 주의: 30분 후에 링크가 만료됩니다. 빨리 인증해 주세요!**
                            """.formatted(authLink)
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                    .setFooter("참고: DM을 허용해야 인증 성공 메시지를 받을 수 있습니다.", "https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                    .build();
            event.replyEmbeds(embed).setEphemeral(true).queue();
        } else {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:amount:1168266548541145298> 오류 | 유저인증 <a:amount:1168266548541145298>")
                    .setDescription("""
                            > **약관에 동의하지 않으셨습니다.**
                            > **서비스 이용을 원하시면 약관에 동의해 주세요.**
                            """)
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                    .setFooter("문제가 있으시면 관리자에게 연락해 주세요.", "https://file.starly.kr/images/Logo/StarlyOne/StarlyOne_YELLOW.png")
                    .build();
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }
    }
}