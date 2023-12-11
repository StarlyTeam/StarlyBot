package kr.starly.discordbot.command.slash.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.external.CFImagesUtil;
import kr.starly.discordbot.util.security.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.Color;
import java.io.IOException;

@BotSlashCommand(
        command = "이미지",
        description = "이미지를 관리합니다.",
        subcommands = {
                @BotSlashCommand.SubCommand(
                        name = "업로드",
                        description = "이미지를 업로드합니다.",
                        optionName = {"이미지"},
                        optionType = {OptionType.ATTACHMENT},
                        optionDescription = {"이미지를 업로드하세요."},
                        optionRequired = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "삭제",
                        description = "이미지를 삭제합니다.",
                        optionName = {"아이디"},
                        optionType = {OptionType.STRING},
                        optionDescription = {"삭제할 이미지의 아이디를 입력하세요."},
                        optionRequired = {true}
                ),
                @BotSlashCommand.SubCommand(
                        name = "목록",
                        description = "이미지 목록을 확인합니다."
                )
        }
)
public class ImageUploadCommand implements DiscordSlashCommand {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event);
            return;
        }

        String subCommand = event.getSubcommandName();
        switch (subCommand) {
            case "업로드" -> {
                Message.Attachment image = event.getOption("이미지").getAsAttachment();
                if (!image.isImage()) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 잘못된 입력 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지 파일만 업로드할 수 있습니다. (jpg, jpeg, webp, png, gif)**")
                            .build();
                    event.replyEmbeds(embed).queue();
                }

                String imageUrl;
                try {
                    imageUrl = CFImagesUtil.uploadImage(image.getUrl());
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 이미지 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지를 업로드하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                if (imageUrl == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 이미지 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지를 업로드하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1168266537262657626> 성공 | 이미지 <a:success:1168266537262657626>")
                        .setDescription("> **성공적으로 이미지를 업로드했습니다.**\n\n```" + imageUrl + "```")
                        .build();
                event.replyEmbeds(embed).queue();
            }

            case "삭제" -> {
                String imageId = event.getOption("아이디").getAsString();

                try {
                    CFImagesUtil.deleteImage(imageId);

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1168266537262657626> 삭제 완료 | 이미지 <a:success:1168266537262657626>")
                            .setDescription("> **성공적으로 이미지를 삭제했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 이미지 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지를 삭제하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                }
            }

            case "목록" -> {
                JsonArray images;
                try {
                    images = CFImagesUtil.listImage();
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 이미지 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지 목록을 불러오지 못했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                if (images == null) {
                    MessageEmbed embed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1168266572847128709> 오류 | 이미지 <a:loading:1168266572847128709>")
                            .setDescription("> **이미지 목록을 불러오지 못했습니다.**")
                            .build();
                    event.replyEmbeds(embed).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                images.forEach(image -> {
                    JsonObject imageData = image.getAsJsonObject();
                    String filename = imageData.get("filename").getAsString();
                    String id = imageData.get("id").getAsString();

                    String imageLink = "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/" + id + "/public";

                    sb.append("> **[바로가기](" + imageLink + ")")
                      .append(" : (" + filename + ")**")
                      .append("\n");
                });

                MessageEmbed embed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1168266572847128709> 목록 | 이미지 <a:loading:1168266572847128709>")
                        .setDescription(sb)
                        .build();
                event.replyEmbeds(embed).queue();
            }
        }
    }
}