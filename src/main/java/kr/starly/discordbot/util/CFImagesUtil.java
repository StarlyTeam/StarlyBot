package kr.starly.discordbot.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.configuration.ConfigProvider;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

public class CFImagesUtil {

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String ACCOUNT_ID = configProvider.getString("ACCOUNT_ID");
    private static final String API_TOKEN = configProvider.getString("API_TOKEN");

    public static String uploadImage(String imageUrl) throws IOException {
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
        request.writeBytes(imageUrl + crlf);
        request.writeBytes("--" + boundary + "--");
        request.flush();
        request.close();

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
            System.out.println(errors);
            return null;
        }

        JsonObject result = jsonObject.get("result").getAsJsonObject();
        JsonArray variants = result.getAsJsonArray("variants");
        return variants.get(0).getAsString();
    }

    public static void deleteImage(String imageId) throws IOException {
        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/images/v1/" + imageId);

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);

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
            System.out.println(errors);
        }
    }

    public static JsonArray listImage() throws IOException {
        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/images/v2");

        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setDoInput(true);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + API_TOKEN);

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
            System.out.println(errors);
            return null;
        }

        return jsonObject.get("result").getAsJsonObject().get("images").getAsJsonArray();
    }
}
