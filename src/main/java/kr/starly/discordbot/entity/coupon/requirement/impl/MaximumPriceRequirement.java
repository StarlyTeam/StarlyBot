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
public class MaximumPriceRequirement extends CouponRequirement {

    private final int maximumPrice;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.MAXIMUM_PRICE;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        return item.getPrice() <= maximumPrice;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("maximumPrice", maximumPrice);

        return document;
    }

    public static MaximumPriceRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.MAXIMUM_PRICE.name())) {
            throw new IllegalArgumentException("document is not MaximumPriceRequirement");
        }

        int maximumPrice = document.getInteger("maximumPrice");
        return new MaximumPriceRequirement(maximumPrice);
    }
}
