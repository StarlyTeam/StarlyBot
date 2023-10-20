package kr.starly.discordbot.listener.verify;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.http.service.AuthService;
import kr.starly.discordbot.listener.BotEvent;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

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

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:success:1141625729386287206> 유저인증 | 인증 단계를 완료해 주세요! <a:success:1141625729386287206>")
                    .setDescription("> **커뮤니티를 막힘 없이 이용하려면, 인증을 완료해 주세요! 😊**\n"
                            + "> **[여기를 클릭](" + authLink + ")하여 인증을 완료해 주세요.**\n"
                            + "> **🕒 주의: 30분 후에 링크가 만료됩니다. 빨리 인증해 주세요!**\n\u1CBB")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .setFooter("참고: DM을 허용해야 인증 성공 메시지를 받을 수 있습니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .build();
            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();

        } else {
            MessageEmbed errorMessage = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:amount:1141721287526465656> 오류 | 인증 실패! <a:amount:1141721287526465656>")
                    .setDescription("> **약관에 동의하지 않으셨습니다.**\n"
                            + "> **서비스 이용을 원하시면 약관에 동의해 주세요.**\n\u1CBB")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .setFooter("문제가 있으시면 관리자에게 연락해 주세요.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/474a5e10-44fd-4a6d-da08-9053a1149600/public")
                    .build();
            event.replyEmbeds(errorMessage).setEphemeral(true).queue();
        }
    }
}