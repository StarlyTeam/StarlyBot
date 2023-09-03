package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.entity.TicketModalInfo;
import kr.starly.discordbot.entity.TicketType;
import kr.starly.discordbot.listener.BotEvent;

import kr.starly.discordbot.repository.impl.TicketInfoFileRepository;
import kr.starly.discordbot.service.TicketModalService;
import kr.starly.discordbot.util.AdminRoleChecker;
import kr.starly.discordbot.util.MemberRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.ChannelUnion;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@BotEvent
public class TicketModalInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();

    private final String AUTH_ROLE = configProvider.getString("AUTH_ROLE");

    private final TicketModalService ticketModalService = TicketModalService.getInstance();

    private final TicketInfoFileRepository ticketInfoFileRepository = TicketInfoFileRepository.getInstance();

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String categoryId = configProvider.getString("PREMIUM_CATEGORY_ID");

        Category ticketCategory = event.getGuild().getCategoryById(categoryId);

        String channelName = ticketCategory.getChannels().size() + "-" + event.getUser().getName();
        ticketCategory.createTextChannel(channelName).queue();

        event.reply("성공적으로 티켓 생성이 완료 되었습니다!").setEphemeral(true).queue();

        String title = event.getInteraction().getValue("ticket-title").getAsString();
        String message = event.getInteraction().getValue("ticket-message").getAsString();

        String userId = event.getUser().getId();

        TicketModalInfo origin = ticketModalService.getTicketModalInfo(userId);

        TicketModalInfo ticketModalInfo = new TicketModalInfo(userId, title, message);
        ticketModalInfo.setType(origin.getType());

        ticketModalService.replaceModalInfo(userId, ticketModalInfo);
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        TextChannel currentChannel = event.getChannel().asTextChannel();

        Guild guild = event.getGuild();
        event.getChannel().asTextChannel().getMembers();

        Button button = Button.danger("close", "닫기");

        Role role = guild.getRoleById(AUTH_ROLE);
        Member user = getMembersByRole(event.getChannel(), role).get(0);

        String currentChannelId = currentChannel.getId();

        TicketModalInfo ticketModalInfo = ticketModalService.getTicketModalInfo(user.getId());

        DatabaseConfig.getTicketService().save(new TicketInfo(user.getId(), currentChannelId, ticketModalInfo.getType(), LocalDateTime.now().toString(), ""));

        MessageEmbed embed = new EmbedBuilder()
                .setTitle("티켓 알림")
                .setDescription("<@" + user.getId() + ">님이 티켓을 열었습니다!" + "\n"
                        + "티켓 " + ticketModalInfo.getType().name() + "\n"
                        + "사유 : " + ticketModalInfo.getTitle() + "\n"
                        + "설명 : " + ticketModalInfo.getMessage())
                .setTimestamp(OffsetDateTime.now())
                .build();

        currentChannel.sendMessageEmbeds(embed).addActionRow(button).queue();

        getAdminMembers(event.getChannel()).forEach(admin -> {
            currentChannel.sendMessage("<@" + admin.getId() + ">").queue((message -> {
                message.delete().queue();
            }));
        });

        ticketInfoFileRepository.setTicketInfo(ticketModalInfo);
        ticketInfoFileRepository.save(ticketModalInfo, currentChannelId);

        ticketModalService.removeById(user.getId());
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();

        if (componentId.equalsIgnoreCase("ticket-selectMenu")) {
            String value = event.getSelectedOptions().get(0).getValue();

            switch (value) {
                case "ticket-purchase" -> createModal(TicketType.PURCHASE, event);
                case "report-bug" -> createModal(TicketType.REPORT, event);
                case "ticket-default" -> createModal(TicketType.NORMAL, event);
            }
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (event.getButton().getId().equals("close")) {
            if (!AdminRoleChecker.hasAdminRole(event.getMember())) {
                event.reply("관리자만 티켓을 닫을 수 있습니다!").setEphemeral(true).queue();
                return;
            }

            Role role = event.getGuild().getRoleById(AUTH_ROLE);

            DatabaseConfig.getTicketService().update(event.getChannel().getId());

            AtomicReference<User> openBy = new AtomicReference<>();

            getMembersByRole(event.getChannel(), role).forEach(member -> {
                openBy.set(member.getUser());
            });

            MessageHistory history = MessageHistory.getHistoryFromBeginning(event.getChannel()).complete();
            List<Message> message = history.getRetrievedHistory();

            String id = event.getChannel().getId();

            TicketInfo ticketInfo = DatabaseConfig.getTicketInfoRepository().findTicketInfoById(id);
            ticketInfoFileRepository.save(ticketInfo, id, message);

            event.getChannel().delete().queue();
        }
    }

    private void createModal(TicketType type, StringSelectInteractionEvent event) {
        TextInput ticketTitle = TextInput.create("ticket-title", "Name", TextInputStyle.SHORT)
                .setRequired(true)
                .build();

        TextInput ticketMessage = TextInput.create("ticket-message", "Message", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .build();

        Modal modal = null;

        String userId = event.getUser().getId();

        switch (type) {
            case REPORT -> {
                modal = Modal.create("ticket-modal", "버그 문의")
                        .addActionRows(ActionRow.of(ticketTitle), ActionRow.of(ticketMessage))
                        .build();


                ticketModalService.setTicketType(userId, TicketType.REPORT);
            }
            case NORMAL -> {
                modal = Modal.create("ticket-modal", "일반 문의")
                        .addActionRows(ActionRow.of(ticketTitle), ActionRow.of(ticketMessage))
                        .build();
                ticketModalService.setTicketType(userId, TicketType.NORMAL);
            }
            case PURCHASE -> {
                modal = Modal.create("ticket-modal", "구매 문의")
                        .addActionRows(ActionRow.of(ticketTitle), ActionRow.of(ticketMessage))
                        .build();
                ticketModalService.setTicketType(userId, TicketType.PURCHASE);


            }
        }
        event.replyModal(modal).queue();
    }

    private List<Member> getAdminMembers(ChannelUnion channel) {
        List<Member> members = new ArrayList<>();

        for (Member member : channel.asTextChannel().getMembers()) {
            if (AdminRoleChecker.hasAdminRole(member)) {
                members.add(member);
            }
        }
        return members;
    }

    private List<Member> getMembersByRole(MessageChannelUnion channel, Role role) {
        List<Member> members = new ArrayList<>();
        for (Member member : channel.asTextChannel().getMembers()) {
            if (MemberRoleChecker.hasRole(member, role)) {
                members.add(member);
            }
        }
        return members;
    }


    private List<Member> getMembersByRole(ChannelUnion channel, Role role) {
        List<Member> members = new ArrayList<>();
        for (Member member : channel.asTextChannel().getMembers()) {
            if (MemberRoleChecker.hasRole(member, role)) {
                members.add(member);
            }
        }
        return members;
    }
}