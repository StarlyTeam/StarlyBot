package kr.starly.discordbot.rank.perk;


import org.bson.Document;

public abstract class RankPerk {

    abstract public RankPerkType getType();

    public Document serialize() {
        Document document = new Document();
        document.put("type", getType().name());

        return document;
    }
}