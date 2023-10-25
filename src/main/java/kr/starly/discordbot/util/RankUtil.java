package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.perk.impl.CouponPerk;
import kr.starly.discordbot.entity.perk.impl.RolePerk;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.awt.Color;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RankUtil {

    private RankUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String GUILD_ID = configProvider.getString("GUILD_ID");
    private static final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private static final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));


    public static void giveRank(long userId, Rank rank) {
        // User 객체 수정
        UserService userService = DatabaseManager.getUserService();
        User user = userService.getDataByDiscordId(userId);
        user.rank().add(rank);
        userService.repository().put(user);

        // JDA User 검색
        JDA jda = DiscordBotManager.getInstance().getJda();
        net.dv8tion.jda.api.entities.User user1 = jda.getUserById(userId);

        // Perk 적용
        rank.getPerks().values().forEach(perk -> {
            switch (perk.getType()) {
                case ROLE -> {
                    Role role = ((RolePerk) perk).getRole();

                    Guild guild = jda.getGuildById(GUILD_ID);
                    guild.addRoleToMember(UserSnowflake.fromId(userId), role).queue();
                }

                case COUPON -> {
                    String codeStr = ((CouponPerk) perk).getCoupons().stream()
                            .map(Coupon::getCode)
                            .collect(Collectors.joining(", "));
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR)
                            .setTitle("쿠폰이 발급되었습니다.")
                            .setDescription("쿠폰 코드: " + codeStr)
                            .build();

                    user1.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(embed))
                            .queue(null, (error) -> {
                                AuditLogger.warning(new EmbedBuilder()
                                        .setTitle("발급 실패")
                                        .setDescription("유저: " + user1.getAsTag() + "\n" +
                                                "쿠폰 코드: " + codeStr + "\n" +
                                                "사유: " + error.getMessage())
                                );
                            });
                }
            }
        });

        // 메시지 전송
        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("제목")
                .setDescription(format("랭크가 %s(으)로 상승하였습니다.", rank.getName()))
                .build();
        user1.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed))
                .queue(null, (error) -> {
                    AuditLogger.warning(new EmbedBuilder()
                            .setTitle("랭크 상승 안내 메시지 전송 실패")
                            .setDescription("유저: " + user1.getAsTag() + "\n" +
                                    "랭크: " + rank.getName() + "\n" +
                                    "사유: " + error.getMessage())
                    );
                });
    }
} // TODO : 메시지 디자인