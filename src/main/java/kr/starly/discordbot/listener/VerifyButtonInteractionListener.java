package kr.starly.discordbot.listener;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@BotEvent
public class VerifyButtonInteractionListener extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();

        System.out.println(buttonId);
        switch (buttonId) {

            case "successVerify" -> {
                event.reply("완료").queue();
            }

            case "helpVerify" -> {
                event.reply("응 안돼").queue();
            }
        }
    }
}
