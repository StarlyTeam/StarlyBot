package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.configuration.DatabaseManager;
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
public class MaxUsePerCouponRequirement extends CouponRequirement {

    private final int maxUsePerCoupon;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.MAX_USE_PER_COUPON;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        CouponRedeemService couponRedeemService = DatabaseManager.getCouponRedeemService();
        List<CouponRedeem> redeems = couponRedeemService.getData(coupon);

        return redeems.size() < maxUsePerCoupon;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("maxUsePerCoupon", maxUsePerCoupon);

        return document;
    }

    public static MaxUsePerCouponRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.MAX_USE_PER_COUPON.name())) {
            throw new IllegalArgumentException("document is not MaxUsePerCouponRequirement");
        }

        int maxUsePerCoupon = document.getInteger("maxUsePerCoupon");
        return new MaxUsePerCouponRequirement(maxUsePerCoupon);
    }
}
