package kr.starly.discordbot.product.entity.impl;

import kr.starly.discordbot.product.entity.Product;
import kr.starly.discordbot.product.enums.ProductType;
import lombok.Getter;
import org.bson.Document;

@Getter
public class OutSourcingProduct extends Product {

    private final String projectName;

    public OutSourcingProduct(String projectName, int price, String note) {
        super(price, note);

        this.projectName = projectName;
    }

    @Override
    public ProductType getType() {
        return ProductType.OUTSOURCING;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("projectName", projectName);

        return document;
    }

    @Override
    public String toString() {
        return projectName + "(" + getPrice() + ")";
    }
}