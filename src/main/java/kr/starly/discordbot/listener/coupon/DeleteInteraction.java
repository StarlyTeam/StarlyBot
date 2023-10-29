package kr.starly.discordbot.listener.coupon;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.enums.CouponSessionType;
import kr.starly.discordbot.listener.BotEvent;
import kr.starly.discordbot.repository.RepositoryManager;
import kr.starly.discordbot.repository.impl.CouponSessionRepository;
import kr.starly.discordbot.service.CouponService;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;

@BotEvent
public class DeleteInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final String ID_PREFIX = "coupon-manager-";

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        // Repository 변수 선언
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();

        // 변수 선언
        long userId = event.getUser().getIdLong();
        if (!sessionRepository.hasSession(userId)) return;

        String couponCode = sessionRepository.retrieveCoupon(userId);
        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);

        String componentId = event.getComponentId();
        if (componentId.equals((ID_PREFIX + "delete-confirm"))) {
            if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                PermissionUtil.sendPermissionError(event.getChannel());
                return;
            }

            if (sessionType != CouponSessionType.DELETE) return;

            CouponService couponService = DatabaseManager.getCouponService();
            couponService.deleteData(couponCode);

            event.replyEmbeds(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_SUCCESS)
                                    .setTitle("쿠폰 삭제 완료")
                                    .setDescription("쿠폰을 삭제하였습니다.")
                                    .build()
                    )
                    .setEphemeral(true)
                    .queue();

            sessionRepository.stopSession(userId);
        }
    }
}