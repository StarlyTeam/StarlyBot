package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.listener.BotEvent;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@BotEvent
public class TicketModalInteraction extends ListenerAdapter {

    private final ConfigManager configManager = ConfigManager.getInstance();

    private TicketInfo ticketInfo;

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String categoryId = configManager.getString("PREMIUM_CATEGORY_ID");

        System.out.println("categoryId " + categoryId);

        Category ticketCategory = event.getGuild().getCategoryById("1141704277392375918");

        String channelName = ticketCategory.getChannels().size() + "-" + event.getUser().getName();
        ticketCategory.createTextChannel(channelName).queue();

        event.reply("성공적으로 티켓 생성이 완료 되었습니다!").setEphemeral(true).queue();

        String title = event.getInteraction().getValue("ticket-title").getAsString();
        String message = event.getInteraction().getValue("ticket-message").getAsString();

        ticketInfo = new TicketInfo(channelName, title, message);
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        TextChannel currentChannel = event.getChannel().asTextChannel();
        String channelName = ticketInfo.channelName();

        if (currentChannel.getName().equals(channelName)) {
            String title = ticketInfo.title();
            String message = ticketInfo.description();

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(ticketInfo.title())
                    .setDescription("건의 사항 : " + message);

            Button button = Button.danger("close", "채널 닫기");

            String id = event.getChannel().getId();

            event.getChannel().asTextChannel().sendMessageEmbeds(embedBuilder.build()).addActionRow(button).queue();

            File dir = new File("tickets/" + id + "/");

            if (!dir.exists())
                dir.mkdirs();

            try {
                FileWriter myWriter = new FileWriter("tickets/" + id + "/info.txt");

                myWriter.write("title : " + title + "\n");
                myWriter.write("description : " + message + "\n");
                myWriter.write("date : " + LocalDateTime.now() + "\n");

                myWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
