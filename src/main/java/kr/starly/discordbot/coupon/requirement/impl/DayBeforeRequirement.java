package kr.starly.discordbot.coupon.requirement.impl;

import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.coupon.requirement.CouponRequirementType;
import kr.starly.discordbot.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.Date;

@Getter
@AllArgsConstructor
public class DayBeforeRequirement extends CouponRequirement {

    private Date dayBefore;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.DAY_BEFORE;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        return new Date().before(dayBefore);
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", CouponRequirementType.DAY_BEFORE);
        document.put("dayBefore", dayBefore);

        return document;
    }

    public static DayBeforeRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.DAY_BEFORE.name())) {
            throw new IllegalArgumentException("document is not DayUntilRequirement");
        }

        Date dayBefore = document.getDate("dayBefore");
        return new DayBeforeRequirement(dayBefore);
    }
}