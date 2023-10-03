package kr.starly.discordbot.command.slash.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.command.slash.BotSlashCommand;
import kr.starly.discordbot.command.slash.DiscordSlashCommand;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.util.PermissionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

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
    private final String ACCOUNT_ID = configProvider.getString("ACCOUNT_ID");
    private final String API_TOKEN = configProvider.getString("API_TOKEN");
    private final Color EMBED_COLOR = Color.decode(configProvider.getString("EMBED_COLOR"));
    private final Color EMBED_COLOR_SUCCESS = Color.decode(configProvider.getString("EMBED_COLOR_SUCCESS"));
    private final Color EMBED_COLOR_ERROR = Color.decode(configProvider.getString("EMBED_COLOR_ERROR"));

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        if (!PermissionUtil.hasPermission(event.getMember(), Permission.ADMINISTRATOR)) {
            PermissionUtil.sendPermissionError(event.getChannel());
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

                try {
                    final String boundary = "A1B2C3";
                    final String crlf = "\r\n";

                    URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/images/v1");

                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);

                    OutputStream httpConnOutputStream = conn.getOutputStream();
                    DataOutputStream request = new DataOutputStream(httpConnOutputStream);

                    request.writeBytes("--" + boundary + crlf);
                    request.writeBytes("Content-Disposition: form-data; name=\"url\"" + crlf);
                    request.writeBytes(crlf);
                    request.writeBytes(image.getUrl() + crlf);
                    request.writeBytes("--" + boundary + "--");
                    request.flush();
                    request.close();

                    int status = conn.getResponseCode();
                    if (status == HttpsURLConnection.HTTP_OK) {
                        InputStream is;
                        if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                            is = conn.getInputStream();
                        } else {
                            is = conn.getErrorStream();
                        }

                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        JsonObject jsonObject = JsonParser.parseString(br.lines().collect(Collectors.joining(""))).getAsJsonObject();

                        JsonArray errors = jsonObject.get("errors").getAsJsonArray();
                        if (!errors.isEmpty()) {
                            MessageEmbed messageEmbed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1141623256558866482> 오류 | 요청 실패 <a:loading:1141623256558866482>")
                                    .setDescription("**이미지를 업로드하지 못했습니다.**")
                                    .build();
                            event.replyEmbeds(messageEmbed).queue();
                            return;
                        }

                        JsonObject result = jsonObject.get("result").getAsJsonObject();
                        JsonArray variants = result.getAsJsonArray("variants");

                        MessageEmbed messageEmbed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_SUCCESS)
                                .setTitle("<a:success:1141625729386287206> 업로드 완료 | 이미지 <a:success:1141625729386287206>")
                                .setDescription("**성공적으로 이미지 업로드했습니다.**\n\n```" + variants.get(0).getAsString() + "```")
                                .build();
                        event.replyEmbeds(messageEmbed).queue();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지를 업로드하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                }
            }

            case "삭제" -> {
                String imageId = event.getOption("아이디").getAsString();

                try {
                    URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/images/v1/" + imageId);

                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setRequestMethod("DELETE");
                    conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);

                    int status = conn.getResponseCode();
                    if (status == HttpsURLConnection.HTTP_OK) {
                        InputStream is;
                        if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                            is = conn.getInputStream();
                        } else {
                            is = conn.getErrorStream();
                        }

                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        JsonObject jsonObject = JsonParser.parseString(br.lines().collect(Collectors.joining(""))).getAsJsonObject();

                        JsonArray errors = jsonObject.get("errors").getAsJsonArray();
                        if (!errors.isEmpty()) {
                            MessageEmbed messageEmbed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1141623256558866482> 오류 | 요청 실패 <a:loading:1141623256558866482>")
                                    .setDescription("**이미지를 삭제하지 못했습니다.**")
                                    .build();
                            event.replyEmbeds(messageEmbed).queue();
                            return;
                        }

                        MessageEmbed messageEmbed = new EmbedBuilder()
                                .setColor(EMBED_COLOR_SUCCESS)
                                .setTitle("<a:success:1141625729386287206> 삭제 완료 | 이미지 <a:success:1141625729386287206>")
                                .setDescription("**성공적으로 이미지를 삭제했습니다.**")
                                .build();
                        event.replyEmbeds(messageEmbed).queue();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지를 삭제하지 못했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                }
            }

            case "목록" -> {
                try {
                    URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/images/v2");

                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);

                    int status = conn.getResponseCode();
                    if (status == HttpsURLConnection.HTTP_OK) {
                        InputStream is;
                        if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                            is = conn.getInputStream();
                        } else {
                            is = conn.getErrorStream();
                        }

                        BufferedReader br = new BufferedReader(new InputStreamReader(is));
                        JsonObject jsonObject = JsonParser.parseString(br.lines().collect(Collectors.joining(""))).getAsJsonObject();

                        JsonArray errors = jsonObject.get("errors").getAsJsonArray();
                        if (!errors.isEmpty()) {
                            MessageEmbed messageEmbed = new EmbedBuilder()
                                    .setColor(EMBED_COLOR_ERROR)
                                    .setTitle("<a:loading:1141623256558866482> 오류 | 요청 실패 <a:loading:1141623256558866482>")
                                    .setDescription("**이미지 목록을 불러오지 못했습니다.**")
                                    .build();
                            event.replyEmbeds(messageEmbed).queue();
                            return;
                        }

                        JsonArray images = jsonObject.get("result").getAsJsonObject().get("images").getAsJsonArray();

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
                } catch (IOException ex) {
                    ex.printStackTrace();

                    MessageEmbed messageEmbed = new EmbedBuilder()
                            .setColor(EMBED_COLOR_ERROR)
                            .setTitle("<a:loading:1141623256558866482> 오류 | 내부 프로세스 <a:loading:1141623256558866482>")
                            .setDescription("**이미지 목록을 불러오지 못했습니다.**")
                            .build();
                    event.replyEmbeds(messageEmbed).queue();
                }
            }
        }
    }
}