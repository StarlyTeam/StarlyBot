package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.TicketInfo;
import kr.starly.discordbot.entity.TicketModalInfo;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.util.MemberRoleChecker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

@BotEvent
public class TicketModalInteraction extends ListenerAdapter {

    private final ConfigManager configManager = ConfigManager.getInstance();

    private TicketInfo ticketInfo;
    private TicketType ticketType;
    private TicketModalInfo ticketModalInfo;

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        String categoryId = configManager.getString("PREMIUM_CATEGORY_ID");

        Category ticketCategory = event.getGuild().getCategoryById(categoryId);

        String channelName = ticketCategory.getChannels().size() + "-" + event.getUser().getName();
        ticketCategory.createTextChannel(channelName).queue();

        event.reply("성공적으로 티켓 생성이 완료 되었습니다!").setEphemeral(true).queue();

        String title = event.getInteraction().getValue("ticket-title").getAsString();
        String message = event.getInteraction().getValue("ticket-message").getAsString();

        this.ticketModalInfo = new TicketModalInfo(event.getUser().getId(), title, message);
    }

    @Override
    public void onChannelCreate(@NotNull ChannelCreateEvent event) {
        TextChannel currentChannel = event.getChannel().asTextChannel();

        String id = currentChannel.getId();

        Guild guild = event.getGuild();
        event.getChannel().asTextChannel().getMembers();

        Button button = Button.danger("close", "닫기");

        MessageEmbed messageEmbed = new EmbedBuilder()
                .setTitle("플러그인 요청 완료!")
                .setDescription("관리자의 대답을 기다려 주세요.")
                .build();
        currentChannel.sendMessageEmbeds(messageEmbed).addActionRow(button).queue();

        for (Member member : currentChannel.getMembers()) {
            Role role = guild.getRolesByName("인증됨", true).get(0);

            if (MemberRoleChecker.hasRole(member, role)) {
                this.ticketInfo = new TicketInfo(member.getId(), id, ticketType, LocalDateTime.now().toString(), "");

                DatabaseConfig.getTicketService().save(ticketInfo);
                break;
            }
        }
    }

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        String componentId = event.getComponentId();


        if (componentId.equalsIgnoreCase("ticket-selectMenu")) {
            String value = event.getSelectedOptions().get(0).getValue();

            switch (value) {
                case "ticket-purchase" -> {
                    create(TicketType.PURCHASE_TICKET, event);


                }
                case "report-bug" -> {
                    create(TicketType.REPORT_TICKET, event);

                }

                case "ticket-default" -> {
                    create(TicketType.DEFAULT_TICKET, event);
                }
            }
        }
    }

    private void create(TicketType type, StringSelectInteractionEvent event) {
        TextInput test_name = TextInput.create("ticket-title", "Name", TextInputStyle.SHORT)
                .setRequired(true)
                .build();

        TextInput message = TextInput.create("ticket-message", "Message", TextInputStyle.PARAGRAPH)
                .setRequired(true)
                .build();

        Modal modal = null;

        switch (type) {
            case REPORT_TICKET -> {
                modal = Modal.create("test-modal", "버그 문의")
                        .addActionRows(ActionRow.of(test_name), ActionRow.of(message))
                        .build();

                this.ticketType = TicketType.REPORT_TICKET;
            }
            case DEFAULT_TICKET -> {
                modal = Modal.create("test-modal", "일반 문의")
                        .addActionRows(ActionRow.of(test_name), ActionRow.of(message))
                        .build();
                this.ticketType = TicketType.DEFAULT_TICKET;

            }
            case PURCHASE_TICKET -> {
                modal = Modal.create("test-modal", "구매 문의")
                        .addActionRows(ActionRow.of(test_name), ActionRow.of(message))
                        .build();
                this.ticketType = TicketType.PURCHASE_TICKET;
            }
        }

        event.replyModal(modal).queue();
    }
}