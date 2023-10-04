package kr.starly.discordbot.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum MCVersion {
    v1_12(Arrays.asList("1.12", "1.12.1", "1.12.2")),
    v1_13(Arrays.asList("1.13", "1.13.1", "1.13.2")),
    v1_14(Arrays.asList("1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4")),
    v1_15(Arrays.asList("1.15", "1.15.1", "1.15.2")),
    v1_16(Arrays.asList("1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5")),
    v1_17(Arrays.asList("1.17", "1.17.1")),
    v1_18(Arrays.asList("1.18", "1.18.1", "1.18.2")),
    v1_19(Arrays.asList("1.19", "1.19.1", "1.19.2", "1.19.3", "1.19.4")),
    v1_20(Arrays.asList("1.20", "1.20.1", "1.20.2"));

    private final List<String> specificVersions;
}
