package kr.starly.discordbot.entity.product.impl;

import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.ProductType;
import lombok.Getter;
import org.bson.Document;

@Getter
public class PremiumPluginProduct extends Product {

    private final Plugin plugin;

    public PremiumPluginProduct(Plugin plugin, String summary) {
        super(plugin == null ? -1 : plugin.getPrice(), summary);

        this.plugin = plugin;
    }

    @Override
    public ProductType getType() {
        return ProductType.PREMIUM_RESOURCE;
    }

    @Override
    public Document serialize() {
        Document document = super.serialize();
        document.put("plugin", plugin.getENName());

        return document;
    }

    @Override
    public String toString() {
        return plugin.getKRName();
    }
}