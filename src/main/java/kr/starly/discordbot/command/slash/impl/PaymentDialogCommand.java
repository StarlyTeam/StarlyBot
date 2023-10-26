package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;

@BotSlashCommand(
        command = "결제창",
        description = "결제창을 생성합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "커스텀",
                        description = "커스텀 결제창을 생성합니다.",
                        optionName = {"이름", "결제금액"},
                        optionType = {
                                OptionType.STRING,
                                OptionType.INTEGER
                        },
                        optionDescription = {
                                "주문명을 입력하세요.",
                                "결제 금액을 입력하세요."
                        },
                        optionRequired = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "주문제작",
                        description = "주문제작 결제창을 생성합니다.",
                        optionName = {"이름", "결제금액"},
                        optionType = {
                                OptionType.STRING,
                                OptionType.INTEGER
                        },
                        optionDescription = {
                                "프로젝트명을 입력하세요.",
                                "결제 금액을 입력하세요."
                        },
                        optionRequired = {true, true}
                )
        }
)
public class PaymentDialogCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String productType = event.getSubcommandName();
        String orderName = event.getOption("이름").getAsString();
        int price = event.getOption("결제금액").getAsInt();

        Button button = Button.primary("payment-start-" + (productType.equals("커스텀") ? "CUSTOM!!!!PRICE" : "OUT!!!!SOURCING") + "§" + orderName + "§" + price, "결제하기");
        MessageEmbed embed1 = new EmbedBuilder()
                .setColor(EMBED_COLOR)
                .setTitle("결제창")
                .setDescription("""
                       [ 결제창 (카드 전용) ]
                       > 결제금액: %,d원
                        """.formatted(price))
                .build();
        event.getChannel().sendMessageEmbeds(embed1)
                .addActionRow(button)
                .queue();

        MessageEmbed embed2 = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("결제창")
                .setDescription("결제창을 생성했습니다.")
                .build();
        event.replyEmbeds(embed2)
                .setEphemeral(true)
                .queue();
    }
}