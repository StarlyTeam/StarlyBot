package kr.starly.discordbot.entity.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class RefundAccount {

    @NotNull private final String holder;
    @NotNull private final String number;
    @NotNull private final String bank;
}