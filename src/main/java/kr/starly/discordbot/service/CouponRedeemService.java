package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponRedeem;
import kr.starly.discordbot.entity.coupon.CouponState;
import kr.starly.discordbot.repository.CouponRedeemRepository;

import java.util.Date;
import java.util.List;
import java.util.UUID;

public record CouponRedeemService(CouponRedeemRepository repository) {

    public void saveData(UUID redeemId, Coupon coupon, CouponState couponState, long requestedBy, Date requestedAt) {
        CouponRedeem couponRedeem = new CouponRedeem(
                redeemId,
                coupon,
                couponState,
                requestedBy,
                requestedAt
        );
        repository.put(couponRedeem);
    }

    public CouponRedeem getData(UUID redeemId) {
        return repository.findOne(redeemId);
    }

    public List<CouponRedeem> getData(Coupon coupon) {
        return repository.findMany(coupon);
    }

    public List<CouponRedeem> getData(long userId) {
        return repository.findMany(userId);
    }

    public List<CouponRedeem> getData(long userId, Coupon coupon) {
        return repository.findMany(userId, coupon);
    }

    public void deleteData(UUID redeemId) {
        repository.deleteOne(redeemId);
    }

    public void deleteData(Coupon coupon) {
        getData(coupon)
                .forEach(couponRedeem -> {
                    repository.deleteOne(couponRedeem.getRedeemId());
                });
    }

    public void deleteData(long userId) {
        getData(userId)
                .forEach(couponRedeem -> {
                    repository.deleteOne(couponRedeem.getRedeemId());
                });
    }
}