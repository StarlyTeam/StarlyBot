package kr.starly.discordbot.entity.coupon;

import lombok.Getter;
import org.bson.Document;

import java.util.Date;

@Getter
public class CouponState extends Coupon {

    private final Date statedAt;

    public CouponState(Coupon coupon) {
        this(coupon, new Date());
    }

    public CouponState(Coupon coupon, Date statedAt) {
        super(coupon.getCode(), coupon.getName(), coupon.getDescription(), coupon.getRequirements(), coupon.getDiscount(), coupon.getCreatedAt(), coupon.getCreatedBy());

        this.statedAt = statedAt;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("statedAt", statedAt);

        return document;
    }

    public static CouponState deserialize(Document document) {
        if (document == null) return null;

        Coupon coupon = Coupon.deserialize(document);

        Date statedAt = document.getDate("statedAt");
        return new CouponState(coupon, statedAt);
    }
}