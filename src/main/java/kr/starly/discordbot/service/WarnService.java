package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.Warn;
import kr.starly.discordbot.repository.WarnRepository;

import java.util.List;

public record WarnService(WarnRepository warnRepository) {

    public void saveData(Warn warn) {
        warnRepository.put(warn);
    }

    public List<Warn> getDataByDiscordId(long discordId) {
        return warnRepository.findByDiscordId(discordId);
    }

    public int getTotalWarn(long discordId) {
        return warnRepository.findByDiscordId(discordId).stream()
                .mapToInt(Warn::amount)
                .sum();
    }
}