package kr.starly.discordbot.entity.payment;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.enums.PaymentMethod;
import kr.starly.discordbot.entity.payment.impl.BankTransferPayment;
import kr.starly.discordbot.entity.payment.impl.CreditCardPayment;
import kr.starly.discordbot.entity.payment.impl.CulturelandPayment;
import kr.starly.discordbot.entity.product.Product;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;

@Getter
public class Payment {

    @NotNull private final UUID paymentId;

    @NotNull private final PaymentMethod method;
    @NotNull private final Integer usedPoint;
    @Nullable private final CouponState usedCoupon;
    @NotNull private final Product product;
    @NotNull private final Long requestedBy;

    @NotNull private boolean isAccepted;
    @Nullable private Date approvedAt;
    @Nullable private Date refundedAt;

    public Payment(
            @NotNull UUID paymentId, @NotNull long requestedBy,
            @NotNull PaymentMethod method, @NotNull int usedPoint, @Nullable Coupon usedCoupon, @NotNull Product product
    ) {
        this.paymentId = paymentId;
        this.method = method;
        this.usedPoint = usedPoint;
        this.usedCoupon = usedCoupon == null ? null : usedCoupon.capture();
        this.product = product;
        this.requestedBy = requestedBy;
        this.isAccepted = false;
        this.approvedAt = null;
        this.refundedAt = null;
    }

    public boolean isRefunded() {
        return refundedAt != null;
    }

    public void updateAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public void updateApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
    }

    public void updateRefundedAt(Date refundedAt) {
        this.refundedAt = refundedAt;
    }

    public CreditCardPayment asCreditCard() {
        return (CreditCardPayment) this;
    }

    public BankTransferPayment asBankTransfer() {
        return (BankTransferPayment) this;
    }

    public CulturelandPayment asCultureland() {
        return (CulturelandPayment) this;
    }

    public int getFinalPrice() {
        int price = product.getPrice();

        if (usedCoupon != null) {
            price = usedCoupon.getDiscount().computeFinalPrice(price);
        }

        if (method == PaymentMethod.CULTURELAND) {
            price = (int) Math.ceil(price * 1.12);
        }

        return price - usedPoint;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("paymentId", paymentId.toString());
        document.put("method", method.name());
        document.put("usedPoint", usedPoint);
        document.put("usedCoupon", usedCoupon == null ? null : usedCoupon.serialize());
        document.put("product", product.serialize());
        document.put("requestedBy", requestedBy);
        document.put("accepted", isAccepted);
        document.put("approvedAt", approvedAt);
        document.put("refundedAt", refundedAt);

        return document;
    }

    public static Payment deserialize(Document document) {
        if (document == null) return null;

        PaymentMethod method = PaymentMethod.valueOf(document.getString("method"));
        return switch (method) {
            case CREDIT_CARD -> CreditCardPayment.deserialize(document);
            case BANK_TRANSFER -> BankTransferPayment.deserialize(document);
            case CULTURELAND -> CulturelandPayment.deserialize(document);

            case NONE -> {
                UUID paymentId = UUID.fromString(document.getString("paymentId"));
                int usedPoint = document.getInteger("usedPoint");
                CouponState usedCoupon = CouponState.deserialize(document.get("usedCoupon", Document.class));
                Product product = Product.deserialize(document.get("product", Document.class));
                long requestedBy = document.getLong("requestedBy");
                Payment payment = new Payment(paymentId, requestedBy, method, usedPoint, usedCoupon, product);

                boolean accepted = document.getBoolean("accepted");
                payment.updateAccepted(accepted);
                Date approvedAt = document.getDate("approvedAt");
                payment.updateApprovedAt(approvedAt);
                Date refundedAt = document.getDate("refundedAt");
                payment.updateRefundedAt(refundedAt);

                yield payment;
            }
        };
    }
}