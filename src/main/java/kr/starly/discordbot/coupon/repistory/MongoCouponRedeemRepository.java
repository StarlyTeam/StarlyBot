package kr.starly.discordbot.coupon.repistory;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.coupon.discount.Discount;
import kr.starly.discordbot.coupon.discount.DiscountType;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.entity.CouponRedeem;
import kr.starly.discordbot.coupon.entity.CouponState;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Getter
@AllArgsConstructor
public class MongoCouponRedeemRepository implements CouponRedeemRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(CouponRedeem couponRedeem) {
        Document filter = new Document("redeemId", couponRedeem.getRedeemId());
        Document document = couponRedeem.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public CouponRedeem findByRedeemId(UUID redeemId) {
        Document filter = new Document("redeemId", redeemId);
        return parseCouponRedeem(collection.find(filter).first());
    }

    @Override
    public List<CouponRedeem> findByCoupon(Coupon coupon) {
        Document filter = new Document("couponState.code", coupon.getCode());

        List<CouponRedeem> redeems = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            redeems.add(parseCouponRedeem(document));
        }

        return redeems;
    }

    @Override
    public List<CouponRedeem> findByUser(long userId) {
        Document filter = new Document("requestedBy", userId);

        List<CouponRedeem> redeems = new ArrayList<>();
        for (Document document : collection.find(filter)) {
            redeems.add(parseCouponRedeem(document));
        }

        return redeems;
    }

    @Override
    public List<CouponRedeem> findAll() {
        return StreamSupport.stream(collection.find().spliterator(), false)
                .map(this::parseCouponRedeem)
                .toList();
    }

    @Override
    public void deleteOne(UUID redeemId) {
        Document filter = new Document("redeemId", redeemId);
        collection.deleteOne(filter);
    }

    private CouponRedeem parseCouponRedeem(Document document) {
        if (document == null) return null;

        UUID redeemId = document.get("redeemId", UUID.class);
        CouponState couponState = parseCouponState(document.get("couponState", Document.class));
        Coupon coupon = DatabaseManager.getCouponService().getData(couponState.getCode());
        long requestedBy = document.getLong("requestedBy");
        Date requestedAt = document.getDate("requestedAt");

        return new CouponRedeem(redeemId, coupon, couponState, requestedBy, requestedAt);
    }

    private CouponState parseCouponState(Document document) {
        if (document == null) return null;

        Coupon coupon = parseCoupon(document.get("couponState", Document.class));
        Date statedAt = document.getDate("statedAt");

        return new CouponState(coupon, statedAt);
    }

    private Coupon parseCoupon(Document document) {
        if (document == null) return null;

        String code = document.getString("code");
        String name = document.getString("name");
        String description = document.getString("description");
        Date createdAt = document.getDate("createdAt");
        long createdBy = document.getLong("createdBy");

        String type = document.getString("discount.type");
        int discountValue = document.getInteger("discount.value");
        DiscountType typeAsOriginal = DiscountType.valueOf(type);
        Discount discount = new Discount(typeAsOriginal, discountValue);

        List<CouponRequirement> requirements = new ArrayList<>();
        for (Document requirement : document.getList("requirements", Document.class)) {
            CouponRequirement requirementAsOriginal = CouponRequirement.deserialize(requirement);
            requirements.add(requirementAsOriginal);
        }

        return new Coupon(code, name, description, requirements, discount, createdAt, createdBy);
    }
}