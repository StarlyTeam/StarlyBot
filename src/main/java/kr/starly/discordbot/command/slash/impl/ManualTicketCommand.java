package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Ticket;
import kr.starly.discordbot.enums.TicketType;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.repository.impl.TicketModalDataRepository;
import kr.starly.discordbot.repository.impl.TicketUserDataRepository;
import kr.starly.discordbot.service.TicketService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.EnumSet;
import java.util.Map;

@BotSlashCommand(
        command = "수동티켓",
        description = "수동으로 티켓을 생성합니다.",
        optionName = {"유저"},
        optionType = {OptionType.USER},
        optionDescription = {"티켓을 열 유저를 선택해주세요."},
        optionRequired = {true}
)
public class ManualTicketCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String TICKET_CATEGORY_ID = configProvider.getString("TICKET_CATEGORY_ID");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        Member target = event.getOption("유저").getAsMember();
        if (target == null) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 오류 | 수동티켓 <a:loading:1168266572847128709>")
                    .setDescription("> **해당 유저는 서버에 존재하지 않습니다.**")
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).queue();
            return;
        }

        long userId = target.getIdLong();
        JDA jda = DiscordBotManager.getInstance().getJda();
        TicketService ticketService = DatabaseManager.getTicketService();

        long ticketIndex = ticketService.getLastIndex() + 1;
        TicketType ticketType = TicketType.GENERAL;

        Category category = jda.getCategoryById(TICKET_CATEGORY_ID);
        TextChannel ticketChannel = category.createTextChannel(ticketIndex + "-" + target.getEffectiveName() + "-" + ticketType.getName())
                .addMemberPermissionOverride(userId, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
                .complete();

        ticketService.recordTicket(new Ticket(
                userId,
                0,
                ticketChannel.getIdLong(),
                ticketType,
                ticketIndex
        ));

        net.dv8tion.jda.api.entities.User user = jda.getUserById(userId);

        TicketUserDataRepository ticketUserDataRepository = TicketUserDataRepository.getInstance();
        ticketUserDataRepository.registerUser(userId, user);

        Map<Long, TicketType> userTicketTypeMap = TicketType.getUserTicketTypeMap();
        userTicketTypeMap.put(userId, ticketType);

        TicketModalDataRepository ticketModalDataRepository = TicketModalDataRepository.getInstance();
        ticketModalDataRepository.registerModalData(
                ticketChannel.getIdLong(),
                "수동티켓.",
                "수동생성된 티켓입니다."
        );

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 수동티켓 <a:success:1168266537262657626>")
                .setDescription("> **해당 유저의 티켓 생성이 완료되었습니다.**")
                .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/Starly/white.png")
                .build();
        event.replyEmbeds(embed).queue();
    }
}