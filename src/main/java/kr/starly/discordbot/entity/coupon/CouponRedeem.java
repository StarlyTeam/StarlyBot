package kr.starly.discordbot.entity.coupon;

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

    @NotNull private final UUID redeemId;

    @Nullable private final Coupon coupon;
    @NotNull private final CouponState couponState;

    @NotNull private final long requestedBy;
    @NotNull private final Date requestedAt;
    
    public Document serialize() {
        Document document = new Document();
        document.put("redeemId", getRedeemId());
        document.put("couponState", getCouponState().serialize());
        document.put("coupon", getCoupon().serialize());
        document.put("requestedBy", getRequestedBy());
        document.put("requestedAt", getRequestedAt());
        
        return document;
    }
}