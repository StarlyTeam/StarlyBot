package kr.starly.discordbot.rank.perk.impl;

import kr.starly.discordbot.manager.DiscordBotManager;
import kr.starly.discordbot.rank.perk.RankPerk;
import kr.starly.discordbot.rank.perk.RankPerkType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import org.bson.Document;

@Getter
@AllArgsConstructor
public class RolePerk extends RankPerk {

    private final Role role;

    public RolePerk(long roleId) {
        JDA jda = DiscordBotManager.getInstance().getJda();
        this.role = jda.getRoleById(roleId);
    }

    @Override
    public RankPerkType getType() {
        return RankPerkType.ROLE;
    }

    public Document serialize() {
        Document document = super.serialize();
        document.put("role", role.getId());

        return document;
    }

    public static RolePerk deserialize(Document document) {
        if (document == null) return null;

        JDA jda = DiscordBotManager.getInstance().getJda();
        long roleId = document.getLong("role");
        Role role = jda.getRoleById(roleId);

        return new RolePerk(role);
    }
}