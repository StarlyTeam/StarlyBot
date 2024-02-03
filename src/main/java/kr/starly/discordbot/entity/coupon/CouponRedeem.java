package kr.starly.discordbot.entity.coupon;

import kr.starly.discordbot.configuration.DatabaseManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class CouponRedeem {

    @NotNull
    private final UUID redeemId;

    @Nullable
    private final Coupon coupon;
    @NotNull
    private final CouponState couponState;

    private final long requestedBy;
    @NotNull
    private final Date requestedAt;
    
    public Document serialize() {
        Document document = new Document();
        document.put("redeemId", redeemId.toString());
        document.put("couponState", couponState.serialize());
        document.put("coupon", coupon.serialize());
        document.put("requestedBy", requestedAt);
        document.put("requestedAt", requestedAt);
        
        return document;
    }


    public static CouponRedeem deserialize(Document document) {
        if (document == null) return null;

        UUID redeemId = UUID.fromString(document.getString("redeemId"));
        CouponState couponState = CouponState.deserialize(document.get("couponState", Document.class));
        Coupon coupon = DatabaseManager.getCouponService().getData(couponState.getCode());
        long requestedBy = document.getLong("requestedBy");
        Date requestedAt = document.getDate("requestedAt");

        return new CouponRedeem(redeemId, coupon, couponState, requestedBy, requestedAt);
    }
}