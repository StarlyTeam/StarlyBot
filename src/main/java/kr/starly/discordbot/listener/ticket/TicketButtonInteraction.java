package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.listener.BotEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@BotEvent
public class TicketButtonInteraction extends ListenerAdapter {

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getButton().getId();
        String channelId = event.getChannel().getId();

        switch (buttonId) {
            case "ticket-report", "ticket-buy", "ticket-default" -> event.replyModal(create()).queue();
            case "close" -> {
                TextChannel textChannel = event.getChannel().asTextChannel();

                MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
                List<Message> messages = history.getRetrievedHistory();

                File dir = new File("tickets/" + channelId + "/");
                if (!dir.exists())
                    dir.mkdirs();

                textChannel.delete().queue();

                FileWriter myWriter;

                try {
                    myWriter = new FileWriter("tickets/" + channelId + "/" + channelId + ".txt");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                FileWriter finalMyWriter = myWriter;

                messages.forEach(message -> {
                    String id = message.getAuthor().getId();
                    if (!id.equals("")) {
                        String name = message.getAuthor().getName();
                        String contentDisplay = message.getContentDisplay();
                        String time = message.getTimeCreated().toString();

                        String str = "{" + id + "," + name + "} : " + contentDisplay + ", " + time;

                        try {
                            finalMyWriter.write(str);
                            finalMyWriter.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        }
    }

    private Modal create() {
        TextInput test_name = TextInput.create("ticket-title", "Name", TextInputStyle.SHORT)
                .setRequired(true)
                .build();

        TextInput message = TextInput.create("ticket-message", "Message", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .build();

        return Modal.create("test-modal", "플러그인 문의")
                .addActionRows(ActionRow.of(test_name), ActionRow.of(message))
                .build();
    }
}
