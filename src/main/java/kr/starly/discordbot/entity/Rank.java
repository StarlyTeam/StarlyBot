package kr.starly.discordbot.entity;

import kr.starly.discordbot.entity.perk.RankPerk;
import kr.starly.discordbot.enums.RankPerkType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@AllArgsConstructor
public class Rank {

    private final int ordinal;
    private final String name;
    private final String description;

    private final Map<RankPerkType, RankPerk> perks = new HashMap<>();

    public RankPerk getPerk(RankPerkType type) {
        return perks.get(type);
    }

    public void addPerk(RankPerk perk) {
        perks.put(perk.getType(), perk);
    }

    public void removePerk(RankPerkType type) {
        perks.remove(type);
    }

    public boolean hasPerk(RankPerkType type) {
        return perks.containsKey(type);
    }
}