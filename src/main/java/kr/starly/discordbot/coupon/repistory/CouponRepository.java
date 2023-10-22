package kr.starly.discordbot.coupon.repistory;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.coupon.entity.Coupon;
import org.bson.Document;

import java.util.List;

public interface CouponRepository {

    void put(Coupon coupon);
    Coupon findOne(String code);
    void deleteOne(String code);
    List<Coupon> findAll();

    MongoCollection<Document> getCollection();
}