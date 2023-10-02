package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Blacklist;

import java.util.List;

public interface BlacklistRepository {
    void put(Blacklist blacklist);

    void deleteByUserId(long userId);

    Blacklist findByUserId(long userId);

    List<Blacklist> findAll();
}
