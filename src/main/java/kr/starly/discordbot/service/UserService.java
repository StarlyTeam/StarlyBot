package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.repository.UserRepository;

import java.util.Date;
import java.util.List;

public record UserService(UserRepository userRepository) {

    public void saveData(long discordId, String ip, Date verifiedAt, int point) {
        User user = new User(discordId, ip, verifiedAt, point);
        userRepository.put(user);
    }

    public User getDataByDiscordId(long discordId) {
        return userRepository.findByDiscordId(discordId);
    }

    public void addPoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) + amount);
    }

    public void removePoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) - amount);
    }

    public void setPoint(long discordId, int newPoint) {
        User user = userRepository.findByDiscordId(discordId);
        if (user != null) {
            userRepository.put(new User(discordId, user.ip(), user.verifiedAt(), newPoint));
        }
    }

    public int getPoint(long discordId) {
        User user = userRepository.findByDiscordId(discordId);
        return (user != null) ? user.point() : 0;
    }

    public List<User> getTopUsersByPoints(int limit) {
        return userRepository.findAll()
                .stream()
                .sorted((user1, user2) -> user2.point() - user1.point())
                .limit(limit)
                .toList();
    }
}