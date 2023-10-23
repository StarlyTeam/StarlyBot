package kr.starly.discordbot.rank.perk.impl;

import kr.starly.discordbot.rank.perk.RankPerk;
import kr.starly.discordbot.rank.perk.RankPerkType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class CashbackPerk extends RankPerk {

    private final int percentage;

    @Override
    public RankPerkType getType() {
        return RankPerkType.CASHBACK;
    }

    public Document serialize() {
        Document document = super.serialize();
        document.put("percentage", percentage);

        return document;
    }

    public static CashbackPerk deserialize(Document document) {
        if (document == null) return null;

        int percentage = document.getInteger("percentage");

        return new CashbackPerk(percentage);
    }
}