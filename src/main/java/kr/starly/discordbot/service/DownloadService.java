package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Download;
import kr.starly.discordbot.repository.DownloadRepository;

import java.util.List;

public record DownloadService(DownloadRepository repository) {

    public void saveData(Download download) {
        repository.put(download);
    }

    public Download getDataByToken(String token) {
        return repository.findOne(token);
    }

    public List<Download> getDataByUserId(long userId) {
        return repository.findMany(userId);
    }

    public List<Download> getDataByUserIp(String userIp) {
        return repository.findMany(userIp);
    }

    public List<Download> getAllData() {
        return repository.findAll();
    }

    public void deleteData(String token) {
        repository.deleteOne(token);
    }

    public void deleteData(long userId) {
        repository.deleteMany(userId);
    }
}