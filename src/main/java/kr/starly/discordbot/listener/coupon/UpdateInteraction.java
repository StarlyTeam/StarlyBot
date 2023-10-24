package kr.starly.discordbot.listener.coupon;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.enums.CouponRequirementType;
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
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@BotEvent
public class UpdateInteraction extends ListenerAdapter {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final Map<Long, CouponRequirementType> requirementTypeMap = new HashMap<>();

    private final String ID_PREFIX = "coupon-manager-";
    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");


    // SELECT MENU
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
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

        String componentId = event.getComponentId();
        if (componentId.equals((ID_PREFIX + "requirement"))) {
            String requirementTypeStr = event.getSelectedOptions().get(0).getValue();
            CouponRequirementType requirementType = CouponRequirementType.valueOf(requirementTypeStr);

            // 세션 데이터 저장
            requirementTypeMap.put(userId, requirementType);

            // 메시지 전송
            Button add = Button.success(ID_PREFIX + "requirements-add", "추가");
            Button delete = Button.danger(ID_PREFIX + "requirements-delete", "삭제");
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("사용 조건 수정")
                    .setDescription("사용 조건을 어떻게 수정할지 선택해주세요.")
                    .build();
            event.replyEmbeds(embed)
                    .addActionRow(add, delete, CANCEL_BUTTON)
                    .setEphemeral(true)
                    .queue();
        }
    }

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
        switch (modalId) {
            case ID_PREFIX + "update" -> {
                if (sessionType != CouponSessionType.UPDATE) return;

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

                event.replyEmbeds(
                                new EmbedBuilder()
                                        .setColor(EMBED_COLOR_SUCCESS)
                                        .setTitle("쿠폰 수정 완료")
                                        .setDescription("쿠폰을 수정하였습니다.")
                                        .build()
                        )
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
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

        String componentId = event.getComponentId();
        switch (componentId) {
            case ID_PREFIX + "update-info" -> {
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    event.replyEmbeds(
                                    new EmbedBuilder()
                                            .setColor(EMBED_COLOR_ERROR)
                                            .setTitle("쿠폰 미존재")
                                            .setDescription("존재하지 않는 쿠폰입니다.")
                                            .build()
                            )
                            .setEphemeral(true)
                            .queue();

                    sessionRepository.stopSession(userId);
                    return;
                }

                TextInput id = TextInput.create("id", "ID (변경불가)", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setValue(couponCode)
                        .setPlaceholder("쿠폰 ID를 입력해주세요.")
                        .build();
                TextInput name = TextInput.create("name", "이름", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("쿠폰 이름을 입력해주세요.")
                        .build();
                TextInput description = TextInput.create("description", "설명", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("쿠폰 설명을 입력해주세요.")
                        .build();
                TextInput discountType = TextInput.create("discount-type", "할인 타입", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("할인 타입을 입력해주세요. [비율액 - PERCENTAGE, 고정액 - FIXED]")
                        .build();
                TextInput discountValue = TextInput.create("discount-value", "할인 값", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("할인 값을 입력해주세요. [xxx% 할인, xxx원 할인 등]")
                        .build();
                Modal modal = Modal.create(ID_PREFIX + "update", "쿠폰 수정")
                        .addActionRow(id)
                        .addActionRow(name)
                        .addActionRow(description)
                        .addActionRow(discountType)
                        .addActionRow(discountValue)
                        .build();

                event.replyModal(modal).queue();
            }

            case ID_PREFIX + "update-requirements" -> {
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    event.replyEmbeds(
                                    new EmbedBuilder()
                                            .setColor(EMBED_COLOR_ERROR)
                                            .setTitle("쿠폰 미존재")
                                            .setDescription("존재하지 않는 쿠폰입니다.")
                                            .build()
                            )
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("사용 조건 수정")
                        .setDescription("수정할 사용 조건을 선택해주세요.")
                        .build();
                event.replyEmbeds(embed)
                        .addActionRow(createRequirementSelectMenu())
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "requirements-add" -> {
                List<LayoutComponent> components = new ArrayList<>();

                CouponRequirementType requirementType = requirementTypeMap.get(userId);
                switch (requirementType) {
                    // TODO : type별 핸들링
                }
            }

            case ID_PREFIX + "requirements-delete" -> {
                // Coupon 찾기
                CouponService couponService = DatabaseManager.getCouponService();
                Coupon coupon = couponService.getData(couponCode);

                // Requirement 삭제
                CouponRequirementType requirementType = requirementTypeMap.get(userId);
                coupon.getRequirements().removeIf(requirement -> requirement.getType() == requirementType);

                // DB 저장
                couponService.repository().put(coupon);

                // 메시지 전송
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("사용 조건 삭제 완료")
                        .setDescription("사용 조건을 삭제하였습니다.")
                        .build();
                event.replyEmbeds(embed)
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    // UTILITY
    private SelectMenu createRequirementSelectMenu() {
        return StringSelectMenu.create(ID_PREFIX + "requirement")
                .addOption("DAY_BEFORE", "DAY_BEFORE", "지정일 전까지만 사용 가능")
                .addOption("DAY_AFTER", "DAY_AFTER", "지정일 후부터만 사용 가능")
                .addOption("DAY_AFTER_VERIFY", "DAY_AFTER_VERIFY", "인증을 완료 후, 지정한 시각만큼 지나야 사용 가능")
                .addOption("MAX_USE_PER_USER", "MAX_USE_PER_USER", "유저당 최대 사용 횟수")
                .addOption("MAX_USE_PER_COUPON", "MAX_USE_PER_COUPON", "쿠폰당 최대 사용 횟수")
                .addOption("TRANSACTION_TYPE", "TRANSACTION_TYPE", "거래 유형이 일치해야 사용 가능")
                .addOption("MINIMUM_PRICE", "MINIMUM_PRICE", "최소 금액 미만일 시 사용 불가")
                .addOption("MAXIMUM_PRICE", "MAXIMUM_PRICE", "최대 금액 초과시 사용 불가")
                .addOption("ROLE", "ROLE", "지정된 역할을 보유/미보유 하여야 사용 가능")
                .build();
    }
}