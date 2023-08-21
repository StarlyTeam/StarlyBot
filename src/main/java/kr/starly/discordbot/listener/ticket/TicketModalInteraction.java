package kr.starly.discordbot.listener.ticket;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.listener.BotEvent;

import kr.starly.discordbot.repository.TicketInfoRepo;
import kr.starly.discordbot.repository.impl.MongoTicketInfoRepo;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.util.MongoUtil;
import kr.starly.discordbot.util.StringData;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.Category;

import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import java.time.LocalDateTime;

@BotEvent
@AllArgsConstructor
public class TicketModalInteraction extends ListenerAdapter {


    private final ConfigManager configManager = ConfigManager.getInstance();


    private String ticketChannelName;


    private String title;

    private String message;


    private final TicketService ticketService;
    private final TicketInfoRepo ticketInfoRepo;


    public TicketModalInteraction() {
        MongoCollection<Document> collection = MongoUtil.DB_COLLECTION_PLUGIN(StringData.DB_CONNECTION_TICKET);
        ticketInfoRepo = new MongoTicketInfoRepo(collection);
        this.ticketService = new TicketService(ticketInfoRepo);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        title = event.getInteraction().getValue("ticket-title").getAsString();
        message = event.getInteraction().getValue("ticket-message").getAsString();

        String categoryId = configManager.getString("PREMIUM_CATEGORY_ID");

        Category ticketCategory = event.getGuild().getCategoryById(categoryId);
        ticketChannelName = ticketCategory.getChannels().size() + "-" + event.getUser().getName();

        ticketCategory.createTextChannel(ticketChannelName).queue();
        event.reply("성공적으로 티켓 생성이 완료 되었습니다!").setEphemeral(true).queue();

        request(event);
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        if (event.getChannel().asTextChannel().getName().equals(ticketChannelName)) {

            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setTitle(title)
                    .setDescription("건의 사항 : " + message);

            Button button = Button.danger("close", "채널 닫기");

            event.getChannel().asTextChannel().sendMessageEmbeds(embedBuilder.build()).addActionRow(button).queue();
        }
    }

    public void request(ModalInteractionEvent event) {
        String title = event.getInteraction().getValue("ticket-title").getAsString();
        String description = event.getInteraction().getValue("ticket-message").getAsString();

        TicketInfo ticketInfo = new TicketInfo(event.getUser().getId(), title, description, LocalDateTime.now().toString());
        ticketService.registerPlugin(ticketInfo);
    }
}
