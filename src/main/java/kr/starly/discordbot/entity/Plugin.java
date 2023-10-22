package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bson.Document;

import java.util.List;

@Getter
@AllArgsConstructor
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
        private Integer price;
        // nullable 여부 표기 불가능 (Register System 특성)

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
        
        public Document serialize() {
            Document document = new Document();
            document.put("ENName", ENName);
            document.put("KRName", KRName);
            document.put("emoji", emoji);
            document.put("wikiUrl", wikiUrl);
            document.put("iconUrl", iconUrl);
            document.put("videoUrl", videoUrl);
            document.put("gifUrl", gifUrl);
            document.put("dependency", dependency);
            document.put("manager", manager);
            document.put("buyerRole", buyerRole);
            document.put("threadId", threadId);
            document.put("version", version);
            document.put("price", price);
            return document;
        }


        public static Plugin deserialize(Document document) {
            if (document == null) return null;

            return new Plugin(
                    document.getString("ENName"),
                    document.getString("KRName"),
                    document.getString("emoji"),
                    document.getString("wikiUrl"),
                    document.getString("iconUrl"),
                    document.getString("videoUrl"),
                    document.getString("gifUrl"),
                    document.getList("dependency", String.class),
                    document.getList("manager", Long.class),
                    document.getLong("buyerRole"),
                    document.getLong("threadId"),
                    document.getString("version"),
                    document.getInteger("price")
            );
        }
}