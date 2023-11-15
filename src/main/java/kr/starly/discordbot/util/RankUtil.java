package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.rank.perk.impl.CouponPerk;
import kr.starly.discordbot.entity.rank.perk.impl.RolePerk;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;

import java.awt.Color;
import java.util.stream.Collectors;

public class RankUtil {

    private RankUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String GUILD_ID = configProvider.getString("GUILD_ID");
    private static final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

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

                    Guild guild = DiscordBotManager.getInstance().getJda().getGuildById(GUILD_ID);
                    guild.addRoleToMember(UserSnowflake.fromId(userId), role).queue();
                }

                case COUPON -> {
                    String codeStr = ((CouponPerk) perk).getCoupons().stream()
                            .map(Coupon::getCode)
                            .collect(Collectors.joining(", "));

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 발급 성공 | 쿠폰 <a:success:1168266537262657626>")
                            .setDescription("""
                                    
                                    > **쿠폰이 발급되었습니다.**
                                    > **쿠폰 코드: %s
                                    """
                                    .formatted(codeStr)
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                            .build();

                    user1.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(embed))
                            .queue(null, (error) -> {
                                AuditLogger.warning(new EmbedBuilder()
                                        .setTitle("<a:loading:1168266572847128709> 발급 실패 | 쿠폰 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                            > **유저: %s**
                                                                        
                                            > **쿠폰 코드: %s**
                                            > **사유: %s**
                                                                        
                                            ─────────────────────────────────────────────────"""
                                                .formatted(user1.getAsTag(), codeStr, error.getMessage())
                                        )
                                );
                            });
                }
            }
        });

        // 메시지 전송
        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 랭크 <a:success:1168266537262657626>")
                .setDescription("""
                                    > **랭크가 %s(으)로 상승하였습니다.**
                                     """
                        .formatted(rank.getName())
                )
                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                .build();

        user1.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed))
                .queue(null, (error) -> {
                    AuditLogger.warning(new EmbedBuilder()
                            .setTitle("<a:loading:1168266572847128709> 메시지 전송 실패 | 랭크 <a:loading:1168266572847128709>")
                            .setDescription("""
                                            > **유저: %s**
                                                                        
                                            > **랭크: %s**
                                            > **사유: %s**
                                                                        
                                            ─────────────────────────────────────────────────"""
                                    .formatted(user1.getAsTag(), rank.getName(), error.getMessage())
                            )
                    );
                });
    }
}