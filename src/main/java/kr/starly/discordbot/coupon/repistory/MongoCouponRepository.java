package kr.starly.discordbot.coupon.repistory;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.coupon.discount.Discount;
import kr.starly.discordbot.coupon.discount.DiscountType;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.StreamSupport;

@Getter
@AllArgsConstructor
public class MongoCouponRepository implements CouponRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Coupon coupon) {
        Document filter = new Document("code", coupon.getCode());
        Document document = coupon.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Coupon findOne(String code) {
        Document filter = new Document("code", code);
        return parseCoupon(collection.find(filter).first());
    }

    @Override
    public List<Coupon> findAll() {
        return StreamSupport.stream(collection.find().spliterator(), false)
                .map(this::parseCoupon)
                .toList();
    }

    @Override
    public void deleteOne(String code) {
        Document filter = new Document("code", code);
        collection.deleteOne(filter);
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