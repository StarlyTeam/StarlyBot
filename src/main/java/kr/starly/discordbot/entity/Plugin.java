package kr.starly.discordbot.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
@Getter
public class Plugin {

        @NotNull String ENName;
        @NotNull String KRName;
        @NotNull String emoji;
        @NotNull String wikiUrl;
        @NotNull String iconUrl;
        @Nullable String videoUrl;
        @NotNull String gifUrl;
        @NotNull List<String> dependency;
        @NotNull List<Long> manager;
        @Nullable Long buyerRole;
        @NotNull Long threadId;
        @NotNull String version;
        @NotNull int price;

        public void updateENName(@NotNull String ENName) {
            this.ENName = ENName;
        }

        public void updateKRName(@NotNull String KRName) {
            this.KRName = KRName;
        }

        public void updateEmoji(@NotNull String emoji) {
            this.emoji = emoji;
        }

        public void updateWikiUrl(@NotNull String wikiUrl) {
            this.wikiUrl = wikiUrl;
        }

        public void updateIconUrl(@NotNull String iconUrl) {
            this.iconUrl = iconUrl;
        }

        public void updateVideoUrl(@Nullable String videoUrl) {
            this.videoUrl = videoUrl;
        }

        public void updateGifUrl(@NotNull String gifUrl) {
            this.gifUrl = gifUrl;
        }

        public void updateDependency(@NotNull List<String> dependency) {
            this.dependency = dependency;
        }

        public void updateManager(@NotNull List<Long> manager) {
            this.manager = manager;
        }

        public void updateBuyerRole(@Nullable Long buyerRole) {
            this.buyerRole = buyerRole;
        }

        public void updateThreadId(@NotNull Long threadId) {
            this.threadId = threadId;
        }

        public void updateVersion(@NotNull String version) {
            this.version = version;
        }

        public void updatePrice(@NotNull int price) {
            this.price = price;
        }


        public void addManager(@NotNull Long manager) {
            this.manager.add(manager);
        }

        public void removeManager(@NotNull Long manager) {
            this.manager.remove(manager);
        }

        public void addDependency(@NotNull String dependency) {
            this.dependency.add(dependency);
        }
}