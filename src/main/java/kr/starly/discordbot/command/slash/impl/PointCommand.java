package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigManager;
import kr.starly.discordbot.configuration.DatabaseConfig;
import kr.starly.discordbot.entity.UserInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.List;

@BotSlashCommand(
        command = "포인트",
        description = "유저의 포인트를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "지급",
                        description = "포인트를 지급합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "지급할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "제거",
                        description = "포인트를 제거합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "제거할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "설정",
                        description = "포인트를 설정합니다.",
                        names = {"유저", "포인트"},
                        optionType = {OptionType.USER, OptionType.INTEGER},
                        optionDescription = {"유저를 선택하세요.", "설정할 포인트를 입력하세요."},
                        required = {true, true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "확인",
                        description = "포인트를 확인합니다.",
                        names = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"유저를 선택하세요."},
                        required = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "초기화",
                        description = "포인트를 초기화합니다.",
                        names = {"유저"},
                        optionType = {OptionType.USER},
                        optionDescription = {"유저를 선택하세요."},
                        required = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "순위",
                        description = "포인트 순위를 확인합니다."
                )
        }
)
public class PointCommand extends DiscordSlashCommand {

    private final ConfigManager configManager = ConfigManager.getInstance();
    private final String EMBED_COLOR = configManager.getString("EMBED_COLOR");
    private final String EMBED_COLOR_SUCCESS = configManager.getString("EMBED_COLOR_SUCCESS");
    private final String EMBED_COLOR_ERROR = configManager.getString("EMBED_COLOR_ERROR");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String subCommand = event.getSubcommandName();
        EmbedBuilder embedBuilder;

        switch (subCommand) {
            case "지급":
                String userIdForAdd = event.getOption("유저").getAsUser().getId();
                int pointToAdd = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().addPoint(userIdForAdd, pointToAdd);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("✅ 포인트 지급 완료")
                        .setDescription("사용자에게 **" + pointToAdd + " 포인트**가 지급되었습니다.");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
            case "제거":
                String userIdForRemove = event.getOption("유저").getAsUser().getId();
                int pointToRemove = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().removePoint(userIdForRemove, pointToRemove);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("❌ 포인트 제거 완료")
                        .setDescription("사용자의 **" + pointToRemove + " 포인트**가 제거되었습니다.");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
            case "설정":
                String userIdForSet = event.getOption("유저").getAsUser().getId();
                int pointToSet = getSafeIntFromOption(event.getOption("포인트"));
                DatabaseConfig.getUserInfoService().setPoint(userIdForSet, pointToSet);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("🔧 포인트 설정 완료")
                        .setDescription("사용자의 포인트가 **" + pointToSet + "로** 설정되었습니다.");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
            case "확인":
                String userIdForCheck = event.getOption("유저").getAsUser().getId();
                int currentPoint = DatabaseConfig.getUserInfoService().getPoint(userIdForCheck);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("🔍 포인트 확인")
                        .setDescription("사용자의 현재 포인트: **" + currentPoint + "**");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
            case "초기화":
                String userIdForReset = event.getOption("유저").getAsUser().getId();
                DatabaseConfig.getUserInfoService().setPoint(userIdForReset, 0);
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                        .setTitle("🔄 포인트 초기화 완료")
                        .setDescription("사용자의 포인트가 초기화되었습니다.");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
            case "순위":
                List<UserInfo> topUsers = DatabaseConfig.getUserInfoService().getTopUsersByPoints(10);
                StringBuilder rankMessage = new StringBuilder();
                int rank = 1;
                for (UserInfo user : topUsers) {
                    String username = event.getJDA().getUserById(user.discordId()).getName();
                    rankMessage.append(rank).append(". ").append(username).append(": ").append(user.point()).append(" 포인트\n");
                    rank++;
                }
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR))
                        .setTitle("📊 포인트 순위")
                        .setDescription(rankMessage.toString());
                event.replyEmbeds(embedBuilder.build()).queue();
                break;

            default:
                embedBuilder = new EmbedBuilder()
                        .setColor(Color.decode(EMBED_COLOR_ERROR))
                        .setTitle("⚠️ 포인트 오류")
                        .setDescription("알 수 없는 명령입니다.");
                event.replyEmbeds(embedBuilder.build()).queue();
                break;
        }
    }

    private int getSafeIntFromOption(OptionMapping option) {
        long value = option.getAsLong();
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            return 0;
        }
        return (int) value;
    }
}