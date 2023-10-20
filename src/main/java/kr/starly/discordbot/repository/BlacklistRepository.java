package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.Blacklist;

import java.util.List;

public interface BlacklistRepository {

    void put(Blacklist blacklist);
    Blacklist findByUserId(long userId);
    Blacklist findByIpAddress(String ipAddress);
    List<Blacklist> findAll();
    void deleteByUserId(long userId);
    void deleteByIpAddress(String ipAddress);
}
