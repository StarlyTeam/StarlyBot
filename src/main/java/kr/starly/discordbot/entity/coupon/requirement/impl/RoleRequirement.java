package kr.starly.discordbot.entity.coupon.requirement.impl;

import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;
import kr.starly.discordbot.entity.product.Product;
import kr.starly.discordbot.enums.CouponRequirementType;
import kr.starly.discordbot.manager.DiscordBotManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class RoleRequirement extends CouponRequirement {

    private final long roleId;
    private final boolean expect;

    @Override
    public CouponRequirementType getType() {
        return CouponRequirementType.ROLE;
    }

    @Override
    public boolean isSatisfied(long userId, Coupon coupon, Product item) {
        Guild guild = DiscordBotManager.getInstance().getGuild();
        Member member = guild.getMemberById(userId);

        Role role = guild.getRoleById(roleId);
        boolean hasRole = member.getRoles().contains(role);
        return expect == hasRole;
    }

    @Override
    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());
        document.put("roleId", roleId);
        document.put("expected", expect);

        return document;
    }

    public static RoleRequirement deserialize(Document document) {
        if (document == null) return null;

        if (!document.getString("type").equals(CouponRequirementType.ROLE.name())) {
            throw new IllegalArgumentException("document is not RoleRequirement");
        }

        long roleId = document.getLong("roleId");
        boolean required = document.getBoolean("expected");
        return new RoleRequirement(roleId, required);
    }
}
