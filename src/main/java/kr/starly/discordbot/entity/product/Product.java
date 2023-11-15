package kr.starly.discordbot.entity.product;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.entity.Plugin;
import kr.starly.discordbot.enums.ProductType;
import kr.starly.discordbot.entity.product.impl.CustomPriceProduct;
import kr.starly.discordbot.entity.product.impl.OutSourcingProduct;
import kr.starly.discordbot.entity.product.impl.PremiumPluginProduct;
import kr.starly.discordbot.service.PluginService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public abstract class Product {

    private final int price;
    private final String note;

    public abstract ProductType getType();

    public String getName() {
        if (this instanceof PremiumPluginProduct this1) {
            Plugin plugin = this1.getPlugin();
            return plugin.getKRName() + "(" + plugin.getENName() + ")";
        } else if (this instanceof OutSourcingProduct this1) {
            return this1.getName();
        } else if (this instanceof CustomPriceProduct this1) {
            return this1.getOrderName();
        }
    }

    public PremiumPluginProduct asPremiumPlugin() {
        return (PremiumPluginProduct) this;
    }

    public OutSourcingProduct asOutSourcing() {
        return (OutSourcingProduct) this;
    }

    public CustomPriceProduct asCustomPrice() {
        return (CustomPriceProduct) this;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("price", price);
        document.put("note", note);

        return document;
    }

    public static Product deserialize(Document document) {
        if (document == null) return null;

        int price = document.getInteger("price");
        String note = document.getString("note");

        ProductType type = ProductType.valueOf(document.getString("type"));
        switch (type) {
            case PREMIUM_RESOURCE -> {
                String pluginENName = document.getString("plugin");

                PluginService pluginService = DatabaseManager.getPluginService();
                Plugin plugin = pluginService.getDataByENName(pluginENName);

                return new PremiumPluginProduct(plugin, note);
            }

            case OUTSOURCING -> {
                String projectName = document.getString("projectName");

                return new OutSourcingProduct(projectName, price, note);
            }

            case CUSTOM_PRICE -> {
                String orderName = document.getString("orderName");

                return new CustomPriceProduct(orderName, price, note);
            }

            default -> {
                return null;
            }
        }
    }
}