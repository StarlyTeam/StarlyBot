package kr.starly.discordbot.payment.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.entity.CouponState;
import kr.starly.discordbot.payment.entity.Payment;
import kr.starly.discordbot.payment.enums.PaymentMethod;
import kr.starly.discordbot.product.entity.Product;
import kr.starly.discordbot.util.security.AESUtil;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
public class CreditCardPayment extends Payment {

    // 카드 정보
    @NotNull private final String cardNumber;
    @NotNull private final String cardExpirationYear;
    @NotNull private final String cardExpirationMonth;
    @NotNull private final Integer cardInstallmentPlan;

    // 결제자 정보
    @NotNull private final String customerBirthdate;
    @NotNull private final String customerEmail;
    @NotNull private final String customerName;

    // 데이터 암호화를 위한 salt
    @NotNull private final String secureSalt;

    // 결제 완료 후, 토스페이먼츠로부터 받은 응답
    @Nullable private JsonObject responseBody;
    @Nullable private String maskedCardNumber;
    @Nullable private String receiptUrl;

    public CreditCardPayment(
            @NotNull Product product, @NotNull long requestedBy,
            @NotNull String cardNumber, @NotNull String cardExpirationYear, @NotNull String cardExpirationMonth, @NotNull Integer cardInstallmentPlan,
            @NotNull String customerBirthdate, @NotNull String customerEmail, @NotNull String customerName,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon,
            @NotNull String secureSalt
    ) {
        this(UUID.randomUUID(), product, requestedBy, cardNumber, cardExpirationYear, cardExpirationMonth, cardInstallmentPlan, customerBirthdate, customerEmail, customerName, usedPoint, usedCoupon, secureSalt, null, null, null);
    }

    public CreditCardPayment(
            @NotNull UUID paymentId, @NotNull Product product, @NotNull long requestedBy,
            @NotNull String cardNumber, @NotNull String cardExpirationYear, @NotNull String cardExpirationMonth, @NotNull Integer cardInstallmentPlan,
            @NotNull String customerBirthdate, @NotNull String customerEmail, @NotNull String customerName,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon,
            @NotNull String secureSalt,
            @Nullable JsonObject responseBody, @Nullable String maskedCardNumber, @Nullable String receiptUrl
    ) {
        super(paymentId, requestedBy, PaymentMethod.CREDIT_CARD, usedPoint, usedCoupon, product);

        this.cardNumber = cardNumber;
        this.cardExpirationYear = cardExpirationYear;
        this.cardExpirationMonth = cardExpirationMonth;
        this.cardInstallmentPlan = cardInstallmentPlan;

        this.customerBirthdate = customerBirthdate;
        this.customerEmail = customerEmail;
        this.customerName = customerName;

        this.secureSalt = secureSalt;

        this.responseBody = responseBody;
        this.maskedCardNumber = maskedCardNumber;
        this.receiptUrl = receiptUrl;
    }

    public void updateResponse(JsonObject responseBody) {
        this.responseBody = responseBody;
        this.maskedCardNumber = responseBody
                .get("card").getAsJsonObject()
                .get("number").getAsString();
        this.receiptUrl = responseBody
                .get("receipt").getAsJsonObject()
                .get("url").getAsString();
    }

    @Override
    public Document serialize() {
        try {
            String key = new String(
                    (secureSalt + getRequestedBy()).getBytes(),
                    0,
                    32
            );

            Document document = super.serialize();
            document.put("cardNumber", AESUtil.encode(cardNumber, key));
            document.put("cardExpirationYear", AESUtil.encode(cardExpirationYear, key));
            document.put("cardExpirationMonth", AESUtil.encode(cardExpirationMonth, key));
            document.put("cardInstallmentPlan", AESUtil.encode(cardInstallmentPlan.toString(), key));
            document.put("customerBirthdate", AESUtil.encode(customerBirthdate, key));
            document.put("customerEmail", customerEmail);
            document.put("customerName", customerName);
            document.put("secureSalt", secureSalt);
            document.put("responseBody", responseBody != null ? responseBody.toString() : null);
            document.put("maskedCardNumber", maskedCardNumber);
            document.put("receiptUrl", receiptUrl);

            return document;
        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }
    }

    public static CreditCardPayment deserialize(Document document) {
        UUID paymentId = UUID.fromString(document.getString("paymentId"));
        Product product = Product.deserialize(document.get("product", Document.class));
        Long requestedBy = document.getLong("requestedBy");
        Integer usedPoint = document.getInteger("usedPoint");
        CouponState usedCoupon = CouponState.deserialize(document.get("usedCoupon", Document.class));

        String secureSalt = document.getString("secureSalt");
        String key = new String(
                (secureSalt + requestedBy).getBytes(),
                0,
                32
        );

        String cardNumber, cardExpirationYear, cardExpirationMonth; int cardInstallmentPlan;
        cardNumber = AESUtil.decode(document.getString("cardNumber"), key);
        cardExpirationYear = AESUtil.decode(document.getString("cardExpirationYear"), key);
        cardExpirationMonth = AESUtil.decode(document.getString("cardExpirationMonth"), key);
        cardInstallmentPlan = Integer.parseInt(AESUtil.decode(document.getString("cardInstallmentPlan"), key));

        String customerBirthdate = AESUtil.decode(document.getString("customerBirthdate"), key);
        String customerEmail = document.getString("customerEmail");
        String customerName = document.getString("customerName");

        JsonObject responseBody = JsonParser.parseString(document.getString("responseBody")).getAsJsonObject();
        String maskedCardNumber = document.getString("maskedCardNumber");
        String receiptUrl = document.getString("receiptUrl");

        return new CreditCardPayment(paymentId, product, requestedBy, cardNumber, cardExpirationYear, cardExpirationMonth, cardInstallmentPlan, customerBirthdate, customerEmail, customerName, usedPoint, usedCoupon, secureSalt, responseBody, maskedCardNumber, receiptUrl);
    }
}