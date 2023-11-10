package kr.starly.discordbot.repository.impl;

import kr.starly.discordbot.configuration.ConfigProvider;
import kr.starly.discordbot.configuration.DatabaseManager;
import kr.starly.discordbot.enums.DiscountType;
import kr.starly.discordbot.entity.coupon.Coupon;
import kr.starly.discordbot.entity.coupon.requirement.impl.DayAfterVerifyRequirement;
import kr.starly.discordbot.entity.coupon.requirement.impl.RoleRequirement;
import kr.starly.discordbot.service.CouponService;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.entity.rank.perk.impl.CashbackPerk;
import kr.starly.discordbot.entity.rank.perk.impl.CouponPerk;
import kr.starly.discordbot.entity.rank.perk.impl.PreReleasePerk;
import kr.starly.discordbot.entity.rank.perk.impl.RolePerk;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class RankRepository {

    private static RankRepository instance;

    public static RankRepository getInstance() {
        if (instance == null) instance = new RankRepository();
        return instance;
    }

    private RankRepository() {}

    private final Map<Integer, Rank> rankMap = new HashMap<>();

    public Rank getRank(int ordinal) {
        return rankMap.get(ordinal);
    }

    public void addRank(Rank rank) {
        rankMap.put(rank.getOrdinal(), rank);
    }

    public void removeRank(int ordinal) {
        rankMap.remove(ordinal);
    }

    public static void $initialize() {
        // 상수 선언
        final String RANK1_COUPON_ID = "STARLY1";
        final String RANK4_COUPON_ID = "STARLY4";
        final String RANK5_COUPON_ID = "STARLY5";

        final ConfigProvider configProvider = ConfigProvider.getInstance();
        final long RANK1_ROLE_ID = Long.parseLong(configProvider.getString("VERIFIED_ROLE_ID")); // 유성
        final long RANK2_ROLE_ID = Long.parseLong(configProvider.getString("BUYER_ROLE_ID")); // 위성
        final long RANK3_ROLE_ID = Long.parseLong(configProvider.getString("RANK3_ROLE_ID")); // 행성
        final long RANK4_ROLE_ID = Long.parseLong(configProvider.getString("RANK4_ROLE_ID")); // 은하
        final long RANK5_ROLE_ID = Long.parseLong(configProvider.getString("RANK5_ROLE_ID")); // 우주

        // 변수 선언
        CouponService couponService = DatabaseManager.getCouponService();
        RankRepository rankRepository = RankRepository.getInstance();

        // 쿠폰 생성
        if (couponService.getData(RANK1_COUPON_ID) == null) {
            couponService.saveData(
                    RANK1_COUPON_ID,
                    "10% 할인 쿠폰 [유성 랭크]",
                    "상품 구매가의 10%를 할인해주는 쿠폰입니다. 매달 자동으로 지급됩니다.",
                    List.of(
                            new DayAfterVerifyRequirement(0),
                            new RoleRequirement(RANK1_ROLE_ID, true),
                            new RoleRequirement(RANK2_ROLE_ID, false)
                    ),
                    DiscountType.PERCENTAGE, 10,
                    0L
            );
        }
        if (couponService.getData(RANK4_COUPON_ID) == null) {
            couponService.saveData(
                    RANK4_COUPON_ID,
                    "3000원 할인 쿠폰 [은하 랭크]",
                    "상품 구매가에서 3000원을 할인해주는 쿠폰입니다. 매달 자동으로 지급됩니다.",
                    List.of(
                            new DayAfterVerifyRequirement(0),
                            new RoleRequirement(RANK4_ROLE_ID, true),
                            new RoleRequirement(RANK5_ROLE_ID, true)
                    ),
                    DiscountType.FIXED, 3000,
                    0L
            );
        }
        if (couponService.getData(RANK5_COUPON_ID) == null) {
            couponService.saveData(
                    RANK5_COUPON_ID,
                    "5000원 할인 쿠폰 [우주 랭크]",
                    "상품 구매가에서 5000원을 할인해주는 쿠폰입니다. 매달 자동으로 지급됩니다.",
                    List.of(
                            new DayAfterVerifyRequirement(0),
                            new RoleRequirement(RANK5_ROLE_ID, true)
                    ),
                    DiscountType.FIXED, 5000,
                    0L
            );
        }

        // 랭크 생성
        Rank rank1 = new Rank(1, "유성", "인증을 하면 받을 수 있는 가장 기본적인 랭크입니다.");
        Coupon rank1Coupon = couponService.getData(RANK1_COUPON_ID);
        rank1.addPerk(new RolePerk(RANK1_ROLE_ID));
        rank1.addPerk(new CouponPerk(List.of(rank1Coupon), true));
        rankRepository.addRank(rank1);

        Rank rank2 = new Rank(2, "위성", "스탈리 스토어에서 상품을 구매하면 받을 수 있는 랭크입니다.");
        rank2.addPerk(new RolePerk(RANK2_ROLE_ID));
        rankRepository.addRank(rank2);

        Rank rank3 = new Rank(3, "행성", "스탈리 스토어에서 구매한 상품들의 총액이 50만원 이상이면 받을 수 있는 랭크입니다.");
        rank3.addPerk(new RolePerk(RANK3_ROLE_ID));
        rank3.addPerk(new CashbackPerk(2));
        rankRepository.addRank(rank3);

        Rank rank4 = new Rank(4, "은하", "스탈리 스토어에서 구매한 상품들의 총액이 100만원 이상이면 받을 수 있는 랭크입니다.");
        Coupon rank4Coupon = couponService.getData(RANK4_COUPON_ID);
        rank4.addPerk(new RolePerk(RANK4_ROLE_ID));
        rank4.addPerk(new CashbackPerk(5));
        rank4.addPerk(new CouponPerk(List.of(rank4Coupon), false));
        rank4.addPerk(new PreReleasePerk());
        rankRepository.addRank(rank4);

        Rank rank5 = new Rank(5, "우주", "스탈리 스토어에서 구매한 상품들의 총액이 300만원 이상이면 받을 수 있는 랭크입니다.");
        Coupon rank5Coupon = couponService.getData(RANK5_COUPON_ID);
        rank5.addPerk(new RolePerk(RANK5_ROLE_ID));
        rank5.addPerk(new CashbackPerk(7));
        rank5.addPerk(new CouponPerk(List.of(rank5Coupon), false));
        rank5.addPerk(new PreReleasePerk());
        rankRepository.addRank(rank5);
    }
}