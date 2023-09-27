package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.UserInfo;
import kr.starly.discordbot.repository.UserInfoRepository;

import java.time.LocalDateTime;
import java.util.List;

public record UserInfoService(UserInfoRepository userInfoRepository) {

    public void recordUserInfo(String discordId, String ip, LocalDateTime verifyDate, int point) {
        UserInfo userInfo = new UserInfo(discordId, ip, verifyDate, point, 0);
        userInfoRepository.save(userInfo);
    }

    public UserInfo getUserInfo(String discordId) {
        return userInfoRepository.findByDiscordId(discordId);
    }

    public void addWarn(String discordId, int warnToAdd) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null) {
            int newPoint = userInfo.warn() + warnToAdd;
            userInfoRepository.updateWarn(discordId, newPoint);
        }
    }

    public void removeWarn(String discordId, int warnToAdd) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null) {
            int newPoint = userInfo.point() + warnToAdd;
            userInfoRepository.updateWarn(discordId, newPoint);
        }
    }

    public void setWarn(String discordId, int newWarn) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null) {
            userInfoRepository.updateWarn(discordId, newWarn);
        }
    }

    public int getWarn(String discordId) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        return (userInfo != null) ? userInfo.warn() : 0;
    }

    public void addPoint(String discordId, int pointToAdd) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null) {
            int newPoint = userInfo.point() + pointToAdd;
            userInfoRepository.updatePoint(discordId, newPoint);
        }
    }

    public void removePoint(String discordId, int pointToRemove) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null && userInfo.point() >= pointToRemove) {
            int newPoint = userInfo.point() - pointToRemove;
            userInfoRepository.updatePoint(discordId, newPoint);
        }
    }

    public void setPoint(String discordId, int newPoint) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        if (userInfo != null) {
            userInfoRepository.updatePoint(discordId, newPoint);
        }
    }


    public int getPoint(String discordId) {
        UserInfo userInfo = userInfoRepository.findByDiscordId(discordId);
        return (userInfo != null) ? userInfo.point() : 0;
    }

    public List<UserInfo> getTopUsersByPoints(int limit) {
        return userInfoRepository.getTopUsersByPoints(limit);
    }
}