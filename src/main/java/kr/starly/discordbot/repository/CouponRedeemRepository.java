package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponRedeem;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public interface CouponRedeemRepository {

    void put(CouponRedeem redeem);
    CouponRedeem findOne(UUID redeemId);
    List<CouponRedeem> findMany(Coupon coupon);
    List<CouponRedeem> findMany(long userId);
    List<CouponRedeem> findMany(long userId, Coupon coupon);
    List<CouponRedeem> findAll();
    void deleteOne(UUID redeemId);

    MongoCollection<Document> getCollection();
}