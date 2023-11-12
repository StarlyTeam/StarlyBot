package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponRedeem;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.service.CouponRedeemService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.Date;
import java.util.List;

@Getter
@AllArgsConstructor
public class MaxUsePerMonthRequirement extends CouponRequirement {

    private final int maxUsePerMonth;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.MAX_USE_PER_MONTH;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        CouponRedeemService redeemService = DatabaseManager.getCouponRedeemService();
        List<CouponRedeem> redeems = redeemService.getData(userId, coupon);

        int currentMonth = new Date().getMonth();
        return redeems.stream()
                .map(CouponRedeem::getRequestedAt)
                .filter(requestedAt -> requestedAt.getMonth() == currentMonth)
                .count()
                < maxUsePerMonth;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("maxUsePerMonth", maxUsePerMonth);

        return document;
    }
}