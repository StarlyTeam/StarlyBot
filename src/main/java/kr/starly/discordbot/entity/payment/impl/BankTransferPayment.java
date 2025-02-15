package kr.starly.discordbot.entity.payment.impl;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.enums.PaymentMethod;
import kr.starly.discordbot.entity.product.Product;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;

@Getter
public class BankTransferPayment extends Payment {

    @NotNull private final String depositor;

    public BankTransferPayment(
            @NotNull Product product, @NotNull long requestedBy,
            @NotNull String depositor,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon
    ) {
        this(UUID.randomUUID(), product, requestedBy, depositor, usedPoint, usedCoupon);
    }

    public BankTransferPayment(
            @NotNull UUID paymentId, @NotNull Product product, @NotNull long requestedBy,
            @NotNull String depositor,
            @NotNull Integer usedPoint, @Nullable Coupon usedCoupon
    ) {
        super(paymentId, requestedBy, PaymentMethod.BANK_TRANSFER, usedPoint, usedCoupon, product);

        this.depositor = depositor;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("depositor", depositor);

        return document;
    }

    public static BankTransferPayment deserialize(Document document) {
        if (document == null) return null;

        UUID paymentId = UUID.fromString(document.getString("paymentId"));
        Product product = Product.deserialize(document.get("product", Document.class));
        Long requestedBy = document.getLong("requestedBy");
        Integer usedPoint = document.getInteger("usedPoint");
        CouponState usedCoupon = CouponState.deserialize(document.get("usedCoupon", Document.class));

        String depositor = document.getString("depositor");
        BankTransferPayment payment = new BankTransferPayment(paymentId, product, requestedBy, depositor, usedPoint, usedCoupon);

        boolean accepted = document.getBoolean("accepted");
        payment.updateAccepted(accepted);
        Date approvedAt = document.getDate("approvedAt");
        payment.updateApprovedAt(approvedAt);
        Date refundedAt = document.getDate("refundedAt");
        payment.updateRefundedAt(refundedAt);

        return payment;
    }
}