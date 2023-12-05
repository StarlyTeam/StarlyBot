package kr.starly.discordbot.util.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class TossPaymentsUtil {

    private TossPaymentsUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String TOSSPAYMENTS_SECRET_KEY = configProvider.getString("TOSSPAYMENTS_SECRET_KEY");

    public static void request(CreditCardPayment payment) throws Exception {
        int price = payment.getFinalPrice();
        String summary = payment.getProduct().getSummary();
        String orderName = summary != null ? summary : payment.getProduct()
                .asPremiumPlugin()
                .getPlugin().getENName();

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("amount", price);
        requestBody.addProperty("orderId", payment.getPaymentId().toString());
        requestBody.addProperty("orderName", orderName);
        requestBody.addProperty("cardNumber", payment.getCardNumber());
        requestBody.addProperty("cardExpirationYear", payment.getCardExpirationYear());
        requestBody.addProperty("cardExpirationMonth", payment.getCardExpirationMonth());
        requestBody.addProperty("cardInstallmentPlan", payment.getCardInstallmentPlan());
        requestBody.addProperty("customerIdentityNumber", payment.getCustomerBirthdate());
        requestBody.addProperty("customerEmail", payment.getCustomerEmail());
        requestBody.addProperty("customerName", payment.getCustomerName());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tosspayments.com/v1/payments/key-in"))
                .header("Authorization", createAuthorization())
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();

        String errCode = responseBody.has("code") ? responseBody.get("code").getAsString() : null;
        String errMessage = responseBody.has("message") ? responseBody.get("message").getAsString() : null;
        if (errCode != null || errMessage != null) {
            throw new IllegalStateException("Request is not successful. [" + errCode + ", " + errMessage + "]");
        } else if (response.statusCode() != 200) {
            throw new IllegalStateException("Request is not successful. [" + response.statusCode() + "]");
        }

        String status = responseBody.get("status").getAsString();
        if (!status.equals("DONE")) {
            throw new IllegalStateException("Payment status is not DONE. [" + status + "]");
        }

        int paidAmount = responseBody.get("totalAmount").getAsInt();
        if (paidAmount != price) {
            throw new IllegalStateException("Paid amount is not equal to final price. [" + paidAmount + ", " + price + "]");
        }

        String approvedAtStr = responseBody.get("approvedAt").getAsString();
        Date approvedAt = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX")
                .parse(approvedAtStr);

        payment.updateAccepted(true);
        payment.updateApprovedAt(approvedAt);
        payment.updateResponse(responseBody);
    }

    public static void refund(CreditCardPayment payment, String cancelReason) throws Exception {
        String paymentKey = payment.getPaymentKey();

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("cancelReason", cancelReason);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tosspayments.com/v1/payments/" + paymentKey + "/cancel"))
                .header("Authorization", createAuthorization())
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();

        String errCode = responseBody.has("code") ? responseBody.get("code").getAsString() : null;
        String errMessage = responseBody.has("message") ? responseBody.get("message").getAsString() : null;
        if (errCode != null || errMessage != null) {
            throw new IllegalStateException("Request is not successful. [" + errCode + ", " + errMessage + "]");
        } else if (response.statusCode() != 200) {
            throw new IllegalStateException("Request is not successful. [" + response.statusCode() + "]");
        }

        payment.updateRefundedAt(new Date());
    }

    private static String createAuthorization() {
        return "Basic " + Base64.getEncoder().encodeToString((TOSSPAYMENTS_SECRET_KEY + ":").getBytes());
    }
}