package kr.starly.discordbot.entity.product.impl;

import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.ProductType;
import lombok.Getter;
import org.bson.Document;

@Getter
public class OutSourcingProduct extends Product {

    private final String projectName;

    public OutSourcingProduct(String projectName, int price, String summary) {
        super(price, summary);

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