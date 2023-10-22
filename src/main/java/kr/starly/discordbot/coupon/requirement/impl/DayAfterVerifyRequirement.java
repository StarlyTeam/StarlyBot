package kr.starly.discordbot.coupon.requirement.impl;

import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.coupon.entity.Coupon;
import kr.starly.discordbot.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.coupon.requirement.CouponRequirementType;
import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.product.entity.Product;
import kr.starly.discordbot.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.Date;

@Getter
@AllArgsConstructor
public class DayAfterVerifyRequirement extends CouponRequirement {

    private long dayAfterVerify;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.DAY_AFTER_VERIFY;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        UserService userService = DatabaseManager.getUserService();
        User user = userService.getDataByDiscordId(userId);

        if (user == null) return false;
        return new Date().getTime() - user.verifiedAt().getTime() >= dayAfterVerify * 24 * 60 * 60 * 1000;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("dayAfterVerify", dayAfterVerify);

        return document;
    }

    public static DayAfterVerifyRequirement deserialize(Document document) {
        if (!document.getString("type").equals(CouponRequirementType.DAY_AFTER_VERIFY.name())) {
            throw new IllegalArgumentException("document is not DayAfterJoinRequirement");
        }

        long dayAfterVerify = document.getLong("dayAfterVerify");
        return new DayAfterVerifyRequirement(dayAfterVerify);
    }
}