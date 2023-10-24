package kr.starly.discordbot.repository.impl;

import kr.starly.discordbot.enums.CouponSessionType;

import java.util.HashMap;
import java.util.Map;

public class CouponSessionRepository {

    private final Map<Long, String> couponMap = new HashMap<>();
    private final Map<Long, CouponSessionType> sessionTypeMap = new HashMap<>();

    public void registerSession(long userId, String couponCode, CouponSessionType type) {
        couponMap.put(userId, couponCode);
        sessionTypeMap.put(userId, type);
    }

    public void updateCoupon(long userId, String couponCode) {
        couponMap.put(userId, couponCode);
    }

    public void updateSessionType(long userId, CouponSessionType type) {
        sessionTypeMap.put(userId, type);
    }

    public String retrieveCoupon(long userId) {
        return couponMap.get(userId);
    }

    public CouponSessionType retrieveSessionType(long userId) {
        return sessionTypeMap.get(userId);
    }

    public boolean hasSession(long userId) {
        return couponMap.containsKey(userId) || sessionTypeMap.containsKey(userId);
    }

    public void stopSession(long userId) {
        couponMap.remove(userId);
        sessionTypeMap.remove(userId);
    }
}