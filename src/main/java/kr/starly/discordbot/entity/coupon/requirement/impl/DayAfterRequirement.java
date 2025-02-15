package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.Date;

@Getter
@AllArgsConstructor
public class DayAfterRequirement extends CouponRequirement {

    private Date dayAfter;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.DAY_AFTER;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        return dayAfter.after(new Date());
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("dayAfter", dayAfter);

        return document;
    }

    public static DayAfterRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.DAY_AFTER.name())) {
            throw new IllegalArgumentException("document is not DaySinceRequirement");
        }

        Date dayAfter = document.getDate("dayAfter");
        return new DayAfterRequirement(dayAfter);
    }
}