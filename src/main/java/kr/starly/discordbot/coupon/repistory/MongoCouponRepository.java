package kr.starly.discordbot.coupon.repistory;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.coupon.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

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
        return Coupon.deserialize(collection.find(filter).first());
    }

    @Override
    public List<Coupon> findAll() {
        return StreamSupport.stream(collection.find().spliterator(), false)
                .map(Coupon::deserialize)
                .toList();
    }

    @Override
    public void deleteOne(String code) {
        Document filter = new Document("code", code);
        collection.deleteOne(filter);
    }
}