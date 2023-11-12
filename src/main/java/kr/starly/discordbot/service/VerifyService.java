package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Verify;
import kr.starly.discordbot.repository.VerifyRepository;

import java.util.List;

public record VerifyService(VerifyRepository repository) {

    public void saveData(Verify verify) {
        repository.put(verify);
    }

    public Verify getDataByToken(String token) {
        return repository.findOne(token);
    }

    public List<Verify> getDataByUserId(long userId) {
        return repository.findMany(userId);
    }

    public List<Verify> getDataByUserIp(String userIp) {
        return repository.findMany(userIp);
    }

    public List<Verify> getAllData() {
        return repository.findAll();
    }

    public void deleteDataByToken(String token) {
        repository.deleteOne(token);
    }

    public void deleteDataByUserId(long userId) {
        repository.deleteMany(userId);
    }
}