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
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final String ID_PREFIX = "coupon-manager-";

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        // 퍼미션 검증
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        // Repository 변수 선언
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();

        // 변수 선언
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
                event.replyEmbeds(
                                new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("할인 타입 오류")
                                        .setDescription("할인 타입을 찾을 수 없습니다.")
                                        .build()
                        )
                        .setEphemeral(true)
                        .queue();

                sessionRepository.stopSession(userId);
                return;
            }

            int discountValue;
            try {
                discountValue = Integer.parseInt(discountValueStr);
            } catch (NumberFormatException ignored) {
                event.replyEmbeds(
                                new EmbedBuilder()
                                        .setColor(EMBED_COLOR_ERROR)
                                        .setTitle("할인 값 오류")
                                        .setDescription("할인 값이 올바르지 않습니다.")
                                        .build()
                        )
                        .setEphemeral(true)
                        .queue();

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
                    .setTitle("쿠폰 생성 성공")
                    .setDescription("쿠폰을 생성하였습니다.")
                    .build();
            event.replyEmbeds(embed)
                    .setEphemeral(true)
                    .queue();
        }
    }
}