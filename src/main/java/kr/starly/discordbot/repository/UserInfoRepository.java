package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.User;

import java.util.List;

public interface UserInfoRepository {

    void put(User user);
    User findByDiscordId(long discordId);
    List<User> findAll();
}