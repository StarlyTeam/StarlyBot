package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.enums.CouponSessionType;
import kr.starly.discordbot.repository.RepositoryManager;
import kr.starly.discordbot.repository.impl.CouponSessionRepository;
import kr.starly.discordbot.service.CouponService;
import kr.starly.discordbot.util.FileUploadUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;
import java.util.List;

@BotSlashCommand(
        command = "쿠폰관리",
        description = "쿠폰을 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "생성",
                        description = "쿠폰을 생성합니다.",
                        optionName = {"코드"},
                        optionType = {
                                OptionType.STRING
                        },
                        optionDescription = {
                                "쿠폰 코드를 입력하세요."
                        },
                        optionRequired = {
                                true
                        }
                ),
                @BotSlashCommand.SubCommand(
                        name = "수정",
                        description = "쿠폰을 수정합니다.",
                        optionName = {"코드"},
                        optionType = {
                                OptionType.STRING
                        },
                        optionDescription = {
                                "쿠폰 코드를 입력하세요."
                        },
                        optionRequired = {
                                true
                        }
                ),
                @BotSlashCommand.SubCommand(
                        name = "삭제",
                        description = "쿠폰을 삭제합니다.",
                        optionName = {"코드"},
                        optionType = {
                                OptionType.STRING,
                        },
                        optionDescription = {
                                "쿠폰 코드를 입력하세요.",
                        },
                        optionRequired = {
                                true,
                        }
                ),
                @BotSlashCommand.SubCommand(
                        name = "목록",
                        description = "쿠폰 목록을 확인합니다."
                )
        }
)
public class CouponCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    private final String ID_PREFIX = "coupon-manager-";
    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        long userId = event.getUser().getIdLong();
        String subCommand = event.getSubcommandName();

        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();
        if (sessionRepository.hasSession(userId)) {
            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:loading:1168266572847128709> 세션 존재 | 쿠폰 <a:loading:1168266572847128709>")
                    .setDescription("""
                            > **이미 진행 중인 세션이 존재합니다.**
                                                        
                            ─────────────────────────────────────────────────
                            """
                    )
                    .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                    .build();

            event.replyEmbeds(embed).setEphemeral(true).queue();
            return;
        }

        if (subCommand.equals("목록")) {
            StringBuilder sb = new StringBuilder();
            List<Coupon> coupons = DatabaseManager.getCouponService().getAllData();
            coupons.forEach(coupon -> sb
                    .append("%s [%s | %s]".formatted(coupon.getCode(), coupon.getName(), coupon.getDescription()))
                    .append("\n"));

            event.replyFiles(FileUploadUtil.createFileUpload(sb.toString())).queue();
            return;
        }

        sessionRepository.updateSessionType(userId, switch (subCommand) {
            case "생성" -> CouponSessionType.CREATE;
            case "삭제" -> CouponSessionType.DELETE;
            case "수정" -> CouponSessionType.UPDATE;
            default -> throw new IllegalArgumentException("Unexpected value: " + subCommand);
        });

        String couponCode = event.getOption("코드").getAsString();
        sessionRepository.updateCoupon(userId, couponCode);

        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);
        sessionRepository.updateSessionType(userId, sessionType);

        switch (sessionType) {
            case CREATE -> {
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) != null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 코드 오류 | 쿠폰 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **이미 존재하는 쿠폰 코드입니다.**
                                                                
                                    ─────────────────────────────────────────────────
                                    """
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .build();

                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    sessionRepository.stopSession(userId);
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
                Modal modal = Modal.create(ID_PREFIX + "create", "쿠폰 생성")
                        .addActionRow(name)
                        .addActionRow(description)
                        .addActionRow(discountType)
                        .addActionRow(discountValue)
                        .build();

                event.replyModal(modal).queue();
            }

            case DELETE -> {
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 코드 오류 | 쿠폰 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **존재하지 않는 쿠폰 코드입니다.**
                                                                
                                    ─────────────────────────────────────────────────
                                    """
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    sessionRepository.stopSession(userId);
                    return;
                }

                Button confirm = Button.primary(ID_PREFIX + "delete-confirm", "삭제");
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 삭제 확인 | 쿠폰 <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **정말로 쿠폰을 삭제하시겠습니까?**
                                                                
                                    ─────────────────────────────────────────────────
                                    """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .build();
                event.replyEmbeds(embed).addActionRow(confirm, CANCEL_BUTTON).queue();
            }

            case UPDATE -> {
                CouponService couponService = DatabaseManager.getCouponService();
                if (couponService.getData(couponCode) == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 코드 오류 | 쿠폰 <a:loading:1168266572847128709>")
                            .setDescription("""
                                    > **존재하지 않는 쿠폰 코드입니다.**
                                                                
                                    ─────────────────────────────────────────────────
                                    """
                            )
                            .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                            .build();
                    event.replyEmbeds(embed).setEphemeral(true).queue();

                    sessionRepository.stopSession(userId);
                    return;
                }

                Button updateInfo = Button.primary(ID_PREFIX + "update-info", "정보 수정");
                Button updateRequirements = Button.secondary(ID_PREFIX + "update-requirements", "사용조건 수정");

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("<a:loading:1168266572847128709> 수정 | 쿠폰 <a:loading:1168266572847128709>")
                        .setDescription("""
                                    > **수정할 정보를 선택해 주세요..**
                                                                
                                    ─────────────────────────────────────────────────
                                    """
                        )
                        .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .setFooter("문제가 발생한 경우, 고객 상담을 통해 문의해 주십시오.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/c51e380e-1d18-4eb5-6bee-21921b2ee100/public")
                        .build();
                event.replyEmbeds(embed).addActionRow(updateInfo, updateRequirements, CANCEL_BUTTON).setEphemeral(true).queue();
            }
        }
    }
}