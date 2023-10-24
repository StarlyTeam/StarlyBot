package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.payment.Payment;
import kr.starly.discordbot.repository.PaymentRepository;

import java.util.List;
import java.util.UUID;

public record PaymentService(PaymentRepository repository) {

    public void saveData(Payment payment) {
        repository.put(payment);
    }

    public Payment getDataByPaymentId(String paymentId) {
        return getDataByPaymentId(UUID.fromString(paymentId));
    }

    public Payment getDataByPaymentId(UUID paymentId) {
        return repository.findOne(paymentId);
    }

    public List<Payment> getDataByUserId(long userId) {
        return repository.findByUserId(userId);
    }

    public List<Payment> getAllData() {
        return repository.findAll();
    }

    public void deleteDataByPaymentId(String paymentId) {
        deleteDataByPaymentId(UUID.fromString(paymentId));
    }

    public void deleteDataByPaymentId(UUID paymentId) {
        repository.deleteOne(paymentId);
    }
}