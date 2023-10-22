package kr.starly.discordbot.payment.repository;

import com.mongodb.client.MongoCollection;
import kr.starly.discordbot.payment.entity.Payment;
import org.bson.Document;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository {

    void put(Payment payment);
    Payment findOne(UUID paymentId);
    List<Payment> findByUserId(long userId);
    List<Payment> findAll();
    void deleteOne(UUID paymentId);

    MongoCollection<Document> getCollection();
}
