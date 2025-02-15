package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Discount;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.repository.CouponRepository;
import kr.starly.discordbot.entity.coupon.requirement.CouponRequirement;

import java.util.Date;
import java.util.List;

public record CouponService(CouponRepository repository) {

    public void saveData(String code, String name, String description, List<CouponRequirement> requirements, DiscountType discountType, int discountValue, long createdBy) {
        Coupon coupon = new Coupon(
                code,
                name,
                description,
                requirements,
                new Discount(discountType, discountValue),
                new Date(),
                createdBy
        );
        saveData(coupon);
    }

    public void saveData(Coupon coupon) {
        repository.put(coupon);
    }

    public Coupon getData(String code) {
        return repository.findOne(code);
    }

    public List<Coupon> getAllData() {
        return repository.findAll();
    }

    public void deleteData(String code) {
        repository.deleteOne(code);
    }
}