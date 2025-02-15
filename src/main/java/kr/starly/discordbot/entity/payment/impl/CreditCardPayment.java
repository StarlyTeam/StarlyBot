package kr.starly.discordbot.entity.payment.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.enums.PaymentMethod;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.util.security.AESUtil;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Getter
public class CreditCardPayment extends Payment {

    @NotNull private final String cardNumber;
    @NotNull private final String cardExpirationYear;
    @NotNull private final String cardExpirationMonth;
    @NotNull private final Integer cardInstallmentPlan;

    @NotNull private final String customerBirthdate;
    @NotNull private final String customerEmail;
    @NotNull private final String customerName;
    @Nullable private final SecretKey key;
    @Nullable private final IvParameterSpec iv;

    @Nullable private JsonObject responseBody;
    @Nullable private String paymentKey;
    @Nullable private String maskedCardNumber;
    @Nullable private String receiptUrl;

    public CreditCardPayment(
            @NotNull Product product, @NotNull long requestedBy,
            @NotNull String cardNumber, @NotNull String cardExpirationYear, @NotNull String cardExpirationMonth, @NotNull Integer cardInstallmentPlan,
            @NotNull String customerBirthdate, @NotNull String customerEmail, @NotNull String customerName,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon,
            @Nullable SecretKey key, @Nullable IvParameterSpec iv
    ) {
        this(UUID.randomUUID(), product, requestedBy, cardNumber, cardExpirationYear, cardExpirationMonth, cardInstallmentPlan, customerBirthdate, customerEmail, customerName, usedPoint, usedCoupon, key, iv, null, null, null, null);
    }

    public CreditCardPayment(
            @NotNull UUID paymentId, @NotNull Product product, @NotNull long requestedBy,
            @NotNull String cardNumber, @NotNull String cardExpirationYear, @NotNull String cardExpirationMonth, @NotNull Integer cardInstallmentPlan,
            @NotNull String customerBirthdate, @NotNull String customerEmail, @NotNull String customerName,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon,
            @Nullable SecretKey key, @Nullable IvParameterSpec iv,
            @Nullable JsonObject responseBody, @Nullable String paymentKey, @Nullable String maskedCardNumber, @Nullable String receiptUrl
    ) {
        super(paymentId, requestedBy, PaymentMethod.CREDIT_CARD, usedPoint, usedCoupon, product);

        this.cardNumber = cardNumber;
        this.cardExpirationYear = cardExpirationYear;
        this.cardExpirationMonth = cardExpirationMonth;
        this.cardInstallmentPlan = cardInstallmentPlan;

        this.customerBirthdate = customerBirthdate;
        this.customerEmail = customerEmail;
        this.customerName = customerName;

        this.key = key;
        this.iv = iv;

        this.responseBody = responseBody;
        this.paymentKey = paymentKey;
        this.maskedCardNumber = maskedCardNumber;
        this.receiptUrl = receiptUrl;
    }

    public void updateResponse(JsonObject responseBody) {
        this.responseBody = responseBody;

        if (responseBody != null) {
            this.paymentKey = responseBody
                    .get("paymentKey").getAsString();
            this.maskedCardNumber = responseBody
                    .get("card").getAsJsonObject()
                    .get("number").getAsString();
            this.receiptUrl = responseBody
                    .get("receipt").getAsJsonObject()
                    .get("url").getAsString();
        }
    }

    @Override
    public Document serialize() {
        try {
            Document document = super.serialize();
            document.put("cardNumber", AESUtil.encode(cardNumber, key, iv));
            document.put("cardExpirationYear", AESUtil.encode(cardExpirationYear, key, iv));
            document.put("cardExpirationMonth", AESUtil.encode(cardExpirationMonth, key, iv));
            document.put("cardInstallmentPlan", AESUtil.encode(cardInstallmentPlan.toString(), key, iv));
            document.put("customerBirthdate", AESUtil.encode(customerBirthdate, key, iv));
            document.put("customerEmail", customerEmail);
            document.put("customerName", customerName);
            document.put("secureKey", key == null ? null : Base64.getEncoder().encodeToString(key.getEncoded()));
            document.put("secureIV", iv == null ? null : Base64.getEncoder().encodeToString(iv.getIV()));
            document.put("responseBody", responseBody != null ? responseBody.toString() : null);
            document.put("paymentKey", paymentKey);
            document.put("maskedCardNumber", maskedCardNumber);
            document.put("receiptUrl", receiptUrl);

            return document;
        } catch (Exception ex) {
            ex.printStackTrace();

            return null;
        }
    }

    public static CreditCardPayment deserialize(Document document) {
        if (document == null) return null;

        UUID paymentId = UUID.fromString(document.getString("paymentId"));
        Product product = Product.deserialize(document.get("product", Document.class));
        Long requestedBy = document.getLong("requestedBy");
        Integer usedPoint = document.getInteger("usedPoint");
        CouponState usedCoupon = CouponState.deserialize(document.get("usedCoupon", Document.class));

        String rowSecureKey = document.getString("secureKey");
        String rowSecureIV = document.getString("secureIV");

        SecretKey key = rowSecureKey == null ? null : new SecretKeySpec(Base64.getDecoder().decode(rowSecureKey), "AES");
        IvParameterSpec iv = rowSecureIV == null ? null : new IvParameterSpec(Base64.getDecoder().decode(rowSecureIV));

        String cardNumber, cardExpirationYear, cardExpirationMonth; int cardInstallmentPlan;
        cardNumber = AESUtil.decode(document.getString("cardNumber"), key, iv);
        cardExpirationYear = AESUtil.decode(document.getString("cardExpirationYear"), key, iv);
        cardExpirationMonth = AESUtil.decode(document.getString("cardExpirationMonth"), key, iv);
        cardInstallmentPlan = Integer.parseInt(AESUtil.decode(document.getString("cardInstallmentPlan"), key, iv));

        String customerBirthdate = AESUtil.decode(document.getString("customerBirthdate"), key, iv);
        String customerEmail = document.getString("customerEmail");
        String customerName = document.getString("customerName");

        JsonObject responseBody = JsonParser.parseString(document.getString("responseBody")).getAsJsonObject();
        String paymentKey = document.getString("paymentKey");
        String maskedCardNumber = document.getString("maskedCardNumber");
        String receiptUrl = document.getString("receiptUrl");
        CreditCardPayment payment = new CreditCardPayment(
                paymentId, product, requestedBy,
                cardNumber, cardExpirationYear, cardExpirationMonth, cardInstallmentPlan,
                customerBirthdate, customerEmail, customerName,
                usedPoint, usedCoupon,
                key, iv,
                responseBody, paymentKey, maskedCardNumber, receiptUrl
        );

        boolean accepted = document.getBoolean("accepted");
        payment.updateAccepted(accepted);
        Date approvedAt = document.getDate("approvedAt");
        payment.updateApprovedAt(approvedAt);
        Date refundedAt = document.getDate("refundedAt");
        payment.updateRefundedAt(refundedAt);

        return payment;
    }
}