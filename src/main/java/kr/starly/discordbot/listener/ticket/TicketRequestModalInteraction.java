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
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

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
                    .addRolePermissionOverride(Long.parseLong(configProvider.getString("VERIFIED_ROLE_ID")), null, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND))
                    .addMemberPermissionOverride(event.getMember().getIdLong(), EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                    .complete();

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 티켓 생성 완료! <a:success:1168266537262657626>")
                    .setDescription("""
                            > **🥳 축하드려요! 티켓이 성공적으로 생성되었습니다!**
                            > **%s 곧 답변 드리겠습니다. 감사합니다! 🙏**
                            """
                            .formatted(textChannel.getAsMention())
                    )
                    .setFooter("빠르게 답변 드리겠습니다! 감사합니다! 🌟", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .build();
            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
        } catch (Exception ex) {
            ex.printStackTrace();

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 고객센터 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **내부 오류가 발생하였습니다. (관리자에게 문의해 주세요.)**
                            
                            ─────────────────────────────────────────────────
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .build();
            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            return;
        }
        Ticket ticket = new Ticket(discordId, 0, textChannel.getIdLong(), ticketType, 0);
        ticketService.recordTicket(ticket);

        ticketUserDataRepository.registerUser(discordId, event.getUser());
        switch (event.getModalId()) {
            case "modal-general" -> {
                String title = event.getValue("title").getAsString();
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }

            case "modal-question" -> {
                String title = event.getValue("title").getAsString();
                String description = event.getValue("description").getAsString();
                String type = event.getValue("type").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, type);
            }

            case "modal-consulting" -> {
                String title = event.getValue("title").getAsString();
                String isCall = event.getValue("call").getAsString();
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, isCall);
            }

            case "modal-payment" -> {
                String title = event.getValue("title").getAsString();
                String description = event.getValue("description").getAsString();
                String type = event.getValue("type").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, type);
            }

            case "modal-punishment" -> {
                String title = event.getValue("title").getAsString();
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }

            case "modal-plugin-error" -> {
                String version = event.getValue("mcVersion").getAsString();
                String log = event.getValue("log").getAsString();
                String bukkitValue = event.getValue("type").getAsString();
                String bukkit = bukkitValue.isEmpty() ? "spigot" : bukkitValue;
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), version, bukkit, log, description);
            }

            case "modal-other-error" -> {
                String title = event.getValue("title").getAsString();
                String tag = event.getValue("tag").getAsString();
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description, tag);
            }

            case "modal-other" -> {
                String title = event.getValue("title").getAsString();
                String description = event.getValue("description").getAsString();

                ticketModalDataRepository.registerModalData(textChannel.getIdLong(), title, description);
            }
        }
    }
}