package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.repository.ShortenLinkRepository;

import java.util.List;

public record ShortenLinkService(ShortenLinkRepository repository) {

    public void saveData(String originUrl, String shortenUrl) {
        ShortenLink shortenLink = new ShortenLink(originUrl, shortenUrl);
        repository.put(shortenLink);
    }

    public ShortenLink getDataByShortenUrl(String shortenUrl) {
        return repository.findByShortenUrl(shortenUrl);
    }

    public ShortenLink getDataByOriginUrl(String originUrl) {
        return repository.findByOriginUrl(originUrl);
    }

    public List<ShortenLink> getAllData() {
        return repository.findAll();
    }

    public void deleteDataByOriginUrl(String originUrl) {
        repository.deleteByOriginUrl(originUrl);
    }

    public void deleteDataByShortenUrl(String shortenUrl) {
        repository.deleteByShortenUrl(shortenUrl);
    }
}