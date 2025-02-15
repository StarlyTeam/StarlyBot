package kr.starly.discordbot.util;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.enums.RankPerkType;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.rank.perk.impl.CouponPerk;
import kr.starly.discordbot.entity.rank.perk.impl.RolePerk;
import kr.starly.discordbot.service.UserService;
import kr.starly.discordbot.util.messaging.AuditLogger;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.HierarchyException;

import java.awt.Color;
import java.util.stream.Collectors;

public class RankUtil {

    private RankUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    public static void giveRank(long userId, Rank rank) {
        UserService userService = DatabaseManager.getUserService();
        User user = userService.getDataByDiscordId(userId);
        user.rank().add(rank);
        userService.repository().put(user);

        JDA jda = DiscordBotManager.getInstance().getJda();
        net.dv8tion.jda.api.entities.User user1 = jda.getUserById(userId);

        rank.getPerks().values().forEach(perk -> {
            switch (perk.getType()) {
                case ROLE -> {
                    Role role = ((RolePerk) perk).getRole();

                    Guild guild = DiscordBotManager.getInstance().getGuild();
                    guild.addRoleToMember(UserSnowflake.fromId(userId), role).queue();
                }

                case COUPON -> {
                    String codeStr = ((CouponPerk) perk).getCoupons().stream()
                            .map(Coupon::getCode)
                            .collect(Collectors.joining(", "));

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 성공 | 쿠폰 발급 <a:success:1168266537262657626>")
                            .setDescription("""
                                    > **쿠폰이 발급되었습니다.**
                                    > **쿠폰 코드: %s**
                                    """
                                    .formatted(codeStr)
                            )
                            .setThumbnail("https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                            .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                            .build();

                    user1.openPrivateChannel()
                            .flatMap(channel -> channel.sendMessageEmbeds(embed))
                            .queue(null, (error) -> {
                                AuditLogger.warning(new EmbedBuilder()
                                        .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                        .setDescription("""
                                            > **쿠폰 발급에 실패했습니다.**
                                            
                                            ─────────────────────────────────────────────────
                                            > **유저: %s**
                                            > **쿠폰 코드: %s**
                                            > **사유: %s**
                                            """.formatted(user1.getAsTag(), codeStr, error.getMessage())
                                        )
                                );
                            });
                }
            }
        });

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("<a:success:1168266537262657626> 성공 | 랭크 <a:success:1168266537262657626>")
                .setDescription("> **랭크가 %s(으)로 상승하였습니다.**".formatted(rank.getName())
                )
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .build();
        user1.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed))
                .queue(null, (error) -> {
                    AuditLogger.warning(new EmbedBuilder()
                            .setTitle("<a:loading:1168266572847128709> 오류 | 랭크 <a:loading:1168266572847128709>")
                            .setDescription("""
                                            > **랭크 알림을 전송하지 못했습니다.**
                                            
                                            ─────────────────────────────────────────────────
                                            > **유저: %s**
                                            > **랭크: %s**
                                            > **사유: %s**
                                    """
                                    .formatted(user1.getAsTag(), rank.getName(), error.getMessage())
                            )
                    );
                });
    }

    public static void takeRank(long userId, Rank rank) {
        UserService userService = DatabaseManager.getUserService();
        User user = userService.getDataByDiscordId(userId);
        if (user.rank().stream()
                .map(Rank::getOrdinal)
                .anyMatch(ordinal -> ordinal > rank.getOrdinal())
        ) {
            throw new HierarchyException("Higher rank must be removed first.");
        }

        user.rank().remove(rank);
        userService.saveData(user);

        JDA jda = DiscordBotManager.getInstance().getJda();
        net.dv8tion.jda.api.entities.User user1 = jda.getUserById(userId);

        rank.getPerks().forEach((type, perk) -> {
            if (type != RankPerkType.ROLE) return;

            RolePerk perk1 = (RolePerk) perk;
            Role perkRole = perk1.getRole();

            Guild guild = DiscordBotManager.getInstance().getGuild();
            guild.removeRoleFromMember(UserSnowflake.fromId(userId), perkRole);
        });

        MessageEmbed embed = new EmbedBuilder()
                .setColor(EMBED_COLOR_SUCCESS)
                .setTitle("제목")
                .setDescription("> **%s 랭크를 잃었습니다.**".formatted(rank.getName()))
                .setThumbnail("https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해주십시오.", "https://file.starly.kr/images/Logo/StarlyPlugin/StarlyPlugin_YELLOW.png")
                .build();
        user1.openPrivateChannel()
                .flatMap(channel -> channel.sendMessageEmbeds(embed))
                .queue(null, (error) -> {
                    AuditLogger.warning(new EmbedBuilder()
                            .setTitle("<a:loading:1168266572847128709> 오류 | 랭크 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **랭크 알림을 전송하지 못했습니다.**
                                    
                                    ─────────────────────────────────────────────────
                                    > **유저: %s**
                                    > **랭크: %s**
                                    > **사유: %s**
                                    """
                                    .formatted(user1.getAsTag(), rank.getName(), error.getMessage())
                            )
                    );
                });
    }
}