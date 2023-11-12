package kr.starly.discordbot.listener.coupon;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.enums.CouponSessionType;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.RepositoryManager;
import kr.starly.discordbot.repository.impl.CouponSessionRepository;
import kr.starly.discordbot.service.CouponService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;

@BotEvent
public class CreateInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final String ID_PREFIX = "coupon-manager-";

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();
        long userId = event.getUser().getIdLong();
        if (!sessionRepository.hasSession(userId)) return;

        String couponCode = sessionRepository.retrieveCoupon(userId);
        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);

        String modalId = event.getModalId();
        if (modalId.equals((ID_PREFIX + "create"))) {
            if (sessionType != CouponSessionType.CREATE) return;

            String name = event.getValue("name").getAsString();
            String description = event.getValue("description").getAsString();

            String discountTypeStr = event.getValue("discount-type").getAsString();
            String discountValueStr = event.getValue("discount-value").getAsString();

            DiscountType discountType;
            try {
                discountType = DiscountType.valueOf(discountTypeStr);
            } catch (IllegalArgumentException ignored) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                        .setDescription("> **할인 타입을 찾을 수 없습니다.**")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                sessionRepository.stopSession(userId);
                return;
            }

            int discountValue;
            try {
                discountValue = Integer.parseInt(discountValueStr);
            } catch (NumberFormatException ignored) {
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                        .setDescription("> **할인 값이 올바르지 않습니다.**")
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();

                sessionRepository.stopSession(userId);
                return;
            }

            CouponService couponService = DatabaseManager.getCouponService();
            couponService.saveData(
                    couponCode,
                    name,
                    description,
                    new ArrayList<>(),
                    discountType,
                    discountValue,
                    userId
            );

            sessionRepository.stopSession(userId);

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1168266537262657626> 성공 | 쿠폰 <a:success:1168266537262657626>")
                    .setDescription("> **쿠폰을 생성하였습니다.**")
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();
            event.replyEmbeds(embed).setEphemeral(true).queue();
        }
    }
}