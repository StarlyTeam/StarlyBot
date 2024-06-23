package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.TicketFileRepository;
import kr.starly.discordbot.repository.impl.TicketModalFileRepository;
import kr.starly.discordbot.service.BlacklistService;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
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
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final String WARN_CHANNEL_ID = configProvider.getString("WARN_CHANNEL_ID");
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final TicketService ticketService = DatabaseManager.getTicketService();
    private final WarnService warnService = DatabaseManager.getWarnService();

    private final TicketFileRepository ticketFileRepository = TicketFileRepository.getInstance();
    private final TicketModalFileRepository ticketModalFileRepository = TicketModalFileRepository.getInstance();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!(event.getChannel() instanceof TextChannel ticketChannel)) return;
        if (ticketChannel.getParentCategory() != null
            && !ticketChannel.getParentCategory().getId().equals(TICKET_CATEGORY_ID)) return;

        Member member = event.getMember();

        if (event.getComponentId().startsWith("ticket-close")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            String userId = event.getComponentId().replace("ticket-close", "");

            Button closeButtonCheck = Button.primary("ticket-check-twice-close-" + userId, "확인");
            Button closeButtonJoke = Button.danger("ticket-check-joke-" + userId, "장난 티켓");

            event.getInteraction().editComponents(ActionRow.of(closeButtonCheck, closeButtonJoke)).queue();
            return;
        }

        if (event.getComponentId().contains("ticket-check-twice-close")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            long ticketUserId = Long.valueOf(event.getComponentId().replace("ticket-check-twice-close-", ""));

            MessageEmbed embed1 = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle(" <a:loading:1168266572847128709> 평가하기 | 고객센터 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **상담이 도움 되셨나요? 아래 별점을 통해 평가해주세요.**
                            
                            ─────────────────────────────────────────────────
                            """
                    )
                    .setThumbnail("https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                    .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://file.starly.kr/images/Logo/StarlyTicket/StarlyTicket_YELLOW.png")
                    .build();

            StringSelectMenu rateSelectMenu = StringSelectMenu
                    .create("ticket-rate-select-menu-" + ticketChannel.getId())
                    .setPlaceholder("상담 평가하기")
                    .addOption("매우 만족", "ticket-rate-5", "", Emoji.fromUnicode("\uD83D\uDE0D"))
                    .addOption("만족", "ticket-rate-4", "", Emoji.fromUnicode("\uD83E\uDD70"))
                    .addOption("보통", "ticket-rate-3", "", Emoji.fromUnicode("\uD83D\uDE42"))
                    .addOption("불만족", "ticket-rate-2", "", Emoji.fromUnicode("\uD83E\uDD72"))
                    .addOption("매우 불만족", "ticket-rate-1", "", Emoji.fromUnicode("\uD83D\uDE21"))
                    .build();

            User ticketUser = ticketChannel.getJDA().getUserById(ticketUserId);
            if (ticketUser != null) {
                ticketUser
                        .openPrivateChannel().complete()
                        .sendMessageEmbeds(embed1)
                        .addComponents(ActionRow.of(rateSelectMenu))
                        .queue(null, (ignored) -> {});
            }

            Ticket ticketInfo = ticketService.findByChannel(ticketChannel.getIdLong());

            MessageHistory history = MessageHistory.getHistoryFromBeginning(ticketChannel).complete();
            ticketFileRepository.save(history.getRetrievedHistory(), ticketInfo);

            ticketService.recordTicket(
                    new Ticket(ticketUserId, event.getUser().getIdLong(), ticketChannel.getIdLong(), ticketInfo.ticketType(), ticketInfo.index())
            );

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 티켓 <a:success:1168266537262657626>")
                    .setDescription("""
                                > **5초 후 채널이 삭제됩니다.**
                                
                                """
                    )
                    .build();
            event.replyEmbeds(embed).setEphemeral(true).complete();

            event.getChannel().delete().queueAfter(5, TimeUnit.SECONDS);
            return;
        }

        if (event.getComponentId().contains("ticket-check-joke-")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event);
                return;
            }

            long ticketUserId = Long.valueOf(event.getComponentId().replace("ticket-check-joke-", ""));
            Ticket ticketInfo = ticketService.findByChannel(ticketChannel.getIdLong());
            ticketService.recordTicket(
                    new Ticket(ticketUserId, 0, ticketChannel.getIdLong(), ticketInfo.ticketType(), ticketInfo.index())
            );

            Warn warnInfo = new Warn(ticketUserId, event.getUser().getIdLong(), "장난 티켓", 1, new Date());
            warnService.saveData(warnInfo);

            if (warnService.getTotalWarn(ticketUserId) >= 3) {
                Guild guild = DiscordBotManager.getInstance().getGuild();
                guild.kick(UserSnowflake.fromId(ticketUserId))
                        .reason("경고 3회 이상 누적")
                        .queueAfter(5, TimeUnit.SECONDS);

                BlacklistService blacklistService = DatabaseManager.getBlacklistService();
                blacklistService.saveData(ticketUserId, null, 0, "경고 3회 이상 누적");
            }

            User ticketUser = ticketChannel.getJDA().getUserById(ticketUserId);
            if (ticketUser != null) {
                ticketUser
                        .openPrivateChannel()
                        .queue(privateChannel -> {
                            MessageEmbed embed1 = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_SUCCESS)
                                    .setTitle("<a:warn:1168266548541145298> 경고 알림 <a:warn:1168266548541145298>")
                                    .setDescription("""
                                        > **%s님에게 경고 %d회가 추가되었습니다.**
                                        > **사유: %s**
                                        """
                                            .formatted(ticketUser.getAsMention(), warnInfo.amount(), warnInfo.reason())
                                    )
                                    .setThumbnail(ticketUser.getAvatarUrl())
                                    .build();

                            if (warnService.getTotalWarn(ticketUser.getIdLong()) >= 3) {
                                MessageEmbed embed2 = new EmbedBuilder()
                                        .setColor(EMBED_COLOR)
                                        .setTitle("<a:success:1168266537262657626> 차단 완료 | 경고 <a:success:1168266537262657626>")
                                        .setDescription("> **" + ticketUser.getAsMention() + " 님을 성공적으로 차단처리 하였습니다.**\n" +
                                                "> 사유 : 경고 3회 이상 누적")
                                        .setThumbnail(ticketUser.getAvatarUrl())
                                        .build();

                                event.replyEmbeds(embed1, embed2).queue();
                                event.getJDA().getTextChannelById(WARN_CHANNEL_ID).sendMessageEmbeds(embed1, embed2).queue();
                                privateChannel.sendMessageEmbeds(embed1, embed2).queue();
                            } else {
                                event.replyEmbeds(embed1).queue();
                                event.getJDA().getTextChannelById(WARN_CHANNEL_ID).sendMessageEmbeds(embed1).queue();
                                privateChannel.sendMessageEmbeds(embed1).queue();
                            }
                        });
            }

            ticketModalFileRepository.delete(ticketService.findByDiscordId(ticketUserId));
            event.getChannel().delete().queue();
        }
    }
}