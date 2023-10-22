package kr.starly.discordbot.util.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.payment.enums.PaymentMethod;
import kr.starly.discordbot.payment.impl.CreditCardPayment;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public class TossPaymentsUtil {

    private TossPaymentsUtil() {}

    private static final ConfigProvider configProvider = ConfigProvider.getInstance();
    private static final String TOSSPAYMENTS_SECRET_KEY = configProvider.getString("TOSSPAYMENTS_SECRET_KEY");

    public static void request(CreditCardPayment payment) throws IOException, IllegalStateException, InterruptedException, ParseException {
        // Payment 객체 검증
        if (payment.getMethod() != PaymentMethod.CREDIT_CARD) {
            throw new UnsupportedOperationException("Unsupported PaymentMethod type.");
        }

        // 금액 산출
        int price = payment.getProduct().getPrice();

        Coupon usedCoupon = payment.getUsedCoupon();
        if (usedCoupon != null) price = usedCoupon.getDiscount().computeFinalPrice(payment.getProduct().getPrice());

        price = price - payment.getUsedPoint();

        // API 요청
        String note = payment.getProduct().getNote();
        String orderName = note != null ? note : payment.getProduct()
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
                .header("Authorization", "Basic " + Base64.getEncoder().encodeToString((TOSSPAYMENTS_SECRET_KEY + ":").getBytes()))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        JsonObject responseBody = JsonParser.parseString(response.body()).getAsJsonObject();

        // 응답 검증
        String errCode = responseBody.has("code") ? responseBody.get("code").getAsString() : null;
        String errMessage = responseBody.has("message") ? responseBody.get("message").getAsString() : null;

        if (errCode != null || errMessage != null) {
            throw new IllegalStateException("Payment request is not successful. [" + errCode + ", " + errMessage + "]");
        } else if (response.statusCode() != 200) {
            throw new IllegalStateException("Payment request is not successful. [" + response.statusCode() + "]");
        }

        // 무결성 검증
        String status = responseBody.get("status").getAsString();
        int paidAmount = responseBody.get("totalAmount").getAsInt();

        if (!status.equals("DONE")) {
            throw new IllegalStateException("Payment status is not DONE. [" + status + "]");
        } else if (paidAmount != price) {
            throw new IllegalStateException("Paid amount is not equal to final price. [" + paidAmount + ", " + price + "]");
        }

        // Payment 객체: update
        String approvedAtStr = responseBody.get("approvedAt").getAsString();
        Date approvedAt = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssX").parse(approvedAtStr);

        payment.updateAccepted(true);
        payment.updateApprovedAt(approvedAt);

        // 결과 저장
        payment.updateResponse(responseBody);
    }
}