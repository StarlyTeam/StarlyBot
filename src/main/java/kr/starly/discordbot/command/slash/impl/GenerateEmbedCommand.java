package kr.starly.discordbot.command.slash.impl;

import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;

@BotSlashCommand(
        command = "임베드생성",
        description = "임베드를 생성합니다.",
        optionName = {"제목", "제목링크", "내용", "색상", "썸네일", "이미지", "푸터", "푸터이미지", "작성자", "작성자링크", "작성자이미지"},
        optionType = {
                OptionType.STRING,
                OptionType.STRING,
                OptionType.STRING,
                OptionType.STRING,
                OptionType.ATTACHMENT,
                OptionType.ATTACHMENT,
                OptionType.STRING,
                OptionType.ATTACHMENT,
                OptionType.STRING,
                OptionType.STRING,
                OptionType.ATTACHMENT
        },
        optionDescription = {
                "임베드의 title을 입력하세요.",
                "임베드의 title-url을 입력하세요.",
                "임베드의 description을 입력하세요. (\\n 사용가능)",
                "임베드의 color를 입력하세요.",
                "임베드의 thumbnail을 첨부하세요.",
                "임베드의 image를 첨부하세요.",
                "임베드의 footer-text를 입력하세요.",
                "임베드의 footer-icon을 첨부하세요.",
                "임베드의 author-text를 입력하세요.",
                "임베드의 author-url을 입력하세요.",
                "임베드의 footer-icon을 첨부하세요."
        },
        optionRequired = {false, false, false, false, false, false, false, false, false, false, false}
)
public class GenerateEmbedCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        try {
            OptionMapping title = event.getOption("제목");
            OptionMapping titleUrl = event.getOption("제목링크");
            OptionMapping description = event.getOption("내용");
            OptionMapping color = event.getOption("색상");
            OptionMapping thumbnail = event.getOption("썸네일");
            OptionMapping image = event.getOption("이미지");
            OptionMapping footerText = event.getOption("푸터");
            OptionMapping footerIcon = event.getOption("푸터이미지");
            OptionMapping authorText = event.getOption("작성자");
            OptionMapping authorUrl = event.getOption("작성자링크");
            OptionMapping authorIcon = event.getOption("작성자이미지");

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(getSafeString(title), getSafeString(titleUrl));
            embedBuilder.setDescription(description == null ? null : description.getAsString().replace("\\n", "\n"));
            embedBuilder.setColor(color == null ? null : Color.decode(color.getAsString()));
            embedBuilder.setThumbnail(getSafeAttachmentUrl(thumbnail));
            embedBuilder.setImage(getSafeAttachmentUrl(image));
            embedBuilder.setFooter(getSafeString(footerText), getSafeAttachmentUrl(footerIcon));
            embedBuilder.setAuthor(getSafeString(authorText), getSafeString(authorUrl), getSafeAttachmentUrl(authorIcon));

            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();


            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_SUCCESS)
                    .setTitle("<a:success:1141625729386287206> 성공 | 임베드생성 완료 <a:success:1141625729386287206>")
                    .setDescription("**성공적으로 임베드를 생성했습니다.**")
                    .build();
            event.replyEmbeds(embed).queue();
        } catch (Exception ex) {
            ex.printStackTrace();

            MessageEmbed embed = new EmbedBuilder()
                    .setColor(EMBED_COLOR_ERROR)
                    .setTitle("<a:success:1141625729386287206> 오류 | 임베드생성 실패 <a:success:1141625729386287206>")
                    .setDescription("**임베드를 생성하지 못 했습니다.**")
                    .build();
            event.replyEmbeds(embed).queue();
        }
    }

    private String getSafeString(OptionMapping optionMapping) {
        return optionMapping == null ? null : optionMapping.getAsString();
    }

    private String getSafeAttachmentUrl(OptionMapping optionMapping) {
        return optionMapping == null ? null : optionMapping.getAsAttachment().getUrl();
    }
}