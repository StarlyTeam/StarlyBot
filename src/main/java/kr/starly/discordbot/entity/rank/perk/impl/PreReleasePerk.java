package kr.starly.discordbot.entity.rank.perk.impl;

import kr.starly.discordbot.entity.rank.perk.RankPerk;
import kr.starly.discordbot.enums.RankPerkType;

public class PreReleasePerk extends RankPerk {

    @Override
    public RankPerkType getType() {
        return RankPerkType.PRE_RELEASE;
    }
}