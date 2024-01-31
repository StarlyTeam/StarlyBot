package kr.starly.discordbot.listener.coupon;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.impl.*;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.enums.CouponSessionType;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.enums.ProductType;
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
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;

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
        if (!event.getComponentId().startsWith(ID_PREFIX)) return;
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();
        long userId = event.getUser().getIdLong();
        if (!sessionRepository.hasSession(userId)) return;

        String couponCode = sessionRepository.retrieveCoupon(userId);
        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);

        String componentId = event.getComponentId();
        if (componentId.equals((ID_PREFIX + "requirement"))) {
            if (sessionType != CouponSessionType.UPDATE) return;

            String requirementTypeStr = event.getSelectedOptions().get(0).getValue();
            CouponRequirementType requirementType = CouponRequirementType.valueOf(requirementTypeStr);
            requirementTypeMap.put(userId, requirementType);

            // 메시지 전송
            Button add = Button.success(ID_PREFIX + "add-requirements", "추가");
            Button delete = Button.primary(ID_PREFIX + "remove-requirements", "삭제");

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR)
                    .setTitle("<a:loading:1168266572847128709> 수정 | 쿠폰 <a:loading:1168266572847128709>")
                    .setDescription("> **수정할 사용 조건을 선택해 주세요.**")
                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                    .build();
            event.replyEmbeds(embed).addActionRow(add, delete, CANCEL_BUTTON).setEphemeral(true).queue();
        }
    }

    // MODAL
    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();
        long userId = event.getUser().getIdLong();
        if (!sessionRepository.hasSession(userId)) return;

        String couponCode = sessionRepository.retrieveCoupon(userId);
        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);

        String modalId = event.getModalId();
        switch (modalId) {
            case ID_PREFIX + "update" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }
                if (sessionType != CouponSessionType.UPDATE) return;

                String name = event.getValue("name").getAsString();
                String description = event.getValue("description").getAsString();

                String discountTypeStr = event.getValue("discount-type").getAsString();
                String discountValueStr = event.getValue("discount-value").getAsString();

                DiscountType discountType;
                try {
                    discountType = DiscountType.valueOf(discountTypeStr);
                } catch (IllegalArgumentException ignored) {
                    event.replyEmbeds(new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("할인 타입 오류")
                                    .setDescription("할인 타입을 찾을 수 없습니다.")
                                    .build())
                            .setEphemeral(true)
                            .queue();

                    stopSession(userId);
                    return;
                }

                int discountValue;
                try {
                    discountValue = Integer.parseInt(discountValueStr);
                } catch (NumberFormatException ignored) {
                    event.replyEmbeds(new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("할인 값 오류")
                                    .setDescription("할인 값이 올바르지 않습니다.")
                                    .build())
                            .setEphemeral(true)
                            .queue();

                    stopSession(userId);
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

                event.replyEmbeds(new EmbedBuilder()
                                .setColor(EMBED_COLOR_SUCCESS)
                                .setTitle("쿠폰 수정 완료")
                                .setDescription("쿠폰을 수정하였습니다.")
                                .build())
                        .setEphemeral(true)
                        .queue();
            }

            case ID_PREFIX + "add-requirements" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }

                if (sessionType != CouponSessionType.UPDATE) return;

                CouponRequirementType requirementType = requirementTypeMap.get(userId);
                CouponService couponService = DatabaseManager.getCouponService();
                Coupon coupon = couponService.getData(couponCode);

                switch (requirementType) {
                    case DAY_BEFORE, DAY_AFTER -> {
                        String dayStr = event.getValue(requirementType.name()).getAsString();
                        Date date;
                        try {
                            date = new SimpleDateFormat("yyyy-MM-dd").parse(dayStr);
                        } catch (ParseException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **지정일 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        if (requirementType == CouponRequirementType.DAY_BEFORE) {
                            coupon.getRequirements().add(
                                    new DayBeforeRequirement(date)
                            );
                        } else {
                            coupon.getRequirements().add(
                                    new DayAfterRequirement(date)
                            );
                        }
                    }

                    case DAY_AFTER_VERIFY -> {
                        String dayStr = event.getValue(requirementType.name()).getAsString();
                        int day;
                        try {
                            day = Integer.parseInt(dayStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **지정시간 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new DayAfterVerifyRequirement(day)
                        );
                    }

                    case MAX_USE_PER_USER -> {
                        String maxUsePerUserStr = event.getValue(requirementType.name()).getAsString();
                        int maxUsePerUser;
                        try {
                            maxUsePerUser = Integer.parseInt(maxUsePerUserStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **최대 사용 횟수 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();
                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new MaxUsePerUserRequirement(maxUsePerUser)
                        );
                    }

                    case MAX_USE_PER_COUPON -> {
                        String maxUsePerCouponStr = event.getValue(requirementType.name()).getAsString();
                        int maxUsePerCoupon;
                        try {
                            maxUsePerCoupon = Integer.parseInt(maxUsePerCouponStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **최대 사용 횟수 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new MaxUsePerCouponRequirement(maxUsePerCoupon)
                        );
                    }

                    case PRODUCT_TYPE -> {
                        String productTypeStr = event.getValue(requirementType.name()).getAsString();
                        ProductType productType;
                        try {
                            productType = ProductType.valueOf(productTypeStr);
                        } catch (IllegalArgumentException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **상품 유형을 찾을 수 없습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new TransactionTypeRequirement(productType)
                        );
                    }

                    case MINIMUM_PRICE -> {
                        String minimumPriceStr = event.getValue(requirementType.name()).getAsString();
                        int minimumPrice;
                        try {
                            minimumPrice = Integer.parseInt(minimumPriceStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **최소 금액 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new MinimumPriceRequirement(minimumPrice)
                        );
                    }

                    case MAXIMUM_PRICE -> {
                        String maximumPriceStr = event.getValue(requirementType.name()).getAsString();
                        int maximumPrice;
                        try {
                            maximumPrice = Integer.parseInt(maximumPriceStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **최대 금액 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new MaximumPriceRequirement(maximumPrice)
                        );
                    }

                    case ROLE -> {
                        String roleIdStr = event.getValue(requirementType.name()).getAsString();
                        long roleId;
                        try {
                            roleId = Long.parseLong(roleIdStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **역할 ID 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        String roleRequiredStr = event.getValue(requirementType.name() + "-required").getAsString();
                        boolean roleRequired;
                        try {
                            roleRequired = Boolean.parseBoolean(roleRequiredStr);
                        } catch (NumberFormatException ignored) {
                            MessageEmbed embed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1168266572847128709> 오류 | 쿠폰 <a:loading:1168266572847128709>")
                                    .setDescription("> **역할 요구 여부 형식이 올바르지 않습니다.**")
                                    .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                                    .build();
                            event.replyEmbeds(embed).setEphemeral(true).queue();

                            stopSession(userId);
                            return;
                        }

                        coupon.getRequirements().add(
                                new RoleRequirement(roleId, roleRequired)
                        );
                    }
                }

                couponService.saveData(coupon);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 쿠폰 <a:success:1168266537262657626>")
                        .setDescription("> **사용 조건을 추가하였습니다.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
            }
        }
    }

    // BUTTON
    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        // Repository 변수 선언
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();

        // 변수 선언
        long userId = event.getUser().getIdLong();
        if (!sessionRepository.hasSession(userId)) return;

        String couponCode = sessionRepository.retrieveCoupon(userId);

        String componentId = event.getComponentId();
        switch (componentId) {
            case ID_PREFIX + "update-info" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 미존재 | 쿠폰 <a:loading:1168266572847128709>")
                            .setDescription("> **존재하지 않는 쿠폰입니다.**")
                            .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    stopSession(userId);
                    return;
                }

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
                        .setPlaceholder("할인 타입을 입력해주세요. [PERCENTAGE, FIXED]")
                        .build();
                TextInput discountValue = TextInput.create("discount-value", "할인 값", TextInputStyle.SHORT)
                        .setRequired(true)
                        .setPlaceholder("할인 값을 입력해주세요. [xxx% 할인, xxx원 할인 등]")
                        .build();
                Modal modal = Modal.create(ID_PREFIX + "update", "쿠폰 수정")
                        .addActionRow(name)
                        .addActionRow(description)
                        .addActionRow(discountType)
                        .addActionRow(discountValue)
                        .build();

                event.replyModal(modal).queue();
            }

            case ID_PREFIX + "update-requirements" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }

                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 미존재 | 쿠폰 <a:loading:1168266572847128709>")
                            .setDescription("> **존재하지 않는 쿠폰입니다.**")
                            .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 수정 | 쿠폰 <a:loading:1168266572847128709>")
                        .setDescription("> **수정할 사용 조건을 선택해 주세요.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).addActionRow(createRequirementSelectMenu()).setEphemeral(true).queue();
            }

            case ID_PREFIX + "add-requirements" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }

                List<TextInput> textInputs = new ArrayList<>();

                CouponRequirementType requirementType = requirementTypeMap.get(userId);
                switch (requirementType) {
                    case DAY_BEFORE -> {
                        TextInput dayBefore = TextInput.create(requirementType.name(), "지정일 (년-월-일 형식)", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("~까지 사용 가능")
                                .build();
                        textInputs.add(dayBefore);
                    }

                    case DAY_AFTER -> {
                        TextInput dayAfter = TextInput.create(requirementType.name(), "지정일 (년-월-일 형식)", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("~부터 사용 가능")
                                .build();
                        textInputs.add(dayAfter);
                    }

                    case DAY_AFTER_VERIFY -> {
                        TextInput dayAfterVerify = TextInput.create(requirementType.name(), "지정시간", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("인증을 완료한 후, ~분이 지난 후부터 사용 가능")
                                .build();
                        textInputs.add(dayAfterVerify);
                    }

                    case MAX_USE_PER_USER -> {
                        TextInput maxUsePerUser = TextInput.create(requirementType.name(), "최대 사용 횟수", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("유저당 최대 ~회까지 사용 가능")
                                .build();
                        textInputs.add(maxUsePerUser);
                    }

                    case MAX_USE_PER_COUPON -> {
                        TextInput maxUsePerCoupon = TextInput.create(requirementType.name(), "최대 사용 횟수", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("쿠폰당 최대 ~회까지 사용 가능")
                                .build();
                        textInputs.add(maxUsePerCoupon);
                    }

                    case PRODUCT_TYPE -> {
                        TextInput transactionType = TextInput.create(requirementType.name(), "상품 유형", TextInputStyle.PARAGRAPH)
                                .setRequired(true)
                                .setPlaceholder("~ 상품에만 사용 가능 [PREMIUM_RESOURCE, OUTSOURCING, CUSTOM_PRICE]")
                                .build();
                        textInputs.add(transactionType);
                    }

                    case MINIMUM_PRICE -> {
                        TextInput minimumPrice = TextInput.create(requirementType.name(), "최소 금액", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("~원 이상 구매해야 사용 가능")
                                .build();
                        textInputs.add(minimumPrice);
                    }

                    case MAXIMUM_PRICE -> {
                        TextInput maximumPrice = TextInput.create(requirementType.name(), "최대 금액", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("~원 이하 구매해야 사용 가능")
                                .build();
                        textInputs.add(maximumPrice);
                    }

                    case ROLE -> {
                        TextInput role = TextInput.create(requirementType.name(), "역할", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("지정된 역할을 보유/미보유 해야 사용 가능")
                                .build();
                        TextInput required = TextInput.create(requirementType.name() + "-required", "보유 여부", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setPlaceholder("보유 여부를 입력해주세요. [true, false]")
                                .build();
                        textInputs.add(role);
                        textInputs.add(required);
                    }
                }

                Modal modal = Modal.create(ID_PREFIX + "add-requirements", "사용 조건 추가")
                        .addComponents(
                                textInputs.stream()
                                        .map(ActionRow::of)
                                        .toList()
                        )
                        .build();
                event.replyModal(modal).queue();
            }

            case ID_PREFIX + "remove-requirements" -> {
                if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
                    PermissionUtil.sendPermissionError(event);
                    return;
                }

                CouponService couponService = DatabaseManager.getCouponService();
                Coupon coupon = couponService.getData(couponCode);

                CouponRequirementType requirementType = requirementTypeMap.get(userId);
                coupon.getRequirements().removeIf(requirement -> requirement.getType() == requirementType);

                couponService.repository().put(coupon);

                stopSession(userId);

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 쿠폰 <a:success:1168266537262657626>")
                        .setDescription("> **사용 조건을 삭제하였습니다.**")
                        .setThumbnail("https://file.starly.kr/images/Logo/Starly/white.png")
                        .build();
                event.replyEmbeds(embed).setEphemeral(true).queue();
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
                .addOption("TRANSACTION_TYPE", "TRANSACTION_TYPE", "상품 유형이 일치해야 사용 가능")
                .addOption("MINIMUM_PRICE", "MINIMUM_PRICE", "최소 금액 미만일 시 사용 불가")
                .addOption("MAXIMUM_PRICE", "MAXIMUM_PRICE", "최대 금액 초과시 사용 불가")
                .addOption("ROLE", "ROLE", "지정된 역할을 보유/미보유 하여야 사용 가능")
                .build();
    }

    private void stopSession(long userId) {
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();
        sessionRepository.stopSession(userId);;

        requirementTypeMap.remove(userId);
    }
}