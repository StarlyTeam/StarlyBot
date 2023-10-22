package kr.starly.discordbot.rank.perk.impl;

import kr.starly.discordbot.rank.perk.RankPerk;
import kr.starly.discordbot.rank.perk.RankPerkType;

public class PreReleasePerk extends RankPerk {

    @Override
    public RankPerkType getType() {
        return RankPerkType.PRE_RELEASE;
    }
}