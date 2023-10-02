package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.repository.UserInfoRepository;

import java.util.Date;
import java.util.List;

public record UserInfoService(UserInfoRepository userInfoRepository) {

    public void saveData(long discordId, String ip, Date verifiedAt, int point) {
        User user = new User(discordId, ip, verifiedAt, point);
        userInfoRepository.put(user);
    }

    public User getDataByDiscordId(long discordId) {
        return userInfoRepository.findByDiscordId(discordId);
    }

    public void addPoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) + amount);
    }

    public void removePoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) - amount);
    }

    public void setPoint(long discordId, int newPoint) {
        User user = userInfoRepository.findByDiscordId(discordId);
        if (user != null) {
            userInfoRepository.put(new User(discordId, user.ip(), user.verifiedAt(), newPoint));
        }
    }

    public int getPoint(long discordId) {
        User user = userInfoRepository.findByDiscordId(discordId);
        return (user != null) ? user.point() : 0;
    }

    public List<User> getTopUsersByPoints(int limit) {
        return userInfoRepository.findAll()
                .stream()
                .sorted((user1, user2) -> user2.point() - user1.point())
                .limit(limit)
                .toList();
    }
}