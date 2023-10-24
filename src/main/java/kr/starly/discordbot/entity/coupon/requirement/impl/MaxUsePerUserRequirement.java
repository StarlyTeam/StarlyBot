package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Discount;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponRedeem;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.service.CouponRedeemService;
import kr.starly.discordbot.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.List;

@Getter
@AllArgsConstructor
public class MaxUsePerUserRequirement extends CouponRequirement {

    private final int maxUsePerUser;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.MAX_USE_PER_USER;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        CouponRedeemService couponRedeemService = DatabaseManager.getCouponRedeemService();
        List<CouponRedeem> redeems = couponRedeemService.getData(coupon);

        Discount discount = coupon.getDiscount();
        if (discount.getType() == DiscountType.PERCENTAGE) {
            return redeems.size() < maxUsePerUser;
        } else if (discount.getType() == DiscountType.FIXED) {
            long useCount = redeems.stream()
                    .filter(redeem -> redeem.getRequestedBy() == userId)
                    .count();

            return useCount < maxUsePerUser;
        } else {
            throw new IllegalArgumentException("Unknown discount type");
        }
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", CouponRequirementType.MAX_USE_PER_USER.name());
        document.put("maxUsePerUser", maxUsePerUser);

        return document;
    }

    public static MaxUsePerUserRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.MAX_USE_PER_USER.name())) {
            throw new IllegalArgumentException("document is not MaxUsePerUserRequirement");
        }

        int maxUsePerUser = document.getInteger("maxUsePerUser");
        return new MaxUsePerUserRequirement(maxUsePerUser);
    }
}
