package kr.starly.discordbot.listener.ticket;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.util.messaging.AuditLogger;
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
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final TicketService ticketService = DatabaseManager.getTicketService();

    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("ticket-select-category")) {
            User user = event.getUser();
            long discordId = user.getIdLong();

            String selectedValue = event.getValues().get(0);

            TicketType ticketType = TicketType.valueOf(selectedValue.toUpperCase(Locale.ROOT).replace("-", "_"));
            Ticket ticket = ticketService.findByDiscordId(discordId);

            if (ticket != null && isExistUserTicket(event.getJDA(), ticket.channelId())) {
                TextChannel textChannel = event.getJDA().getTextChannelById(ticket.channelId());

                String message = textChannel != null ? "관리자가 수동으로 티켓을 닫았습니다. 관리자에게 문의하여 주세요." : "이미 티켓이 열려 있습니다!" + textChannel.getAsMention();
                event.reply(message).setEphemeral(true).queue();
            } else {
                setTicketStatus(discordId, ticketType);

                MessageEmbed messageEmbed = generateEmbedForType(ticketType);
                List<Button> button = generateButtonsForType(ticketType);

                event.replyEmbeds(messageEmbed).addActionRow(button).setEphemeral(true).queue();
            }

            event.editSelectMenu(event.getSelectMenu()).queue();
        }

        else if (event.getComponentId().contains("ticket-rate-select-menu-")) {
            long channelId = Long.valueOf(event.getComponentId().replace("ticket-rate-select-menu-", ""));
            byte value = Byte.valueOf(event.getValues().get(0).replace("ticket-rate-", ""));

            MessageEmbed messageEmbed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 고객센터 <a:success:1168266537262657626>")
                    .setDescription("""
                            > **평가를 남겨 주셔서 감사합니다 🥳**
                            > **앞으로도 좋은 서비스를 제공할 수 있도록 노력하겠습니다!**
                            
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                    .build();

            if (value <= 2) {
                messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 고객센터 <a:success:1168266537262657626>")
                        .setDescription("""
                            > **평가를 남겨 주셔서 감사합니다 🥳**
                            > **다음에는 더욱 좋은 서비스를 경험하실 수 있도록 노력하는 스탈리가 되겠습니다.**
                            
                            """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .setFooter("문의하실 내용이 있으시면 언제든지 연락주시기 바랍니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/fd6f9e61-52e6-478d-82fd-d3e9e4e91b00/public")
                        .build();
            }

            event.replyEmbeds(messageEmbed).queue();
            event.editSelectMenu(null).queue();

            ticketService.updateRate(channelId, value);

            AuditLogger.info(
                    new EmbedBuilder()
                            .setTitle("티켓 평가")
                            .setDescription("""
                                    티켓 평가가 완료되었습니다.
                                    
                                    > 티켓: <#%d>
                                    > 평가자: <@%d>
                                    > 평가: %d/5
                                    """.formatted(channelId, event.getUser().getIdLong(), value)
                            )
            );
        }
    }

    private void setTicketStatus(long discordId, TicketType ticketType) {
        if (!TicketType.getUserTicketTypeMap().containsKey(discordId)) {
            TicketType.getUserTicketTypeMap().put(discordId, ticketType);
            return;
        }

        TicketType.getUserTicketTypeMap().replace(discordId, ticketType);
    }

    private List<Button> generateButtonsForType(TicketType ticketType) {
        List<Button> buttons = new ArrayList<>();

        switch (ticketType) {
            case GENERAL -> {
                Button normalButton = Button.primary("button-general", "일반 문의하기");
                buttons.add(normalButton);
            }
            case QUESTION -> {
                Button questionButton = Button.primary("button-question", "질문하기");
                buttons.add(questionButton);
            }
            case CONSULTING -> {
                Button consultingButton = Button.primary("button-consulting", "상담하기");
                buttons.add(consultingButton);
            }
            case PAYMENT -> {
                Button purchaseInquiryButton = Button.primary("button-payment", "결제 문의하기");
                buttons.add(purchaseInquiryButton);
            }
            case PUNISHMENT -> {
                Button restrictionButton = Button.primary("button-punishment", "문의하기");
                buttons.add(restrictionButton);
            }
            case ERROR -> {
                Button bukkitBugButton = Button.primary("button-plugin-error", "플러그인 버그");
                Button systemBugButton = Button.primary("button-other-error", "기타 버그");

                buttons.add(bukkitBugButton);
                buttons.add(systemBugButton);
            }
            case OTHER -> {
                Button restrictionButton = Button.primary("button-other", "기타 문의하기");
                buttons.add(restrictionButton);
            }
        }
        return buttons;
    }

    private MessageEmbed generateEmbedForType(TicketType ticketType) {
        return switch (ticketType) {
            case GENERAL -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("자주 묻는 질문")
                    .addField("Q: 어떻게 플러그인을 적용하나요?", "A: .zip파일 을 압축 해제하여 폴더 안에 있는 .jar를 plugins 폴더에 넣으시면 됩니다.", true)
                    .addField("Q: 라이선스 양도가 가능할까요?", "A: 아뇨, 라이선스를 양도하는건 불가능합니다.", true)
                    .build();

            case QUESTION -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case CONSULTING -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case PAYMENT -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case PUNISHMENT -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case ERROR -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("주의")
                    .setDescription("자주 묻는 질문")
                    .build();

            case OTHER -> new EmbedBuilder()
                    .setColor(EMBED_COLOR)
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
// TODO 디자인