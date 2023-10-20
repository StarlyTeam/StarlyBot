package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Blacklist;
import kr.starly.discordbot.repository.BlacklistRepository;

import java.util.Date;
import java.util.List;

public record BlacklistService(BlacklistRepository repository) {

    public void saveData(Long userId, String ipAddress, long moderatorId, String reason) {
        Blacklist blacklist = new Blacklist(userId, ipAddress, moderatorId, reason, new Date());
        repository.put(blacklist);
    }

    public Blacklist getDataByUserId(long userId) {
        return repository.findByUserId(userId);
    }

    public Blacklist getDataByIpAddress(String ipAddress) {
        return repository.findByIpAddress(ipAddress);
    }

    public List<Blacklist> getAllData() {
        return repository.findAll();
    }

    public void deleteDataByUserId(long userId) {
        repository.deleteByUserId(userId);
    }

    public void deleteDataByIpAddress(String ipAddress) {
        repository.deleteByIpAddress(ipAddress);
    }
}