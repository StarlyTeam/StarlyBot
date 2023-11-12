package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.User;
import kr.starly.discordbot.entity.Rank;
import kr.starly.discordbot.repository.UserRepository;

import java.util.Date;
import java.util.List;

public record UserService(UserRepository repository) {

    public void saveData(long discordId, String ip, Date verifiedAt, int point, List<Rank> rank) {
        User user = new User(discordId, ip, verifiedAt, point, rank);
        repository.put(user);
    }

    public User getDataByDiscordId(long discordId) {
        return repository.findByDiscordId(discordId);
    }

    public void deleteDataByDiscordId(long discordId) {
        repository.deleteByDiscordId(discordId);
    }

    public void addPoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) + amount);
    }

    public void removePoint(long discordId, int amount) {
        setPoint(discordId, getPoint(discordId) - amount);
    }

    public void setPoint(long discordId, int newPoint) {
        User user = repository.findByDiscordId(discordId);
        if (user != null) {
            repository.put(new User(discordId, user.ip(), user.verifiedAt(), newPoint, user.rank()));
        }
    }

    public int getPoint(long discordId) {
        User user = repository.findByDiscordId(discordId);
        return (user != null) ? user.point() : 0;
    }

    public List<User> getTopUsersByPoints(int limit) {
        return repository.findAll()
                .stream()
                .sorted((user1, user2) -> user2.point() - user1.point())
                .limit(limit)
                .toList();
    }
}