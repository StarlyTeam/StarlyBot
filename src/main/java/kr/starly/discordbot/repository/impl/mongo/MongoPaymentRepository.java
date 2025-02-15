package kr.starly.discordbot.repository.impl.mongo;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.repository.PaymentRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Getter
@AllArgsConstructor
public class MongoPaymentRepository implements PaymentRepository {

    private final MongoCollection<Document> collection;

    @Override
    public void put(Payment payment) {
        Document filter = new Document("paymentId", payment.getPaymentId().toString());
        Document document = payment.serialize();

        if (collection.find(filter).first() != null) {
            collection.updateOne(filter, new Document("$set", document));
        } else {
            collection.insertOne(document);
        }
    }

    @Override
    public Payment findOne(UUID paymentId) {
        Document filter = new Document("paymentId", paymentId.toString());
        return Payment.deserialize(collection.find(filter).first());
    }

    @Override
    public List<Payment> findByUserId(long userId) {
        Document filter = new Document("requestedBy", userId);
        return collection.find(filter)
                .map(Payment::deserialize)
                .into(new ArrayList<>());
    }

    @Override
    public List<Payment> findAll() {
        return StreamSupport.stream(collection.find().spliterator(), false)
                .map(Payment::deserialize)
                .toList();
    }

    @Override
    public void deleteOne(UUID paymentId) {
        Document filter = new Document("paymentId", paymentId.toString());
        collection.deleteOne(filter);
    }

    @Override
    public void deleteMany(long userId) {
        Document filter = new Document("requestedBy", userId);
        collection.deleteMany(filter);
    }
}