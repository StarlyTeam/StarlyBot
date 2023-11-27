package kr.starly.discordbot.http.handler.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.service.PluginService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class APIv1Handler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) return;

        String[] pathSegments = exchange.getRequestURI().getPath().split("/");
        String apiPath = String.join("/", Arrays.copyOfRange(pathSegments, 3, pathSegments.length));
        if (apiPath.equals("plugins")) {
            JsonObject responseBody = new JsonObject();

            try {
                JsonObject dataObject = new JsonObject();

                PluginService pluginService = DatabaseManager.getPluginService();
                List<Plugin> plugins = pluginService.getAllData();
                plugins.forEach(plugin -> dataObject.add(plugin.getENName(), toJSONObject(plugin)));

                responseBody.addProperty("status", "DONE");
                responseBody.add("data", dataObject);
            } catch (Exception ex) {
                ex.printStackTrace();

                responseBody.addProperty("status", "ERROR");
                responseBody.addProperty("error", ex.getMessage());
            }

            if (responseBody.get("status").getAsString().equals("DONE")) {
                sendResponse(exchange, 200, responseBody);
            } else {
                sendResponse(exchange, 500, responseBody);
            }
        } else if (apiPath.startsWith("plugins/")) {
            String pluginENName = apiPath.split("/")[1];

            JsonObject responseBody = new JsonObject();

            try {
                PluginService pluginService = DatabaseManager.getPluginService();
                Plugin plugin = pluginService.getDataByENName(pluginENName);

                if (plugin == null) {
                    responseBody.addProperty("status", "ERROR");
                    responseBody.addProperty("error", "존재하지 않는 플러그인입니다.");
                } else {
                    responseBody.addProperty("status", "DONE");
                    responseBody.add("data", toJSONObject(plugin));
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                responseBody.addProperty("status", "ERROR");
                responseBody.addProperty("error", ex.getMessage());
            }

            if (responseBody.get("status").getAsString().equals("DONE")) {
                sendResponse(exchange, 200, responseBody);
            } else {
                sendResponse(exchange, 500, responseBody);
            }
        } else {
            JsonObject responseBody = new JsonObject();
            responseBody.addProperty("status", "ERROR");
            responseBody.addProperty("error", "존재하지 않는 API입니다.");
        }
    }

    private void sendResponse(HttpExchange exchange, int rCode, JsonObject bodyObject) throws IOException {
        String bodyStr = bodyObject.toString();
        byte[] bodyBytes = bodyStr.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(rCode, bodyBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bodyBytes);
        }
    }

    private JsonObject toJSONObject(Plugin plugin) {
        JsonObject pluginObject = new JsonObject();
        pluginObject.addProperty("ENName", plugin.getENName());
        pluginObject.addProperty("KRName", plugin.getKRName());
        pluginObject.addProperty("emoji", plugin.getEmoji());
        pluginObject.addProperty("wikiUrl", plugin.getWikiUrl());
        pluginObject.addProperty("iconUrl", plugin.getIconUrl());
        pluginObject.addProperty("videoUrl", plugin.getVideoUrl());
        pluginObject.addProperty("gifUrl", plugin.getGifUrl());
        pluginObject.addProperty("version", plugin.getVersion());
        pluginObject.addProperty("price", plugin.getPrice());

        JsonArray dependencyArray = new JsonArray();
        plugin.getDependency().forEach(dependencyArray::add);
        pluginObject.add("dependency", dependencyArray);

        JsonArray managerArray = new JsonArray();
        JDA jda = DiscordBotManager.getInstance().getJda();
        plugin.getManager().forEach(managerId -> {
            JsonObject managerObject = new JsonObject();
            User user = jda.getUserById(managerId);
            managerObject.addProperty("id", user.getIdLong());
            managerObject.addProperty("name", user.getName());
            managerObject.addProperty("avatarUrl", user.getAvatarUrl());
            managerArray.add(managerObject);
        });
        pluginObject.add("manager", managerArray);

        return pluginObject;
    }
}