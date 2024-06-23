package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketModalFileRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.TicketService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.io.File;
import java.util.EnumSet;
import java.util.List;

@BotEvent
public class TicketRequestChannelCreate extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final String ADMIN_ROLE = configProvider.getString("ADMIN_ROLE");

    private final TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();
    private final TicketModalFileRepository ticketModalFileRepository = TicketModalFileRepository.getInstance();
    private final TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
    private final TicketService ticketService = DatabaseManager.getTicketService();

    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (textChannel.getParentCategory() != null
                && !textChannel.getParentCategoryId().equals(TICKET_CATEGORY_ID)) return;

        Long channelId = event.getChannel().getIdLong();
        User user = getUserFromTextChannel(textChannel.getMembers());

        List<String> data = ticketModalDataRepository.retrieveModalData(channelId);
        if (data.isEmpty()) {
            user.openPrivateChannel().queue(
                    privateChannel -> {
                        MessageEmbed embed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_ERROR)
                                .setTitle("<a:loading:1168266572847128709> 오류 | 고객센터 <a:loading:1168266572847128709>")
                                .setDescription("""
                                        > **티켓을 생성하는 도중에 오류가 발생하였습니다.**
                                        
                                        ─────────────────────────────────────────────────
                                        > **티켓: %s**
                                        """
                                )
                                .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                                .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                                .build();
                        privateChannel.sendMessageEmbeds(embed).queue();
                    }
            );
            return;
        }

        TicketType ticketType = TicketType.getUserTicketTypeMap().get(user.getIdLong());
        Ticket ticket = ticketService.findByDiscordId(user.getIdLong());

        Button button = Button.danger("ticket-close" + user.getIdLong(), "닫기");
        MessageEmbed embed = null;

        switch (ticketType) {
            case GENERAL, PUNISHMENT -> {
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 알림 | 고객센터 <a:loading:1168266572847128709>")
                        .addField("제목", "```" + data.get(0) + "```", true)
                        .addField("설명", "```" + data.get(1) + "```", false)
                        .build();
            }

            case PAYMENT -> {
                embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 알림 | 고객센터 <a:loading:1168266572847128709>")
                        .addField("종류", "```" + data.get(0) + "```", true)
                        .addField("설명", "```" + data.get(1) + "```", false)
                        .build();
            }
        }

        textChannel.sendMessageEmbeds(embed).addActionRow(button).queue();
        textChannel.sendMessage(event.getJDA().getRoleById(ADMIN_ROLE).getAsMention())
                .setAllowedMentions(EnumSet.of(Message.MentionType.ROLE))
                .mentionRoles(ADMIN_ROLE)
                .queue(message -> message.delete().queue());

        ticketModalDataRepository.removeModalData(channelId);
        ticketUserDataRepository.deleteUser(user.getIdLong());
    }

    private User getUserFromTextChannel(List<Member> members) {
        for (Member member : members) {
            User user = member.getUser();
            if (ticketUserDataRepository.contains(user.getIdLong())) {
                return ticketUserDataRepository.retrieveUser(user.getIdLong());
            }
        }
        return null;
    }
}