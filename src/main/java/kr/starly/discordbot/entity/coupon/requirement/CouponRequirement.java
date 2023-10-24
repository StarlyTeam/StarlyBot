package kr.starly.discordbot.entity.coupon.requirement;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.impl.*;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.CouponRequirementType;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class CouponRequirement {

    private static final Map<CouponRequirementType, Function<Document, CouponRequirement>> deserializers = new HashMap<>();

    public abstract CouponRequirementType getType();
    public abstract boolean isSatisfied(long userId, Coupon coupon, Product item);
    public abstract Document serialize();

    public static CouponRequirement deserialize(Document document) {
        if (document == null) return null;

        if (deserializers.isEmpty()) {
            deserializers.put(CouponRequirementType.DAY_AFTER, DayAfterRequirement::deserialize);
            deserializers.put(CouponRequirementType.DAY_BEFORE, DayBeforeRequirement::deserialize);
            deserializers.put(CouponRequirementType.DAY_AFTER_VERIFY, DayAfterVerifyRequirement::deserialize);
            deserializers.put(CouponRequirementType.MAX_USE_PER_USER, MaxUsePerUserRequirement::deserialize);
            deserializers.put(CouponRequirementType.MAX_USE_PER_COUPON, MaxUsePerCouponRequirement::deserialize);
            deserializers.put(CouponRequirementType.TRANSACTION_TYPE, TransactionTypeRequirement::deserialize);
            deserializers.put(CouponRequirementType.MINIMUM_PRICE, MinimumPriceRequirement::deserialize);
            deserializers.put(CouponRequirementType.MAXIMUM_PRICE, MaximumPriceRequirement::deserialize);
            deserializers.put(CouponRequirementType.ROLE, RoleRequirement::deserialize);
        }

        CouponRequirementType type = CouponRequirementType.valueOf(document.getString("type"));
        return deserializers.get(type).apply(document);
    }
}