package kr.starly.discordbot.http.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.enums.UploadStatus;
import kr.starly.discordbot.listener.plugin.PluginManagerChatInteraction;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PluginFileUploadHandler implements HttpHandler {

    private final ConfigProvider configProvider = ConfigProvider.getInstance();
    private final String EMBED_COLOR_SUCCESS = configProvider.getString("EMBED_COLOR_SUCCESS");

    @Override
    public void handle(HttpExchange exchange) {
        try {
            String providedToken = getQueryParam(exchange, "token");
            long userId = Long.parseLong(exchange.getRequestURI().getPath().split("/")[2]);
            String expectedToken = PluginManagerChatInteraction.getUserTokens().get(userId);
            if (expectedToken == null || !providedToken.equals(expectedToken)) {
                sendErrorResponse(exchange, "Invalid or expired token.");
                return;
            }

            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String htmlForm = loadAndModifyHtmlFromResources("uploadForm.html", String.valueOf(userId), providedToken);
                exchange.sendResponseHeaders(200, htmlForm.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(htmlForm.getBytes());
                }
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, Object> parsedData = parseMultipartData(exchange);
                byte[] fileData = (byte[]) parsedData.get("uploadedFile");
                String originalFileName = (String) parsedData.get("fileName");

                if (fileData != null) {
                    String outputFilePath = "plugins/" + originalFileName;
                    Files.write(Path.of(outputFilePath), fileData, StandardOpenOption.CREATE);

                    String response = "File uploaded successfully!";
                    exchange.sendResponseHeaders(200, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }

                    PluginManagerChatInteraction.setUserUploadStatus(userId, UploadStatus.FILE_UPLOADED);
                    MessageChannel channel = PluginManagerChatInteraction.getLastEvent().getChannel();
                    if (channel instanceof TextChannel) {
                        MessageEmbed successEmbed = new EmbedBuilder()
                                .setColor(Color.decode(EMBED_COLOR_SUCCESS))
                                .setTitle("<a:success:1141625729386287206> 플러그인 관리 | 스탈리 (관리자 전용) <a:success:1141625729386287206>")
                                .setDescription("> **`\uD83C\uDD99` 플러그인 파일 업로드가 완료 되었습니다. `\uD83C\uDD99`** \n" +
                                        "> **`🚫` 15초 뒤에 모든 메시지가 삭제됩니다. `🚫`** \n\n" +
                                        "─────────────────────────────────────────────────\n"
                                )
                                .setThumbnail("https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                .setFooter("이 기능은 관리자 전용입니다.", "https://imagedelivery.net/zI1a4o7oosLEca8Wq4ML6w/e7a1b4a6-854c-499b-5bb2-5737af369900/public")
                                .build();

                        channel.sendMessageEmbeds(successEmbed).queue();
                        PluginManagerChatInteraction.checkAndDeleteMessages((TextChannel) channel, userId);
                        PluginManagerChatInteraction.createPluginChannel();
                        PluginManagerChatInteraction.releaseNotice();
                    }
                } else {
                    sendErrorResponse(exchange, "No file uploaded.");
                }
            } else {
                sendErrorResponse(exchange, "Invalid request method.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadAndModifyHtmlFromResources(String fileName, String userId, String token) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        if (is == null) {
            throw new FileNotFoundException("Resource not found: " + fileName);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String htmlContent = reader.lines().collect(Collectors.joining("\n"));
            String WEB_ADDRESS = configProvider.getString("WEB_ADDRESS");
            int WEB_PORT = configProvider.getInt("WEB_PORT");
            return htmlContent.replace("URL", "http://" + WEB_ADDRESS + ":" + WEB_PORT + "/upload/" + userId + "?token=" + token);
        }
    }

    private String getQueryParam(HttpExchange exchange, String paramName) {
        URI requestedUri = exchange.getRequestURI();
        String query = requestedUri.getRawQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length > 1 && keyValue[0].equals(paramName)) {
                return keyValue[1];
            }
        }
        return null;
    }

    private Map<String, Object> parseMultipartData(HttpExchange exchange) throws IOException {
        Map<String, Object> resultMap = new HashMap<>();
        String boundary = getBoundary(exchange.getRequestHeaders().getFirst("Content-Type"));
        byte[] boundaryBytes = ("--" + boundary).getBytes();

        try (InputStream is = exchange.getRequestBody()) {
            while (true) {
                byte[] line = readLine(is);
                if (line == null || new String(line).startsWith("--" + boundary + "--")) {
                    break;
                }
                if (new String(line).startsWith("--" + boundary)) {
                    line = readLine(is);
                    String[] tokens = new String(line).split(";");
                    String fieldName = tokens[1].split("=")[1].replaceAll("\"", "").trim();
                    String fileName = null;
                    if (tokens.length > 2) {
                        fileName = tokens[2].split("=")[1].replaceAll("\"", "").trim();
                    }

                    readLine(is);
                    readLine(is);

                    ByteArrayOutputStream fileOut = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        if (endsWith(buffer, bytesRead, boundaryBytes)) {
                            fileOut.write(buffer, 0, bytesRead - boundaryBytes.length - 4);
                            break;
                        }
                        fileOut.write(buffer, 0, bytesRead);
                    }

                    if (fileName != null && fieldName.equals("uploadedFile")) {
                        byte[] fileBytes = fileOut.toByteArray();
                        resultMap.put("uploadedFile", fileBytes);
                        resultMap.put("fileName", fileName);
                    }
                }
            }
        }
        return resultMap;
    }

    private String getBoundary(String contentType) {
        for (String param : contentType.split(";")) {
            if (param.trim().startsWith("boundary=")) {
                return param.split("=")[1];
            }
        }
        return null;
    }

    private byte[] readLine(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = is.read()) != -1 && b != '\n') {
            baos.write(b);
        }
        return b == -1 && baos.size() == 0 ? null : baos.toByteArray();
    }

    private boolean endsWith(byte[] buffer, int bytesRead, byte[] boundaryBytes) {
        if (bytesRead < boundaryBytes.length) {
            return false;
        }
        for (int i = 0; i < boundaryBytes.length; i++) {
            if (buffer[bytesRead - boundaryBytes.length + i] != boundaryBytes[i]) {
                return false;
            }
        }
        return true;
    }

    private void sendErrorResponse(HttpExchange exchange, String errorMessage) throws IOException {
        exchange.sendResponseHeaders(400, errorMessage.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(errorMessage.getBytes());
        }
    }
}