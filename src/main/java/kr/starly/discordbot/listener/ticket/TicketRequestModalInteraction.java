package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.TicketService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.EnumSet;

@BotEvent
public class TicketRequestModalInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CHANNEL_ID = configProvider.getString("TICKET_CHANNEL_ID");
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();

    private final TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (!textChannel.getId().equals(TICKET_CHANNEL_ID)) return;

        long discordId = event.getUser().getIdLong();
        TicketType ticketType = TicketType.getUserTicketTypeMap().get(discordId);

        try {
            Category category = event.getGuild().getCategoryById(TICKET_CATEGORY_ID);
            textChannel = category.createTextChannel((ticketService.getLastIndex() + 1) + "-" + event.getUser().getEffectiveName() + "-" + ticketType.getName())
                    .addRolePermissionOverride(event.getGuild().getPublicRole().getIdLong(), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                    .addRolePermissionOverride(Long.parseLong(configProvider.getString("AUTHORIZED_ROLE_ID")), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                    .addMemberPermissionOverride(event.getMember().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .complete();

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                    .setDescription("> **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!** \n" +
                            "> **" + textChannel.getAsMention() + " 곧 답변 드리겠습니다. 감사합니다! 🙏**\n\u1CBB")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .build();

            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
        } catch (ErrorResponseException exception) {
            event.reply("모달 처리 과정 중 오류가 발생하였습니다. 잠시만 기다려 주십시오.").setEphemeral(true).queue();
            return;
        }
        Ticket ticket = new Ticket(discordId, 0, textChannel.getIdLong(), ticketType, 0);
        ticketService.recordTicket(ticket);

        ticketUserDataRepository.registerUser(discordId, event.getUser());
        switch (event.getModalId()) {
            case "modal-general-ticket" -> {
                String title = event.getValue("text-input-normal-title").getAsString();
                String description = event.getValue("text-input-normal-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }

            case "modal-question-ticket" -> {
                String title = event.getValue("text-question-title").getAsString();
                String description = event.getValue("text-question-description").getAsString();
                String type = event.getValue("text-question-type").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, type);
            }

            case "modal-consulting-ticket" -> {
                String title = event.getValue("text-consulting-title").getAsString();
                String isCall = event.getValue("text-consulting-call").getAsString();
                String description = event.getValue("text-consulting-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, isCall);
            }

            case "modal-payment-ticket" -> {
                String title = event.getValue("text-payment-title").getAsString();
                String description = event.getValue("text-payment-description").getAsString();
                String type = event.getValue("text-payment-type").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, type);
            }

            case "modal-punishment-ticket" -> {
                String title = event.getValue("text-punishment-title").getAsString();
                String description = event.getValue("text-punishment-title").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }

            case "modal-error-ticket" -> {
                String version = event.getValue("text-error-version").getAsString();
                String log = event.getValue("text-error-log").getAsString();
                String bukkitValue = event.getValue("text-error-type").getAsString();
                String bukkit = bukkitValue == "" ? "spigot" : bukkitValue;
                String description = event.getValue("text-error-description").getAsString();

                if (!log.isEmpty()) {
                    ticketModalDataRepository.registerModalData(textChannel.getIdLong(), version, bukkit, log, description);
                    return;
                }

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), version, bukkit, description);

            }

            case "modal-bug-report-other-ticket" -> {
                String title = event.getValue("text-bug-report-other-title").getAsString();
                String tag = event.getValue("text-bug-report-other-tag").getAsString();
                String description = event.getValue("text-bug-report-other-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, tag);
            }

            case "modal-other-ticket" -> {
                String title = event.getValue("text-other-title").getAsString();
                String description = event.getValue("text-other-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }
        }
    }
}