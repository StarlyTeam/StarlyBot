package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class MinimumPriceRequirement extends CouponRequirement {

    private final int minimumPrice;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.MINIMUM_PRICE;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        return item.getPrice() >= minimumPrice;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("minimumPrice", minimumPrice);

        return document;
    }

    public static MinimumPriceRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.MINIMUM_PRICE.name())) {
            throw new IllegalArgumentException("document is not MinimumPriceRequirement");
        }

        int minimumPrice = document.getInteger("minimumPrice");
        return new MinimumPriceRequirement(minimumPrice);
    }
}
