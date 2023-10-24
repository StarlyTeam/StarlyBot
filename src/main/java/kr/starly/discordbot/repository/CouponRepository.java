package kr.starly.discordbot.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.coupon.Coupon;
import org.bson.Document;

import java.util.List;

public interface CouponRepository {

    void put(Coupon coupon);
    Coupon findOne(String code);
    void deleteOne(String code);
    List<Coupon> findAll();

    MongoCollection<Document> getCollection();
}