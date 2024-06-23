package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.TicketService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

@BotEvent
public class TicketRequestButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CHANNEL_ID = configProvider.getString("TICKET_CHANNEL_ID");
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (!textChannel.getId().equals(TICKET_CHANNEL_ID)) return;

        long userId = event.getUser().getIdLong();
        String buttonId = event.getComponentId();

        Ticket ticket = ticketService.findByDiscordId(userId);
        if (ticket != null) {
            TextChannel ticketChannel = event.getJDA().getTextChannelById(ticket.channelId());
            if (ticketChannel == null) {
                ticketService.recordTicket(
                        new Ticket(ticket.openBy(), event.getUser().getIdLong(), ticket.channelId(), ticket.ticketType(), ticket.index())
                );
            }
        }

        TicketType ticketTypeFormat = TicketType.valueOf(buttonId.replace("button-", "").replace("-", "_").toUpperCase());
        addUserTicketStatus(userId, ticketTypeFormat);

        TicketType ticketType = TicketType.getUserTicketTypeMap().get(userId);
        Modal modal = generateModalForType(ticketType);
        event.getInteraction().replyModal(modal).queue();
    }

    private void addUserTicketStatus(long userId, TicketType ticketType) {
        if (!TicketType.getUserTicketTypeMap().containsKey(userId)) {
            TicketType.getUserTicketTypeMap().put(userId, ticketType);
            return;
        }

        TicketType.getUserTicketTypeMap().replace(userId, ticketType);
    }

    private Modal generateModalForType(TicketType ticketType) {
        Modal modal = null;

        switch (ticketType) {
            case GENERAL -> {
                TextInput normalTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput normalDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("내용을 입력해주시기 바랍니다.")
                        .setMaxLength(500)
                        .build();

                modal = Modal.create("modal-general", "일반 문의")
                        .addComponents(
                                ActionRow.of(normalTitle),
                                ActionRow.of(normalDescription)
                        ).build();
            }

            case PAYMENT -> {
                TextInput purchaseInquiryType = TextInput.create("type", "타입", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("결제 관련 상담 사유를 입력해주시기 바랍니다.\n(예: 결제, 환불, 청구)")
                        .setMaxLength(50)
                        .build();

                TextInput purchaseInquiryDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("내용을 입력해주시기 바랍니다.")
                        .setMaxLength(500)
                        .build();

                modal = Modal.create("modal-payment", "결제 문의")
                        .addComponents(
                                ActionRow.of(purchaseInquiryType),
                                ActionRow.of(purchaseInquiryDescription)
                        )
                        .build();
            }

            case PUNISHMENT -> {
                TextInput useRestrictionTitle = TextInput.create("title", "제목", TextInputStyle.SHORT)
                        .setPlaceholder("제목을 입력해주시기 바랍니다.")
                        .setMaxLength(50)
                        .build();

                TextInput useRestrictionDescription = TextInput.create("description", "본문", TextInputStyle.PARAGRAPH)
                        .setPlaceholder("내용을 입력해주시기 바랍니다.")
                        .setMaxLength(500)
                        .build();

                modal = Modal.create("modal-punishment", "처벌 문의")
                        .addComponents(
                                ActionRow.of(useRestrictionTitle),
                                ActionRow.of(useRestrictionDescription)
                        )
                        .build();
            }
        }
        return modal;
    }
}