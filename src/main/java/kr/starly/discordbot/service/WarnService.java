package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.repository.WarnRepository;

import java.util.Date;
import java.util.List;

public record WarnService(WarnRepository repository) {

    public void saveData(Warn warn) {
        repository.put(warn);
    }

    public List<Warn> getDataByDiscordId(long discordId) {
        return repository.findMany(discordId);
    }

    public void deleteDataByDiscordIdAndDate(long discordId, Date date) {
        repository.deleteOne(discordId, date);
    }

    public void deleteDataByDiscordId(long discordId) {
        repository.deleteMany(discordId);
    }

    public int getTotalWarn(long discordId) {
        return repository.findMany(discordId).stream()
                .mapToInt(Warn::amount)
                .sum();
    }
}