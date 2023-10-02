package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Warn;

import java.util.List;

public interface WarnRepository {

    void put(Warn warn);
    List<Warn> findByDiscordId(long discordId);
}
