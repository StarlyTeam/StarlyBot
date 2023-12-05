package kr.starly.discordbot.entity.product.impl;

import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.ProductType;
import lombok.Getter;
import org.bson.Document;

@Getter
public class CustomPriceProduct extends Product {

    private final String orderName;

    public CustomPriceProduct(String orderName, int price, String summary) {
        super(price, summary);

        this.orderName = orderName;
    }

    @Override
    public ProductType getType() {
        return ProductType.CUSTOM_PRICE;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("orderName", orderName);

        return document;
    }

    @Override
    public String toString() {
        return orderName + "(" + getPrice() + ")";
    }
}