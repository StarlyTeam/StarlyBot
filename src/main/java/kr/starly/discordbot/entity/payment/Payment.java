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

    @NotNull private final UUID paymentId; // 거래 고유번호

    @NotNull private final PaymentMethod method; // 결제 수단
    @NotNull private final Integer usedPoint; // 사용된 포인트
    @Nullable private final CouponState usedCoupon; // 사용된 쿠폰
    @NotNull private final Product product; // 구매한 상품
    @NotNull private final Long requestedBy; // 구매자

    @NotNull private boolean accepted; // 구매 승인 여부
    @Nullable private Date approvedAt; // 구매 완료 시각

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
        this.accepted = false;
        this.approvedAt = null;
    }

    public void updateAccepted(boolean isAccepted) {
        this.accepted = isAccepted;
    }

    public void updateApprovedAt(Date approvedAt) {
        this.approvedAt = approvedAt;
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

    public Document serialize() {
        Document document = new Document();
        document.put("paymentId", paymentId.toString());
        document.put("method", method.name());
        document.put("usedPoint", usedPoint);
        document.put("usedCoupon", usedCoupon == null ? null : usedCoupon.serialize());
        document.put("product", product.serialize());
        document.put("requestedBy", requestedBy);
        document.put("accepted", accepted);
        document.put("approvedAt", approvedAt);

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
                CouponState usedCoupon = CouponState.deserialize((Document) document.get("usedCoupon"));
                Product product = Product.deserialize((Document) document.get("product"));
                long requestedBy = document.getLong("requestedBy");

                Payment payment = new Payment(paymentId, requestedBy, method, usedPoint, usedCoupon, product);

                boolean accepted = document.getBoolean("accepted");
                Date approvedAt = document.getDate("approvedAt");
                payment.updateAccepted(accepted);
                payment.updateApprovedAt(approvedAt);

                yield payment;
            }
        };
    }
}