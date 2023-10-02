package kr.starly.discordbot.service;

import kr.starly.discordbot.entity.ShortenLink;
import kr.starly.discordbot.repository.ShortenLinkRepository;

import java.util.List;

public record ShortenLinkService(ShortenLinkRepository shortenLinkRepository) {

    public void saveData(String originUrl, String shortenUrl) {
        ShortenLink shortenLink = new ShortenLink(originUrl, shortenUrl);
        shortenLinkRepository.put(shortenLink);
    }

    public ShortenLink getDataByShortenUrl(String shortenUrl) {
        return shortenLinkRepository.findByShortenUrl(shortenUrl);
    }

    public ShortenLink getDataByOriginUrl(String originUrl) {
        return shortenLinkRepository.findByOriginUrl(originUrl);
    }

    public List<ShortenLink> getAllData() {
        return shortenLinkRepository.findAll();
    }

    public void deleteDataByOriginUrl(String originUrl) {
        shortenLinkRepository.deleteByOriginUrl(originUrl);
    }

    public void deleteDataByShortenUrl(String shortenUrl) {
        shortenLinkRepository.deleteByShortenUrl(shortenUrl);
    }
}