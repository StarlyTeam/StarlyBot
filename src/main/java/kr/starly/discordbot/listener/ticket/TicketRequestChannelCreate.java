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
                        privateChannel.sendMessage("티켓을 생성하는데 오류가 발생 하였습니다.").queue();
                    }
            );
            return;
        }

        TicketType ticketType = TicketType.getUserTicketTypeMap().get(user.getIdLong());
        Ticket ticket = ticketService.findByDiscordId(user.getIdLong());

        Button button = Button.danger("ticket-close" + user.getIdLong(), "닫기");
        MessageEmbed messageEmbed = null;

        switch (ticketType) {
            case GENERAL -> {
                String title = data.get(0);
                String description = data.get(1);
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("제목", "```" + title + "```", true)
                        .addField("설명", "```" + description + "```", false)
                        .build();

                ticketModalFileRepository.save(ticket,
                        "제목: " + title + "\n" +
                                "본문: " + description);
            }

            case OTHER_ERROR -> {
                String title = data.get(0);
                String description = data.get(1);
                String tag = data.get(2);

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("제목", "`" + title + "`", true)
                        .addField("태그", "`" + tag + "`", true)
                        .addField("설명", "```" + description + "```", false)
                        .build();

                ticketModalFileRepository.save(ticket,
                        "제목: " + title + "\n" +
                                "태그: " + tag + "\n" +
                                "본문: " + description);
            }

            case CONSULTING -> {
                String title = data.get(0);
                String description = data.get(1);

                String type = "네".equals(data.get(2)) && data.get(2) != null ? "통화" : "채팅";

                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("제목", "`" + title + "`", true)
                        .addField("통화 여부", "`" + type + "`", true)
                        .addField("설명", "```" + description + "```", false)
                        .build();

                ticketModalFileRepository.save(ticket,
                        "제목: " + title + "\n" +
                                "본문: " + description);

            }
            case PLUGIN_ERROR -> {
                MessageEmbed descriptionEmbed;

                if (data.size() == 3) {
                    String version = data.get(0);
                    String bukkit = data.get(1);
                    String description = data.get(2);

                    descriptionEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("고객센터 알림")
                            .addField("버전", "`" + version + "`", false)
                            .addField("버킷", "`" + bukkit + "`", false)
                            .addField("설명", "```" + description + "```", false)
                            .build();

                    ticketModalFileRepository.save(ticket,
                            "version: " + version + "\n" +
                                    "본문: " + description);

                    textChannel.sendMessageEmbeds(descriptionEmbed).addComponents(ActionRow.of(button)).queue();
                    ticketModalDataRepository.removeModalData(channelId);
                    return;
                }

                String version = data.get(0);
                String bukkit = data.get(1);
                String description = data.get(2);

                String log = data.get(3);
                ticketModalFileRepository.save(ticket,
                        "버전: " + version + "\n" +
                                "본문: " + description + "\n" +
                                "로그 \n" + log);

                File file = ticketModalFileRepository.getFile(ticket);

                descriptionEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("버전", "`" + version + "`", true)
                        .addField("버킷", "`" + bukkit + "`", true)
                        .addField("설명", "```" + description + "```", false)
                        .build();

                FileUpload fileUpload = FileUpload.fromData(file);

                textChannel.sendMessageEmbeds(descriptionEmbed).queue();
                textChannel.sendFiles(fileUpload).queue();
                textChannel.sendMessageComponents(ActionRow.of(button)).queue();

                ticketModalDataRepository.removeModalData(channelId);
                return;
            }


            case ERROR -> {
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("버전", "`" + data.get(0) + "`", false)
                        .addField("버킷", "`" + data.get(1) + "`", false)
                        .addField("설명", "```" + data.get(2) + "```", false)
                        .build();
            }

            case PUNISHMENT, PAYMENT -> {
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("제목", "```" + data.get(0) + "```", true)
                        .addField("사유", "```" + data.get(1) + "```", false)
                        .addField("설명", "```" + data.get(2) + "```", false)
                        .build();

                ticketModalDataRepository.removeModalData(channelId);
            }

            case QUESTION -> {
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("고객센터 알림")
                        .addField("제목", "`" + data.get(0) + "`", true)
                        .addField("사유", "`" + data.get(2) + "`", true)
                        .addField("내용", "```" + data.get(1) + "```", false)
                        .build();
            }
        }

        textChannel.sendMessageEmbeds(messageEmbed).addActionRow(button).queue();

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