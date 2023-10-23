package kr.starly.discordbot.coupon.discount;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class Discount {

    private final DiscountType type;
    private final int value;

    public int computeFinalPrice(int originalPrice) {
        switch (type) {
            case PERCENTAGE -> {
                return originalPrice * (100 - value);
            }

            case FIXED -> {
                return originalPrice - value;
            }

            default -> {
                return originalPrice;
            }
        }
    }

    public Document serialize() {
        Document document = new Document();
        document.put("type", type.name());
        document.put("value", value);

        return document;
    }

    @Override
    public String toString() {
        return switch (type) {
            case PERCENTAGE -> value + "%";
            case FIXED -> value + "원";
        };
    }

    public static Discount deserialize(Document document) {
        if (document == null) return null;

        DiscountType type = DiscountType.valueOf(document.getString("type"));
        int value = document.getInteger("value");

        return new Discount(type, value);
    }
}