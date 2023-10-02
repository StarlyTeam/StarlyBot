package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.TicketService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@BotEvent
public class TicketRequestMenuInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR = configProvider.getString("EMBED_COLOR");

    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("ticket-select-category")) {
            User user = event.getUser();
            long discordId = user.getIdLong();

            String selectedValue = event.getValues().get(0);

            TicketType ticketStatus = TicketType.valueOf(selectedValue.toUpperCase(Locale.ROOT).replace("-", "_"));
            Ticket ticket = ticketService.findByDiscordId(discordId);

            if (ticket != null && isExistUserTicket(event.getJDA(), ticket.channelId())) {
                TextChannel textChannel = event.getJDA().getTextChannelById(ticket.channelId());

                String message = textChannel != null ? "관리자가 수동으로 티켓을 닫았습니다. 관리자에게 문의하여 주세요." : "이미 티켓이 열려 있습니다!" + textChannel.getAsMention();
                event.reply(message).setEphemeral(true).queue();
                return;
            }

            setTicketStatus(discordId, ticketStatus);

            MessageEmbed messageEmbed = generateEmbedForType(ticketStatus);
            List<Button> button = generateButtonsForType(ticketStatus);

            event.replyEmbeds(messageEmbed).addActionRow(button).setEphemeral(true).queue();
            event.editSelectMenu(event.getSelectMenu()).queue();

            return;
        }

        if (event.getComponentId().contains("ticket-rate-select-menu-")) {
            long channelId = Long.valueOf(event.getComponentId().replace("ticket-rate-select-menu-", ""));
            byte value = Byte.valueOf(event.getValues().get(0).replace("ticket-rate-", ""));

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("평점을 해주셔서 감사합니다!")
                    .setDescription("좋은평점을 주셔서 감사합니다!")
                    .build();

            if (value <= 2) {
                messageEmbed = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("평점을 해주셔서 감사합니다!")
                        .setDescription("다음에는 더욱 노력하여 성실히 답변에 임하겠습니다 :)")
                        .build();
            }

            event.replyEmbeds(messageEmbed).setEphemeral(true).queue();
            event.getMessage().delete().queue();

            ticketService.updateRate(channelId, value);
        }
    }

    private void setTicketStatus(long discordId, TicketType ticketStatus) {
        if (!TicketType.getUserTicketStatusMap().containsKey(discordId)) {
            TicketType.getUserTicketStatusMap().put(discordId, ticketStatus);
            return;
        }

        TicketType.getUserTicketStatusMap().replace(discordId, ticketStatus);
    }

    private List<Button> generateButtonsForType(TicketType ticketStatus) {
        List<Button> buttons = new ArrayList<>();

        switch (ticketStatus) {
            case NORMAL_TICKET -> {
                Button normalButton = Button.primary("button-normal-ticket", "일반 문의하기");
                buttons.add(normalButton);
            }
            case QUESTION_TICKET -> {
                Button questionButton = Button.primary("button-question-ticket", "질문하기");
                buttons.add(questionButton);
            }
            case CONSULTING_TICKET -> {
                Button consultingButton = Button.primary("button-consulting-ticket", "상담하기");
                buttons.add(consultingButton);
            }
            case PURCHASE_INQUIRY_TICKET -> {
                Button purchaseInquiryButton = Button.primary("button-purchase-inquiry-ticket", "결제 문의하기");
                buttons.add(purchaseInquiryButton);
            }
            case USE_RESTRICTION_TICKET -> {
                Button restrictionButton = Button.primary("button-use-restriction-ticket", "문의하기");
                buttons.add(restrictionButton);
            }
            case BUG_REPORT_TICKET -> {
                Button bukkitBugButton = Button.primary("button-bug-report-bukkit-ticket", "플러그인 버그");
                Button systemBugButton = Button.primary("button-bug-report-etc-ticket", "기타 버그");

                buttons.add(bukkitBugButton);
                buttons.add(systemBugButton);
            }
            case ETC_TICKET -> {
                Button restrictionButton = Button.primary("button-etc-ticket", "기타 문의하기");
                buttons.add(restrictionButton);
            }
        }
        return buttons;
    }

    private MessageEmbed generateEmbedForType(TicketType ticketStatus) {
        return switch (ticketStatus) {
            case NORMAL_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("자주 묻는 질문")
                    .addField("Q: 어떻게 플러그인을 적용하나요?", "A: .zip파일 을 압축 해제하여 폴더 안에 있는 .jar를 plugins 폴더에 넣으시면 됩니다.", true)
                    .addField("Q: 라이선스 양도가 가능할까요?", "A: 아뇨, 라이선스를 양도하는건 불가능합니다.", true)
                    .build();

            case QUESTION_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case CONSULTING_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case PURCHASE_INQUIRY_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case USE_RESTRICTION_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case BUG_REPORT_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case ETC_TICKET -> new EmbedBuilder()
                    .setColor(Color.decode(EMBED_COLOR))
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            default -> null;
        };
    }

    private boolean isExistUserTicket(JDA jda, long channelId) {
        return jda.getTextChannelById(channelId) != null;
    }
}
