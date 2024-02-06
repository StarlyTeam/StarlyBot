package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.CouponRedeem;
import kr.starly.discordbot.repository.CouponRedeemRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class MongoCouponRedeemRepository implements CouponRedeemRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(CouponRedeem couponRedeem) {
        Document filter = new Document("redeemId", couponRedeem.getRedeemId().toString());
        Document document = couponRedeem.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public CouponRedeem findOne(UUID redeemId) {
        Document filter = new Document("redeemId", redeemId);
        return CouponRedeem.deserialize(collection.find(filter).first());
    }

    @Override
    public List<CouponRedeem> findMany(Coupon coupon) {
        Document filter = new Document("couponState.code", coupon.getCode());

        return collection.find(filter)
                .map(CouponRedeem::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<CouponRedeem> findMany(long userId) {
        Document filter = new Document("requestedBy", userId);

        return collection.find(filter)
                .map(CouponRedeem::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<CouponRedeem> findMany(long userId, Coupon coupon) {
        Document filter = new Document();
        filter.put("requestedBy", userId);
        filter.put("couponState.code", coupon.getCode());

        return collection.find()
                .map(CouponRedeem::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<CouponRedeem> findAll() {
        return collection.find()
                .map(CouponRedeem::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public void deleteOne(UUID redeemId) {
        Document filter = new Document("redeemId", redeemId);
        collection.deleteOne(filter);
    }
}