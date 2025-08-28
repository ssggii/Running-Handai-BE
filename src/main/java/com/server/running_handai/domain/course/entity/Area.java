package com.server.running_handai.domain.course.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.server.running_handai.domain.course.service.KakaoMapService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Area {
    HAEUN_GWANGAN("해운/광안", List.of("해운대구", "수영구")),
    SONGJEONG_GIJANG("송정/기장", List.of("송정동", "기장군")),
    SEOMYEON_DONGNAE("서면/동래", List.of("부산진구", "동래구", "연제구")),
    WONDOSIM("원도심/영도", List.of("중구", "동구", "서구", "영도구")),
    SOUTHERN_COAST("남부해안", List.of("남구")),
    WESTERN_NAKDONGRIVER("서부/낙동강", List.of("사상구", "강서구", "사하구")),
    NORTHERN_BUSAN("북부산", List.of("금정구", "북구")),
    ETC("기타", List.of());

    private final String description;
    private final List<String> subRegions;

    /**
     * 카카오 지도 API에서 가져온 주소 정보에서 행정구역(Area)을 결정합니다.
     *
     * @param addressInfo 주소 정보 Record
     * @return Area, 없으면 Area.ETC
     */
    public static Area fromAddress(KakaoMapService.AddressInfo addressInfo) {
        // addressInfo가 null로 반환되거나 districtName이 없으면 ETC로 설정
        if (addressInfo == null || addressInfo.districtName() == null || addressInfo.districtName().isBlank()) {
            return Area.ETC;
        }

        // "해운대구 송정동"만 SONGJEONG_GIJANG으로 분류
        if (addressInfo.districtName().equals("해운대구") && addressInfo.dongName() != null && addressInfo.dongName().equals("송정동")) {
            return Area.SONGJEONG_GIJANG;
        }

        // districtName으로 Area 설정, 매칭되는 Area 없으면 ETC로 설정
        return Area.findBySubRegion(addressInfo.districtName()).orElse(Area.ETC);
    }


    /**
     * 하위 지역명(String)을 포함하는 Area enum을 찾아 반환합니다.
     *
     * @param subRegionName (ex. "해운대구", "중구")
     * @return 일치하는 Area enum을 Optional로 감싸서 반환, 없으면 Optional.empty()
     */
    public static Optional<Area> findBySubRegion(String subRegionName) {
        return Arrays.stream(Area.values())
                .filter(area -> area.getSubRegions().contains(subRegionName))
                .findFirst();
    }
}
