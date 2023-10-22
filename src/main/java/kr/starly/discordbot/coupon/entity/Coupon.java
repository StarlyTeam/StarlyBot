package kr.starly.discordbot.coupon.entity;

import kr.starly.discordbot.coupon.discount.Discount;
import kr.starly.discordbot.coupon.discount.DiscountType;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
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

    public CouponState capture() {
        return new CouponState(this);
    }

    public Document serialize() {
        Document document = new Document();
        document.put("code", getCode());
        document.put("name", getName());
        document.put("description", getDescription());
        document.put("discount.type", discount.getType().name());
        document.put("discount.value", discount.getValue());
        document.put("createdAt", getCreatedAt());
        document.put("createdBy", getCreatedBy());

        return document;
    }

    public static Coupon deserialize(Document document) {
        String code = document.getString("code");
        String name = document.getString("name");
        String description = document.getString("description");
        Date createdAt = document.getDate("createdAt");
        long createdBy = document.getLong("createdBy");

        String type = document.getString("discount.type");
        int discountValue = document.getInteger("discount.value");
        DiscountType typeAsOriginal = DiscountType.valueOf(type);
        Discount discount = new Discount(typeAsOriginal, discountValue);

        List<CouponRequirement> requirements = new ArrayList<>();
        for (Document requirement : document.getList("requirements", Document.class)) {
            CouponRequirement requirementAsOriginal = CouponRequirement.deserialize(requirement);
            requirements.add(requirementAsOriginal);
        }

        return new Coupon(code, name, description, requirements, discount, createdAt, createdBy);
    }
}