package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.UserInfo;

import java.util.List;

public interface UserInfoRepository {

    void save(UserInfo userInfo);
    UserInfo findByDiscordId(String discordId);
    void updatePoint(String discordId, int newPoint);
    List<UserInfo> getTopUsersByPoints(int limit);
}