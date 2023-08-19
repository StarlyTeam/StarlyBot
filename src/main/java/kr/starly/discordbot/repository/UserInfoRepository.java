package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.UserInfo;

public interface UserInfoRepository {

    void save(UserInfo userInfo);
    UserInfo findByDiscordId(String discordId);
}
