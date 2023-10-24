package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.enums.CouponSessionType;
import kr.starly.discordbot.repository.RepositoryManager;
import kr.starly.discordbot.repository.impl.CouponSessionRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;

import java.awt.*;

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
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));

    private final String ID_PREFIX = "coupon-manager-";
    private final Button CANCEL_BUTTON = Button.danger(ID_PREFIX + "cancel", "취소");

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // 변수 선언
        long userId = event.getUser().getIdLong();
        String subCommand = event.getSubcommandName();

        // Repository 변수 선언
        CouponSessionRepository sessionRepository = RepositoryManager.getCouponSessionRepository();

        // 세션 검증
        if (sessionRepository.hasSession(userId)) {
            event.replyEmbeds(
                            new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("세션 존재")
                                    .setDescription("이미 진행중인 세션이 존재합니다.")
                                    .build()
                    )
                    .setEphemeral(true)
                    .queue();
            return;
        }

        // 세션 데이터 저장
        sessionRepository.updateSessionType(userId, switch (subCommand) {
            case "생성" -> CouponSessionType.CREATE;
            case "삭제" -> CouponSessionType.DELETE;
            case "수정" -> CouponSessionType.UPDATE;
            case "목록" -> CouponSessionType.LIST;
            default -> throw new IllegalArgumentException("Unexpected value: " + subCommand);
        });

        // DB 저장
        String couponCode = event.getOption("코드").getAsString();
        CouponSessionType sessionType = sessionRepository.retrieveSessionType(userId);

        sessionRepository.updateCoupon(userId, couponCode);
        sessionRepository.updateSessionType(userId, sessionType);

        // 응답 전송
        switch (sessionType) {
            case CREATE -> {
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
                Modal modal = Modal.create(ID_PREFIX + "create", "쿠폰 생성")
                        .addActionRow(name)
                        .addActionRow(description)
                        .addActionRow(discountType)
                        .addActionRow(discountValue)
                        .build();

                event.replyModal(modal).queue();
            }

            case DELETE -> {
                Button confirm = Button.danger(ID_PREFIX + "delete-confirm", "삭제");
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_ERROR)
                        .setTitle("삭제 확인")
                        .setDescription("정말로 쿠폰을 삭제하시겠습니까?")
                        .build();

                event.replyEmbeds(embed)
                        .addActionRow(confirm, CANCEL_BUTTON)
                        .queue();
            }

            case UPDATE -> {
                Button updateInfo = Button.primary(ID_PREFIX + "update-info", "정보 수정");
                Button updateRequirements = Button.primary(ID_PREFIX + "update-requirements", "사용조건 수정");
                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("쿠폰 수정")
                        .setDescription("수정할 정보를 선택해주세요.")
                        .build();

                event.replyEmbeds(embed)
                        .addActionRow(updateInfo, updateRequirements, CANCEL_BUTTON)
                        .queue();
            }

            case LIST -> {
                // TODO : txt 파일 생성 후 업로드
            }
        }
    }
}