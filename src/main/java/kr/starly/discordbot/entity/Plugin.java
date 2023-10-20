package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class Plugin {

        private String ENName;
        private String KRName;
        private String emoji;
        private String wikiUrl;
        private String iconUrl;
        private String videoUrl;
        private String gifUrl;
        private List<String> dependency;
        private List<Long> manager;
        private Long buyerRole;
        private Long threadId;
        private String version;
        private int price;

        public void updateENName(String ENName) {
            this.ENName = ENName;
        }

        public void updateKRName(String KRName) {
            this.KRName = KRName;
        }

        public void updateEmoji(String emoji) {
            this.emoji = emoji;
        }

        public void updateWikiUrl(String wikiUrl) {
            this.wikiUrl = wikiUrl;
        }

        public void updateIconUrl(String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public void updateVideoUrl(String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public void updateGifUrl(String gifUrl) {
            this.gifUrl = gifUrl;
        }

        public void updateDependency(List<String> dependency) {
            this.dependency = dependency;
        }

        public void updateManager(List<Long> manager) {
            this.manager = manager;
        }

        public void updateBuyerRole(Long buyerRole) {
            this.buyerRole = buyerRole;
        }

        public void updateThreadId(Long threadId) {
            this.threadId = threadId;
        }

        public void updateVersion(String version) {
            this.version = version;
        }

        public void updatePrice(int price) {
            this.price = price;
        }


        public void addManager(Long manager) {
            this.manager.add(manager);
        }

        public void removeManager(Long manager) {
            this.manager.remove(manager);
        }

        public void addDependency(String dependency) {
            this.dependency.add(dependency);
        }
}