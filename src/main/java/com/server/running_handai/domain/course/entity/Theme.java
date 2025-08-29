package com.server.running_handai.domain.course.entity;

import java.util.Arrays;
import java.util.List;

import com.server.running_handai.domain.course.service.KakaoMapService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Theme {
    SEA("바다", List.of("해운대구", "수영구", "기장군", "남구")),
    RIVERSIDE("강변", List.of("사상구", "강서구", "사하구", "금정구", "북구")),
    MOUNTAIN("산", List.of("부산진구", "금정구", "남구")),
    DOWNTOWN("도심", List.of("부산진구", "동래구", "연제구", "중구", "동구", "서구", "영도구")),
    ETC("기타", List.of());

    private final String description;
    private final List<String> subRegions;

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 테마(Theme)을 결정합니다.
     *
     * @param addressInfo 주소 정보 Record
     * @return List<Theme>, 없으면 List.of(Theme.ETC)
     */
    public static List<Theme> fromAddress(KakaoMapService.AddressInfo addressInfo) {
        // addressInfo가 null로 반환되거나 districtName이 없으면 ETC로 설정
        if (addressInfo == null || addressInfo.districtName() == null || addressInfo.districtName().isBlank()) {
            return List.of(Theme.ETC);
        }

        // districtName으로 Theme 설정
        List<Theme> themes = Theme.findBySubRegion(addressInfo.districtName());

        // 매칭되는 Theme 없으면 ETC로 설정
        if (themes.isEmpty()) {
            return List.of(Theme.ETC);
        }

        return themes;
    }

    /**
     * 하위 지역명(String)을 포함하는 모든 Theme을 찾아 반환합니다.
     *
     * @param subRegionName (ex. "해운대구", "중구")
     * @return 일치하는 Theme enum 리스트, 없으면 빈 리스트
     */
    public static List<Theme> findBySubRegion(String subRegionName) {
        return Arrays.stream(Theme.values())
                .filter(theme -> theme.getSubRegions().contains(subRegionName))
                .toList();
    }
}
