package kr.starly.discordbot.coupon.requirement.impl;

import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.coupon.requirement.CouponRequirementType;
import kr.starly.discordbot.product.entity.Product;
import kr.starly.discordbot.product.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class TransactionTypeRequirement extends CouponRequirement {

    private final ProductType productType;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.TRANSACTION_TYPE;
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

        if (!document.getString("type").equals(CouponRequirementType.TRANSACTION_TYPE.name())) {
            throw new IllegalArgumentException("document is not TransactionTypeRequirement");
        }

        ProductType productType = document.get("transactionType", ProductType.class);
        return new TransactionTypeRequirement(productType);
    }
}
