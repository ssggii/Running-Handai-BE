package com.server.running_handai.domain.course.entity;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Theme {
    SEA("바다", List.of("해운대구", "수영구", "기장군", "남구")),
    RIVERSIDE("강변", List.of("사상구", "강서구", "사하구", "금정구", "북구")),
    MOUNTAIN("산", List.of("부산진구", "금정구", "남구")),
    DOWNTOWN("도심", List.of("부산진구", "동래구", "연제구", "중구", "동구", "서구", "영도구"));

    private final String description;
    private final List<String> subRegions;

    /**
     * 하위 지역명(String)을 포함하는 모든 Theme을 찾아 반환합니다.
     *
     * @param subRegionName (ex. "해운대구", "중구")
     * @return 일치하는 Theme enum을 Optional로 감싸서 반환, 없으면 Optional.empty()
     */
    public static List<Theme> findBySubRegion(String subRegionName) {
        return Arrays.stream(Theme.values())
                .filter(theme -> theme.getSubRegions().contains(subRegionName))
                .toList();
    }
}
