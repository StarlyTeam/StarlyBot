package kr.starly.discordbot.coupon.repistory;

import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.entity.CouponRedeem;

import java.util.List;
import java.util.UUID;

public interface CouponRedeemRepository {
    void put(CouponRedeem redeem);

    CouponRedeem findByRedeemId(UUID redeemId);

    List<CouponRedeem> findByCoupon(Coupon coupon);

    List<CouponRedeem> findByUser(long userId);

    List<CouponRedeem> findAll();

    void deleteOne(UUID redeemId);
}
