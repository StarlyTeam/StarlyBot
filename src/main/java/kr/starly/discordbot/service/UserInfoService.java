package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.UserInfo;
import kr.starly.discordbot.repository.UserInfoRepository;

import java.time.LocalDateTime;

public record UserInfoService(UserInfoRepository userInfoRepository) {

    public void recordUserInfo(String takenDiscordId, String takenIp, LocalDateTime takenLocalDateTime) {
        String discordId = takenDiscordId;
        String ip = takenIp;
        LocalDateTime verifyDate = takenLocalDateTime;
        UserInfo userInfo = new UserInfo(discordId, ip, verifyDate);
        userInfoRepository.save(userInfo);
    }

    public UserInfo getUserInfo(String discordId) {
        return userInfoRepository.findByDiscordId(discordId);
    }
}
