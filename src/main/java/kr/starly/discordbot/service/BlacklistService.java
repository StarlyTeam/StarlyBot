package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.repository.BlacklistRepository;

import java.util.Date;
import java.util.List;

public record BlacklistService(BlacklistRepository blacklistRepository) {

    public void saveData(long userId, long moderatorId, String reason, Date listedAt) {
        Blacklist blacklist = new Blacklist(userId, moderatorId, reason, listedAt);
        blacklistRepository.put(blacklist);
    }

    public Blacklist getDataByUserId(long userId) {
        return blacklistRepository.findByUserId(userId);
    }

    public List<Blacklist> getAllData() {
        return blacklistRepository.findAll();
    }

    public void deleteDataByUserId(long userId) {
        blacklistRepository.deleteByUserId(userId);
    }
}
