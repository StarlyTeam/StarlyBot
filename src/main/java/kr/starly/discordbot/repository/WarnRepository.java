package kr.starly.discordbot.repository;

import kr.starly.discordbot.entity.WarnInfo;

import java.util.List;

public interface WarnRepository {

    void recordWarn(WarnInfo warnInfo);
    void removeWarn(WarnInfo warnInfo);
    int getWarnByDiscordId(long discordId);
    List<WarnInfo> findAllByDiscordId(long discordId);
}
