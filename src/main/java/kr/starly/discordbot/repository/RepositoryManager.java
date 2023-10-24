package kr.starly.discordbot.repository;

import kr.starly.discordbot.repository.impl.CouponSessionRepository;
import lombok.Getter;

public class RepositoryManager {

    @Getter private static final CouponSessionRepository couponSessionRepository = new CouponSessionRepository();
}