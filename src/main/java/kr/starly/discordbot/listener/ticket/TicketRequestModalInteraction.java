package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.TicketModalDataRepository;
import kr.starly.discordbot.repository.TicketUserDataRepository;
import kr.starly.discordbot.service.TicketInfoService;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

@BotEvent
public class TicketRequestModalInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CHANNEL_ID = configProvider.getString("TICKET_CHANNEL_ID");
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();

    private final TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
    private final TicketInfoService ticketInfoService = DatabaseConfig.getTicketInfoService();

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getChannel().asTextChannel().getId().equals(TICKET_CHANNEL_ID)) return;

        TextChannel textChannel;

        long discordId = event.getUser().getIdLong();
        TicketType ticketStatus = TicketType.getUserTicketStatusMap().get(discordId);

        try {
            Category category = event.getGuild().getCategoryById(TICKET_CATEGORY_ID);
            textChannel = category.createTextChannel((ticketInfoService.getLastIndex() + 1) + "-" + event.getUser().getGlobalName() + "-" + ticketStatus.getName())
                    .addMemberPermissionOverride(event.getMember().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL), null)
                    .complete();

            event.reply("성공적으로 티켓을 생성 하였습니다! " + textChannel.getAsMention()).setEphemeral(true).queue();
        } catch (ErrorResponseException exception) {
            event.reply("모달 처리 과정 중 오류가 발생하였습니다. 잠시만 기달려 주십시오.").setEphemeral(true).queue();
            return;
        }
        TicketInfo ticketInfo = new TicketInfo(discordId, 0, textChannel.getIdLong(), ticketStatus, 0);
        ticketInfoService.recordTicketInfo(ticketInfo);

        ticketUserDataRepository.registerUser(discordId, event.getUser());
        switch (event.getModalId()) {
            case "modal-normal-ticket" -> {
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

            case "modal-purchase-inquiry-ticket" -> {
                String title = event.getValue("text-purchase-inquiry-title").getAsString();
                String description = event.getValue("text-purchase-inquiry-description").getAsString();
                String type = event.getValue("text-purchase-inquiry-type").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, type);
            }
            case "modal-use-restriction-ticket" -> {
                String title = event.getValue("text-use-restriction-title").getAsString();
                String description = event.getValue("text-use-restriction-title").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }

            case "modal-bug-report-bukkit-ticket" -> {
                String version = event.getValue("text-bug-report-bukkit-version").getAsString();
                String log = event.getValue("text-bug-report-bukkit-log").getAsString();
                String bukkitValue = event.getValue("text-bug-report-bukkit-type").getAsString();
                String bukkit = bukkitValue == "" ? "spigot" : bukkitValue;
                String description = event.getValue("text-bug-report-bukkit-description").getAsString();

                if (!log.isEmpty()) {
                    ticketModalDataRepository.registerModalData(textChannel.getIdLong(), version, bukkit, log, description);
                    return;
                }

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), version, bukkit, description);

            }
            case "modal-bug-report-etc-ticket" -> {
                String title = event.getValue("text-bug-report-etc-title").getAsString();
                String tag = event.getValue("text-bug-report-etc-tag").getAsString();
                String description = event.getValue("text-bug-report-etc-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, tag);
            }

            case "modal-etc-ticket" -> {
                String title = event.getValue("text-etc-title").getAsString();
                String description = event.getValue("text-etc-description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }
        }
    }
}
