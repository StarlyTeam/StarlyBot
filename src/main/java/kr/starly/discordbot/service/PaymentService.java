package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

public record PaymentService(PaymentRepository repository) {

    public void saveData(Payment payment) {
        repository.put(payment);
    }

    public Payment getDataByPaymentId(String rawPaymentId) {
        UUID paymentId;
        try {
            paymentId = UUID.fromString(rawPaymentId);
        } catch (IllegalArgumentException ignored) {
            return null;
        }

        return getDataByPaymentId(paymentId);
    }

    public Payment getDataByPaymentId(UUID paymentId) {
        return repository.findOne(paymentId);
    }

    public List<Payment> getDataByUserId(long userId) {
        return repository.findByUserId(userId);
    }

    public long getTotalPaidPrice(long userId) {
        List<Payment> payments = getDataByUserId(userId);
        return payments.stream()
                .filter(Payment::isAccepted)
                .filter(payment -> !payment.isRefunded())
                .mapToLong(Payment::getFinalPrice)
                .sum();
    }

    public List<Payment> getAllData() {
        return repository.findAll();
    }

    public void deleteData(UUID paymentId) {
        repository.deleteOne(paymentId);
    }

    public void deleteData(long userId) {
        repository.deleteMany(userId);
    }
}