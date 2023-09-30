package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.entity.WarnInfo;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.TicketFileRepository;
import kr.starly.discordbot.repository.TicketModalFileRepository;
import kr.starly.discordbot.service.TicketInfoService;
import kr.starly.discordbot.service.WarnService;
import kr.starly.discordbot.util.PermissionUtil;
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

import java.awt.Color;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@BotEvent
public class TicketManagerButtonInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configProvider.getString("EMBED_COLOR_ERROR");

    private final String WARN_CHANNEL_ID = configProvider.getString("WARN_CHANNEL_ID");
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");

    private final TicketInfoService ticketInfoService = DatabaseConfig.getTicketInfoService();
    private final WarnService warnService = DatabaseConfig.getWarnService();

    private final TicketFileRepository ticketFileRepository = TicketFileRepository.getInstance();

    private final TicketModalFileRepository ticketModalFileRepository = TicketModalFileRepository.getInstance();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        TextChannel textChannel = event.getChannel().asTextChannel();
        if (!textChannel.getParentCategory().getId().equals(TICKET_CATEGORY_ID)) return;

        Member member = event.getMember();

        if (event.getComponentId().contains("ticket-close")) {
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
                    .setColor(Color.decode(EMBED_COLOR_SUCCESS))
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

            ticketUser.openPrivateChannel().queue(
                    privateChannel -> {
                        privateChannel.sendMessageEmbeds(messageEmbed).addComponents(ActionRow.of(rateSelectMenu)).queue();
                    }
            );

            TicketInfo ticketInfo = ticketInfoService.findByChannel(textChannel.getIdLong());

            MessageHistory history = MessageHistory.getHistoryFromBeginning(textChannel).complete();
            ticketFileRepository.save(history.getRetrievedHistory(), ticketInfo);

            ticketInfoService.recordTicketInfo(
                    new TicketInfo(ticketUserId, event.getUser().getIdLong(), textChannel.getIdLong(), ticketInfo.ticketStatus(), ticketInfo.index())
            );

            event.getChannel().delete().queueAfter(3, TimeUnit.SECONDS);
            return;
        }

        if (event.getComponentId().contains("ticket-check-joke-")) {
            if (!PermissionUtil.hasPermission(member, Permission.ADMINISTRATOR)) {
                event.reply("관리자만 사용이 가능합니다.").setEphemeral(true).queue();
                return;
            }

            long ticketUserId = Long.valueOf(event.getComponentId().replace("ticket-check-joke-", ""));
            User ticketUser = textChannel.getJDA().getUserById(ticketUserId);

            TicketInfo ticketInfo = ticketInfoService.findByChannel(textChannel.getIdLong());

            ticketInfoService.recordTicketInfo(
                    new TicketInfo(ticketUserId, 0, textChannel.getIdLong(), ticketInfo.ticketStatus(), ticketInfo.index())
            );

            ticketUser.openPrivateChannel().queue(privateChannel -> {
                WarnInfo warnInfo = new WarnInfo(ticketUser.getIdLong(), event.getUser().getIdLong(), "장난 티켓", 1, new Date());
                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("<a:success:1141625729386287206> 추가 완료 | 경고 <a:success:1141625729386287206>")
                        .setDescription("> **" + ticketUser.getAsMention() + " 님에게 " + warnInfo.warn() + "경고를 추가 하였습니다.** \n" +
                                "> 사유 : " + warnInfo.reason())
                        .setThumbnail(ticketUser.getAvatarUrl())
                        .build();


                warnService.addWarn(warnInfo);
                event.getJDA().getTextChannelById(WARN_CHANNEL_ID).sendMessageEmbeds(messageEmbed).queue();

                privateChannel.sendMessageEmbeds(messageEmbed).queue();
            });

            ticketModalFileRepository.delete(ticketInfoService.findByDiscordId(ticketUser.getIdLong()));

            event.getChannel().delete().queue();
        }
    }
}