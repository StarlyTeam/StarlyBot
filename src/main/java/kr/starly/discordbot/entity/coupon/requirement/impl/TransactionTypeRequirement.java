package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class TransactionTypeRequirement extends CouponRequirement {

    private final ProductType productType;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.PRODUCT_TYPE;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        return item.getType() == productType;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("transactionType", productType);

        return document;
    }

    public static TransactionTypeRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.PRODUCT_TYPE.name())) {
            throw new IllegalArgumentException("document is not TransactionTypeRequirement");
        }

        ProductType productType = document.get("transactionType", ProductType.class);
        return new TransactionTypeRequirement(productType);
    }
}
