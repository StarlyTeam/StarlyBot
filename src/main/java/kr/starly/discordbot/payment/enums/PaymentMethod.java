package kr.starly.discordbot.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PaymentMethod {
    CREDIT_CARD("신용카드"),
    BANK_TRANSFER("계좌이체"),
    CULTURELAND("문화상품권"),
    NONE("해당없음");

    private final String KRName;
}