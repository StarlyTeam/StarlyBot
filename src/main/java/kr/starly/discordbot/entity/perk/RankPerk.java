package kr.starly.discordbot.entity.perk;


import kr.starly.discordbot.enums.RankPerkType;
import org.bson.Document;

public abstract class RankPerk {

    abstract public RankPerkType getType();

    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());

        return document;
    }
}