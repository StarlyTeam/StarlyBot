package kr.starly.discordbot.rank.perk;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum RankPerkType {
    COUPON("무료 쿠폰", "매달 쿠폰을 지급받습니다."),
    ROLE("디스코드 역할 지급", "디스코드 역할을 지급받습니다."),
    CASHBACK("캐시백", "구매 금액의 일부를 돌려 받습니다."),
    PRE_RELEASE("선공개", "제품(무료 플러그인)이 출시되기 전, 랭크 보유자에게만 선공개 됩니다.");

    @NotNull private String name;
    @NotNull private String description;
} // TODO : 메시지 디자인