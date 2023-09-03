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
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");
    private final String EMBED_COLOR_ERROR = configProvider.getString("EMBED_COLOR_ERROR");
    private final String AUTH_LINK = configProvider.getString("AUTH_LINK");
    private final int AUTH_PORT = configProvider.getInt("AUTH_PORT");

    private final AuthService authService = AuthService.getInstance();

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String response = event.getInteraction().getValue("verify-tos").getAsString();

        if ("네".equals(response)) {
            String discordId = event.getMember().getId();
            String token = authService.generateToken(discordId);
            String authLink = AUTH_LINK + ":" + AUTH_PORT + "/auth/" + discordId + "/" + token;

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("<a:success:1141625729386287206> 유저인증 | 인증 단계를 완료해주세요! <a:success:1141625729386287206>")
                    .setDescription("> **커뮤니티를 막힘 없이 이용하려면, 인증을 완료해주세요! 😊**\n"
                            + "> **[여기를 클릭](" + authLink + ")하여 인증을 완료해 주세요.**\n"
                            + "> **🕒 주의: 30분 후에 링크가 만료됩니다. 빨리 인증해주세요!**\n\u1CBB")
                    .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                    .setFooter("참고: DM을 허용해야 인증 성공 메시지를 받을 수 있습니다.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                    .build();
            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();

        } else {
            MessageEmbed errorMessage = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR_ERROR))
                    .setTitle("<a:warn:1141721287526465656> 오류 | 인증 실패! <a:warn:1141721287526465656>")
                    .setDescription("> **약관에 동의하지 않으셨습니다.**\n"
                            + "> **서비스 이용을 원하시면 약관에 동의해주세요.**\n\u1CBB")
                    .setThumbnail("https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                    .setFooter("문제가 있으시면 관리자에게 연락해주세요.", "https://media.discordapp.net/attachments/1059420652722999386/1141710970859835423/KakaoTalk_20230725_014437871_01.png?width=569&height=569")
                    .build();
            event.replyEmbeds(errorMessage).setEphemeral(true).queue();
        }
    }
}