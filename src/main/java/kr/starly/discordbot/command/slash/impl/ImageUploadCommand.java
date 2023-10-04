package kr.starly.discordbot.command.slash.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.CFImagesUtil;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.io.*;

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
            PermissionUtil.sendPermissionError(event.getChannel());
            return;
        }

        String subCommand = event.getSubcommandName();
        switch (subCommand) {
            case "업로드" -> {
                Message.Attachment image = event.getOption("이미지").getAsAttachment();
                if (!image.isImage()) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 잘못된 입력 <a:loading:1141623256558866482>")
                            .setDescription("**이미지 파일만 업로드할 수 있습니다. (jpg, jpeg, webp, png, gif)**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                }

                String imageUrl;
                try {
                    imageUrl = CFImagesUtil.uploadImage(image.getUrl());
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지를 업로드하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                if (imageUrl == null) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 요청 실패 <a:loading:1141623256558866482>")
                            .setDescription("**이미지를 업로드하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR_SUCCESS)
                        .setTitle("<a:success:1141625729386287206> 업로드 완료 | 이미지 <a:success:1141625729386287206>")
                        .setDescription("**성공적으로 이미지를 업로드했습니다.**\n\n```" + imageUrl + "```")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }

            case "삭제" -> {
                String imageId = event.getOption("아이디").getAsString();

                try {
                    CFImagesUtil.deleteImage(imageId);

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_SUCCESS)
                            .setTitle("<a:success:1141625729386287206> 삭제 완료 | 이미지 <a:success:1141625729386287206>")
                            .setDescription("**성공적으로 이미지를 삭제했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지를 삭제하지 못 했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                }
            }

            case "목록" -> {
                JsonArray images;
                try {
                    images = CFImagesUtil.listImage();
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지 목록을 불러오지 못 했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                if (images == null) {
                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지 목록을 불러오지 못 했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                    return;
                }

                StringBuilder sb = new StringBuilder();
                images.forEach(image -> {
                    JsonObject imageData = image.getAsJsonObject();
                    String filename = imageData.get("filename").getAsString();
                    String id = imageData.get("id").getAsString();

                    sb.append(id + " (" + filename + ")\n");
                });

                MessageEmbed messageEmbed = new EmbedBuilder()
                        .setColor(EMBED_COLOR)
                        .setTitle("<a:loading:1141623256558866482> 목록 | 이미지 <a:loading:1141623256558866482>")
                        .setDescription("```" + sb + "```")
                        .build();
                event.replyEmbeds(messageEmbed).queue();
            }
        }
    }
}