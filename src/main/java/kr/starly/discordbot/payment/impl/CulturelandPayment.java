package kr.starly.discordbot.payment.impl;

import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.entity.CouponState;
import kr.starly.discordbot.payment.entity.Payment;
import kr.starly.discordbot.payment.enums.PaymentMethod;
import kr.starly.discordbot.product.entity.Product;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class CulturelandPayment extends Payment {

    @NotNull private final String pinNumber;

    public CulturelandPayment(
            @NotNull Product product, @NotNull long requestedBy,
            @NotNull String pinNumber,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon
    ) {
        this(UUID.randomUUID(), product, requestedBy, pinNumber, usedPoint, usedCoupon);
    }

    public CulturelandPayment(
            @NotNull UUID paymentId, @NotNull Product product, @NotNull long requestedBy,
            @NotNull String pinNumber,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon
    ) {
        super(paymentId, requestedBy, PaymentMethod.CULTURELAND, usedPoint, usedCoupon, product);

        this.pinNumber = pinNumber;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("pinNumber", pinNumber);

        return document;
    }

    public static CulturelandPayment deserialize(Document document) {
        UUID paymentId = UUID.fromString(document.getString("paymentId"));
        Product product = Product.deserialize(document.get("product", Document.class));
        Long requestedBy = document.getLong("requestedBy");
        Integer usedPoint = document.getInteger("usedPoint");
        CouponState usedCoupon = CouponState.deserialize(document.get("usedCoupon", Document.class));

        String pinNumber = document.getString("pinNumber");
        return new CulturelandPayment(paymentId, product, requestedBy, pinNumber, usedPoint, usedCoupon);
    }
}