package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.WarnInfo;
import kr.starly.discordbot.repository.WarnRepository;

public record WarnService(WarnRepository warnRepository) {
    public void addWarn(WarnInfo warnInfo) {
        warnRepository.recordWarn(warnInfo);
    }

    public void removeWarn(WarnInfo warnInfo) {
        warnRepository.removeWarn(warnInfo);
    }

    public int getWarn(long discordId) {
        return warnRepository.getWarnByDiscordId(discordId);
    }
}