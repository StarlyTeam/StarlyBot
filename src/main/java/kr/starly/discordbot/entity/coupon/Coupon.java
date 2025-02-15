package kr.starly.discordbot.entity.coupon;

import kr.starly.discordbot.entity.Discount;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.entity.product.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@AllArgsConstructor
public class Coupon {

    @NotNull private final String code;
    @NotNull private String name;
    @NotNull private String description;

    @NotNull private List<CouponRequirement> requirements;
    @NotNull private Discount discount;

    @NotNull private final Date createdAt;
    @NotNull private final Long createdBy;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateDiscount(Discount discount) {
        this.discount = discount;
    }

    public boolean isUsable(long userId, Product item) {
        return requirements.stream()
                .allMatch(requirement -> requirement.isSatisfied(userId, this, item));
    }

    public CouponState capture() {
        return new CouponState(this);
    }

    public Document serialize() {
        Document document = new Document();
        document.put("code", getCode());
        document.put("name", getName());
        document.put("description", getDescription());
        document.put("discount", discount.serialize());
        document.put("createdAt", getCreatedAt());
        document.put("createdBy", getCreatedBy());
        document.put("requirements", requirements.stream()
                .map(CouponRequirement::serialize)
                .toList()
        );

        return document;
    }

    public static Coupon deserialize(Document document) {
        if (document == null) return null;

        String code = document.getString("code");
        String name = document.getString("name");
        String description = document.getString("description");
        Date createdAt = document.getDate("createdAt");
        long createdBy = document.getLong("createdBy");

        Discount discount = Discount.deserialize(document.get("discount", Document.class));

        List<CouponRequirement> requirements = new ArrayList<>();
        for (Document requirement : document.getList("requirements", Document.class)) {
            CouponRequirement requirementAsOriginal = CouponRequirement.deserialize(requirement);
            requirements.add(requirementAsOriginal);
        }

        return new Coupon(code, name, description, requirements, discount, createdAt, createdBy);
    }
}