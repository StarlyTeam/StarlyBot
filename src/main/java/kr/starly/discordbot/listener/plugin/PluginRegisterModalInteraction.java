package kr.starly.discordbot.listener.plugin;

import kr.starly.discordbot.listener.BotEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

@BotEvent
public class PluginRegisterModalInteraction extends ListenerAdapter {

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        
    }
}
