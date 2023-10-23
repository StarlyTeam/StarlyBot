package kr.starly.discordbot.rank.perk.impl;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.service.CouponService;
import kr.starly.discordbot.rank.perk.RankPerk;
import kr.starly.discordbot.rank.perk.RankPerkType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.List;

@Getter
@AllArgsConstructor
public class CouponPerk extends RankPerk {

    private final List<Coupon> coupons; // 지급할 쿠폰
    private final boolean oneTime; // 최초 1회만 지급할지 여부

    @Override
    public RankPerkType getType() {
        return RankPerkType.COUPON;
    }

    public Document serialize() {
        Document document = super.serialize();
        document.put(
                "coupons", coupons.stream()
                        .map(Coupon::getCode)
                        .toList()
        );
        document.put("oneTime", oneTime);

        return document;
    }

    public static CouponPerk deserialize(Document document) {
        if (document == null) return null;

        CouponService couponService = DatabaseManager.getCouponService();
        List<String> coupons = document.getList("coupons", String.class);
        List<Coupon> coupon = coupons.stream()
                .map(couponService::getData)
                .toList();

        boolean oneTime = document.getBoolean("oneTime");

        return new CouponPerk(coupon, oneTime);
    }
}