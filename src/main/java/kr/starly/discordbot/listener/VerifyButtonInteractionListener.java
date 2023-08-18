package kr.starly.discordbot.listener;

import kr.starly.discordbot.configuration.ConfigManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class VerifyButtonInteractionListener extends ListenerAdapter {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();

        switch (buttonId) {

            case "successVerify" -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("<a:success:1141625729386287206> 성공 | 인증 완료 <a:success:1141625729386287206>")
                        .setDescription("****");
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            }

            case "helpVerify" -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<:notice:1141720944935719002> 도움말 | 인증이 안되시나요? <:notice:1141720944935719002>")
                        .setDescription("****");
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            }

            case "termsOfService" -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 이용약관 | 필독사항 <a:loading:1141623256558866482>")
                        .setDescription("****");
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            }

            case "serverRule" -> {
                EmbedBuilder embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("<a:loading:1141623256558866482> 이용약관 | 서버규칙 <a:loading:1141623256558866482>")
                        .setDescription("****");
                event.replyEmbeds(embedBuilder.build()).setEphemeral(true).queue();
            }
        }
    }
}
