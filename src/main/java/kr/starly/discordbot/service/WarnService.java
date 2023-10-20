package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.repository.WarnRepository;

import java.util.List;

public record WarnService(WarnRepository repository) {

    public void saveData(Warn warn) {
        repository.put(warn);
    }

    public List<Warn> getDataByDiscordId(long discordId) {
        return repository.findByDiscordId(discordId);
    }

    public int getTotalWarn(long discordId) {
        return repository.findByDiscordId(discordId).stream()
                .mapToInt(Warn::amount)
                .sum();
    }
}