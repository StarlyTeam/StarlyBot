package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.impl.TicketFileRepository;
import kr.starly.discordbot.repository.impl.TicketModalFileRepository;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@BotEvent
public class TicketManagerButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final String WARN_CHANNEL_ID = configProvider.getString("WARN_CHANNEL_ID");
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final TicketService ticketService = DatabaseManager.getTicketService();
    private final WarnService warnService = DatabaseManager.getWarnService();

    private final TicketFileRepository ticketFileRepository = TicketFileRepository.getInstance();
    private final TicketModalFileRepository ticketModalFileRepository = TicketModalFileRepository.getInstance();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel textChannel)) return;
        if (!textChannel.getParentCategory().getId().equals(TICKET_CATEGORY_ID)) return;

        Member member = event.getMember();

        if (event.getComponentId().startsWith("ticket-close")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                event.reply("관리자만 사용이 가능합니다.").setEphemeral(true).queue();
                return;
            }

            String userId = event.getComponentId().replace("ticket-close", "");
            User user = textChannel.getJDA().getUserById(userId);

            Button closeButtonCheck = Button.primary("ticket-check-twice-close-" + user.getId(), "확인");
            Button closeButtonJoke = Button.danger("ticket-check-joke-" + user.getId(), "장난 티켓");

            event.getInteraction().editComponents(ActionRow.of(closeButtonCheck, closeButtonJoke)).queue();
            return;
        }

        if (event.getComponentId().contains("ticket-check-twice-close")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                event.reply("관리자만 사용이 가능합니다.").setEphemeral(true).queue();
                return;
            }

            long ticketUserId = Long.valueOf(event.getComponentId().replace("ticket-check-twice-close-", ""));
            User ticketUser = textChannel.getJDA().getUserById(ticketUserId);

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("고객센터 도우미")
                    .setDescription("상담이 도움 되셨나요? 아래 별점을 통해 평가해 주세요!")
                    .build();

            StringSelectMenu rateSelectMenu = StringSelectMenu
                    .create("ticket-rate-select-menu-" + textChannel.getId())
                    .setPlaceholder("평가")
                    .addOption("매우 만족", "ticket-rate-5")
                    .addOption("만족", "ticket-rate-4")
                    .addOption("보통", "ticket-rate-3")
                    .addOption("불만족", "ticket-rate-2")
                    .addOption("매우 불만족", "ticket-rate-1")
                    .build();

            try {
                ticketUser.openPrivateChannel().queue(
                        privateChannel -> privateChannel.sendMessageEmbeds(messageEmbed).addComponents(ActionRow.of(rateSelectMenu)).queue()
                );
            } catch (UnsupportedOperationException ignored) {}

            Ticket ticketInfo = ticketService.findByChannel(textChannel.getIdLong());

            MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
            ticketFileRepository.save(history.getRetrievedHistory(), ticketInfo);

            ticketService.recordTicket(
                    new Ticket(ticketUserId, event.getUser().getIdLong(), textChannel.getIdLong(), ticketInfo.ticketType(), ticketInfo.index())
            );

            event.reply("2초 후 채널이 삭제됩니다.").setEphemeral(true).queue();

            event.getChannel().delete().queueAfter(2, TimeUnit.SECONDS);
            return;
        }

        if (event.getComponentId().contains("ticket-check-joke-")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                event.reply("관리자만 사용이 가능합니다.").setEphemeral(true).queue();
                return;
            }

            long ticketUserId = Long.valueOf(event.getComponentId().replace("ticket-check-joke-", ""));
            User ticketUser = textChannel.getJDA().getUserById(ticketUserId);

            Ticket ticketInfo = ticketService.findByChannel(textChannel.getIdLong());

            ticketService.recordTicket(
                    new Ticket(ticketUserId, 0, textChannel.getIdLong(), ticketInfo.ticketType(), ticketInfo.index())
            );

            ticketUser.openPrivateChannel().queue(privateChannel -> {
                Warn warnInfo = new Warn(ticketUser.getIdLong(), event.getUser().getIdLong(), "장난 티켓", 1, new Date());
                warnService.saveData(warnInfo);
                
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:success:1141625729386287206> 추가 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + ticketUser.getAsMention() + " 님에게 " + warnInfo.amount() + "경고를 추가 하였습니다.** \n" +
                                "> 사유 : " + warnInfo.reason())
                        .setThumbnail(ticketUser.getAvatarUrl())
                        .build();
                
                event.getJDA().getTextChannelById(WARN_CHANNEL_ID).sendMessageEmbeds(messageEmbed).queue();

                try {
                    privateChannel.sendMessageEmbeds(messageEmbed).queue();
                } catch (UnsupportedOperationException ignored) {}
            });

            ticketModalFileRepository.delete(ticketService.findByDiscordId(ticketUser.getIdLong()));

            event.getChannel().delete().queue();
        }
    }
}